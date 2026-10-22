package com.nomorereels.app.ui.settings

import com.nomorereels.app.billing.DonationItem
import com.nomorereels.app.data.MindfulBarrierType
import com.nomorereels.app.ui.theme.NmrLanguage
import com.nomorereels.app.ui.theme.NmrThemeMode

/**
 * Immutable UI State for the Settings (Ayarlar) screen following MVI / UDF principles.
 */
data class SettingsUiState(
    val language: NmrLanguage = NmrLanguage.TR,
    val themeMode: NmrThemeMode = NmrThemeMode.AMOLED,
    val mindfulGatewayEnabled: Boolean = true,
    val mindfulBarrierType: MindfulBarrierType = MindfulBarrierType.BREATH_AND_INTENT,
    val nightCompassionEnabled: Boolean = true,
    val overlayFeedbackEnabled: Boolean = true,
    val hapticFeedbackEnabled: Boolean = true,
    val bedtimeEnabled: Boolean = false,
    val bedtimeStartHour: Int = 23,
    val bedtimeStartMinute: Int = 30,
    val bedtimeEndHour: Int = 7,
    val bedtimeEndMinute: Int = 0,
    val isBatteryOptIgnored: Boolean = false,
    val isSupporter: Boolean = false,
    val supporterDonationCount: Int = 0,
    val donationItems: List<DonationItem> = emptyList()
)
