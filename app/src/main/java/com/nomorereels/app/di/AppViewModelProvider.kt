package com.nomorereels.app.di

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nomorereels.app.OutloopApplication
import com.nomorereels.app.ui.today.TodayViewModel

/**
 * Provides Factory to create ViewModel instances with dependencies from [AppContainer].
 */
object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            val app = outloopApplication()
            TodayViewModel(
                application = app,
                settingsStore = app.container.settingsStore,
                quotaStore = app.container.quotaStore
            )
        }
        initializer {
            val app = outloopApplication()
            com.nomorereels.app.ui.protection.ProtectionViewModel(
                application = app,
                settingsStore = app.container.settingsStore,
                quotaStore = app.container.quotaStore
            )
        }
        initializer {
            val app = outloopApplication()
            com.nomorereels.app.ui.insights.InsightsViewModel(
                application = app,
                settingsStore = app.container.settingsStore,
                quotaStore = app.container.quotaStore,
                heatmapStore = app.container.heatmapStore
            )
        }
        initializer {
            val app = outloopApplication()
            com.nomorereels.app.ui.settings.SettingsViewModel(
                application = app,
                settingsStore = app.container.settingsStore,
                quotaStore = app.container.quotaStore,
                heatmapStore = app.container.heatmapStore,
                donationManager = app.container.donationManager
            )
        }
    }
}

/**
 * Extension function to queries for [OutloopApplication] object and returns an instance of
 * [OutloopApplication].
 */
fun CreationExtras.outloopApplication(): OutloopApplication =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as OutloopApplication)
