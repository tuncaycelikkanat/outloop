package com.nomorereels.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import com.nomorereels.app.data.QuotaStore
import com.nomorereels.app.data.SettingsStore
import com.nomorereels.app.detection.DetectionEventLog
import com.nomorereels.app.detection.DetectionLogStore
import com.nomorereels.app.detection.ScreenType
import com.nomorereels.app.policy.DmReelMode
import com.nomorereels.app.policy.ProtectionLevel
import com.nomorereels.app.service.ReelsAccessibilityService
import com.nomorereels.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ============================================================================
// UI STATE DATA HOLDER
// ============================================================================

enum class CameraCapsuleStatus(val text: String, val dotColor: Color) {
    REEL_BLOCKED("Reel engellendi", StatusRed),
    QUOTA_WARNING("Kotanın %80'i doldu · 16/20", StatusOrange),
    QUOTA_EXCEEDED("Kota doldu", StatusRed)
}

data class NoMoreReelsUiState(
    val hasCompletedOnboarding: Boolean = false,
    val onboardingStep: Int = 0,
    val currentTab: NmrBottomTab = NmrBottomTab.KORUMALAR,
    val showLiveLogsScreen: Boolean = false,
    val themeMode: NmrThemeMode = NmrThemeMode.LIGHT,
    val language: NmrLanguage = NmrLanguage.TR,
    val isServiceActive: Boolean = false,
    val generalMode: DmReelMode = DmReelMode.SINGLE_REEL,
    val blockCount: Int = 18,
    val savedMinutesStr: String = "13,5 dk",
    val selectedProfile: ProtectionLevel = ProtectionLevel.BALANCED,
    val dmMode: DmReelMode = DmReelMode.SINGLE_REEL,
    val exploreMode: DmReelMode = DmReelMode.FOLLOW_GLOBAL,
    val feedMode: DmReelMode = DmReelMode.BLOCK_ALL,
    val profileMode: DmReelMode = DmReelMode.FOLLOW_GLOBAL,
    val reelsTabMode: DmReelMode = DmReelMode.BLOCK_ALL,
    val regionBeingEdited: String? = null,
    val todayReelCount: Int = 12,
    val dailyCountLimit: Int = 20,
    val todayMinutesSpent: Int = 8,
    val dailyTimeLimitMinutes: Int = 30,
    val warningPercent: Int = 80,
    val singleReelMaxSeconds: Int = 90,
    val bedtimeEnabled: Boolean = true,
    val bedtimeStartHour: Int = 23,
    val bedtimeStartMinute: Int = 30,
    val bedtimeEndHour: Int = 7,
    val bedtimeEndMinute: Int = 0,
    val strictModeEnabled: Boolean = true,
    val punchHoleAlertEnabled: Boolean = true,
    val cameraCapsuleStatus: CameraCapsuleStatus? = null,
    val showCustomCountDialog: Boolean = false,
    val showCustomTimeDialog: Boolean = false,
    val showWillpowerDialog: Boolean = false,
    val showBedtimePickerDialog: Boolean = false,
    val logFilter: String = "Ekran değişimi",
    val isLogsPaused: Boolean = false,
    val isNodeTreeExpanded: Boolean = false
)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        SettingsStore.init(this)
        QuotaStore.init(this)

        setContent {
            val appThemeMode by SettingsStore.appThemeMode.collectAsState()
            val nmrThemeMode = when (appThemeMode) {
                AppThemeMode.LIGHT -> NmrThemeMode.LIGHT
                AppThemeMode.DARK -> NmrThemeMode.AMOLED
                AppThemeMode.SYSTEM -> NmrThemeMode.SYSTEM
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
    var currentTab by remember { mutableStateOf(NmrBottomTab.KORUMALAR) }
    var showLiveLogsScreen by remember { mutableStateOf(false) }

    val appThemeMode by SettingsStore.appThemeMode.collectAsState()
    val appLanguage by SettingsStore.appLanguage.collectAsState()

    val isServiceActive by ReelsAccessibilityService.isServiceActive.collectAsState()
    val blockCount by ReelsAccessibilityService.actionExecutor.blockCount.collectAsState()
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
    val punchHoleAlertEnabled by SettingsStore.punchHoleAlertEnabled.collectAsState()

    var regionBeingEdited by remember { mutableStateOf<String?>(null) }
    var showCustomCountDialog by remember { mutableStateOf(false) }
    var showCustomTimeDialog by remember { mutableStateOf(false) }
    var showWillpowerDialog by remember { mutableStateOf(false) }
    var pendingLoosenAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var showBedtimePickerDialog by remember { mutableStateOf(false) }
    var cameraCapsuleStatus by remember { mutableStateOf<CameraCapsuleStatus?>(null) }

    val currentThemeMode = when (appThemeMode) {
        AppThemeMode.LIGHT -> NmrThemeMode.LIGHT
        AppThemeMode.DARK -> NmrThemeMode.AMOLED
        AppThemeMode.SYSTEM -> NmrThemeMode.SYSTEM
    }
    val currentLanguage = when (appLanguage) {
        AppLanguage.TR -> NmrLanguage.TR
        AppLanguage.EN -> NmrLanguage.EN
    }

    val requestModeChange: (DmReelMode, DmReelMode, () -> Unit) -> Unit = { target, current, action ->
        if (strictModeEnabled && target == DmReelMode.FREE && current != DmReelMode.FREE) {
            pendingLoosenAction = action
            showWillpowerDialog = true
        } else {
            action()
        }
    }

    // Ekran 10 Kapsül zamanlayıcısı (Test butonuna basıldığında)
    LaunchedEffect(cameraCapsuleStatus) {
        if (cameraCapsuleStatus != null) {
            delay(3200L)
            cameraCapsuleStatus = null
        }
    }

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
            LiveLogsScreen(onBack = { showLiveLogsScreen = false })
        } else {
            // 4 ANA SEKME AKIŞI (Ekran 4, 5, 6, 7)
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when (currentTab) {
                        NmrBottomTab.KORUMALAR -> {
                            ScreenKorumalar(
                                isServiceActive = isServiceActive,
                                generalMode = generalMode,
                                blockCount = blockCount,
                                strictModeEnabled = strictModeEnabled,
                                onOpenAccessibilitySettings = {
                                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                                },
                                onGeneralModeChange = { newMode ->
                                    requestModeChange(newMode, generalMode) {
                                        SettingsStore.setGeneralMode(context, newMode)
                                        if (newMode == DmReelMode.COUNT_QUOTA) QuotaStore.setDailyCountLimitEnabled(context, true)
                                        if (newMode == DmReelMode.TIME_QUOTA) QuotaStore.setDailyTimeLimitEnabled(context, true)
                                    }
                                },
                                onApplyProfile = { profile ->
                                    val currentPreset = ProtectionLevel.findMatchingLevel(dmMode, exploreMode, feedMode, profileMode, reelsTabMode)
                                    if (strictModeEnabled && currentPreset == ProtectionLevel.STRICT && profile != ProtectionLevel.STRICT) {
                                        pendingLoosenAction = { SettingsStore.applyProtectionLevel(context, profile) }
                                        showWillpowerDialog = true
                                    } else {
                                        SettingsStore.applyProtectionLevel(context, profile)
                                    }
                                }
                            )
                        }
                        NmrBottomTab.BOLGELER -> {
                            ScreenBolgeler(
                                dmMode = dmMode,
                                exploreMode = exploreMode,
                                feedMode = feedMode,
                                profileMode = profileMode,
                                reelsTabMode = reelsTabMode,
                                onSelectRegion = { regionKey -> regionBeingEdited = regionKey }
                            )
                        }
                        NmrBottomTab.KOTA_SURE -> {
                            ScreenKotaSure(
                                blockCount = blockCount,
                                todayReelCount = todayReelCount,
                                dailyCountLimit = dailyCountLimit,
                                todayMinutesSpent = (todayTimeSpentSeconds / 60).toInt(),
                                dailyTimeLimitMinutes = dailyTimeLimitMinutes,
                                warningPercent = warningPercent,
                                singleReelMaxSeconds = singleReelMaxSeconds,
                                onCountLimitChange = { QuotaStore.setDailyCountLimit(context, it) },
                                onTimeLimitChange = { QuotaStore.setDailyTimeLimitMinutes(context, it) },
                                onWarningPercentChange = { QuotaStore.setWarningPercent(context, it) },
                                onSingleReelTimeoutChange = { QuotaStore.setSingleReelMaxSeconds(context, it) },
                                onOpenCustomCount = { showCustomCountDialog = true },
                                onOpenCustomTime = { showCustomTimeDialog = true }
                            )
                        }
                        NmrBottomTab.AYARLAR -> {
                            ScreenAyarlar(
                                language = currentLanguage,
                                themeMode = currentThemeMode,
                                bedtimeEnabled = bedtimeEnabled,
                                bedtimeStartHour = bedtimeStartHour,
                                bedtimeStartMinute = bedtimeStartMinute,
                                bedtimeEndHour = bedtimeEndHour,
                                bedtimeEndMinute = bedtimeEndMinute,
                                strictModeEnabled = strictModeEnabled,
                                punchHoleAlertEnabled = punchHoleAlertEnabled,
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
                                onBedtimeToggle = { SettingsStore.setBedtimeModeEnabled(context, it) },
                                onOpenBedtimePicker = { showBedtimePickerDialog = true },
                                onStrictModeToggle = { SettingsStore.setStrictModeEnabled(context, it) },
                                onPunchHoleToggle = { SettingsStore.setPunchHoleAlertEnabled(context, it) },
                                onTestPunchHole = {
                                    // Döngüsel 3 durum arasında test simülasyonu
                                    val nextState = when (cameraCapsuleStatus) {
                                        null -> CameraCapsuleStatus.REEL_BLOCKED
                                        CameraCapsuleStatus.REEL_BLOCKED -> CameraCapsuleStatus.QUOTA_WARNING
                                        CameraCapsuleStatus.QUOTA_WARNING -> CameraCapsuleStatus.QUOTA_EXCEEDED
                                        CameraCapsuleStatus.QUOTA_EXCEEDED -> CameraCapsuleStatus.REEL_BLOCKED
                                    }
                                    cameraCapsuleStatus = nextState

                                    ReelsAccessibilityService.overlayController.showPunchHolePill(
                                        title = "No More Reels",
                                        subtitle = nextState.text,
                                        accentColor = 0xFF1E6B5C.toInt(),
                                        fallbackContext = context,
                                        durationMs = 3000L
                                    )
                                },
                                onOpenLiveLogs = { showLiveLogsScreen = true }
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

        // ====================================================================
        // EKRAN 10 — NOKTA KAMERA KAPSÜLÜ OVERLAY (Ekranın üst ortasında)
        // ====================================================================
        AnimatedVisibility(
            visible = cameraCapsuleStatus != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp)
        ) {
            cameraCapsuleStatus?.let { capsule ->
                NmrCameraCapsuleOverlay(
                    text = capsule.text,
                    dotColor = capsule.dotColor
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
            title = "Özel sayı gir",
            description = "Günlük en fazla kaç Reel izlenebilsin?",
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
            title = "Özel dakika gir",
            description = "Günlük en fazla kaç dakika Reels izlenebilsin?",
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

    // (c) "Mod seç" Alt Paneli (ModalBottomSheet)
    if (regionBeingEdited != null) {
        val regionKey = regionBeingEdited!!
        val regionTitle = when (regionKey) {
            "DM" -> "DM / Mesajlar"
            "EXPLORE" -> "Keşfet Izgarası"
            "FEED" -> "Ana Sayfa / Akış"
            "PROFILE" -> "Profil"
            "REELS_TAB" -> "Alt Bar Reels Sekmesi"
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
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NmrFilledButton(
                text = when (step) {
                    0 -> "Devam"
                    1 -> if (isServiceActive) "Devam" else "Erişilebilirlik ayarlarını aç"
                    else -> "Başla"
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
                        text = "Geri",
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
            text = "Sıfır internet. Sıfır takip.",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))
        NmrHairlineDivider()

        PrivacyFeatureRow(
            title = "İnternet izni bile yok",
            subtitle = "Uygulama dışarıya tek bayt gönderemez."
        )
        NmrHairlineDivider()

        PrivacyFeatureRow(
            title = "Verin telefonundan asla çıkmaz",
            subtitle = "Sunucu yok, telemetri yok, hesap yok."
        )
        NmrHairlineDivider()

        PrivacyFeatureRow(
            title = "Açık kaynak ve şeffaf",
            subtitle = "Kodun tamamını GitHub'da inceleyebilirsin."
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
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Tek izin: Erişilebilirlik",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Yalnızca Instagram'da hangi ekranda olduğunu anlar. Mesajlarını veya içeriğini okumaz.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Numaralı dikey adımlar
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            NumberedStepRow(stepNumber = 1, text = "Ayarlar'ı aç")
            NumberedStepRow(stepNumber = 2, text = "No More Reels'i seç")
            NumberedStepRow(stepNumber = 3, text = "Servisi aç")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Durum Çipi (Turuncu StatusOrange: "İzin bekleniyor")
        if (!isServiceActive) {
            NmrStatusChip(
                text = "İzin bekleniyor",
                statusColor = NmrTheme.colors.statusOrange
            )
        } else {
            NmrStatusChip(
                text = "İzin verildi",
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
    var selectedPreset by remember { mutableStateOf(ProtectionLevel.BALANCED) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Bir profil seç",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 3 Seçilebilir Profil Satırı
        NmrSelectableRow(
            title = "Dengeli",
            subtitle = "Günlük kotayla dengeli izleme",
            icon = NmrIcons.Dengeli,
            selected = selectedPreset == ProtectionLevel.BALANCED,
            onClick = {
                selectedPreset = ProtectionLevel.BALANCED
                onSelectProfile(ProtectionLevel.BALANCED)
            },
            useRadioIndicator = false
        )

        NmrSelectableRow(
            title = "Bilinçli",
            subtitle = "Gevşetmek için 5 sn basılı tutma",
            icon = NmrIcons.Bilincli,
            selected = selectedPreset == ProtectionLevel.MINDFUL,
            onClick = {
                selectedPreset = ProtectionLevel.MINDFUL
                onSelectProfile(ProtectionLevel.MINDFUL)
            },
            useRadioIndicator = false
        )

        NmrSelectableRow(
            title = "Komple Yasak",
            subtitle = "Sıfır video",
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
            text = "Sonra istediğin gibi değiştirebilirsin.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ============================================================================
// EKRAN 4: SEKME 1 — "KORUMALAR"
// ============================================================================

@Composable
fun ScreenKorumalar(
    isServiceActive: Boolean,
    generalMode: DmReelMode,
    blockCount: Int,
    strictModeEnabled: Boolean,
    onOpenAccessibilitySettings: () -> Unit,
    onGeneralModeChange: (DmReelMode) -> Unit,
    onApplyProfile: (ProtectionLevel) -> Unit
) {
    val savedSeconds = blockCount * 45L
    val savedMinutes = savedSeconds / 60
    val savedTimeDisplay = if (blockCount > 0) "${savedMinutes},5 dk kurtarıldı" else "13,5 dk kurtarıldı"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = NmrTheme.dimens.screenHorizontalMargin),
        verticalArrangement = Arrangement.spacedBy(NmrTheme.dimens.gridUnit)
    ) {
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "No More Reels",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // NmrHeroCard
        item {
            NmrHeroCard {
                // Üst satır
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isServiceActive) "Koruma aktif" else "Koruma kapalı",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Genel mod: ${generalMode.title}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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

                Spacer(modifier = Modifier.height(16.dp))
                NmrHairlineDivider()
                Spacer(modifier = Modifier.height(16.dp))

                // Sayaç satırı
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = if (blockCount > 0) "$blockCount" else "18",
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Column {
                        Text(
                            text = "Reel bugün engellendi",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = savedTimeDisplay,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // GENEL MOD Başlığı
        item {
            NmrSectionHeader(title = "GENEL MOD")
        }

        // 4 Mod Satırı
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                NmrSelectableRow(
                    title = "Komple Yasak",
                    subtitle = "Sıfır video",
                    icon = NmrIcons.KompleYasak,
                    selected = generalMode == DmReelMode.BLOCK_ALL,
                    onClick = { onGeneralModeChange(DmReelMode.BLOCK_ALL) }
                )
                NmrSelectableRow(
                    title = "Sadece Bir Tane",
                    subtitle = "İlk video serbest, kaydırınca kapanır",
                    icon = NmrIcons.SadeceBirTane,
                    selected = generalMode == DmReelMode.SINGLE_REEL,
                    onClick = { onGeneralModeChange(DmReelMode.SINGLE_REEL) }
                )
                NmrSelectableRow(
                    title = "X Tane",
                    subtitle = "Günlük adet kadar",
                    icon = NmrIcons.XTane,
                    selected = generalMode == DmReelMode.COUNT_QUOTA,
                    onClick = { onGeneralModeChange(DmReelMode.COUNT_QUOTA) }
                )
                NmrSelectableRow(
                    title = "X Dakika",
                    subtitle = "Günlük süre kadar",
                    icon = NmrIcons.XDakika,
                    selected = generalMode == DmReelMode.TIME_QUOTA,
                    onClick = { onGeneralModeChange(DmReelMode.TIME_QUOTA) }
                )
            }
        }

        // HAZIR PROFİLLER Başlığı
        item {
            NmrSectionHeader(title = "HAZIR PROFİLLER")
        }

        // Hazır Profiller Çipleri
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(NmrTheme.dimens.gridUnit)
            ) {
                NmrChip(
                    label = "Dengeli",
                    selected = false,
                    onClick = { onApplyProfile(ProtectionLevel.BALANCED) },
                    modifier = Modifier.weight(1f)
                )
                NmrChip(
                    label = "Bilinçli",
                    selected = false,
                    onClick = { onApplyProfile(ProtectionLevel.MINDFUL) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// ============================================================================
// EKRAN 5: SEKME 2 — "BÖLGELER"
// ============================================================================

@Composable
fun ScreenBolgeler(
    dmMode: DmReelMode,
    exploreMode: DmReelMode,
    feedMode: DmReelMode,
    profileMode: DmReelMode,
    reelsTabMode: DmReelMode,
    onSelectRegion: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = NmrTheme.dimens.screenHorizontalMargin),
        verticalArrangement = Arrangement.spacedBy(NmrTheme.dimens.gridUnit)
    ) {
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Bölgeler",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Instagram'ın her bölgesi için ayrı kural",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 5 Bölge Satırı (NmrHairlineDivider ile ayrılmış düz yüzeyler)
        item {
            Column {
                RegionSettingRow(
                    title = "DM / Mesajlar",
                    subtitle = "Sohbetten gelen reeller",
                    icon = NmrIcons.Dm,
                    modeLabel = dmMode.title,
                    onClick = { onSelectRegion("DM") }
                )
                NmrHairlineDivider()

                RegionSettingRow(
                    title = "Keşfet Izgarası",
                    subtitle = "Izgara serbest, reele dokununca kural",
                    icon = NmrIcons.Kesfet,
                    modeLabel = exploreMode.title,
                    onClick = { onSelectRegion("EXPLORE") }
                )
                NmrHairlineDivider()

                RegionSettingRow(
                    title = "Ana Sayfa / Akış",
                    subtitle = "Gönderiler serbest, Reels açılınca kural",
                    icon = NmrIcons.AnaSayfa,
                    modeLabel = feedMode.title,
                    onClick = { onSelectRegion("FEED") }
                )
                NmrHairlineDivider()

                RegionSettingRow(
                    title = "Profil",
                    subtitle = "Profildeki video sekmesi",
                    icon = NmrIcons.Profil,
                    modeLabel = profileMode.title,
                    onClick = { onSelectRegion("PROFILE") }
                )
                NmrHairlineDivider()

                RegionSettingRow(
                    title = "Alt Bar Reels Sekmesi",
                    subtitle = "Basınca Ana Sayfa'ya yönlendirilir",
                    icon = NmrIcons.ReelsSekmesi,
                    modeLabel = reelsTabMode.title,
                    onClick = { onSelectRegion("REELS_TAB") }
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.size(NmrTheme.dimens.iconSize)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
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
        Spacer(modifier = Modifier.width(12.dp))
        // Sağ etiket
        Surface(
            shape = RoundedCornerShape(NmrTheme.dimens.chipRadius),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            tonalElevation = 0.dp
        ) {
            Text(
                text = modeLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }
}

// ============================================================================
// EKRAN 6: SEKME 3 — "KOTA & SÜRE"
// ============================================================================

@Composable
fun ScreenKotaSure(
    blockCount: Int,
    todayReelCount: Int,
    dailyCountLimit: Int,
    todayMinutesSpent: Int,
    dailyTimeLimitMinutes: Int,
    warningPercent: Int,
    singleReelMaxSeconds: Int,
    onCountLimitChange: (Int) -> Unit,
    onTimeLimitChange: (Int) -> Unit,
    onWarningPercentChange: (Int) -> Unit,
    onSingleReelTimeoutChange: (Int) -> Unit,
    onOpenCustomCount: () -> Unit,
    onOpenCustomTime: () -> Unit
) {
    var warningSliderValue by remember(warningPercent) { mutableFloatStateOf(warningPercent.toFloat()) }
    var singleReelSliderValue by remember(singleReelMaxSeconds) { mutableFloatStateOf(singleReelMaxSeconds.toFloat()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = NmrTheme.dimens.screenHorizontalMargin),
        verticalArrangement = Arrangement.spacedBy(NmrTheme.dimens.gridUnit)
    ) {
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Kökle Kotanı",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // NmrHeroCard: KURTARILAN ODAK SÜRESİ
        item {
            NmrHeroCard {
                Text(
                    text = "KURTARILAN ODAK SÜRESİ",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (blockCount > 0) "$blockCount Reel = ${(blockCount * 45) / 60},5 dk kurtarıldı" else "18 Reel = 13,5 dk kurtarıldı",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Engellenen her Reel için ortalama 45 sn",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))
                NmrHairlineDivider()
                Spacer(modifier = Modifier.height(16.dp))

                // İlerleme Çubuğu 1: Reel kotası (12 / 20) - StatusOrange
                val countDisplay = if (todayReelCount > 0) todayReelCount else 12
                val limitDisplay = if (dailyCountLimit > 0) dailyCountLimit else 20
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Reel kotası",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "$countDisplay / $limitDisplay",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { (countDisplay.toFloat() / limitDisplay.toFloat()).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = NmrTheme.colors.statusOrange,
                    trackColor = MaterialTheme.colorScheme.outline
                )

                Spacer(modifier = Modifier.height(14.dp))

                // İlerleme Çubuğu 2: Süre (8 / 30 dk) - StatusGreen
                val minDisplay = if (todayMinutesSpent > 0) todayMinutesSpent else 8
                val minLimitDisplay = if (dailyTimeLimitMinutes > 0) dailyTimeLimitMinutes else 30
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Süre",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "$minDisplay / $minLimitDisplay dk",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { (minDisplay.toFloat() / minLimitDisplay.toFloat()).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = NmrTheme.colors.statusGreen,
                    trackColor = MaterialTheme.colorScheme.outline
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Her gece 00:00'da sıfırlanır",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // GÜNLÜK REEL SINIRI
        item {
            NmrSectionHeader(title = "GÜNLÜK REEL SINIRI")
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
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
                    label = if (isCustom) "$dailyCountLimit" else "Özel",
                    selected = isCustom,
                    onClick = onOpenCustomCount
                )
            }
        }

        // GÜNLÜK SÜRE SINIRI
        item {
            NmrSectionHeader(title = "GÜNLÜK SÜRE SINIRI")
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(5, 15, 30, 60).forEach { mins ->
                    NmrChip(
                        label = "$mins dk",
                        selected = dailyTimeLimitMinutes == mins,
                        onClick = { onTimeLimitChange(mins) }
                    )
                }
                val isTimeCustom = dailyTimeLimitMinutes !in listOf(5, 15, 30, 60)
                NmrChip(
                    label = if (isTimeCustom) "$dailyTimeLimitMinutes dk" else "Özel",
                    selected = isTimeCustom,
                    onClick = onOpenCustomTime
                )
            }
        }

        // Slider 1: Uyarı eşiği (%80, aralık 50..90)
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Uyarı eşiği",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "%${warningSliderValue.toInt()}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
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
                    inactiveTrackColor = MaterialTheme.colorScheme.outline
                )
            )
        }

        // Slider 2: Tek Reel süre sınırı (90 sn, aralık 30..180)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tek Reel süre sınırı",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "${singleReelSliderValue.toInt()} sn",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
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
                    inactiveTrackColor = MaterialTheme.colorScheme.outline
                )
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ============================================================================
// EKRAN 7: SEKME 4 — "AYARLAR"
// ============================================================================

@Composable
fun ScreenAyarlar(
    language: NmrLanguage,
    themeMode: NmrThemeMode,
    bedtimeEnabled: Boolean,
    bedtimeStartHour: Int,
    bedtimeStartMinute: Int,
    bedtimeEndHour: Int,
    bedtimeEndMinute: Int,
    strictModeEnabled: Boolean,
    punchHoleAlertEnabled: Boolean,
    onLanguageChange: (NmrLanguage) -> Unit,
    onThemeChange: (NmrThemeMode) -> Unit,
    onBedtimeToggle: (Boolean) -> Unit,
    onOpenBedtimePicker: () -> Unit,
    onStrictModeToggle: (Boolean) -> Unit,
    onPunchHoleToggle: (Boolean) -> Unit,
    onTestPunchHole: () -> Unit,
    onOpenLiveLogs: () -> Unit
) {
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
        verticalArrangement = Arrangement.spacedBy(NmrTheme.dimens.gridUnit)
    ) {
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Ayarlar",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        // BÖLÜM: GÖRÜNÜM
        item {
            NmrSectionHeader(title = "GÖRÜNÜM")

            // Dil Satırı
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Dil",
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
                    text = "Tema",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                NmrSegmentedControl(
                    options = listOf(NmrThemeMode.LIGHT, NmrThemeMode.SYSTEM, NmrThemeMode.AMOLED),
                    selectedOption = themeMode,
                    onOptionSelected = onThemeChange,
                    labelProvider = { it.labelTr }
                )
            }
        }

        // BÖLÜM: KORUMA
        item {
            NmrSectionHeader(title = "KORUMA")

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
                            text = "Gece Odak Saati",
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
                            text = "İrade Kilidi",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Gevşetmeden önce 5 sn bekle",
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

            // Nokta Kamera Kapsülü
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = NmrIcons.KameraKapsulu,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(NmrTheme.dimens.iconSize)
                    )
                    Column {
                        Text(
                            text = "Nokta kamera kapsülü",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Kamera deliğinde bildirim",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Test Et Tonal Butonu
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(NmrTheme.dimens.chipRadius))
                            .clickable(onClick = onTestPunchHole),
                        shape = RoundedCornerShape(NmrTheme.dimens.chipRadius),
                        color = NmrTheme.colors.selectedSurface,
                        tonalElevation = 0.dp
                    ) {
                        Text(
                            text = "Test Et",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }

                    Switch(
                        checked = punchHoleAlertEnabled,
                        onCheckedChange = onPunchHoleToggle,
                        colors = switchColors
                    )
                }
            }
        }

        // BÖLÜM: GELİŞTİRİCİ
        item {
            NmrSectionHeader(title = "GELİŞTİRİCİ")

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .clickable(onClick = onOpenLiveLogs),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Canlı Olay Günlüğü",
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
                text = "Sürüm 1.0 · Açık kaynak",
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

@Composable
fun LiveLogsScreen(
    onBack: () -> Unit
) {
    val liveLogs by DetectionLogStore.logs.collectAsState()
    var isPaused by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("Ekran değişimi") }
    var isNodeTreeExpanded by remember { mutableStateOf(false) }

    // Sabit referans loglar (Geliştirici ekranı örneği)
    val sampleLogs = listOf(
        Triple("14:02:11.230", "EKRAN", "Ana Sayfa"),
        Triple("14:02:12.040", "EKRAN", "Reels sekmesi"),
        Triple("14:02:12.060", "ALGI", "Reels oynatıcısı"),
        Triple("14:02:12.080", "EYLEM", "GERİ tetiklendi"),
        Triple("14:02:12.310", "EKRAN", "Ana Sayfa"),
        Triple("14:02:15.900", "EKRAN", "DM sohbeti"),
        Triple("14:02:19.420", "EKRAN", "DM oynatıcı"),
        Triple("14:02:19.440", "ALGI", "İlk reel serbest (DM)"),
        Triple("14:02:24.870", "ALGI", "İkinci reel kaydırıldı"),
        Triple("14:02:24.890", "EYLEM", "GERİ tetiklendi")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = NmrTheme.dimens.screenHorizontalMargin, vertical = 12.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))
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
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Geri",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    text = "Canlı Olay Günlüğü",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = { isPaused = !isPaused }) {
                    Icon(
                        imageVector = if (isPaused) Icons.Outlined.PlayArrow else Icons.Outlined.Pause,
                        contentDescription = "Duraklat",
                        tint = MaterialTheme.colorScheme.onBackground
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

        // 3 Filtre Çipi
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Ekran değişimi", "Düğüm ağacı", "Eylemler").forEach { filter ->
                NmrChip(
                    label = filter,
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter }
                )
            }
        }

        NmrHairlineDivider()
        Spacer(modifier = Modifier.height(8.dp))

        // Log Listesi (JetBrains Mono 12sp + Renkli Rozetler)
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(sampleLogs) { (time, tag, msg) ->
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

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = time,
                        style = NmrLogTextStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(10.dp))
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
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = msg,
                        style = NmrLogTextStyle,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Açılır/kapanır "Düğüm ağacı (derinlik ≤ 6)" kartı
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(NmrTheme.dimens.cardRadius),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            tonalElevation = 0.dp
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isNodeTreeExpanded = !isNodeTreeExpanded },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Düğüm ağacı (derinlik ≤ 6)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = if (isNodeTreeExpanded) "∧" else "∨",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                AnimatedVisibility(visible = isNodeTreeExpanded) {
                    Column(modifier = Modifier.padding(top = 10.dp)) {
                        Text(
                            text = "FrameLayout\n  ViewGroup\n    TabLayout\n      ImageView · seçili\n    RecyclerView\n      FrameLayout\n        TextureView · Reels oynatıcısı",
                            style = NmrLogTextStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                    label = { Text("Günlük Reel sınırı") },
                    supportingText = { Text("1 – 9999") },
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
                Text(text = "Kaydet", style = MaterialTheme.typography.titleMedium)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "İptal", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
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
                text = "İrade Kilidi",
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
                    text = "Emin misin? Korumayı şimdi gevşetmek istiyor musun?",
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
                Text(text = "Vazgeç", style = MaterialTheme.typography.titleMedium)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onConfirm,
                enabled = secondsRemaining == 0
            ) {
                Text(
                    text = "Yine de gevşet",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (secondsRemaining == 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                text = "Mod seç",
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
                DmReelMode.FOLLOW_GLOBAL to "Genel moda uy",
                DmReelMode.BLOCK_ALL to "Komple Yasak",
                DmReelMode.SINGLE_REEL to "Sadece Bir Tane",
                DmReelMode.COUNT_QUOTA to "X Tane",
                DmReelMode.TIME_QUOTA to "X Dakika"
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
                text = "Gece Odak Saati",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Bu saatler arasında tüm Instagram Reels videoları istisnasız engellenir:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Başlangıç Saati
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Başlangıç (Uyku):", style = MaterialTheme.typography.titleMedium)
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
                    Text(text = "Bitiş (Uyanış):", style = MaterialTheme.typography.titleMedium)
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
                Text(text = "Kaydet", style = MaterialTheme.typography.titleMedium)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "İptal", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            }
        }
    )
}
