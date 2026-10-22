package com.nomorereels.app.ui.protection

import com.nomorereels.app.policy.DmReelMode

/**
 * Immutable UI State for the Protection Center (Koruma Merkezi) screen following MVI / UDF principles.
 */
data class ProtectionUiState(
    val generalMode: DmReelMode = DmReelMode.BLOCK_ALL,
    val dmMode: DmReelMode = DmReelMode.SINGLE_REEL,
    val exploreMode: DmReelMode = DmReelMode.FOLLOW_GLOBAL,
    val feedMode: DmReelMode = DmReelMode.BLOCK_ALL,
    val profileMode: DmReelMode = DmReelMode.FOLLOW_GLOBAL,
    val reelsTabMode: DmReelMode = DmReelMode.BLOCK_ALL,
    val dailyCountLimit: Int = 5,
    val dailyTimeLimitMinutes: Int = 15,
    val isCountLimitEnabled: Boolean = false,
    val isTimeLimitEnabled: Boolean = false
)
