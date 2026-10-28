package com.nomorereels.app.ui.today

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nomorereels.app.data.AppLanguage
import com.nomorereels.app.data.QuotaStore
import com.nomorereels.app.data.SettingsStore
import com.nomorereels.app.policy.DmReelMode
import com.nomorereels.app.policy.PolicySummaryHelper
import com.nomorereels.app.service.ReelsAccessibilityService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class TodaySecondaryStats(
    val persistedCount: Int,
    val snoozeEndTime: Long,
    val language: AppLanguage,
    val tickMs: Long
)

private data class TodayRegionModes(
    val dmMode: DmReelMode,
    val exploreMode: DmReelMode,
    val feedMode: DmReelMode,
    val profileMode: DmReelMode,
    val reelsTabMode: DmReelMode
)

private data class TodayQuotaAndBedtime(
    val dailyCountLimit: Int,
    val dailyTimeLimitMinutes: Int,
    val bedtimeEnabled: Boolean,
    val bedtimeStartHour: Int,
    val bedtimeStartMinute: Int,
    val bedtimeEndHour: Int,
    val bedtimeEndMinute: Int
)

class TodayViewModel(
    application: Application,
    private val settingsStore: SettingsStore = SettingsStore,
    private val quotaStore: QuotaStore = QuotaStore
) : AndroidViewModel(application) {

    private val tickerFlow = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(15_000L)
        }
    }

    private val secondaryStatsFlow = combine(
        quotaStore.todayBlockCount,
        settingsStore.snoozeEndTimeMs,
        settingsStore.appLanguage,
        tickerFlow
    ) { persisted, snoozeEnd, lang, tickMs ->
        TodaySecondaryStats(persisted, snoozeEnd, lang, tickMs)
    }

    private val regionModesFlow = combine(
        settingsStore.dmReelMode,
        settingsStore.exploreReelMode,
        settingsStore.feedReelMode,
        settingsStore.profileReelMode,
        settingsStore.reelsTabMode
    ) { dm, explore, feed, profile, reelsTab ->
        TodayRegionModes(dm, explore, feed, profile, reelsTab)
    }

    private val quotaAndBedtimeFlow = combine(
        quotaStore.dailyCountLimit,
        quotaStore.dailyTimeLimitMinutes,
        settingsStore.bedtimeModeEnabled,
        settingsStore.bedtimeStartHour,
        settingsStore.bedtimeEndHour
    ) { countLimit, timeLimit, bedtimeEnabled, startH, endH ->
        TodayQuotaAndBedtime(
            dailyCountLimit = countLimit,
            dailyTimeLimitMinutes = timeLimit,
            bedtimeEnabled = bedtimeEnabled,
            bedtimeStartHour = startH,
            bedtimeStartMinute = settingsStore.bedtimeStartMinute.value,
            bedtimeEndHour = endH,
            bedtimeEndMinute = settingsStore.bedtimeEndMinute.value
        )
    }

    val uiState: StateFlow<TodayUiState> = combine(
        ReelsAccessibilityService.isServiceActive,
        settingsStore.protectionMasterEnabled,
        settingsStore.generalMode,
        secondaryStatsFlow,
        combine(regionModesFlow, quotaAndBedtimeFlow) { r, q -> r to q }
    ) { isServiceActive, masterEnabled, generalMode, stats, (regions, quotaBedtime) ->
        val totalBlockCount = stats.persistedCount
        val savedSec = quotaStore.estimateSavedSecondsForBlocks(totalBlockCount)
        val savedMin = savedSec / 60L

        val isEnglish = stats.language == AppLanguage.EN
        val nowMs = System.currentTimeMillis()
        if (stats.snoozeEndTime in 1..nowMs) {
            settingsStore.checkSnoozeExpiration(getApplication(), nowMs)
        }
        val isSnooze = nowMs < stats.snoozeEndTime
        val snoozeRemainingMin = if (isSnooze) {
            (((stats.snoozeEndTime - nowMs) + 59_999L) / 60_000L).coerceAtLeast(1L).toInt()
        } else 0

        val formattedDate = try {
            val now = LocalDate.now()
            val formatter = DateTimeFormatter.ofPattern(
                "EEEE, d MMMM",
                if (isEnglish) Locale.ENGLISH else Locale("tr")
            )
            now.format(formatter).uppercase()
        } catch (_: Exception) {
            if (isEnglish) "TODAY" else "BUGÜN"
        }

        val policySummarySentence = PolicySummaryHelper.getSummarySentence(
            generalMode = generalMode,
            dmMode = regions.dmMode,
            exploreMode = regions.exploreMode,
            feedMode = regions.feedMode,
            profileMode = regions.profileMode,
            reelsTabMode = regions.reelsTabMode,
            isBedtimeActive = settingsStore.isBedtimeActiveNow(),
            isSnoozeActive = isSnooze,
            snoozeRemainingMinutes = snoozeRemainingMin,
            dailyCountLimit = quotaBedtime.dailyCountLimit,
            dailyTimeLimitMinutes = quotaBedtime.dailyTimeLimitMinutes,
            isEnglish = isEnglish,
            isProtectionEnabled = masterEnabled
        )

        TodayUiState(
            todayDateFormatted = formattedDate,
            isServiceActive = isServiceActive,
            protectionMasterEnabled = masterEnabled,
            generalMode = generalMode,
            blockCount = totalBlockCount,
            savedSeconds = savedSec,
            savedMinutes = savedMin,
            policySummary = policySummarySentence,
            isSnoozeActive = isSnooze,
            snoozeRemainingMinutes = snoozeRemainingMin
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TodayUiState()
    )

    fun toggleProtectionMaster(enabled: Boolean) {
        settingsStore.setProtectionMasterEnabled(getApplication(), enabled)
    }

    fun startSnooze(durationMinutes: Int, microPlan: String) {
        settingsStore.startSnooze(
            context = getApplication(),
            durationMinutes = durationMinutes,
            microPlan = microPlan
        )
    }

    fun stopSnooze() {
        settingsStore.stopSnooze(getApplication())
    }
}
