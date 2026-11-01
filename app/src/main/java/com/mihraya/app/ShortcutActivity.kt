package com.mihraya.app

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import com.mihraya.app.widget.Destinations

/**
 * Entry point for the launcher shortcuts in res/xml/shortcuts.xml. Android starts static shortcuts with
 * FLAG_ACTIVITY_CLEAR_TASK, which would rebuild a running app, so this forwards to the existing [MainActivity].
 */
class ShortcutActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dest = intent?.getStringExtra(Destinations.EXTRA)?.takeIf { it in SHORTCUT_DESTINATIONS }
        startActivity(
            Intent.makeMainActivity(ComponentName(this, MainActivity::class.java))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(Destinations.FROM_SHORTCUT, true)
                .apply { if (dest != null) putExtra(Destinations.EXTRA, dest) },
        )
        finish()
    }

    private companion object {
        val SHORTCUT_DESTINATIONS = setOf(Destinations.QURAN, Destinations.LISTEN, Destinations.TASBIH)
    }
}
