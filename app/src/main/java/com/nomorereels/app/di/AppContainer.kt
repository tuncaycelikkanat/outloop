package com.nomorereels.app.di

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.nomorereels.app.billing.DonationManager
import com.nomorereels.app.detection.DetectionLogStore
import com.nomorereels.app.data.HeatmapStore
import com.nomorereels.app.data.QuotaStore
import com.nomorereels.app.data.SettingsStore

/**
 * Dependency Injection (DI) Container interface defining all singleton dependencies.
 * Follows Google's recommended Dependency Injection Container pattern for high performance,
 * zero-reflection, and 100% testable architecture.
 */
interface AppContainer {
    val applicationContext: Context
    val settingsStore: SettingsStore
    val quotaStore: QuotaStore
    val heatmapStore: HeatmapStore
    val detectionLogStore: DetectionLogStore
    val donationManager: DonationManager
}

/**
 * Default production implementation of [AppContainer].
 */
class DefaultAppContainer(override val applicationContext: Context) : AppContainer {
    override val settingsStore: SettingsStore = SettingsStore
    override val quotaStore: QuotaStore = QuotaStore
    override val heatmapStore: HeatmapStore = HeatmapStore
    override val detectionLogStore: DetectionLogStore = DetectionLogStore
    override val donationManager: DonationManager by lazy {
        DonationManager(applicationContext)
    }

    init {
        // Initialize stores on app container startup
        settingsStore.init(applicationContext)
        quotaStore.init(applicationContext)
        heatmapStore.init(applicationContext)
    }
}
