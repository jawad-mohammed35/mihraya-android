package com.mihraya.app

import android.app.Application
import com.mihraya.app.notifications.Channels

class MihrayaApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Channels.ensure(this)
        container.start()
    }
}
