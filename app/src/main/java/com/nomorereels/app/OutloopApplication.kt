package com.nomorereels.app

import android.app.Application
import com.nomorereels.app.di.AppContainer
import com.nomorereels.app.di.DefaultAppContainer

/**
 * Main Application class for Outloop.
 * Hosts the root [AppContainer] dependency injection container.
 */
class OutloopApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        com.nomorereels.app.data.LocalDiagnosticsStore.installCrashHandler(this)
        com.nomorereels.app.ui.MindfulActivities.init(this)
        com.nomorereels.app.ui.MindfulQuotes.init(this)
        com.nomorereels.app.ui.PracticalSuggestions.init(this)
        container = DefaultAppContainer(this)
    }
}
