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
    val persistedBlockCount: Int,
    val todayReelCount: Int,
    val todayTimeSpentSeconds: Long,
    val todayGatewayDismiss: Int,
    val totalGatewayDismiss: Int
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
        quotaStore.todayBlockCount,
        quotaStore.todayReelCount,
        quotaStore.todayTimeSpentSeconds,
        quotaStore.todayGatewayDismissCount,
        quotaStore.totalGatewayDismissCount
    ) { persisted, reelCount, timeSpent, todayDismiss, totalDismiss ->
        InsightsCounts(persisted, reelCount, timeSpent, todayDismiss, totalDismiss.toInt())
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
        combine(quotaStore.dailyHistory, quotaStore.dailyBlocksHistory) { h, b -> h to b }
    ) { generalMode, counts, quotas, matrix, (dailyHistory, blocksHistory) ->
        val totalBlock = counts.persistedBlockCount
        val minutesSpent = (counts.todayTimeSpentSeconds / 60L).toInt()
        val todaySavedMin = quotaStore.estimateSavedSecondsForBlocks(totalBlock) / 60L

        val todayDate = java.time.LocalDate.now()
        val todayStr = todayDate.toString()
        val weekBlocks = (0..6).sumOf { offset ->
            val d = todayDate.minusDays(offset.toLong()).toString()
            if (d == todayStr) maxOf(blocksHistory[d] ?: 0, totalBlock) else (blocksHistory[d] ?: 0)
        }
        val monthBlocks = (0..29).sumOf { offset ->
            val d = todayDate.minusDays(offset.toLong()).toString()
            if (d == todayStr) maxOf(blocksHistory[d] ?: 0, totalBlock) else (blocksHistory[d] ?: 0)
        }
        val weeklySavedMin = maxOf(todaySavedMin, quotaStore.estimateSavedSecondsForBlocks(weekBlocks) / 60L)
        val monthlySavedMin = maxOf(weeklySavedMin, quotaStore.estimateSavedSecondsForBlocks(monthBlocks) / 60L)

        InsightsUiState(
            generalMode = generalMode,
            blockCount = totalBlock,
            todaySavedMinutes = todaySavedMin,
            weeklySavedMinutes = weeklySavedMin,
            monthlySavedMinutes = monthlySavedMin,
            todayGatewayDismissCount = counts.todayGatewayDismiss,
            totalGatewayDismissCount = maxOf(counts.totalGatewayDismiss, counts.todayGatewayDismiss),
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
