package com.nomorereels.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.BatteryAlert
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Delete
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.WifiOff
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.nomorereels.app.data.SettingsBackupHelper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.window.Dialog
import com.nomorereels.app.data.MindfulBarrierType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.nomorereels.app.data.AppLanguage
import com.nomorereels.app.data.AppThemeMode
import com.nomorereels.app.data.HeatmapStore
import com.nomorereels.app.data.QuotaStore
import com.nomorereels.app.data.SettingsStore
import androidx.compose.runtime.CompositionLocalProvider
import com.nomorereels.app.ui.i18n.AppStrings
import com.nomorereels.app.ui.i18n.LocalAppStrings
import com.nomorereels.app.ui.i18n.TurkishStrings
import com.nomorereels.app.detection.DetectionEventLog
import com.nomorereels.app.detection.DetectionLogStore
import com.nomorereels.app.detection.ScreenType
import com.nomorereels.app.policy.DmReelMode
import com.nomorereels.app.policy.ProtectionLevel
import com.nomorereels.app.policy.ReelsPolicy
import com.nomorereels.app.service.ReelsAccessibilityService
import com.nomorereels.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        SettingsStore.init(this)
        QuotaStore.init(this)
        HeatmapStore.init(this)

        setContent {
            val appThemeMode by SettingsStore.appThemeMode.collectAsState()
            val nmrThemeMode = when (appThemeMode) {
                AppThemeMode.LIGHT -> NmrThemeMode.LIGHT
                AppThemeMode.DARK -> NmrThemeMode.AMOLED
                AppThemeMode.SYSTEM -> NmrThemeMode.SYSTEM
            }

            val isDark = when (nmrThemeMode) {
                NmrThemeMode.LIGHT -> false
                NmrThemeMode.AMOLED -> true
                NmrThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            DisposableEffect(isDark) {
                enableEdgeToEdge(
                    statusBarStyle = if (!isDark) {
                        SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT
                        )
                    } else {
                        SystemBarStyle.dark(
                            android.graphics.Color.TRANSPARENT
                        )
                    },
                    navigationBarStyle = if (!isDark) {
                        SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT
                        )
                    } else {
                        SystemBarStyle.dark(
                            android.graphics.Color.TRANSPARENT
                        )
                    }
                )
                onDispose {}
            }

            NoMoreReelsTheme(themeMode = nmrThemeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    NoMoreReelsAppRoot()
                }
            }
        }
    }
}

