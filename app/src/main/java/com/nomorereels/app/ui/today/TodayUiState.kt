package com.nomorereels.app.ui.today

import com.nomorereels.app.policy.DmReelMode

/**
 * Immutable UI State for the Today (Bugün) screen following MVI / UDF principles.
 */
data class TodayUiState(
    val todayDateFormatted: String = "",
    val isServiceActive: Boolean = false,
    val protectionMasterEnabled: Boolean = true,
    val generalMode: DmReelMode = DmReelMode.BLOCK_ALL,
    val blockCount: Int = 0,
    val savedSeconds: Long = 0L,
    val savedMinutes: Long = 0L,
    val policySummary: String = "",
    val isSnoozeActive: Boolean = false,
    val snoozeRemainingMinutes: Int = 0
)
