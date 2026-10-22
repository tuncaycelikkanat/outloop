package com.nomorereels.app.ui.today

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nomorereels.app.data.AppLanguage
import com.nomorereels.app.data.QuotaStore
import com.nomorereels.app.data.SettingsStore
import com.nomorereels.app.policy.PolicySummaryHelper
import com.nomorereels.app.service.ReelsAccessibilityService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class TodaySecondaryStats(
    val inMemoryCount: Int,
    val persistedCount: Int,
    val snoozeEndTime: Long,
    val language: AppLanguage
)

class TodayViewModel(
    application: Application,
    private val settingsStore: SettingsStore = SettingsStore,
    private val quotaStore: QuotaStore = QuotaStore
) : AndroidViewModel(application) {

    private val secondaryStatsFlow = combine(
        ReelsAccessibilityService.actionExecutor.blockCount,
        quotaStore.todayBlockCount,
        settingsStore.snoozeEndTimeMs,
        settingsStore.appLanguage
    ) { inMemory, persisted, snoozeEnd, lang ->
        TodaySecondaryStats(inMemory, persisted, snoozeEnd, lang)
    }

    val uiState: StateFlow<TodayUiState> = combine(
        ReelsAccessibilityService.isServiceActive,
        settingsStore.protectionMasterEnabled,
        settingsStore.generalMode,
        secondaryStatsFlow
    ) { isServiceActive, masterEnabled, generalMode, stats ->
        val totalBlockCount = maxOf(stats.inMemoryCount, stats.persistedCount)
        val savedSec = totalBlockCount * 45L
        val savedMin = savedSec / 60L

        val isEnglish = stats.language == AppLanguage.EN
        val nowMs = System.currentTimeMillis()
        val isSnooze = nowMs < stats.snoozeEndTime
        val snoozeRemainingMin = if (isSnooze) {
            (((stats.snoozeEndTime - nowMs) / 60000L) + 1).coerceAtLeast(1).toInt()
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
            dmMode = settingsStore.dmReelMode.value,
            exploreMode = settingsStore.exploreReelMode.value,
            feedMode = settingsStore.feedReelMode.value,
            profileMode = settingsStore.profileReelMode.value,
            reelsTabMode = settingsStore.reelsTabMode.value,
            isBedtimeActive = settingsStore.isBedtimeActiveNow(),
            isSnoozeActive = isSnooze,
            snoozeRemainingMinutes = snoozeRemainingMin,
            dailyCountLimit = quotaStore.dailyCountLimit.value,
            dailyTimeLimitMinutes = quotaStore.dailyTimeLimitMinutes.value,
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