// ============================================================================
// APP ROOT & NAVIGATION
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoMoreReelsAppRoot() {
    val context = LocalContext.current

    val hasCompletedOnboarding by SettingsStore.hasCompletedOnboarding.collectAsState()
    var onboardingStep by remember { mutableIntStateOf(0) }
    var currentTab by remember { mutableStateOf(NmrBottomTab.BUGUN) }
    var showLiveLogsScreen by remember { mutableStateOf(false) }

    val appThemeMode by SettingsStore.appThemeMode.collectAsState()
    val appLanguage by SettingsStore.appLanguage.collectAsState()

    val isServiceActive by ReelsAccessibilityService.isServiceActive.collectAsState()
    val inMemoryBlockCount by ReelsAccessibilityService.actionExecutor.blockCount.collectAsState()
    val persistedTodayBlockCount by QuotaStore.todayBlockCount.collectAsState()
    val blockCount = maxOf(inMemoryBlockCount, persistedTodayBlockCount)
    val generalMode by SettingsStore.generalMode.collectAsState()

    val dmMode by SettingsStore.dmReelMode.collectAsState()
    val exploreMode by SettingsStore.exploreReelMode.collectAsState()
    val feedMode by SettingsStore.feedReelMode.collectAsState()
    val profileMode by SettingsStore.profileReelMode.collectAsState()
    val reelsTabMode by SettingsStore.reelsTabMode.collectAsState()

    val todayReelCount by QuotaStore.todayReelCount.collectAsState()
    val dailyCountLimit by QuotaStore.dailyCountLimit.collectAsState()
    val todayTimeSpentSeconds by QuotaStore.todayTimeSpentSeconds.collectAsState()
    val dailyTimeLimitMinutes by QuotaStore.dailyTimeLimitMinutes.collectAsState()
    val warningPercent by QuotaStore.warningPercent.collectAsState()
    val singleReelMaxSeconds by QuotaStore.singleReelMaxSeconds.collectAsState()

    val bedtimeEnabled by SettingsStore.bedtimeModeEnabled.collectAsState()
    val bedtimeStartHour by SettingsStore.bedtimeStartHour.collectAsState()
    val bedtimeStartMinute by SettingsStore.bedtimeStartMinute.collectAsState()
    val bedtimeEndHour by SettingsStore.bedtimeEndHour.collectAsState()
    val bedtimeEndMinute by SettingsStore.bedtimeEndMinute.collectAsState()

    val strictModeEnabled by SettingsStore.strictModeEnabled.collectAsState()
    val snoozeEndTimeMs by SettingsStore.snoozeEndTimeMs.collectAsState()
    val mindfulGatewayEnabled by SettingsStore.mindfulGatewayEnabled.collectAsState()
    val mindfulBarrierType by SettingsStore.mindfulBarrierType.collectAsState()
    val nightCompassionEnabled by SettingsStore.nightCompassionEnabled.collectAsState()

    val isSnoozeActive = snoozeEndTimeMs > System.currentTimeMillis()
    val snoozeRemainingMinutes = SettingsStore.getSnoozeRemainingMinutes()
    val isBedtimeActive = SettingsStore.isBedtimeActiveNow()

    var showSnoozeDialog by remember { mutableStateOf(false) }
    var showMindfulGatewayDialog by remember { mutableStateOf(false) }

    val currentStrings = AppStrings.get(appLanguage)
    val isEnglish = (appLanguage == AppLanguage.EN)

    val policySummary = com.nomorereels.app.policy.PolicySummaryHelper.getSummarySentence(
        generalMode = generalMode,
        dmMode = dmMode,
        exploreMode = exploreMode,
        feedMode = feedMode,
        profileMode = profileMode,
        reelsTabMode = reelsTabMode,
        isBedtimeActive = isBedtimeActive,
        isSnoozeActive = isSnoozeActive,
        snoozeRemainingMinutes = snoozeRemainingMinutes,
        dailyCountLimit = dailyCountLimit,
        dailyTimeLimitMinutes = dailyTimeLimitMinutes,
        isEnglish = isEnglish
    )

    var regionBeingEdited by remember { mutableStateOf<String?>(null) }
    var showCustomCountDialog by remember { mutableStateOf(false) }
    var showCustomTimeDialog by remember { mutableStateOf(false) }
    var showWillpowerDialog by remember { mutableStateOf(false) }
    var pendingLoosenAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var showBedtimePickerDialog by remember { mutableStateOf(false) }

    val currentThemeMode = when (appThemeMode) {
        AppThemeMode.LIGHT -> NmrThemeMode.LIGHT
        AppThemeMode.DARK -> NmrThemeMode.AMOLED
        AppThemeMode.SYSTEM -> NmrThemeMode.SYSTEM
    }
    val currentLanguage = when (appLanguage) {
        AppLanguage.TR -> NmrLanguage.TR
        AppLanguage.EN -> NmrLanguage.EN
    }

    var isBatteryOptIgnored by remember {
        mutableStateOf(com.nomorereels.app.service.ServiceHealthMonitor.isBatteryOptimizationIgnored(context))
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isBatteryOptIgnored = com.nomorereels.app.service.ServiceHealthMonitor.isBatteryOptimizationIgnored(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val triggerSnooze: (Int) -> Unit = { minutes ->
        if (strictModeEnabled) {
            pendingLoosenAction = { SettingsStore.startSnooze(context, minutes) }
            showWillpowerDialog = true
        } else {
            SettingsStore.startSnooze(context, minutes)
        }
    }

    val requestModeChange: (DmReelMode, DmReelMode, () -> Unit) -> Unit = { target, current, action ->
        val isLoosening = com.nomorereels.app.policy.ReelsPolicy.strictnessRank(target) > com.nomorereels.app.policy.ReelsPolicy.strictnessRank(current)
        if (strictModeEnabled && isLoosening) {
            pendingLoosenAction = action
            showWillpowerDialog = true
        } else {
            action()
        }
    }

    CompositionLocalProvider(LocalAppStrings provides currentStrings) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
        if (!hasCompletedOnboarding) {
            // ONBOARDING AKIŞI (Ekran 1, 2, 3)
            OnboardingFlow(
                step = onboardingStep,
                language = currentLanguage,
                isServiceActive = isServiceActive,
                onLanguageChange = { lang ->
                    SettingsStore.setAppLanguage(context, if (lang == NmrLanguage.TR) AppLanguage.TR else AppLanguage.EN)
                },
                onNextStep = {
                    if (onboardingStep < 2) {
                        onboardingStep++
                    } else {
                        SettingsStore.setHasCompletedOnboarding(context, true)
                    }
                },
                onPreviousStep = {
                    if (onboardingStep > 0) onboardingStep--
                },
                onOpenAccessibilitySettings = {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                },
                onSelectProfile = { profile ->
                    SettingsStore.applyProtectionLevel(context, profile)
                }
            )
        } else if (showLiveLogsScreen) {
            // CANLI OLAY GÜNLÜĞÜ (Ekran 8)
            BackHandler { showLiveLogsScreen = false }
            LiveLogsScreen(onBack = { showLiveLogsScreen = false }, showBackButton = true)
        } else {
            // 4 ANA SEKME AKIŞI (Ekran 4, 5, 6, 7)
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when (currentTab) {
                        NmrBottomTab.BUGUN -> {
                            ScreenBugun(
                                isServiceActive = isServiceActive,
                                generalMode = generalMode,
                                blockCount = blockCount,
                                policySummary = policySummary,
                                isSnoozeActive = isSnoozeActive,
                                snoozeRemainingMinutes = snoozeRemainingMinutes,
                                onOpenAccessibilitySettings = {
                                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                                },
                                onNavigateToProtection = {
                                    currentTab = NmrBottomTab.KORUMA
                                },
                                onStartSnoozeClick = { showMindfulGatewayDialog = true },
                                onEndSnoozeClick = { SettingsStore.stopSnooze(context) }
                            )
                        }
                        NmrBottomTab.KORUMA -> {
                            ScreenKorumaMerkezi(
                                generalMode = generalMode,
                                dmMode = dmMode,
                                exploreMode = exploreMode,
                                feedMode = feedMode,
                                profileMode = profileMode,
                                reelsTabMode = reelsTabMode,
                                onGeneralModeChange = { newMode ->
                                    requestModeChange(newMode, generalMode) {
                                        SettingsStore.applyGeneralModeToAllRegions(context, newMode)
                                        if (newMode == DmReelMode.COUNT_QUOTA) QuotaStore.setDailyCountLimitEnabled(context, true)
                                        if (newMode == DmReelMode.TIME_QUOTA) QuotaStore.setDailyTimeLimitEnabled(context, true)
                                    }
                                },
                                onApplyProfile = { profile ->
                                    val currentPreset = ProtectionLevel.findMatchingLevel(dmMode, exploreMode, feedMode, profileMode, reelsTabMode)
                                    val isLoosening = if (currentPreset != null) {
                                        ReelsPolicy.strictnessRank(profile.reelsTabMode) > ReelsPolicy.strictnessRank(currentPreset.reelsTabMode)
                                    } else false

                                    if (strictModeEnabled && isLoosening) {
                                        pendingLoosenAction = { SettingsStore.applyProtectionLevel(context, profile) }
                                        showWillpowerDialog = true
                                    } else {
                                        SettingsStore.applyProtectionLevel(context, profile)
                                    }
                                },
                                onSelectRegion = { regionKey -> regionBeingEdited = regionKey }
                            )
                        }
                        NmrBottomTab.ICGORU -> {
                            ScreenKotaSure(
                                generalMode = generalMode,
                                blockCount = blockCount,
                                todayReelCount = todayReelCount,
                                dailyCountLimit = dailyCountLimit,
                                todayMinutesSpent = (todayTimeSpentSeconds / 60).toInt(),
                                dailyTimeLimitMinutes = dailyTimeLimitMinutes,
                                warningPercent = warningPercent,
                                singleReelMaxSeconds = singleReelMaxSeconds,
                                onActivateQuotaMode = { targetMode ->
                                    requestModeChange(targetMode, generalMode) {
                                        SettingsStore.applyGeneralModeToAllRegions(context, targetMode)
                                        if (targetMode == DmReelMode.COUNT_QUOTA) QuotaStore.setDailyCountLimitEnabled(context, true)
                                        if (targetMode == DmReelMode.TIME_QUOTA) QuotaStore.setDailyTimeLimitEnabled(context, true)
                                    }
                                },
                                onCountLimitChange = { QuotaStore.setDailyCountLimit(context, it) },
                                onTimeLimitChange = { QuotaStore.setDailyTimeLimitMinutes(context, it) },
                                onWarningPercentChange = { QuotaStore.setWarningPercent(context, it) },
                                onSingleReelTimeoutChange = { QuotaStore.setSingleReelMaxSeconds(context, it) },
                                onOpenCustomCount = { showCustomCountDialog = true },
                                onOpenCustomTime = { showCustomTimeDialog = true }
                            )
                        }
                        NmrBottomTab.GUNLUK -> {
                            LiveLogsScreen(onBack = { currentTab = NmrBottomTab.BUGUN })
                        }
                        NmrBottomTab.AYARLAR -> {
                            ScreenAyarlar(
                                language = currentLanguage,
                                themeMode = currentThemeMode,
                                mindfulGatewayEnabled = mindfulGatewayEnabled,
                                mindfulBarrierType = mindfulBarrierType,
                                nightCompassionEnabled = nightCompassionEnabled,
                                bedtimeEnabled = bedtimeEnabled,
                                bedtimeStartHour = bedtimeStartHour,
                                bedtimeStartMinute = bedtimeStartMinute,
                                bedtimeEndHour = bedtimeEndHour,
                                bedtimeEndMinute = bedtimeEndMinute,
                                strictModeEnabled = strictModeEnabled,
                                isBatteryOptIgnored = isBatteryOptIgnored,
                                onRequestIgnoreBatteryOptimization = {
                                    com.nomorereels.app.service.ServiceHealthMonitor.requestIgnoreBatteryOptimization(context)
                                },
                                onLanguageChange = { lang ->
                                    SettingsStore.setAppLanguage(context, if (lang == NmrLanguage.TR) AppLanguage.TR else AppLanguage.EN)
                                },
                                onThemeChange = { theme ->
                                    val mapped = when (theme) {
                                        NmrThemeMode.LIGHT -> AppThemeMode.LIGHT
                                        NmrThemeMode.AMOLED -> AppThemeMode.DARK
                                        NmrThemeMode.SYSTEM -> AppThemeMode.SYSTEM
                                    }
                                    SettingsStore.setAppThemeMode(context, mapped)
                                },
                                onMindfulGatewayToggle = { SettingsStore.setMindfulGatewayEnabled(context, it) },
                                onMindfulBarrierTypeChange = { SettingsStore.setMindfulBarrierType(context, it) },
                                onNightCompassionToggle = { SettingsStore.setNightCompassionEnabled(context, it) },
                                onPreviewGateway = { showMindfulGatewayDialog = true },
                                onBedtimeToggle = { SettingsStore.setBedtimeModeEnabled(context, it) },
                                onOpenBedtimePicker = { showBedtimePickerDialog = true },
                                onStrictModeToggle = { SettingsStore.setStrictModeEnabled(context, it) },
                                onOpenLiveLogs = { currentTab = NmrBottomTab.GUNLUK }
                            )
                        }
                    }
                }

                // Alt Navigasyon Çubuğu
                NmrBottomNavigationBar(
                    currentTab = currentTab,
                    onTabSelected = { currentTab = it }
                )
            }
        }
    }

    // ========================================================================
    // EKRAN 9 — DİYALOGLAR VE ALT PANEL (28.dp Radius)
    // ========================================================================

    // (a) "Özel sayı gir" Diyaloğu
    if (showCustomCountDialog) {
        CustomNumberInputDialog(
            title = currentStrings.customCountTitle,
            description = currentStrings.customCountDesc,
            initialValue = dailyCountLimit,
            onDismiss = { showCustomCountDialog = false },
            onConfirm = { count ->
                QuotaStore.setDailyCountLimit(context, count)
                QuotaStore.setDailyCountLimitEnabled(context, true)
                showCustomCountDialog = false
            }
        )
    }

    if (showCustomTimeDialog) {
        CustomNumberInputDialog(
            title = currentStrings.customTimeTitle,
            description = currentStrings.customTimeDesc,
            initialValue = dailyTimeLimitMinutes,
            onDismiss = { showCustomTimeDialog = false },
            onConfirm = { mins ->
                QuotaStore.setDailyTimeLimitMinutes(context, mins)
                QuotaStore.setDailyTimeLimitEnabled(context, true)
                showCustomTimeDialog = false
            }
        )
    }

    // (b) "İrade Kilidi" Diyaloğu (Geri sayımlı)
    if (showWillpowerDialog && pendingLoosenAction != null) {
        WillpowerLockDialog(
            onDismiss = {
                showWillpowerDialog = false
                pendingLoosenAction = null
            },
            onConfirm = {
                pendingLoosenAction?.invoke()
                pendingLoosenAction = null
                showWillpowerDialog = false
            }
        )
    }

    // (b2) "Süreli Mola" Diyaloğu
    if (showSnoozeDialog) {
        SnoozeSelectionDialog(
            onDismiss = { showSnoozeDialog = false },
            onSelectMinutes = { minutes ->
                showSnoozeDialog = false
                triggerSnooze(minutes)
            }
        )
    }

    // (b3) "Farkındalık Kapısı" Diyaloğu
    if (showMindfulGatewayDialog) {
        MindfulGatewayDialog(
            barrierType = mindfulBarrierType,
            isNightMode = isBedtimeActive,
            onDismiss = { showMindfulGatewayDialog = false },
            onApproved = { durationMinutes, microPlan ->
                showMindfulGatewayDialog = false
                SettingsStore.startSnooze(context, durationMinutes, microPlan = microPlan)
                Toast.makeText(
                    context,
                    if (isEnglish) "Mindful window started ($durationMinutes min)" else "Bilinçli pencere başladı ($durationMinutes dk)",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }

    // (c) "Mod seç" Alt Paneli (ModalBottomSheet)
    if (regionBeingEdited != null) {
        val regionKey = regionBeingEdited!!
        val regionTitle = when (regionKey) {
            "DM" -> currentStrings.dmTitle
            "EXPLORE" -> currentStrings.exploreTitle
            "FEED" -> currentStrings.feedTitle
            "PROFILE" -> currentStrings.profileTitle
            "REELS_TAB" -> currentStrings.reelsTabTitle
            else -> regionKey
        }
        val currentSelectedMode = when (regionKey) {
            "DM" -> dmMode
            "EXPLORE" -> exploreMode
            "FEED" -> feedMode
            "PROFILE" -> profileMode
            "REELS_TAB" -> reelsTabMode
            else -> DmReelMode.FOLLOW_GLOBAL
        }

        RegionModeBottomSheet(
            regionTitle = regionTitle,
            currentMode = currentSelectedMode,
            onDismiss = { regionBeingEdited = null },
            onSelectMode = { selectedMode ->
                requestModeChange(selectedMode, currentSelectedMode) {
                    when (regionKey) {
                        "DM" -> SettingsStore.setDmReelMode(context, selectedMode)
                        "EXPLORE" -> SettingsStore.setExploreReelMode(context, selectedMode)
                        "FEED" -> SettingsStore.setFeedReelMode(context, selectedMode)
                        "PROFILE" -> SettingsStore.setProfileReelMode(context, selectedMode)
                        "REELS_TAB" -> SettingsStore.setReelsTabMode(context, selectedMode)
                    }
                }
                regionBeingEdited = null
            }
        )
    }

    // Gece Odak Saati Düzenleme
    if (showBedtimePickerDialog) {
        BedtimeTimePickerDialog(
            startH = bedtimeStartHour,
            startM = bedtimeStartMinute,
            endH = bedtimeEndHour,
            endM = bedtimeEndMinute,
            onDismiss = { showBedtimePickerDialog = false },
            onConfirm = { sH, sM, eH, eM ->
                SettingsStore.setBedtimeWindow(context, sH, sM, eH, eM)
                showBedtimePickerDialog = false
            }
        )
    }
    }
}

// ============================================================================
// EKRAN 1, 2, 3: ONBOARDING AKIŞI
// ============================================================================

@Composable
fun OnboardingFlow(
    step: Int,
    language: NmrLanguage,
    isServiceActive: Boolean,
    onLanguageChange: (NmrLanguage) -> Unit,
    onNextStep: () -> Unit,
    onPreviousStep: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onSelectProfile: (ProtectionLevel) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = NmrTheme.dimens.screenHorizontalMargin, vertical = 20.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            // Üst Bar: 3 Segmentli İlerleme Çubuğu ve Sağ Üstte Dil Seçimi
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 3 Segmentli İnce İlerleme Çubuğu
                Row(
                    modifier = Modifier.weight(1f).padding(end = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(NmrTheme.dimens.gridUnit)
                ) {
                    repeat(3) { index ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    if (index <= step) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                )
                        )
                    }
                }

                // Sağ Üst Dil Seçimi (Yalnızca Ekran 1'de veya tüm onboarding'de)
                NmrSegmentedControl(
                    options = listOf(NmrLanguage.TR, NmrLanguage.EN),
                    selectedOption = language,
                    onOptionSelected = onLanguageChange,
                    labelProvider = { it.code }
                )
            }

            // Adım İçeriği
            when (step) {
                0 -> OnboardingStepPrivacy()
                1 -> OnboardingStepAccessibility(isServiceActive = isServiceActive)
                2 -> OnboardingStepProfile(onSelectProfile = onSelectProfile)
            }
        }

        // Alt Butonlar
        val strings = LocalAppStrings.current
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NmrFilledButton(
                text = when (step) {
                    0 -> strings.nextButton
                    1 -> if (isServiceActive) strings.nextButton else strings.openSettingsButton
                    else -> strings.startButton
                },
                onClick = {
                    if (step == 1 && !isServiceActive) {
                        onOpenAccessibilitySettings()
                    } else {
                        onNextStep()
                    }
                }
            )

            if (step > 0) {
                TextButton(
                    onClick = onPreviousStep,
                    modifier = Modifier.heightIn(min = NmrTheme.dimens.minTouchTarget)
                ) {
                    Text(
                        text = strings.backButton,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(NmrTheme.dimens.minTouchTarget))
            }
        }
    }
}

