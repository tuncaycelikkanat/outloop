package com.nomorereels.app.ui.settings

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryAlert
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nomorereels.app.policy.PolicyDecision
import com.nomorereels.app.data.HeatmapStore
import com.nomorereels.app.data.SettingsBackupHelper
import com.nomorereels.app.data.SettingsStore
import com.nomorereels.app.service.ReelsAccessibilityService
import com.nomorereels.app.service.ServiceHealthMonitor
import com.nomorereels.app.ui.SupportSettingCard
import com.nomorereels.app.ui.i18n.LocalAppStrings
import com.nomorereels.app.ui.theme.NmrChip
import com.nomorereels.app.ui.theme.NmrHairlineDivider
import com.nomorereels.app.ui.theme.NmrIcons
import com.nomorereels.app.ui.theme.NmrLanguage
import com.nomorereels.app.ui.theme.NmrSectionHeader
import com.nomorereels.app.ui.theme.NmrSegmentedControl
import com.nomorereels.app.ui.theme.NmrStatusChip
import com.nomorereels.app.ui.theme.NmrTheme
import com.nomorereels.app.ui.theme.NmrThemeMode
import com.nomorereels.app.ui.theme.StatusGreen
import com.nomorereels.app.ui.theme.StatusOrange

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onOpenSupportSheet: () -> Unit,
    onOpenBedtimePicker: () -> Unit,
    onPreviewGateway: () -> Unit,
    onOpenWalkthrough: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshBatteryOptimizationStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val context = LocalContext.current

    SettingsScreenContent(
        uiState = uiState,
        onRequestIgnoreBatteryOptimization = {
            ServiceHealthMonitor.requestIgnoreBatteryOptimization(context)
        },
        onLanguageChange = { viewModel.setLanguage(it) },
        onThemeChange = { viewModel.setThemeMode(it) },
        onMindfulGatewayToggle = { viewModel.setMindfulGatewayEnabled(it) },
        onOverlayFeedbackToggle = { viewModel.setOverlayFeedbackEnabled(it) },
        onHapticFeedbackToggle = { viewModel.setHapticFeedbackEnabled(it) },
        onPreviewGateway = onPreviewGateway,
        onBedtimeToggle = { viewModel.setBedtimeModeEnabled(it) },
        onOpenBedtimePicker = onOpenBedtimePicker,
        onResetStatistics = { viewModel.resetStatistics() },
        onResetSupporter = { viewModel.resetSupporterStatus() },
        onOpenSupportSheet = onOpenSupportSheet,
        onOpenWalkthrough = onOpenWalkthrough,
        modifier = modifier
    )
}

