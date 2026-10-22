package com.nomorereels.app.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nomorereels.app.billing.DonationItem
import com.nomorereels.app.billing.DonationManager
import com.nomorereels.app.data.AppLanguage
import com.nomorereels.app.data.AppThemeMode
import com.nomorereels.app.data.HeatmapStore
import com.nomorereels.app.data.MindfulBarrierType
import com.nomorereels.app.data.QuotaStore
import com.nomorereels.app.data.SettingsStore
import com.nomorereels.app.service.ServiceHealthMonitor
import com.nomorereels.app.ui.theme.NmrLanguage
import com.nomorereels.app.ui.theme.NmrThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

private data class BasePrefs(
    val language: NmrLanguage,
    val themeMode: NmrThemeMode,
    val mindfulGatewayEnabled: Boolean,
    val mindfulBarrierType: MindfulBarrierType,
    val nightCompassionEnabled: Boolean
)

private data class FeedbackAndBedtime(
    val overlayFeedbackEnabled: Boolean,
    val hapticFeedbackEnabled: Boolean,
    val bedtimeEnabled: Boolean,
    val bedtimeStartHour: Int,
    val bedtimeStartMinute: Int
)

private data class BedtimeEndAndSupporter(
    val bedtimeEndHour: Int,
    val bedtimeEndMinute: Int,
    val isSupporter: Boolean,
    val supporterDonationCount: Int,
    val isBatteryOptIgnored: Boolean
)

class SettingsViewModel(
    application: Application,
    private val settingsStore: SettingsStore = SettingsStore,
    private val quotaStore: QuotaStore = QuotaStore,
    private val heatmapStore: HeatmapStore = HeatmapStore,
    private val donationManager: DonationManager? = null
) : AndroidViewModel(application) {

    private val isBatteryOptIgnored = MutableStateFlow(
        ServiceHealthMonitor.isBatteryOptimizationIgnored(application)
    )

    fun refreshBatteryOptimizationStatus() {
        isBatteryOptIgnored.value = ServiceHealthMonitor.isBatteryOptimizationIgnored(getApplication())
    }

    private val basePreferencesFlow = combine(
        settingsStore.appLanguage,
        settingsStore.appThemeMode,
        settingsStore.mindfulGatewayEnabled,
        settingsStore.mindfulBarrierType,
        settingsStore.nightCompassionEnabled
    ) { lang, theme, gateway, barrier, night ->
        val uiLang = when (lang) {
            AppLanguage.TR -> NmrLanguage.TR
            AppLanguage.EN -> NmrLanguage.EN
        }
        val uiTheme = when (theme) {
            AppThemeMode.LIGHT -> NmrThemeMode.LIGHT
            AppThemeMode.DARK -> NmrThemeMode.AMOLED
            AppThemeMode.SYSTEM -> NmrThemeMode.SYSTEM
        }
        BasePrefs(uiLang, uiTheme, gateway, barrier, night)
    }

    private val feedbackAndBedtimeFlow = combine(
        settingsStore.overlayFeedbackEnabled,
        settingsStore.hapticFeedbackEnabled,
        settingsStore.bedtimeModeEnabled,
        settingsStore.bedtimeStartHour,
        settingsStore.bedtimeStartMinute
    ) { overlay, haptic, bedtime, startH, startM ->
        FeedbackAndBedtime(overlay, haptic, bedtime, startH, startM)
    }

    private val bedtimeEndAndSupporterFlow = combine(
        settingsStore.bedtimeEndHour,
        settingsStore.bedtimeEndMinute,
        settingsStore.isSupporter,
        settingsStore.supporterDonationCount,
        isBatteryOptIgnored
    ) { endH, endM, isSupporter, donationCount, batteryIgnored ->
        BedtimeEndAndSupporter(endH, endM, isSupporter, donationCount, batteryIgnored)
    }

    private val donationItemsFlow: StateFlow<List<DonationItem>> = if (donationManager != null) {
        combine(donationManager.productDetailsMap) { detailsMapArray ->
            val detailsMap = detailsMapArray[0]
            donationManager.defaultItems.map { item ->
                item.copy(productDetails = detailsMap[item.productId])
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), donationManager.defaultItems)
    } else {
        MutableStateFlow(emptyList())
    }

    val uiState: StateFlow<SettingsUiState> = combine(
        basePreferencesFlow,
        feedbackAndBedtimeFlow,
        bedtimeEndAndSupporterFlow,
        donationItemsFlow
    ) { base, fb, endSup, donationItems ->
        SettingsUiState(
            language = base.language,
            themeMode = base.themeMode,
            mindfulGatewayEnabled = base.mindfulGatewayEnabled,
            mindfulBarrierType = base.mindfulBarrierType,
            nightCompassionEnabled = base.nightCompassionEnabled,
            overlayFeedbackEnabled = fb.overlayFeedbackEnabled,
            hapticFeedbackEnabled = fb.hapticFeedbackEnabled,
            bedtimeEnabled = fb.bedtimeEnabled,
            bedtimeStartHour = fb.bedtimeStartHour,
            bedtimeStartMinute = fb.bedtimeStartMinute,
            bedtimeEndHour = endSup.bedtimeEndHour,
            bedtimeEndMinute = endSup.bedtimeEndMinute,
            isBatteryOptIgnored = endSup.isBatteryOptIgnored,
            isSupporter = endSup.isSupporter,
            supporterDonationCount = endSup.supporterDonationCount,
            donationItems = donationItems
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun setLanguage(lang: NmrLanguage) {
        settingsStore.setAppLanguage(
            getApplication(),
            if (lang == NmrLanguage.TR) AppLanguage.TR else AppLanguage.EN
        )
    }

    fun setThemeMode(theme: NmrThemeMode) {
        val mapped = when (theme) {
            NmrThemeMode.LIGHT -> AppThemeMode.LIGHT
            NmrThemeMode.AMOLED -> AppThemeMode.DARK
            NmrThemeMode.SYSTEM -> AppThemeMode.SYSTEM
        }
        settingsStore.setAppThemeMode(getApplication(), mapped)
    }

    fun setMindfulGatewayEnabled(enabled: Boolean) {
        settingsStore.setMindfulGatewayEnabled(getApplication(), enabled)
    }

    fun setMindfulBarrierType(type: MindfulBarrierType) {
        settingsStore.setMindfulBarrierType(getApplication(), type)
    }

    fun setNightCompassionEnabled(enabled: Boolean) {
        settingsStore.setNightCompassionEnabled(getApplication(), enabled)
    }

    fun setOverlayFeedbackEnabled(enabled: Boolean) {
        settingsStore.setOverlayFeedbackEnabled(getApplication(), enabled)
    }

    fun setHapticFeedbackEnabled(enabled: Boolean) {
        settingsStore.setHapticFeedbackEnabled(getApplication(), enabled)
    }

    fun setBedtimeModeEnabled(enabled: Boolean) {
        settingsStore.setBedtimeModeEnabled(getApplication(), enabled)
    }

    fun setBedtimeWindow(startH: Int, startM: Int, endH: Int, endM: Int) {
        settingsStore.setBedtimeWindow(getApplication(), startH, startM, endH, endM)
    }

    fun resetStatistics() {
        val app = getApplication<Application>()
        quotaStore.resetAllStatistics(app)
        heatmapStore.clear(app)
    }

    fun resetSupporterStatus() {
        settingsStore.resetSupporterStatus(getApplication())
    }
}