// Ekran 1 — Onboarding 1/3 (Gizlilik)
@Composable
fun OnboardingStepPrivacy() {
    val strings = LocalAppStrings.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        // 64.dp Kalkan ve üstü çizili ağ ikonu
        Box(
            modifier = Modifier
                .size(NmrTheme.dimens.heroIconSize)
                .clip(RoundedCornerShape(NmrTheme.dimens.cardRadius))
                .background(NmrTheme.colors.selectedSurface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.WifiOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = strings.privacyHeroTitle,
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))
        NmrHairlineDivider()

        PrivacyFeatureRow(
            title = strings.privacyFeature1Title,
            subtitle = strings.privacyFeature1Sub
        )
        NmrHairlineDivider()

        PrivacyFeatureRow(
            title = strings.privacyFeature2Title,
            subtitle = strings.privacyFeature2Sub
        )
        NmrHairlineDivider()

        PrivacyFeatureRow(
            title = strings.privacyFeature3Title,
            subtitle = strings.privacyFeature3Sub
        )
    }
}

@Composable
fun PrivacyFeatureRow(title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Outlined.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(NmrTheme.dimens.iconSize)
        )
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Ekran 2 — Onboarding 2/3 (Erişilebilirlik İzni)
@Composable
fun OnboardingStepAccessibility(isServiceActive: Boolean) {
    val strings = LocalAppStrings.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = strings.accessibilityStepTitle,
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = strings.accessibilityStepSub,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Numaralı dikey adımlar
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            NumberedStepRow(stepNumber = 1, text = strings.step1Text)
            NumberedStepRow(stepNumber = 2, text = strings.step2Text)
            NumberedStepRow(stepNumber = 3, text = strings.step3Text)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Durum Çipi
        if (!isServiceActive) {
            NmrStatusChip(
                text = strings.waitingPermission,
                statusColor = NmrTheme.colors.statusOrange
            )
        } else {
            NmrStatusChip(
                text = strings.permissionGranted,
                statusColor = NmrTheme.colors.statusGreen
            )
        }
    }
}

@Composable
fun NumberedStepRow(stepNumber: Int, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .border(BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$stepNumber",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

// Ekran 3 — Onboarding 3/3 (Profil Seçimi)
@Composable
fun OnboardingStepProfile(
    onSelectProfile: (ProtectionLevel) -> Unit
) {
    val strings = LocalAppStrings.current
    var selectedPreset by remember { mutableStateOf(ProtectionLevel.BALANCED) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = strings.profileStepTitle,
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 3 Seçilebilir Profil Satırı
        NmrSelectableRow(
            title = strings.presetBalanced,
            subtitle = strings.profileStepBalancedSub,
            icon = NmrIcons.Dengeli,
            selected = selectedPreset == ProtectionLevel.BALANCED,
            onClick = {
                selectedPreset = ProtectionLevel.BALANCED
                onSelectProfile(ProtectionLevel.BALANCED)
            },
            useRadioIndicator = false
        )

        NmrSelectableRow(
            title = strings.presetMindful,
            subtitle = strings.profileStepMindfulSub,
            icon = NmrIcons.Bilincli,
            selected = selectedPreset == ProtectionLevel.MINDFUL,
            onClick = {
                selectedPreset = ProtectionLevel.MINDFUL
                onSelectProfile(ProtectionLevel.MINDFUL)
            },
            useRadioIndicator = false
        )

        NmrSelectableRow(
            title = strings.blockAllTitle,
            subtitle = strings.profileStepStrictSub,
            icon = NmrIcons.KompleYasak,
            selected = selectedPreset == ProtectionLevel.STRICT,
            onClick = {
                selectedPreset = ProtectionLevel.STRICT
                onSelectProfile(ProtectionLevel.STRICT)
            },
            useRadioIndicator = false
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = strings.profileStepFooter,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ============================================================================
// EKRAN 4: SEKME 1 — "BUGÜN" (DASHBOARD)
// ============================================================================

@Composable
fun ScreenBugun(
    isServiceActive: Boolean,
    generalMode: DmReelMode,
    blockCount: Int,
    policySummary: String,
    isSnoozeActive: Boolean,
    snoozeRemainingMinutes: Int,
    onOpenAccessibilitySettings: () -> Unit,
    onNavigateToProtection: () -> Unit,
    onStartSnoozeClick: () -> Unit,
    onEndSnoozeClick: () -> Unit
) {
    val strings = LocalAppStrings.current
    val savedSeconds = blockCount * 45L
    val savedMinutes = savedSeconds / 60

    val todayDateFormatted = remember(strings) {
        try {
            val now = java.time.LocalDate.now()
            val formatter = java.time.format.DateTimeFormatter.ofPattern(
                "EEEE, d MMMM",
                if (strings is com.nomorereels.app.ui.i18n.EnglishStrings) java.util.Locale.ENGLISH else java.util.Locale("tr")
            )
            now.format(formatter).uppercase()
        } catch (_: Exception) {
            "SALI, 20 EKİM"
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = NmrTheme.dimens.screenHorizontalMargin),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Üst Alan: Tarih + Selamlama + Switch
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = todayDateFormatted,
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.4.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = strings.dashboardGreeting,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 25.sp,
                            lineHeight = 31.sp,
                            letterSpacing = (-0.4).sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Switch(
                    checked = isServiceActive,
                    onCheckedChange = { onOpenAccessibilitySettings() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = MaterialTheme.colorScheme.outline
                    )
                )
            }
        }

        // 1. Erişilebilirlik Servisi Kapalı Uyarısı (Eğer kapalıysa)
        if (!isServiceActive) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(NmrTheme.dimens.cardRadius))
                        .clickable(onClick = onOpenAccessibilitySettings),
                    shape = RoundedCornerShape(NmrTheme.dimens.cardRadius),
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.serviceInactiveTitle,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = strings.serviceInactiveDesc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Text(
                            text = strings.openButton,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        // 2. Mola Devrede Banner'ı (Eğer mola aktifse)
        if (isSnoozeActive) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(NmrTheme.dimens.cardRadius),
                    color = StatusOrange.copy(alpha = 0.10f),
                    border = BorderStroke(1.dp, StatusOrange.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier.size(7.dp).clip(CircleShape).background(StatusOrange)
                                )
                                Text(
                                    text = strings.snoozeBannerTitle,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = StatusOrange
                                )
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = strings.snoozeBannerDesc(snoozeRemainingMinutes),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        TextButton(onClick = onEndSnoozeClick) {
                            Text(
                                text = strings.endSnoozeButton,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = StatusOrange
                            )
                        }
                    }
                }
            }
        }

        // 3. KURTARILAN ODAK KARTI (Blueprint Bölüm 3)
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 2.dp,
                        shape = RoundedCornerShape(NmrTheme.dimens.cardRadius),
                        spotColor = Color(0x0A24332A),
                        ambientColor = Color(0x0424332A)
                    ),
                shape = RoundedCornerShape(NmrTheme.dimens.cardRadius),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = strings.savedFocusTitle.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.3.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (savedMinutes > 0) "$savedMinutes" else "$savedSeconds",
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 50.sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = if (savedMinutes > 0) {
                                if (strings is com.nomorereels.app.ui.i18n.EnglishStrings) "min" else "dk"
                            } else {
                                if (strings is com.nomorereels.app.ui.i18n.EnglishStrings) "sec" else "sn"
                            },
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 7.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = strings.savedFocusPauseQuote,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = strings.savedFocusFootnote,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.5.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // 4. KORUMAN AKTİF KARTI (Blueprint Bölüm 3)
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 2.dp,
                        shape = RoundedCornerShape(NmrTheme.dimens.cardRadius),
                        spotColor = Color(0x0A24332A),
                        ambientColor = Color(0x0424332A)
                    ),
                shape = RoundedCornerShape(NmrTheme.dimens.cardRadius),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isServiceActive && !isSnoozeActive) strings.protectionActiveTitle else strings.protectionInactiveTitle,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 17.sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        val isEffectiveActive = isServiceActive && !isSnoozeActive
                        Surface(
                            shape = CircleShape,
                            color = if (isEffectiveActive) NmrTheme.colors.selectedSurface else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (isEffectiveActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                                Text(
                                    text = if (isEffectiveActive) strings.statusActivePill else strings.statusInactivePill,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = 0.sp
                                    ),
                                    color = if (isEffectiveActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = policySummary,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = strings.protectionDescRulesApplied,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onNavigateToProtection,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(NmrTheme.dimens.buttonHeight),
                        shape = RoundedCornerShape(NmrTheme.dimens.buttonRadius),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = strings.editProtectionButton,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.sp
                            )
                        )
                    }
                }
            }
        }

        // 5. İKİ KÜÇÜK METRİK YAN YANA (Kırılan Döngü & Bilinçli Mola)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Sol Kart: Kırılan Döngü
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .shadow(
                            elevation = 2.dp,
                            shape = RoundedCornerShape(NmrTheme.dimens.cardRadius),
                            spotColor = Color(0x0A24332A),
                            ambientColor = Color(0x0424332A)
                        ),
                    shape = RoundedCornerShape(NmrTheme.dimens.cardRadius),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "$blockCount",
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 34.sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = strings.loopsBrokenTitle,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (strings is com.nomorereels.app.ui.i18n.EnglishStrings) "Autopilot stopped" else "Otopilot durduruldu",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Sağ Kart: Bilinçli Mola
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .shadow(
                            elevation = 2.dp,
                            shape = RoundedCornerShape(NmrTheme.dimens.cardRadius),
                            spotColor = Color(0x0A24332A),
                            ambientColor = Color(0x0424332A)
                        )
                        .clip(RoundedCornerShape(NmrTheme.dimens.cardRadius))
                        .clickable(onClick = if (isSnoozeActive) onEndSnoozeClick else onStartSnoozeClick),
                    shape = RoundedCornerShape(NmrTheme.dimens.cardRadius),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (isSnoozeActive) "${snoozeRemainingMinutes} dk" else "10 dk",
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 34.sp
                            ),
                            color = if (isSnoozeActive) StatusOrange else MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = strings.mindfulBreakTitle,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isSnoozeActive) {
                                if (strings is com.nomorereels.app.ui.i18n.EnglishStrings) "Tap to end" else "Bitirmek için tıkla"
                            } else {
                                if (strings is com.nomorereels.app.ui.i18n.EnglishStrings) "Start break →" else "Mola başlat →"
                            },
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = if (isSnoozeActive) StatusOrange else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // 6. GÜNÜN HATIRLATMASI (Blueprint Bölüm 3)
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(NmrTheme.dimens.cardRadius)),
                shape = RoundedCornerShape(NmrTheme.dimens.cardRadius),
                color = NmrTheme.colors.mistBlue.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, NmrTheme.colors.mistBlue)
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🌿", fontSize = 18.sp)
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.todayReminderTitle.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 1.3.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = strings.todayReminderQuote,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 15.sp,
                                lineHeight = 22.sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

