package com.nomorereels.app.ui.protection

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nomorereels.app.data.QuotaStore
import com.nomorereels.app.data.SettingsStore
import com.nomorereels.app.policy.DmReelMode
import com.nomorereels.app.policy.ProtectionLevel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

private data class RegionModes(
    val dmMode: DmReelMode,
    val exploreMode: DmReelMode,
    val feedMode: DmReelMode,
    val profileMode: DmReelMode,
    val reelsTabMode: DmReelMode
)

private data class QuotaLimits(
    val dailyCountLimit: Int,
    val dailyTimeLimitMinutes: Int,
    val isCountEnabled: Boolean,
    val isTimeEnabled: Boolean
)

class ProtectionViewModel(
    application: Application,
    private val settingsStore: SettingsStore = SettingsStore,
    private val quotaStore: QuotaStore = QuotaStore
) : AndroidViewModel(application) {

    private val regionModesFlow = combine(
        settingsStore.dmReelMode,
        settingsStore.exploreReelMode,
        settingsStore.feedReelMode,
        settingsStore.profileReelMode,
        settingsStore.reelsTabMode
    ) { dm, explore, feed, profile, reelsTab ->
        RegionModes(dm, explore, feed, profile, reelsTab)
    }

    private val quotaLimitsFlow = combine(
        quotaStore.dailyCountLimit,
        quotaStore.dailyTimeLimitMinutes,
        quotaStore.dailyCountLimitEnabled,
        quotaStore.dailyTimeLimitEnabled
    ) { count, time, countEnabled, timeEnabled ->
        QuotaLimits(count, time, countEnabled, timeEnabled)
    }

    val uiState: StateFlow<ProtectionUiState> = combine(
        settingsStore.generalMode,
        regionModesFlow,
        quotaLimitsFlow
    ) { generalMode, regions, quotas ->
        ProtectionUiState(
            generalMode = generalMode,
            dmMode = regions.dmMode,
            exploreMode = regions.exploreMode,
            feedMode = regions.feedMode,
            profileMode = regions.profileMode,
            reelsTabMode = regions.reelsTabMode,
            dailyCountLimit = quotas.dailyCountLimit,
            dailyTimeLimitMinutes = quotas.dailyTimeLimitMinutes,
            isCountLimitEnabled = quotas.isCountEnabled,
            isTimeLimitEnabled = quotas.isTimeEnabled
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ProtectionUiState()
    )

    fun setGeneralMode(newMode: DmReelMode) {
        val app = getApplication<Application>()
        settingsStore.applyGeneralModeToAllRegions(app, newMode)
        when (newMode) {
            DmReelMode.COUNT_QUOTA -> {
                quotaStore.setDailyCountLimitEnabled(app, true)
                quotaStore.setDailyTimeLimitEnabled(app, false)
            }
            DmReelMode.TIME_QUOTA -> {
                quotaStore.setDailyTimeLimitEnabled(app, true)
                quotaStore.setDailyCountLimitEnabled(app, false)
            }
            else -> {
                quotaStore.setDailyCountLimitEnabled(app, false)
                quotaStore.setDailyTimeLimitEnabled(app, false)
            }
        }
    }

    fun applyProtectionProfile(level: ProtectionLevel) {
        settingsStore.applyProtectionLevel(getApplication(), level)
    }

    fun setRegionMode(regionKey: String, mode: DmReelMode) {
        val app = getApplication<Application>()
        when (regionKey) {
            "DM" -> settingsStore.setDmReelMode(app, mode)
            "EXPLORE" -> settingsStore.setExploreReelMode(app, mode)
            "FEED" -> settingsStore.setFeedReelMode(app, mode)
            "PROFILE" -> settingsStore.setProfileReelMode(app, mode)
            "REELS_TAB" -> settingsStore.setReelsTabMode(app, mode)
        }
    }

    fun setDailyCountLimit(limit: Int) {
        val app = getApplication<Application>()
        quotaStore.setDailyCountLimit(app, limit)
        quotaStore.setDailyCountLimitEnabled(app, true)
    }

    fun setDailyTimeLimitMinutes(minutes: Int) {
        val app = getApplication<Application>()
        quotaStore.setDailyTimeLimitMinutes(app, minutes)
        quotaStore.setDailyTimeLimitEnabled(app, true)
    }
}
