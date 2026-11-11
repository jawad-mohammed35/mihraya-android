package com.mihraya.app

import android.app.Application
import android.content.res.Configuration
import com.mihraya.app.widget.WidgetRefresh
import com.mihraya.app.notifications.Channels

class MihrayaApp : Application() {

    lateinit var container: AppContainer
        private set

    private var nightMode = Configuration.UI_MODE_NIGHT_UNDEFINED

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val mode = newConfig.uiMode and Configuration.UI_MODE_NIGHT_MASK
        if (mode != nightMode) {
            nightMode = mode
            WidgetRefresh.refresh(this, systemOnly = true)
        }
    }

    override fun onCreate() {
        super.onCreate()
        nightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        container = AppContainer(this)
        Channels.ensure(this)
        container.start()
    }
}
