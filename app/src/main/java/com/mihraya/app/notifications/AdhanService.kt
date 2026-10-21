package com.mihraya.app.notifications

import android.app.PendingIntent
import android.app.Service
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.mihraya.app.R
import com.mihraya.app.appContainer
import com.mihraya.app.data.prayer.Salah
import com.mihraya.app.data.settings.AdhanSound
import com.mihraya.app.data.settings.AlertPreferences
import com.mihraya.app.data.settings.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.time.Instant

class AdhanService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var player: MediaPlayer? = null
    private var focusRequest: AudioFocusRequest? = null
    private var notificationId = 0
    private var salah: Salah? = null
    private var at: Instant = Instant.now()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                finish()
                return START_NOT_STICKY
            }
            ACTION_PLAY -> {
                val prayer = intent.getStringExtra(EXTRA_SALAH)?.let { runCatching { Salah.valueOf(it) }.getOrNull() }
                    ?: Salah.Dhuhr
                startAdhan(prayer, Instant.ofEpochMilli(intent.getLongExtra(EXTRA_AT, System.currentTimeMillis())))
            }
            else -> stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun startAdhan(prayer: Salah, time: Instant) {
        releasePlayer()
        salah = prayer
        at = time
        notificationId = PrayerNotifier.prayerId(prayer)
        val settings = appContainer.settings.state.value ?: AppSettings()
        val stop = PendingIntent.getService(
            this,
            REQUEST_STOP,
            Intent(this, AdhanService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = PrayerNotifier.prayerNotification(
            context = this,
            settings = settings,
            salah = prayer,
            at = time,
            channel = Channels.ADHAN,
            ongoingAction = NotificationCompat.Action(0, "Stop Adhan", stop),
            deleteIntent = stop,
        )
        ServiceCompat.startForeground(
            this,
            notificationId,
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0,
        )
        appContainer.adhanPlaying.value = true
        scope.launch {
            val alerts = appContainer.settings.current().alerts
            play(soundUri(this@AdhanService, alerts))
        }
    }

    private fun play(uri: Uri) {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val audio = getSystemService(AudioManager::class.java)
        focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(attributes)
            .setOnAudioFocusChangeListener { }
            .build()
            .also { audio?.requestAudioFocus(it) }

        val mp = MediaPlayer()
        player = mp
        try {
            mp.setAudioAttributes(attributes)
            mp.setDataSource(this, uri)
            mp.setOnPreparedListener { it.start() }
            mp.setOnCompletionListener { finish() }
            mp.setOnErrorListener { _, _, _ ->
                if (uri != builtInUri(this, R.raw.adhan_azeez)) {
                    releasePlayer()
                    play(builtInUri(this, R.raw.adhan_azeez))
                } else {
                    finish()
                }
                true
            }
            mp.prepareAsync()
        } catch (e: Exception) {
            releasePlayer()
            if (uri != builtInUri(this, R.raw.adhan_azeez)) play(builtInUri(this, R.raw.adhan_azeez)) else finish()
        }
    }

    private fun finish() {
        releasePlayer()
        appContainer.adhanPlaying.value = false
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH)
        val prayer = salah
        val settings = appContainer.settings.state.value
        if (prayer != null && settings != null) {
            PrayerNotifier.post(
                this,
                PrayerNotifier.prayerNotification(this, settings, prayer, at, Channels.SILENT),
                notificationId,
            )
        }
        stopSelf()
    }

    private fun releasePlayer() {
        player?.run {
            runCatching { if (isPlaying) stop() }
            release()
        }
        player = null
        focusRequest?.let { getSystemService(AudioManager::class.java)?.abandonAudioFocusRequest(it) }
        focusRequest = null
    }

    override fun onDestroy() {
        releasePlayer()
        appContainer.adhanPlaying.value = false
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val ACTION_PLAY = "com.mihraya.app.adhan.PLAY"
        private const val ACTION_STOP = "com.mihraya.app.adhan.STOP"
        private const val EXTRA_SALAH = "salah"
        private const val EXTRA_AT = "at"
        private const val REQUEST_STOP = 9001

        /** Returns false when the system refuses to start the service, so callers can fall back to a silent alert. */
        fun start(context: Context, salah: Salah, at: Instant): Boolean = try {
            ContextCompat.startForegroundService(
                context,
                Intent(context, AdhanService::class.java)
                    .setAction(ACTION_PLAY)
                    .putExtra(EXTRA_SALAH, salah.name)
                    .putExtra(EXTRA_AT, at.toEpochMilli()),
            )
            true
        } catch (e: IllegalStateException) {
            false
        } catch (e: SecurityException) {
            false
        }

        fun soundUri(context: Context, alerts: AlertPreferences): Uri = when (alerts.adhanSound) {
            AdhanSound.Azeez -> builtInUri(context, R.raw.adhan_azeez)
            AdhanSound.Mahfoudou -> builtInUri(context, R.raw.adhan_mahfoudou)
            AdhanSound.Custom -> alerts.customSoundUri?.let(Uri::parse) ?: builtInUri(context, R.raw.adhan_azeez)
        }

        fun builtInUri(context: Context, resId: Int): Uri =
            Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/$resId")
    }
}