@Composable
fun SettingsScreenContent(
    uiState: SettingsUiState,
    onRequestIgnoreBatteryOptimization: () -> Unit,
    onLanguageChange: (NmrLanguage) -> Unit,
    onThemeChange: (NmrThemeMode) -> Unit,
    onMindfulGatewayToggle: (Boolean) -> Unit,
    onOverlayFeedbackToggle: (Boolean) -> Unit,
    onHapticFeedbackToggle: (Boolean) -> Unit,
    onPreviewGateway: () -> Unit,
    onBedtimeToggle: (Boolean) -> Unit,
    onOpenBedtimePicker: () -> Unit,
    onResetStatistics: () -> Unit,
    onResetSupporter: (() -> Unit)? = null,
    onOpenSupportSheet: () -> Unit = {},
    onOpenWalkthrough: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val bedtimeSuggestion = remember { HeatmapStore.analyzeForBedtimeSuggestion() }

    val haptic = LocalHapticFeedback.current
    var showResetStatsConfirmDialog by remember { mutableStateOf(false) }

    val language = uiState.language
    val themeMode = uiState.themeMode
    val mindfulGatewayEnabled = uiState.mindfulGatewayEnabled
    val overlayFeedbackEnabled = uiState.overlayFeedbackEnabled
    val hapticFeedbackEnabled = uiState.hapticFeedbackEnabled
    val bedtimeEnabled = uiState.bedtimeEnabled
    val bedtimeStartHour = uiState.bedtimeStartHour
    val bedtimeStartMinute = uiState.bedtimeStartMinute
    val bedtimeEndHour = uiState.bedtimeEndHour
    val bedtimeEndMinute = uiState.bedtimeEndMinute
    val isBatteryOptIgnored = uiState.isBatteryOptIgnored
    val isSupporter = uiState.isSupporter
    val supporterDonationCount = uiState.supporterDonationCount

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            val success = SettingsBackupHelper.exportToUri(context, uri)
            Toast.makeText(
                context,
                if (success) strings.backupExportSuccess else strings.backupExportFail,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val success = SettingsBackupHelper.importFromUri(context, uri)
            Toast.makeText(
                context,
                if (success) strings.backupImportSuccess else strings.backupImportFail,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val switchColors = SwitchDefaults.colors(
        checkedThumbColor = Color.White,
        checkedTrackColor = MaterialTheme.colorScheme.primary,
        uncheckedThumbColor = Color.White,
        uncheckedTrackColor = MaterialTheme.colorScheme.outline
    )

    if (showResetStatsConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetStatsConfirmDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = strings.resetStatsDialogTitle,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            },
            text = {
                Text(
                    text = strings.resetStatsDialogMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onResetStatistics()
                        showResetStatsConfirmDialog = false
                        Toast.makeText(context, strings.resetStatsSuccess, Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text(
                        text = strings.resetStatsConfirm,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetStatsConfirmDialog = false }) {
                    Text(
                        text = strings.resetStatsCancel,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = NmrTheme.dimens.screenHorizontalMargin),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = strings.tabSettings,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    letterSpacing = (-0.5).sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // BÖLÜM: GÖRÜNÜM
        item {
            NmrSectionHeader(title = strings.appearanceHeader)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.languageLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                NmrSegmentedControl(
                    options = listOf(NmrLanguage.TR, NmrLanguage.EN),
                    selectedOption = language,
                    onOptionSelected = onLanguageChange,
                    labelProvider = { it.code }
                )
            }
            NmrHairlineDivider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.themeLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                NmrSegmentedControl(
                    options = listOf(NmrThemeMode.LIGHT, NmrThemeMode.SYSTEM, NmrThemeMode.AMOLED),
                    selectedOption = themeMode,
                    onOptionSelected = onThemeChange,
                    labelProvider = { strings.themeLabel(it) }
                )
            }
        }

        // BÖLÜM: FARKINDALIK KAPISI (MINDFUL GATEWAY)
        item {
            NmrSectionHeader(title = strings.mindfulGatewayHeader)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = NmrIcons.Bilincli,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(NmrTheme.dimens.iconSize)
                    )
                    Column {
                        Text(
                            text = strings.mindfulGatewayTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = strings.mindfulGatewaySub,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = mindfulGatewayEnabled,
                    onCheckedChange = onMindfulGatewayToggle,
                    colors = switchColors
                )
            }

            AnimatedVisibility(
                visible = mindfulGatewayEnabled,
                enter = fadeIn(animationSpec = tween(200, easing = LinearOutSlowInEasing)) +
                    expandVertically(
                        animationSpec = spring(
                            dampingRatio = 0.85f,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        expandFrom = Alignment.Top
                    ),
                exit = fadeOut(animationSpec = tween(140, easing = FastOutLinearInEasing)) +
                    shrinkVertically(
                        animationSpec = spring(
                            dampingRatio = 0.85f,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        shrinkTowards = Alignment.Top
                    )
            ) {
                Column {
                    Spacer(modifier = Modifier.height(10.dp))

                    NmrChip(
                        label = strings.previewGatewayButton,
                        selected = true,
                        onClick = onPreviewGateway
                    )
                }
            }

            NmrHairlineDivider()
        }

        // BÖLÜM: KORUMA
        item {
            NmrSectionHeader(title = strings.protectionHeader)

            if (bedtimeSuggestion != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    shape = RoundedCornerShape(NmrTheme.dimens.cardRadius),
                    color = NmrTheme.colors.selectedSurface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                    tonalElevation = 0.dp
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = strings.habitAnalysisTitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = strings.habitAnalysisText(bedtimeSuggestion.suggestedStartHour),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        NmrChip(
                            label = strings.applyHabitButton(bedtimeSuggestion.suggestedStartHour),
                            selected = false,
                            onClick = {
                                SettingsStore.setBedtimeWindow(context, bedtimeSuggestion.suggestedStartHour, 0, 7, 0)
                                SettingsStore.setBedtimeModeEnabled(context, true)
                            }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp)
                    .clickable(onClick = onOpenBedtimePicker),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = NmrIcons.GeceOdak,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(NmrTheme.dimens.iconSize)
                    )
                    Column {
                        Text(
                            text = strings.bedtimeTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        val timeStr = String.format("%02d:%02d – %02d:%02d", bedtimeStartHour, bedtimeStartMinute, bedtimeEndHour, bedtimeEndMinute)
                        Text(
                            text = timeStr,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = bedtimeEnabled,
                    onCheckedChange = onBedtimeToggle,
                    colors = switchColors
                )
            }
            NmrHairlineDivider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp)
                    .clickable(onClick = onRequestIgnoreBatteryOptimization),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (isBatteryOptIgnored) Icons.Outlined.BatteryChargingFull else Icons.Outlined.BatteryAlert,
                        contentDescription = null,
                        tint = if (isBatteryOptIgnored) MaterialTheme.colorScheme.onBackground else StatusOrange,
                        modifier = Modifier.size(NmrTheme.dimens.iconSize)
                    )
                    Column {
                        Text(
                            text = strings.batteryOptTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = if (isBatteryOptIgnored) strings.batteryOptActiveSub else strings.batteryOptInactiveSub,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isBatteryOptIgnored) MaterialTheme.colorScheme.onSurfaceVariant else StatusOrange
                        )
                    }
                }
                if (isBatteryOptIgnored) {
                    NmrStatusChip(
                        text = strings.batteryOptExemptBadge,
                        statusColor = StatusGreen
                    )
                } else {
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(NmrTheme.dimens.chipRadius))
                            .clickable(onClick = onRequestIgnoreBatteryOptimization),
                        shape = RoundedCornerShape(NmrTheme.dimens.chipRadius),
                        color = StatusOrange.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, StatusOrange.copy(alpha = 0.4f)),
                        tonalElevation = 0.dp
                    ) {
                        Text(
                            text = strings.batteryOptFixButton,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = StatusOrange,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
            NmrHairlineDivider()
        }

        // BÖLÜM: BİLDİRİM VE GERİ BİLDİRİM
        item {
            NmrSectionHeader(title = strings.feedbackHeader)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp)
                    .clickable {
                        ReelsAccessibilityService.overlayController.showBlockBanner(
                            decision = PolicyDecision.BLOCK_IMMEDIATELY,
                            fallbackContext = context
                        )
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = NmrIcons.BildirimRozeti,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(NmrTheme.dimens.iconSize)
                    )
                    Column {
                        Text(
                            text = strings.overlayFeedbackTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = strings.overlayFeedbackSub,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = overlayFeedbackEnabled,
                    onCheckedChange = { isChecked ->
                        onOverlayFeedbackToggle(isChecked)
                        if (isChecked) {
                            ReelsAccessibilityService.overlayController.showBlockBanner(
                                decision = PolicyDecision.BLOCK_IMMEDIATELY,
                                fallbackContext = context
                            )
                        }
                    },
                    colors = switchColors
                )
            }
            NmrHairlineDivider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp)
                    .clickable {
                        triggerHapticPulse(context, haptic)
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = NmrIcons.Titresim,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(NmrTheme.dimens.iconSize)
                    )
                    Column {
                        Text(
                            text = strings.hapticFeedbackTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = strings.hapticFeedbackSub,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = hapticFeedbackEnabled,
                    onCheckedChange = { isChecked ->
                        onHapticFeedbackToggle(isChecked)
                        if (isChecked) {
                            triggerHapticPulse(context, haptic)
                        }
                    },
                    colors = switchColors
                )
            }
            NmrHairlineDivider()
        }

        // BÖLÜM: PROJEYİ DESTEKLE
        item {
            NmrSectionHeader(title = strings.supportHeader)
            SupportSettingCard(
                isSupporter = isSupporter,
                donationCount = supporterDonationCount,
                isEnglish = language == NmrLanguage.EN,
                onClick = onOpenSupportSheet,
                onResetSupporter = onResetSupporter
            )
            Spacer(modifier = Modifier.height(10.dp))
            NmrHairlineDivider()
        }

        // BÖLÜM: YEDEKLEME VE VERİ
        item {
            NmrSectionHeader(title = strings.backupHeader)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp)
                    .clickable { onOpenWalkthrough() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(NmrTheme.dimens.iconSize)
                    )
                    Column {
                        Text(
                            text = strings.walkthroughSettingTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = strings.walkthroughSettingSub,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
            NmrHairlineDivider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp)
                    .clickable { showResetStatsConfirmDialog = true },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(NmrTheme.dimens.iconSize)
                    )
                    Column {
                        Text(
                            text = strings.resetStatsTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = strings.resetStatsSub,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
            NmrHairlineDivider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp)
                    .clickable { exportLauncher.launch("outloop_backup.json") },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FileDownload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(NmrTheme.dimens.iconSize)
                    )
                    Column {
                        Text(
                            text = strings.backupExportTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = strings.backupExportSub,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
            NmrHairlineDivider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp)
                    .clickable { importLauncher.launch(arrayOf("application/json")) },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FileUpload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(NmrTheme.dimens.iconSize)
                    )
                    Column {
                        Text(
                            text = strings.backupImportTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = strings.backupImportSub,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
            NmrHairlineDivider()

            val isEnglish = language == NmrLanguage.EN
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp)
                    .clickable {
                        com.nomorereels.app.data.LocalDiagnosticsStore.shareDiagnosticReport(context)
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(NmrTheme.dimens.iconSize)
                    )
                    Column {
                        Text(
                            text = if (isEnglish) "Share Diagnostic Log" else "Tanılama Günlüğünü Paylaş",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = if (isEnglish)
                                "Export local crash & structural transition summary (no personal data)"
                            else
                                "Yerel çökme ve yapısal geçiş özetini paylaş (kişisel veri içermez)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
            NmrHairlineDivider()
        }

        // Alt Bilgi
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = strings.versionFooter,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun triggerHapticPulse(
    context: Context,
    haptic: HapticFeedback? = null
) {
    try {
        haptic?.performHapticFeedback(HapticFeedbackType.LongPress)
    } catch (_: Exception) {}
    try {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            @Suppress("DEPRECATION")
            vibratorManager?.defaultVibrator ?: (context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        if (vibrator?.hasVibrator() == true) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(50L, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(50L)
            }
        }
    } catch (_: Exception) {}
}