// ============================================================================
// EKRAN 5: SEKME 2 — "KORUMA MERKEZİ" (PROTECTION CENTER)
// ============================================================================

@Composable
fun ScreenKorumaMerkezi(
    generalMode: DmReelMode,
    dmMode: DmReelMode,
    exploreMode: DmReelMode,
    feedMode: DmReelMode,
    profileMode: DmReelMode,
    reelsTabMode: DmReelMode,
    onGeneralModeChange: (DmReelMode) -> Unit,
    onApplyProfile: (ProtectionLevel) -> Unit,
    onSelectRegion: (String) -> Unit
) {
    val strings = LocalAppStrings.current
    val currentPreset = remember(dmMode, exploreMode, feedMode, profileMode, reelsTabMode) {
        ProtectionLevel.findMatchingLevel(dmMode, exploreMode, feedMode, profileMode, reelsTabMode)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = NmrTheme.dimens.screenHorizontalMargin),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Ekran Başlığı (Header)
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Column {
                Text(
                    text = if (strings is com.nomorereels.app.ui.i18n.EnglishStrings)
                        "PROTECTION CENTER"
                    else
                        "KORUMA MERKEZİ",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.6.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = strings.protectionCenterTagline,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 25.sp,
                        lineHeight = 31.sp,
                        letterSpacing = (-0.4).sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = strings.protectionCenterSubtext,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 2. Üst Bölüm: Genel Koruma Modu (2x2 Grid)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Column {
                    Text(
                        text = strings.generalPolicyHeader,
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = strings.generalPolicySubtext,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }

                // Satır 1
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ProtectionCenterModeCard(
                        title = strings.blockAllTitle,
                        desc = strings.blockAllSub,
                        icon = NmrIcons.KompleYasak,
                        selected = generalMode == DmReelMode.BLOCK_ALL,
                        onClick = { onGeneralModeChange(DmReelMode.BLOCK_ALL) },
                        modifier = Modifier.weight(1f)
                    )
                    ProtectionCenterModeCard(
                        title = strings.countQuotaTitle,
                        desc = strings.countQuotaSub,
                        icon = NmrIcons.XTane,
                        selected = generalMode == DmReelMode.COUNT_QUOTA,
                        onClick = { onGeneralModeChange(DmReelMode.COUNT_QUOTA) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Satır 2
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ProtectionCenterModeCard(
                        title = strings.modeTitle(DmReelMode.ASK_WITH_FRICTION),
                        desc = strings.modeSubtitle(DmReelMode.ASK_WITH_FRICTION),
                        icon = NmrIcons.Bilincli,
                        selected = generalMode == DmReelMode.ASK_WITH_FRICTION,
                        onClick = { onGeneralModeChange(DmReelMode.ASK_WITH_FRICTION) },
                        modifier = Modifier.weight(1f)
                    )
                    ProtectionCenterModeCard(
                        title = strings.modeTitle(DmReelMode.FREE),
                        desc = strings.modeSubtitle(DmReelMode.FREE),
                        icon = NmrIcons.KameraKapsulu,
                        selected = generalMode == DmReelMode.FREE,
                        onClick = { onGeneralModeChange(DmReelMode.FREE) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 3. Orta Bölüm: Instagram Yüzeyleri (5 Bölge)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Column {
                    Text(
                        text = strings.surfacesHeader,
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = strings.surfacesSubtext,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    RegionSettingRow(
                        title = strings.dmTitle,
                        subtitle = strings.dmSub,
                        icon = NmrIcons.Dm,
                        modeLabel = strings.modeTitle(dmMode),
                        onClick = { onSelectRegion("DM") }
                    )
                    RegionSettingRow(
                        title = strings.exploreTitle,
                        subtitle = strings.exploreSub,
                        icon = NmrIcons.Kesfet,
                        modeLabel = strings.modeTitle(exploreMode),
                        onClick = { onSelectRegion("EXPLORE") }
                    )
                    RegionSettingRow(
                        title = strings.feedTitle,
                        subtitle = strings.feedSub,
                        icon = NmrIcons.AnaSayfa,
                        modeLabel = strings.modeTitle(feedMode),
                        onClick = { onSelectRegion("FEED") }
                    )
                    RegionSettingRow(
                        title = strings.profileTitle,
                        subtitle = strings.profileSub,
                        icon = NmrIcons.Profil,
                        modeLabel = strings.modeTitle(profileMode),
                        onClick = { onSelectRegion("PROFILE") }
                    )
                    RegionSettingRow(
                        title = strings.reelsTabTitle,
                        subtitle = strings.reelsTabSub,
                        icon = NmrIcons.ReelsSekmesi,
                        modeLabel = strings.modeTitle(reelsTabMode),
                        onClick = { onSelectRegion("REELS_TAB") }
                    )
                }
            }
        }

        // 4. Alt Bölüm: Hazır Seviyeler (Quick Presets)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Column {
                    Text(
                        text = strings.presetsHeaderTitle,
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = strings.presetsSubtext,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProtectionCenterPresetCard(
                        title = "${strings.presetBalanced} (${if (strings is com.nomorereels.app.ui.i18n.EnglishStrings) "Recommended" else "Önerilen"})",
                        desc = strings.presetBalancedDesc,
                        selected = currentPreset == ProtectionLevel.BALANCED,
                        onClick = { onApplyProfile(ProtectionLevel.BALANCED) }
                    )
                    ProtectionCenterPresetCard(
                        title = strings.presetDeepFocusTitle,
                        desc = strings.presetDeepFocusDesc,
                        selected = currentPreset == ProtectionLevel.STRICT,
                        onClick = { onApplyProfile(ProtectionLevel.STRICT) }
                    )
                    ProtectionCenterPresetCard(
                        title = strings.presetMindful,
                        desc = strings.modeSubtitle(DmReelMode.ASK_WITH_FRICTION),
                        selected = currentPreset == ProtectionLevel.MINDFUL,
                        onClick = { onApplyProfile(ProtectionLevel.MINDFUL) }
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun ProtectionCenterModeCard(
    title: String,
    desc: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = if (selected) NmrTheme.colors.selectedSurface else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            if (selected) 1.5.dp else 0.5.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        ),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                if (selected) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp, lineHeight = 15.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
        }
    }
}

@Composable
fun ProtectionCenterPresetCard(
    title: String,
    desc: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = if (selected) NmrTheme.colors.selectedSurface else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            if (selected) 1.5.dp else 0.5.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        ),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (selected) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "● Aktif",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun RegionSettingRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modeLabel: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(NmrTheme.dimens.cardRadius))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(NmrTheme.dimens.cardRadius),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(NmrTheme.colors.selectedSurface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Surface(
                shape = CircleShape,
                color = NmrTheme.colors.selectedSurface,
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                tonalElevation = 0.dp
            ) {
                Text(
                    text = modeLabel,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}


// ============================================================================
// EKRAN 6: SEKME 3 — "KOTA & SÜRE"
// ============================================================================

@Composable
fun ScreenIcgoru(
    generalMode: DmReelMode,
    blockCount: Int,
    todayReelCount: Int,
    dailyCountLimit: Int,
    todayMinutesSpent: Int,
    dailyTimeLimitMinutes: Int,
    warningPercent: Int,
    singleReelMaxSeconds: Int,
    onActivateQuotaMode: (DmReelMode) -> Unit,
    onCountLimitChange: (Int) -> Unit,
    onTimeLimitChange: (Int) -> Unit,
    onWarningPercentChange: (Int) -> Unit,
    onSingleReelTimeoutChange: (Int) -> Unit,
    onOpenCustomCount: () -> Unit,
    onOpenCustomTime: () -> Unit
) {
    val strings = LocalAppStrings.current
    var warningSliderValue by remember(warningPercent) { mutableFloatStateOf(warningPercent.toFloat()) }
    var singleReelSliderValue by remember(singleReelMaxSeconds) { mutableFloatStateOf(singleReelMaxSeconds.toFloat()) }

    val isQuotaModeActive = generalMode == DmReelMode.COUNT_QUOTA || generalMode == DmReelMode.TIME_QUOTA

    val savedSeconds = blockCount * 45L
    val savedMinutes = savedSeconds / 60

    val matrix by HeatmapStore.matrix.collectAsState()

    // Haftalık kazanım verisi (HeatmapStore gerçek matris verisiyle)
    val dayNames = if (strings is com.nomorereels.app.ui.i18n.EnglishStrings) {
        listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    } else {
        listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz")
    }
    val currentDayIndex = remember {
        val dow = java.time.LocalDate.now().dayOfWeek.value
        (dow - 1).coerceIn(0, 6)
    }
    val weeklyDayMinutes = remember(matrix, savedMinutes, currentDayIndex) {
        (0..6).map { dayIdx ->
            val attempts = (0..23).sumOf { h -> matrix[dayIdx * 24 + h] }
            val estMinutes = (attempts * 45L) / 60L
            if (dayIdx == currentDayIndex) maxOf(estMinutes, savedMinutes) else estMinutes
        }
    }
    val maxWeeklyMin = weeklyDayMinutes.maxOrNull()?.coerceAtLeast(15L) ?: 15L
    val totalWeeklyMinutes = weeklyDayMinutes.sum()
    val weeklyTotalHours = (totalWeeklyMinutes / 60).toInt()
    val weeklyTotalRemMinutes = (totalWeeklyMinutes % 60).toInt()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = NmrTheme.dimens.screenHorizontalMargin),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Ekran Başlığı (Header)
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Column {
                Text(
                    text = if (strings is com.nomorereels.app.ui.i18n.EnglishStrings)
                        "INSIGHTS & HABITS"
                    else
                        "İÇGÖRÜLER VE KAZANIMLAR",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.6.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = strings.insightsTagline,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 25.sp,
                        lineHeight = 31.sp,
                        letterSpacing = (-0.4).sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = strings.insightsSubtext,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 2. Üst Kart — Haftalık Kazanım Özeti & Çubuk Grafik
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(NmrTheme.dimens.cardRadius)),
                shape = RoundedCornerShape(NmrTheme.dimens.cardRadius),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                tonalElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier.padding(18.dp)
                ) {
                    Text(
                        text = strings.weeklyFocusTitle,
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = strings.weeklyFocusDesc(weeklyTotalHours, weeklyTotalRemMinutes),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(18.dp))

                    // 7 Günlük Sakin Çubuk Grafik
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        dayNames.forEachIndexed { idx, day ->
                            val mins = weeklyDayMinutes[idx]
                            val barHeightDp = ((mins.toFloat() / maxWeeklyMin.toFloat()).coerceIn(0.2f, 1f) * 60).dp
                            val isCurrentDay = (idx == currentDayIndex)

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .height(64.dp)
                                        .fillMaxWidth(),
                                    contentAlignment = Alignment.BottomCenter
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(16.dp)
                                            .height(barHeightDp)
                                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 2.dp, bottomEnd = 2.dp))
                                            .background(
                                                if (isCurrentDay) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                                            )
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = day,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isCurrentDay) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (isCurrentDay) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                if (isCurrentDay) {
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                } else {
                                    Spacer(modifier = Modifier.size(4.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2b. 7x24 Alışkanlık Isı Haritası (Heatmap)
        item {
            val totalAttempts = remember(matrix) { matrix.sum() }
            val maxCellAttempts = remember(matrix) { matrix.maxOrNull()?.coerceAtLeast(1) ?: 1 }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(NmrTheme.dimens.cardRadius)),
                shape = RoundedCornerShape(NmrTheme.dimens.cardRadius),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                tonalElevation = 0.dp
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = strings.habitHeatmapTitle,
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = strings.habitHeatmapSub,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Saat işaretçileri (00, 06, 12, 18, 23)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.width(28.dp))
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            listOf("00", "06", "12", "18", "23").forEach { hr ->
                                Text(
                                    text = hr,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Normal
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))

                    // 7 Gün x 24 Saat Matrisi
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        dayNames.forEachIndexed { dayIdx, dayName ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = dayName.take(3),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = if (dayIdx == currentDayIndex) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (dayIdx == currentDayIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.width(28.dp)
                                )
                                for (hour in 0..23) {
                                    val cellAttempts = matrix[dayIdx * 24 + hour]
                                    val cellColor = if (cellAttempts == 0) {
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                    } else {
                                        val intensity = (0.3f + (cellAttempts.toFloat() / maxCellAttempts.toFloat()) * 0.7f).coerceIn(0.3f, 1f)
                                        MaterialTheme.colorScheme.primary.copy(alpha = intensity)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(9.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(cellColor)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Lejant ve Bilgi
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (totalAttempts == 0) {
                            Text(
                                text = strings.habitHeatmapEmpty,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        } else {
                            Text(
                                text = "${totalAttempts} ${if (strings is com.nomorereels.app.ui.i18n.EnglishStrings) "total attempts" else "toplam yoklama"}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = strings.habitHeatmapLess,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            )
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
                            )
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                            Text(
                                text = strings.habitHeatmapMore,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // 3. Günlük Bütçe & Kota Durumu (Quiet Sage, No Red Alarms)
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(NmrTheme.dimens.cardRadius)),
                shape = RoundedCornerShape(NmrTheme.dimens.cardRadius),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                tonalElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier.padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.quotaBudgetTitle,
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 1.2.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            shape = CircleShape,
                            color = NmrTheme.colors.selectedSurface,
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                        ) {
                            Text(
                                text = if (isQuotaModeActive) strings.activeBadge else strings.statusInactivePill,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Sayaç 1: Günlük Reel Adedi
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.todayWatchedReels,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "$todayReelCount / $dailyCountLimit",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 17.sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { (todayReelCount.toFloat() / maxOf(1, dailyCountLimit).toFloat()).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Sayaç 2: Günlük Süre
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.todayTimeSpent,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "$todayMinutesSpent / $dailyTimeLimitMinutes ${strings.quotaSuffixMinutes}",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 17.sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { (todayMinutesSpent.toFloat() / maxOf(1, dailyTimeLimitMinutes).toFloat()).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    val isReached = (isQuotaModeActive && (todayReelCount >= dailyCountLimit || todayMinutesSpent >= dailyTimeLimitMinutes))
                    if (isReached) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = NmrTheme.colors.selectedSurface
                        ) {
                            Text(
                                text = strings.quotaReachedSerenity,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    } else {
                        Text(
                            text = if (isQuotaModeActive) strings.quotaUnderBudget else strings.midnightResetNote,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (!isQuotaModeActive) {
                        Spacer(modifier = Modifier.height(12.dp))
                        NmrFilledButton(
                            text = strings.switchToQuotaMode,
                            onClick = { onActivateQuotaMode(DmReelMode.COUNT_QUOTA) }
                        )
                    }
                }
            }
        }

        // 4. Günlük Video Sınırı Ayarı
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = strings.dailyReelLimitHeader,
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(3, 5, 10, 20, 50, 100).forEach { limit ->
                        NmrChip(
                            label = "$limit",
                            selected = dailyCountLimit == limit,
                            onClick = { onCountLimitChange(limit) }
                        )
                    }
                    val isCustom = dailyCountLimit !in listOf(3, 5, 10, 20, 50, 100)
                    NmrChip(
                        label = if (isCustom) "$dailyCountLimit" else strings.customInput,
                        selected = isCustom,
                        onClick = onOpenCustomCount
                    )
                }
            }
        }

        // 5. Günlük Süre Sınırı Ayarı
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = strings.dailyTimeLimitHeader,
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(5, 15, 30, 60).forEach { mins ->
                        NmrChip(
                            label = "$mins ${strings.quotaSuffixMinutes}",
                            selected = dailyTimeLimitMinutes == mins,
                            onClick = { onTimeLimitChange(mins) }
                        )
                    }
                    val isTimeCustom = dailyTimeLimitMinutes !in listOf(5, 15, 30, 60)
                    NmrChip(
                        label = if (isTimeCustom) "$dailyTimeLimitMinutes ${strings.quotaSuffixMinutes}" else strings.customInput,
                        selected = isTimeCustom,
                        onClick = onOpenCustomTime
                    )
                }
            }
        }

        // 6. Yaklaşma Uyarısı Eşiği Slider
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.warningThresholdTitle,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "%${warningSliderValue.toInt()}",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Slider(
                    value = warningSliderValue,
                    onValueChange = { warningSliderValue = it },
                    onValueChangeFinished = { onWarningPercentChange(warningSliderValue.toInt()) },
                    valueRange = 50f..90f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                )
            }
        }

        // 7. Tek Reel Süre Sınırı Slider
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.singleReelTimeoutTitle,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${singleReelSliderValue.toInt()} ${strings.quotaSuffixSeconds}",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Slider(
                    value = singleReelSliderValue,
                    onValueChange = { singleReelSliderValue = it },
                    onValueChangeFinished = { onSingleReelTimeoutChange(singleReelSliderValue.toInt()) },
                    valueRange = 30f..180f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

// Geriye dönük uyumluluk alias'ı
@Composable
fun ScreenKotaSure(
    generalMode: DmReelMode,
    blockCount: Int,
    todayReelCount: Int,
    dailyCountLimit: Int,
    todayMinutesSpent: Int,
    dailyTimeLimitMinutes: Int,
    warningPercent: Int,
    singleReelMaxSeconds: Int,
    onActivateQuotaMode: (DmReelMode) -> Unit,
    onCountLimitChange: (Int) -> Unit,
    onTimeLimitChange: (Int) -> Unit,
    onWarningPercentChange: (Int) -> Unit,
    onSingleReelTimeoutChange: (Int) -> Unit,
    onOpenCustomCount: () -> Unit,
    onOpenCustomTime: () -> Unit
) {
    ScreenIcgoru(
        generalMode = generalMode,
        blockCount = blockCount,
        todayReelCount = todayReelCount,
        dailyCountLimit = dailyCountLimit,
        todayMinutesSpent = todayMinutesSpent,
        dailyTimeLimitMinutes = dailyTimeLimitMinutes,
        warningPercent = warningPercent,
        singleReelMaxSeconds = singleReelMaxSeconds,
        onActivateQuotaMode = onActivateQuotaMode,
        onCountLimitChange = onCountLimitChange,
        onTimeLimitChange = onTimeLimitChange,
        onWarningPercentChange = onWarningPercentChange,
        onSingleReelTimeoutChange = onSingleReelTimeoutChange,
        onOpenCustomCount = onOpenCustomCount,
        onOpenCustomTime = onOpenCustomTime
    )
}

// ============================================================================
// EKRAN 7: SEKME 4 — "AYARLAR"
// ============================================================================

@Composable
fun ScreenAyarlar(
    language: NmrLanguage,
    themeMode: NmrThemeMode,
    mindfulGatewayEnabled: Boolean,
    mindfulBarrierType: MindfulBarrierType,
    nightCompassionEnabled: Boolean,
    bedtimeEnabled: Boolean,
    bedtimeStartHour: Int,
    bedtimeStartMinute: Int,
    bedtimeEndHour: Int,
    bedtimeEndMinute: Int,
    strictModeEnabled: Boolean,
    isBatteryOptIgnored: Boolean,
    onRequestIgnoreBatteryOptimization: () -> Unit,
    onLanguageChange: (NmrLanguage) -> Unit,
    onThemeChange: (NmrThemeMode) -> Unit,
    onMindfulGatewayToggle: (Boolean) -> Unit,
    onMindfulBarrierTypeChange: (MindfulBarrierType) -> Unit,
    onNightCompassionToggle: (Boolean) -> Unit,
    onPreviewGateway: () -> Unit,
    onBedtimeToggle: (Boolean) -> Unit,
    onOpenBedtimePicker: () -> Unit,
    onStrictModeToggle: (Boolean) -> Unit,
    onOpenLiveLogs: () -> Unit
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val bedtimeSuggestion = remember { HeatmapStore.analyzeForBedtimeSuggestion() }

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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = NmrTheme.dimens.screenHorizontalMargin),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Column {
                Text(
                    text = if (strings is com.nomorereels.app.ui.i18n.EnglishStrings)
                        "PERSONAL PREFERENCES"
                    else
                        "KİŞİSEL TERCİHLER",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.6.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (strings is com.nomorereels.app.ui.i18n.EnglishStrings)
                        "A serene experience."
                    else
                        "Huzurlu bir deneyim.",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 25.sp,
                        lineHeight = 31.sp,
                        letterSpacing = (-0.4).sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (strings is com.nomorereels.app.ui.i18n.EnglishStrings)
                        "Appearance, mindful gateways, willpower lock and battery health."
                    else
                        "Görünüm, dil, farkındalık kapısı, irade kilidi ve pil sağlığı.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // BÖLÜM: GÖRÜNÜM
        item {
            NmrSectionHeader(title = strings.appearanceHeader)

            // Dil Satırı
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

            // Tema Satırı
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

            // Ana Anahtar
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
                            style = MaterialTheme.typography.bodyMedium,
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

            if (mindfulGatewayEnabled) {
                Spacer(modifier = Modifier.height(8.dp))
                // Bariyer Türü Seçici
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(
                        text = strings.barrierTypeTitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        NmrChip(
                            label = strings.barrierTypeBreathIntent,
                            selected = mindfulBarrierType == MindfulBarrierType.BREATH_AND_INTENT,
                            onClick = { onMindfulBarrierTypeChange(MindfulBarrierType.BREATH_AND_INTENT) }
                        )
                        NmrChip(
                            label = strings.barrierTypeMath,
                            selected = mindfulBarrierType == MindfulBarrierType.COGNITIVE_MATH,
                            onClick = { onMindfulBarrierTypeChange(MindfulBarrierType.COGNITIVE_MATH) }
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    NmrChip(
                        label = strings.barrierTypeTyping,
                        selected = mindfulBarrierType == MindfulBarrierType.MINDFUL_TYPING,
                        onClick = { onMindfulBarrierTypeChange(MindfulBarrierType.MINDFUL_TYPING) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Gece Şefkat Modu
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 54.dp),
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
                                text = strings.nightCompassionTitle,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = strings.nightCompassionSub,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = nightCompassionEnabled,
                        onCheckedChange = onNightCompassionToggle,
                        colors = switchColors
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Deneyimle Butonu
                NmrChip(
                    label = strings.previewGatewayButton,
                    selected = true,
                    onClick = onPreviewGateway
                )
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

            // Gece Odak Saati
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

            // İrade Kilidi
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
                        imageVector = NmrIcons.IradeKilidi,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(NmrTheme.dimens.iconSize)
                    )
                    Column {
                        Text(
                            text = strings.willpowerTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = strings.willpowerSub,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = strictModeEnabled,
                    onCheckedChange = onStrictModeToggle,
                    colors = switchColors
                )
            }
            NmrHairlineDivider()

            // Pil Optimizasyonu Koruması (Servis Sağlık Bekçisi)
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
                            style = MaterialTheme.typography.bodyMedium,
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

        // BÖLÜM: YEDEKLEME VE VERİ
        item {
            NmrSectionHeader(title = strings.backupHeader)

            // Ayarları Dosyaya Yedekle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp)
                    .clickable { exportLauncher.launch("nomorereels_backup.json") },
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
                            style = MaterialTheme.typography.bodyMedium,
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

            // Ayarları Dosyadan Geri Yükle
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
                            style = MaterialTheme.typography.bodyMedium,
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

        // BÖLÜM: GELİŞTİRİCİ
        item {
            NmrSectionHeader(title = strings.developerHeader)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .clickable(onClick = onOpenLiveLogs),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.liveLogsTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Alt Bilgi (Footer)
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

// ============================================================================
// EKRAN 8: CANLI OLAY GÜNLÜĞÜ (GELİŞTİRİCİ EKRANI)
// ============================================================================

enum class LogFilterType {
    ALL, SCREEN_CHANGE, ACTION, NODE_TREE
}

@Composable
fun LiveLogsScreen(
    onBack: () -> Unit,
    showBackButton: Boolean = false
) {
    val strings = LocalAppStrings.current
    val rawLogs by DetectionLogStore.logs.collectAsState()
    var isPaused by remember { mutableStateOf(false) }
    var displayedLogs by remember { mutableStateOf(rawLogs) }
    var selectedFilter by remember { mutableStateOf(LogFilterType.ALL) }
    var expandedLogId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(rawLogs, isPaused) {
        if (!isPaused) {
            displayedLogs = rawLogs
        }
    }

    val filteredLogs = remember(displayedLogs, selectedFilter) {
        when (selectedFilter) {
            LogFilterType.SCREEN_CHANGE -> displayedLogs.filter { it.actionTaken == null }
            LogFilterType.ACTION -> displayedLogs.filter { it.actionTaken != null }
            LogFilterType.NODE_TREE -> displayedLogs.filter { it.keyNodes.isNotEmpty() }
            LogFilterType.ALL -> displayedLogs
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = NmrTheme.dimens.screenHorizontalMargin, vertical = 12.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))
        // Üst Bar: Geri oku, başlık, sağda duraklat ve temizle ikonları
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                if (showBackButton) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = strings.backButton,
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
                Column {
                    Text(
                        text = if (strings is com.nomorereels.app.ui.i18n.EnglishStrings)
                            "SYSTEM TRANSPARENCY"
                        else
                            "SİSTEM ŞEFFAFLIĞI",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.6.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = if (strings is com.nomorereels.app.ui.i18n.EnglishStrings)
                            "Live Diagnostics"
                        else
                            "Canlı Teşhis",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 25.sp,
                            lineHeight = 31.sp,
                            letterSpacing = (-0.4).sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isPaused) strings.logsPausedStatus else strings.logsActiveStatus(filteredLogs.size),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isPaused) NmrTheme.colors.statusOrange else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = { isPaused = !isPaused }) {
                    Icon(
                        imageVector = if (isPaused) Icons.Outlined.PlayArrow else Icons.Outlined.Pause,
                        contentDescription = if (isPaused) "Devam Et" else "Duraklat",
                        tint = if (isPaused) NmrTheme.colors.statusOrange else MaterialTheme.colorScheme.onBackground
                    )
                }
                IconButton(onClick = { DetectionLogStore.clear() }) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Temizle",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        // Filtre Çipleri
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val filterOptions = listOf(
                LogFilterType.ALL to strings.logsFilterAll,
                LogFilterType.SCREEN_CHANGE to strings.logsFilterScreenChange,
                LogFilterType.ACTION to strings.logsFilterAction,
                LogFilterType.NODE_TREE to strings.logsFilterNodeTree
            )
            filterOptions.forEach { (type, label) ->
                NmrChip(
                    label = label,
                    selected = selectedFilter == type,
                    onClick = { selectedFilter = type }
                )
            }
        }

        NmrHairlineDivider()
        Spacer(modifier = Modifier.height(8.dp))

        if (filteredLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = strings.logsEmptyTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = strings.logsEmptySub,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // Canlı Log Listesi
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredLogs, key = { it.id }) { logItem ->
                    val tag = when {
                        logItem.actionTaken != null -> "EYLEM"
                        logItem.screenType.isReels -> "ALGI"
                        else -> "EKRAN"
                    }
                    val badgeColor = when (tag) {
                        "EKRAN" -> MaterialTheme.colorScheme.outline
                        "ALGI" -> NmrTheme.colors.statusOrange
                        "EYLEM" -> NmrTheme.colors.statusGreen
                        else -> MaterialTheme.colorScheme.outline
                    }
                    val badgeContainer = when (tag) {
                        "EKRAN" -> MaterialTheme.colorScheme.surface
                        "ALGI" -> NmrTheme.colors.statusOrangeContainer
                        "EYLEM" -> NmrTheme.colors.statusGreenContainer
                        else -> MaterialTheme.colorScheme.surface
                    }

                    val isExpanded = expandedLogId == logItem.id

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                expandedLogId = if (isExpanded) null else logItem.id
                            },
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        tonalElevation = 0.dp
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = logItem.timeFormatted,
                                    style = NmrLogTextStyle,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = badgeContainer,
                                    tonalElevation = 0.dp
                                ) {
                                    Text(
                                        text = tag,
                                        style = NmrLogTextStyle.copy(fontWeight = FontWeight.Bold),
                                        color = badgeColor,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = logItem.details,
                                    style = NmrLogTextStyle,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            if (logItem.actionTaken != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "↳ Sonuç: ${logItem.actionTaken}",
                                    style = NmrLogTextStyle.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            if (logItem.keyNodes.isNotEmpty()) {
                                AnimatedVisibility(visible = isExpanded) {
                                    Column(modifier = Modifier.padding(top = 8.dp)) {
                                        NmrHairlineDivider()
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Algılama Düğümleri:",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        logItem.keyNodes.forEach { nodeInfo ->
                                            Text(
                                                text = "• $nodeInfo",
                                                style = NmrLogTextStyle,
                                                color = MaterialTheme.colorScheme.onBackground
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// EKRAN 9: DİYALOGLAR VE ALT PANEL (28.dp Radius)
// ============================================================================

// (a) "Özel sayı gir" Diyaloğu
@Composable
fun CustomNumberInputDialog(
    title: String,
    description: String,
    initialValue: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val strings = LocalAppStrings.current
    var textValue by remember { mutableStateOf(initialValue.toString()) }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(NmrTheme.dimens.dialogRadius),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = textValue,
                    onValueChange = {
                        textValue = it.filter { ch -> ch.isDigit() }
                        val num = textValue.toIntOrNull()
                        isError = num == null || num !in 1..9999
                    },
                    isError = isError,
                    label = { Text(strings.customNumberFieldLabel) },
                    supportingText = { Text(strings.numberRangeText) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val num = textValue.toIntOrNull()
                    if (num != null && num in 1..9999) {
                        onConfirm(num)
                    }
                },
                enabled = !isError && textValue.isNotEmpty(),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(text = strings.save, style = MaterialTheme.typography.titleMedium)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = strings.cancel, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            }
        }
    )
}

// (b) "İrade Kilidi" Diyaloğu (Geri sayımlı)
@Composable
fun WillpowerLockDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val strings = LocalAppStrings.current
    var secondsRemaining by remember { mutableIntStateOf(5) }

    LaunchedEffect(Unit) {
        while (secondsRemaining > 0) {
            delay(1000L)
            secondsRemaining--
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(NmrTheme.dimens.dialogRadius),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        title = {
            Text(
                text = strings.willpowerDialogTitle,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 5 saniyeden geriye sayan dairesel ilerleme halkası
                Box(
                    modifier = Modifier.size(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val strokeWidth = 5.dp
                    val primaryColor = MaterialTheme.colorScheme.primary
                    val outlineColor = MaterialTheme.colorScheme.outline

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = outlineColor,
                            style = Stroke(width = strokeWidth.toPx())
                        )
                        val sweep = (secondsRemaining / 5f) * 360f
                        drawArc(
                            color = primaryColor,
                            startAngle = -90f,
                            sweepAngle = sweep,
                            useCenter = false,
                            style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
                        )
                    }
                    Text(
                        text = "$secondsRemaining",
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Text(
                    text = strings.willpowerDialogDesc,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(text = strings.willpowerDismiss, style = MaterialTheme.typography.titleMedium)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onConfirm,
                enabled = secondsRemaining == 0
            ) {
                Text(
                    text = strings.willpowerConfirm,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (secondsRemaining == 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

// (b2) "Süreli Mola" Seçim Diyaloğu
@Composable
fun SnoozeSelectionDialog(
    onDismiss: () -> Unit,
    onSelectMinutes: (Int) -> Unit
) {
    val strings = LocalAppStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(NmrTheme.dimens.dialogRadius),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        title = {
            Text(
                text = strings.snoozeDialogTitle,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = strings.snoozeDialogDesc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                val snoozeOptions = listOf(
                    15 to strings.snooze15Min,
                    30 to strings.snooze30Min,
                    60 to strings.snooze60Min
                )
                snoozeOptions.forEach { (minutes, label) ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(NmrTheme.dimens.cardRadius))
                            .clickable { onSelectMinutes(minutes) },
                        shape = RoundedCornerShape(NmrTheme.dimens.cardRadius),
                        color = MaterialTheme.colorScheme.background,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        tonalElevation = 0.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Icon(
                                imageVector = Icons.Outlined.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = strings.cancel, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            }
        }
    )
}

// (c) "Mod seç" Alt Paneli (ModalBottomSheet - 28.dp Radius)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegionModeBottomSheet(
    regionTitle: String,
    currentMode: DmReelMode,
    onDismiss: () -> Unit,
    onSelectMode: (DmReelMode) -> Unit
) {
    val strings = LocalAppStrings.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = NmrTheme.dimens.dialogRadius, topEnd = NmrTheme.dimens.dialogRadius),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text(
                text = strings.modeSelectTitle,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = regionTitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))

            val options = listOf(
                DmReelMode.FOLLOW_GLOBAL to strings.modeFollowGlobal,
                DmReelMode.BLOCK_ALL to strings.blockAllTitle,
                DmReelMode.SINGLE_REEL to strings.singleReelTitle,
                DmReelMode.COUNT_QUOTA to strings.countQuotaTitle,
                DmReelMode.TIME_QUOTA to strings.timeQuotaTitle
            )

            options.forEach { (mode, label) ->
                val isSelected = currentMode == mode
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .clickable { onSelectMode(mode) }
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    RadioButton(
                        selected = isSelected,
                        onClick = { onSelectMode(mode) },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = MaterialTheme.colorScheme.primary,
                            unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
                NmrHairlineDivider()
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// Gece Odak Saati Düzenleme Dialogu
@Composable
fun BedtimeTimePickerDialog(
    startH: Int,
    startM: Int,
    endH: Int,
    endM: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int, Int, Int) -> Unit
) {
    val strings = LocalAppStrings.current
    var curStartH by remember { mutableIntStateOf(startH) }
    var curStartM by remember { mutableIntStateOf(startM) }
    var curEndH by remember { mutableIntStateOf(endH) }
    var curEndM by remember { mutableIntStateOf(endM) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(NmrTheme.dimens.dialogRadius),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        title = {
            Text(
                text = strings.bedtimeDialogTitle,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = strings.bedtimeDialogDesc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Başlangıç Saati
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = strings.bedtimeStartLabel, style = MaterialTheme.typography.titleMedium)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Button(
                            onClick = { curStartH = (curStartH + 23) % 24 },
                            shape = CircleShape,
                            modifier = Modifier.size(32.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                        ) { Text("-") }
                        Text(
                            text = String.format("%02d:%02d", curStartH, curStartM),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        Button(
                            onClick = { curStartH = (curStartH + 1) % 24 },
                            shape = CircleShape,
                            modifier = Modifier.size(32.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                        ) { Text("+") }
                    }
                }

                // Bitiş Saati
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = strings.bedtimeEndLabel, style = MaterialTheme.typography.titleMedium)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Button(
                            onClick = { curEndH = (curEndH + 23) % 24 },
                            shape = CircleShape,
                            modifier = Modifier.size(32.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                        ) { Text("-") }
                        Text(
                            text = String.format("%02d:%02d", curEndH, curEndM),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        Button(
                            onClick = { curEndH = (curEndH + 1) % 24 },
                            shape = CircleShape,
                            modifier = Modifier.size(32.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                        ) { Text("+") }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(curStartH, curStartM, curEndH, curEndM) },
                shape = CircleShape
            ) {
                Text(text = strings.save, style = MaterialTheme.typography.titleMedium)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = strings.cancel, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            }
        }
    )
}

// ============================================================================
// FARKINDALIK KAPISI (MINDFUL GATEWAY) DİYALOĞU
// ============================================================================

@Composable
fun MindfulGatewayDialog(
    barrierType: MindfulBarrierType,
    isNightMode: Boolean,
    onDismiss: () -> Unit,
    onApproved: (durationMinutes: Int, microPlan: String) -> Unit
) {
    val strings = LocalAppStrings.current

    val isDark = isNightMode || (MaterialTheme.colorScheme.background == AmoledCanvas)

    val cardBg = if (isDark) AmoledSurface else LightSurface
    val cardBorder = if (isDark) AmoledBorder else LightBorder
    val titleColor = if (isDark) AmoledTextPrimary else LightTextPrimary
    val textColor = if (isDark) AmoledTextMuted else LightTextMuted
    val badgeBg = if (isDark) AmoledAccent else LightAccent
    val badgeText = if (isDark) AmoledPrimary else LightPrimary
    val primaryBtnBg = if (isDark) AmoledPrimary else LightPrimary
    val primaryBtnText = if (isDark) AmoledSurface else Color.White
    val secondaryBtnBg = if (isDark) AmoledAccent else LightAccent
    val secondaryBtnText = if (isDark) AmoledPrimary else LightPrimary

    var step by remember { mutableIntStateOf(if (isNightMode) 99 else 0) }
    var selectedWindowMinutes by remember { mutableIntStateOf(5) }
    var autopilotNoticed by remember { mutableStateOf(false) }

    val editorialTitleStyle = MaterialTheme.typography.headlineMedium.copy(
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.3).sp
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = cardBg,
            border = BorderStroke(1.dp, cardBorder),
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Rozet
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = badgeBg
                ) {
                    Text(
                        text = if (isNightMode) "GECE KORUMA · NIGHT REST" else strings.mindfulGatewayHeader,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = badgeText,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isNightMode) {
                    Text(
                        text = strings.gatewayNightTitle,
                        style = editorialTitleStyle,
                        color = titleColor,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = strings.gatewayNightSub,
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryBtnBg),
                        shape = RoundedCornerShape(22.dp)
                    ) {
                        Text(strings.gatewayNightSleep, color = primaryBtnText, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { onApproved(5, strings.gatewayMicroPlanSleep) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(22.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = secondaryBtnBg)
                    ) {
                        Text(strings.gatewayNight5Min, color = secondaryBtnText, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(onClick = onDismiss) {
                        Text(strings.gatewayExitImmediate, color = textColor)
                    }
                } else if (step == 0) {
                    when (barrierType) {
                        MindfulBarrierType.BREATH_AND_INTENT -> {
                            Text(
                                text = strings.gatewayPauseTitle,
                                style = editorialTitleStyle,
                                color = titleColor,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = strings.gatewayBreathPrompt,
                                style = MaterialTheme.typography.bodyMedium,
                                color = textColor,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))

                            // Dalgalanan nefes halkası
                            val transition = rememberInfiniteTransition(label = "breath")
                            val scale by transition.animateFloat(
                                initialValue = 0.8f,
                                targetValue = 1.15f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(2200, easing = EaseInOut),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "scale"
                            )

                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .scale(scale)
                                    .background(secondaryBtnBg, CircleShape)
                                    .border(3.dp, primaryBtnBg, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(primaryBtnBg, CircleShape)
                                )
                            }

                            Spacer(modifier = Modifier.height(28.dp))

                            Button(
                                onClick = { step = 1 },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = primaryBtnBg),
                                shape = RoundedCornerShape(22.dp)
                            ) {
                                Text(
                                    if (strings is TurkishStrings) "Niyetimi Fark Et ➔" else "Notice My Intention ➔",
                                    color = primaryBtnText,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = onDismiss) {
                                Text(strings.gatewayExitImmediate, color = textColor)
                            }
                        }

                        MindfulBarrierType.COGNITIVE_MATH -> {
                            Text(
                                text = strings.barrierTypeMath,
                                style = editorialTitleStyle,
                                color = titleColor,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = strings.gatewayMathPrompt,
                                style = MaterialTheme.typography.bodyMedium,
                                color = textColor,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "14 + 19 = ?",
                                style = MaterialTheme.typography.headlineMedium,
                                color = primaryBtnBg,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(23 to false, 33 to true, 35 to false).forEach { (num, isCorrect) ->
                                    Button(
                                        onClick = { if (isCorrect) step = 2 },
                                        modifier = Modifier.weight(1f).height(48.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = secondaryBtnBg),
                                        shape = RoundedCornerShape(22.dp)
                                    ) {
                                        Text("$num", color = secondaryBtnText, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            TextButton(onClick = onDismiss) {
                                Text(strings.gatewayExitImmediate, color = textColor)
                            }
                        }

                        MindfulBarrierType.MINDFUL_TYPING -> {
                            Text(
                                text = strings.barrierTypeTyping,
                                style = editorialTitleStyle,
                                color = titleColor,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = strings.gatewayTypingPrompt,
                                style = MaterialTheme.typography.bodyMedium,
                                color = textColor,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            val target = strings.gatewayTypingSentence
                            Text(
                                text = "\"$target\"",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = primaryBtnBg,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            var typedText by remember { mutableStateOf("") }
                            OutlinedTextField(
                                value = typedText,
                                onValueChange = { typedText = it },
                                placeholder = { Text(if (strings is TurkishStrings) "Cümleyi buraya yaz..." else "Type sentence here...", color = textColor) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = titleColor,
                                    unfocusedTextColor = titleColor,
                                    focusedBorderColor = primaryBtnBg,
                                    unfocusedBorderColor = cardBorder
                                )
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            val isMatch = typedText.trim().equals(target.trim(), ignoreCase = true)
                            Button(
                                onClick = { step = 2 },
                                enabled = isMatch,
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = primaryBtnBg,
                                    disabledContainerColor = secondaryBtnBg
                                ),
                                shape = RoundedCornerShape(22.dp)
                            ) {
                                Text(
                                    if (strings is TurkishStrings) "Devam Et" else "Continue",
                                    color = if (isMatch) primaryBtnText else textColor
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = onDismiss) {
                                Text(strings.gatewayExitImmediate, color = textColor)
                            }
                        }
                    }
                } else if (step == 1) {
                    // Niyet Seçimi
                    if (autopilotNoticed) {
                        Text(
                            text = if (strings is TurkishStrings) "Farkındalık" else "Awareness",
                            style = editorialTitleStyle,
                            color = titleColor,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = strings.gatewayAutopilotAck,
                            style = MaterialTheme.typography.bodyMedium,
                            color = primaryBtnBg,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = primaryBtnBg),
                            shape = RoundedCornerShape(22.dp)
                        ) {
                            Text(strings.gatewayPutPhoneDown, color = primaryBtnText, fontWeight = FontWeight.SemiBold)
                        }
                    } else {
                        Text(
                            text = strings.gatewayIntentQuestion,
                            style = editorialTitleStyle,
                            color = titleColor,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (strings is TurkishStrings) "Yargılamadan sadece fark et." else "Notice your state without judgment.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(18.dp))

                        Button(
                            onClick = { autopilotNoticed = true },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(22.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = secondaryBtnBg)
                        ) {
                            Text("🍃 " + strings.gatewayIntentAutopilot, color = secondaryBtnText, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { step = 2 },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(22.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = secondaryBtnBg)
                        ) {
                            Text("☕ " + strings.gatewayIntentBreak, color = secondaryBtnText, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { step = 2 },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(22.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = secondaryBtnBg)
                        ) {
                            Text("🔍 " + strings.gatewayIntentSpecific, color = secondaryBtnText, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        TextButton(onClick = onDismiss) {
                            Text(strings.gatewayExitImmediate, color = textColor)
                        }
                    }
                } else if (step == 2) {
                    // Bilinçli Pencere Seçimi
                    Text(
                        text = if (strings is TurkishStrings) "Seçim Senin" else "Set a Mindful Window",
                        style = editorialTitleStyle,
                        color = titleColor,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (strings is TurkishStrings) "Şu an nasıl devam etmek istersin?" else "How would you like to continue right now?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryBtnBg),
                        shape = RoundedCornerShape(22.dp)
                    ) {
                        Text("📱 " + strings.gatewayPutPhoneDown, color = primaryBtnText, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                selectedWindowMinutes = 5
                                step = 3
                            },
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(22.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = secondaryBtnBg)
                        ) {
                            Text("⏱️ " + strings.gatewayMindful5Min, color = secondaryBtnText, fontWeight = FontWeight.SemiBold)
                        }
                        Button(
                            onClick = {
                                selectedWindowMinutes = 10
                                step = 3
                            },
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(22.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = secondaryBtnBg)
                        ) {
                            Text("⏱️ " + strings.gatewayMindful10Min, color = secondaryBtnText, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    TextButton(onClick = onDismiss) {
                        Text(strings.gatewayExitImmediate, color = textColor)
                    }
                } else if (step == 3) {
                    // Mikro-Plan Seçimi
                    Text(
                        text = "$selectedWindowMinutes " + (if (strings is TurkishStrings) "Dk Bilinçli Pencere" else "Min Mindful Window"),
                        style = editorialTitleStyle,
                        color = titleColor,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = strings.gatewayMicroPlanQuestion,
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    listOf(
                        "💧 " + strings.gatewayMicroPlanWater to strings.gatewayMicroPlanWater,
                        "💼 " + strings.gatewayMicroPlanWork to strings.gatewayMicroPlanWork,
                        "🛌 " + strings.gatewayMicroPlanSleep to strings.gatewayMicroPlanSleep,
                        "🚶 " + strings.gatewayMicroPlanWalk to strings.gatewayMicroPlanWalk
                    ).forEach { (display, plan) ->
                        Button(
                            onClick = {
                                onApproved(selectedWindowMinutes, plan)
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp).padding(vertical = 2.dp),
                            shape = RoundedCornerShape(22.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = secondaryBtnBg)
                        ) {
                            Text(display, color = secondaryBtnText, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    TextButton(onClick = onDismiss) {
                        Text(strings.gatewayExitImmediate, color = textColor)
                    }
                }
            }
        }
    }
}
