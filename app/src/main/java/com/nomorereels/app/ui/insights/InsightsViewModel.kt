package com.nomorereels.app.ui.insights

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nomorereels.app.data.HeatmapStore
import com.nomorereels.app.data.QuotaStore
import com.nomorereels.app.data.SettingsStore
import com.nomorereels.app.service.ReelsAccessibilityService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

private data class InsightsCounts(
    val inMemoryBlockCount: Int,
    val persistedBlockCount: Int,
    val todayReelCount: Int,
    val todayTimeSpentSeconds: Long
)

private data class InsightsQuotas(
    val dailyCountLimit: Int,
    val dailyTimeLimitMinutes: Int,
    val isSupporter: Boolean
)

class InsightsViewModel(
    application: Application,
    private val settingsStore: SettingsStore = SettingsStore,
    private val quotaStore: QuotaStore = QuotaStore,
    private val heatmapStore: HeatmapStore = HeatmapStore
) : AndroidViewModel(application) {

    private val countsFlow = combine(
        ReelsAccessibilityService.actionExecutor.blockCount,
        quotaStore.todayBlockCount,
        quotaStore.todayReelCount,
        quotaStore.todayTimeSpentSeconds
    ) { inMemory, persisted, reelCount, timeSpent ->
        InsightsCounts(inMemory, persisted, reelCount, timeSpent)
    }

    private val quotasFlow = combine(
        quotaStore.dailyCountLimit,
        quotaStore.dailyTimeLimitMinutes,
        settingsStore.isSupporter
    ) { countLimit, timeLimit, isSupporter ->
        InsightsQuotas(countLimit, timeLimit, isSupporter)
    }

    val uiState: StateFlow<InsightsUiState> = combine(
        settingsStore.generalMode,
        countsFlow,
        quotasFlow,
        heatmapStore.matrix,
        quotaStore.dailyHistory
    ) { generalMode, counts, quotas, matrix, dailyHistory ->
        val totalBlock = maxOf(counts.inMemoryBlockCount, counts.persistedBlockCount)
        val minutesSpent = (counts.todayTimeSpentSeconds / 60L).toInt()
        InsightsUiState(
            generalMode = generalMode,
            blockCount = totalBlock,
            todayReelCount = counts.todayReelCount,
            dailyCountLimit = quotas.dailyCountLimit,
            todayMinutesSpent = minutesSpent,
            dailyTimeLimitMinutes = quotas.dailyTimeLimitMinutes,
            isSupporter = quotas.isSupporter,
            matrix = matrix.toList(),
            dailyHistory = dailyHistory
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = InsightsUiState()
    )
}
