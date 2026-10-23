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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.time.Instant

class AdhanService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var player: MediaPlayer? = null
    private var sequence: Job? = null
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
        sequence?.cancel()
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
        sequence = scope.launch {
            val alerts = appContainer.settings.current().alerts
            val dua = if (prayer in alerts.duaAfterAdhan) async { appContainer.duaAudio.ensure() } else null
            val adhanDone = CompletableDeferred<Unit>()
            play(soundUri(this@AdhanService, alerts, prayer)) { adhanDone.complete(Unit) }
            adhanDone.await()
            val file = dua?.await()
            if (file != null) play(Uri.fromFile(file), fallback = false) else finish()
        }
    }

    private fun play(uri: Uri, fallback: Boolean = true, onComplete: () -> Unit = ::finish) {
        releasePlayer()
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
            mp.setOnCompletionListener { onComplete() }
            mp.setOnErrorListener { _, _, _ ->
                fail(uri, fallback, onComplete)
                true
            }
            mp.prepareAsync()
        } catch (e: Exception) {
            fail(uri, fallback, onComplete)
        }
    }

    private fun fail(uri: Uri, fallback: Boolean, onComplete: () -> Unit) {
        releasePlayer()
        val default = builtInUri(this, R.raw.adhan_makkah)
        if (fallback && uri != default) play(default, fallback = false, onComplete) else onComplete()
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

        fun soundUri(context: Context, alerts: AlertPreferences, salah: Salah): Uri =
            soundUri(context, alerts.soundFor(salah), alerts.customSoundUri)

        fun soundUri(context: Context, sound: AdhanSound, customUri: String?): Uri = when (sound) {
            AdhanSound.Custom -> customUri?.let(Uri::parse) ?: builtInUri(context, R.raw.adhan_makkah)
            else -> builtInUri(context, rawRes(sound))
        }

        private fun rawRes(sound: AdhanSound): Int = when (sound) {
            AdhanSound.Makkah, AdhanSound.Custom -> R.raw.adhan_makkah
            AdhanSound.KualaLumpur -> R.raw.adhan_klcc
            AdhanSound.Ahmedabad -> R.raw.adhan_mirza
            AdhanSound.Javed -> R.raw.adhan_javed
            AdhanSound.Shalqar -> R.raw.adhan_shalqar
            AdhanSound.Azeez -> R.raw.adhan_azeez
            AdhanSound.Mahfoudou -> R.raw.adhan_mahfoudou
            AdhanSound.MalmoFajr -> R.raw.adhan_fajr_malmo
            AdhanSound.FouadFajr -> R.raw.adhan_fajr_fouad
        }

        fun builtInUri(context: Context, resId: Int): Uri =
            Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/$resId")
    }
}
