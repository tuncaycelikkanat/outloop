package com.nomorereels.app.ui.insights

import com.nomorereels.app.policy.DmReelMode

/**
 * Immutable UI State for the Insights (İçgörü) screen following MVI / UDF principles.
 */
data class InsightsUiState(
    val generalMode: DmReelMode = DmReelMode.BLOCK_ALL,
    val blockCount: Int = 0,
    val todaySavedMinutes: Long = 0L,
    val weeklySavedMinutes: Long = 0L,
    val monthlySavedMinutes: Long = 0L,
    val todayGatewayDismissCount: Int = 0,
    val totalGatewayDismissCount: Int = 0,
    val todayReelCount: Int = 0,
    val dailyCountLimit: Int = 10,
    val todayMinutesSpent: Int = 0,
    val dailyTimeLimitMinutes: Int = 15,
    val isSupporter: Boolean = false,
    val matrix: List<Int> = emptyList(),
    val dailyHistory: Map<String, Long> = emptyMap()
)
