package com.nomorereels.app.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nomorereels.app.billing.DonationEvent
import com.nomorereels.app.billing.DonationManager
import com.nomorereels.app.data.AppLanguage
import com.nomorereels.app.data.AppThemeMode
import com.nomorereels.app.data.HeatmapStore
import com.nomorereels.app.data.QuotaStore
import com.nomorereels.app.data.SettingsStore
import com.nomorereels.app.di.AppViewModelProvider
import com.nomorereels.app.policy.DmReelMode
import com.nomorereels.app.service.ReelsAccessibilityService
import com.nomorereels.app.ui.dialogs.BedtimeTimePickerDialog
import com.nomorereels.app.ui.dialogs.CustomNumberInputDialog
import com.nomorereels.app.ui.dialogs.MindfulGatewayDialog
import com.nomorereels.app.ui.dialogs.RegionModeBottomSheet
import com.nomorereels.app.ui.dialogs.SnoozeSelectionDialog
import com.nomorereels.app.ui.i18n.AppStrings
import com.nomorereels.app.ui.i18n.LocalAppStrings
import com.nomorereels.app.ui.insights.InsightsScreen
import com.nomorereels.app.ui.insights.InsightsViewModel
import com.nomorereels.app.ui.onboarding.OnboardingFlow
import com.nomorereels.app.ui.protection.ProtectionScreen
import com.nomorereels.app.ui.protection.ProtectionViewModel
import com.nomorereels.app.ui.settings.SettingsScreen
import com.nomorereels.app.ui.settings.SettingsViewModel
import com.nomorereels.app.ui.theme.NmrBottomNavigationBar
import com.nomorereels.app.ui.theme.NmrBottomTab
import com.nomorereels.app.ui.theme.NmrLanguage
import com.nomorereels.app.ui.theme.NmrThemeMode
import com.nomorereels.app.ui.theme.NoMoreReelsTheme
import com.nomorereels.app.ui.today.TodayScreen
import com.nomorereels.app.ui.today.TodayViewModel

class MainActivity : ComponentActivity() {

    private val donationManager: DonationManager
        get() = (application as com.nomorereels.app.OutloopApplication).container.donationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        SettingsStore.init(this)
        QuotaStore.init(this)
        HeatmapStore.init(this)
        donationManager.startConnection()
        com.nomorereels.app.notification.FeedbackNotificationHelper.ensureChannel(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

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
                    NoMoreReelsAppRoot(donationManager = donationManager)
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
fun NoMoreReelsAppRoot(donationManager: DonationManager? = null) {
    val context = LocalContext.current

    val hasCompletedOnboarding by SettingsStore.hasCompletedOnboarding.collectAsState()
    var onboardingStep by rememberSaveable { mutableIntStateOf(0) }
    var showAppWalkthrough by rememberSaveable { mutableStateOf(false) }
    var walkthroughStep by rememberSaveable { mutableIntStateOf(0) }
    var currentTabName by rememberSaveable { mutableStateOf(NmrBottomTab.BUGUN.name) }
    val currentTab = NmrBottomTab.entries.firstOrNull { it.name == currentTabName } ?: NmrBottomTab.BUGUN

    val appLanguage by SettingsStore.appLanguage.collectAsState()
    val isServiceActive by ReelsAccessibilityService.isServiceActive.collectAsState()
    val currentStrings = AppStrings.get(appLanguage)
    val isEnglish = (appLanguage == AppLanguage.EN)

    val currentLanguage = when (appLanguage) {
        AppLanguage.TR -> NmrLanguage.TR
        AppLanguage.EN -> NmrLanguage.EN
    }

    val todayViewModel: TodayViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val protectionViewModel: ProtectionViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val insightsViewModel: InsightsViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val settingsViewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory)

    // Dialog & BottomSheet state
    var regionBeingEdited by remember { mutableStateOf<String?>(null) }
    var showCustomCountDialog by remember { mutableStateOf(false) }
    var showCustomTimeDialog by remember { mutableStateOf(false) }
    var showBedtimePickerDialog by remember { mutableStateOf(false) }
    var showSnoozeDialog by remember { mutableStateOf(false) }
    var showMindfulGatewayDialog by remember { mutableStateOf(false) }
    var showSupportSheet by remember { mutableStateOf(false) }
    var showThankYouDialog by remember { mutableStateOf(false) }

    val isSupporter by SettingsStore.isSupporter.collectAsState()
    val supporterDonationCount by SettingsStore.supporterDonationCount.collectAsState()
    val donationItems = remember(donationManager) {
        donationManager?.defaultItems ?: emptyList()
    }

    LaunchedEffect(donationManager) {
        donationManager?.events?.collect { event ->
            when (event) {
                is DonationEvent.Success -> {
                    showSupportSheet = false
                    showThankYouDialog = true
                }
                is DonationEvent.Error -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
                is DonationEvent.Canceled -> {}
            }
        }
    }

    if (showThankYouDialog) {
        TipThankYouDialog(onDismiss = { showThankYouDialog = false })
    }

    CompositionLocalProvider(LocalAppStrings provides currentStrings) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            if (!hasCompletedOnboarding) {
                OnboardingFlow(
                    step = onboardingStep,
                    language = currentLanguage,
                    isServiceActive = isServiceActive,
                    onLanguageChange = { lang ->
                        SettingsStore.setAppLanguage(context, if (lang == NmrLanguage.TR) AppLanguage.TR else AppLanguage.EN)
                    },
                    onNextStep = {
                        if (onboardingStep < 3) {
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
            } else if (showAppWalkthrough) {
                BackHandler {
                    showAppWalkthrough = false
                }
                OnboardingFlow(
                    step = walkthroughStep,
                    language = currentLanguage,
                    isServiceActive = isServiceActive,
                    isReviewMode = true,
                    onCloseReview = { showAppWalkthrough = false },
                    onLanguageChange = { lang ->
                        SettingsStore.setAppLanguage(context, if (lang == NmrLanguage.TR) AppLanguage.TR else AppLanguage.EN)
                    },
                    onNextStep = {
                        if (walkthroughStep < 3) {
                            walkthroughStep++
                        } else {
                            showAppWalkthrough = false
                        }
                    },
                    onPreviousStep = {
                        if (walkthroughStep > 0) walkthroughStep--
                    },
                    onOpenAccessibilitySettings = {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    },
                    onSelectProfile = { profile ->
                        SettingsStore.applyProtectionLevel(context, profile)
                    }
                )
            } else {
                // Arka planda sekmeleri ön-derleyen (pre-warm) ve bellekte tutarak ilk geçiş dahil
                // her zaman 120 FPS akıcı geçiş sağlayan yapı.
                var warmedTabs by remember { mutableStateOf(setOf(NmrBottomTab.BUGUN)) }
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(120L)
                    warmedTabs = NmrBottomTab.entries.toSet()
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        NmrBottomTab.entries.forEach { tab ->
                            val isCurrent = currentTab == tab
                            val isWarmed = isCurrent || tab in warmedTabs
                            if (isWarmed) {
                                val alpha by animateFloatAsState(
                                    targetValue = if (isCurrent) 1f else 0f,
                                    animationSpec = tween(160, easing = LinearOutSlowInEasing),
                                    label = "tabAlpha_${tab.name}"
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .zIndex(if (isCurrent) 1f else 0f)
                                        .then(
                                            if (!isCurrent) Modifier.clearAndSetSemantics {} else Modifier
                                        )
                                        .graphicsLayer {
                                            this.alpha = alpha
                                            translationY = if (isCurrent || alpha > 0.01f) 0f else 99999f
                                        }
                                ) {
                                    when (tab) {
                                        NmrBottomTab.BUGUN -> {
                                            TodayScreen(
                                                viewModel = todayViewModel,
                                                onNavigateToProtection = { currentTabName = NmrBottomTab.KORUMA.name },
                                                onStartSnoozeClick = { showSnoozeDialog = true }
                                            )
                                        }
                                        NmrBottomTab.KORUMA -> {
                                            ProtectionScreen(
                                                viewModel = protectionViewModel,
                                                onSelectRegion = { regionBeingEdited = it },
                                                onOpenCustomCount = { showCustomCountDialog = true },
                                                onOpenCustomTime = { showCustomTimeDialog = true }
                                            )
                                        }
                                        NmrBottomTab.ICGORU -> {
                                            InsightsScreen(
                                                viewModel = insightsViewModel,
                                                onNavigateToSupport = { showSupportSheet = true }
                                            )
                                        }
                                        NmrBottomTab.AYARLAR -> {
                                            SettingsScreen(
                                                viewModel = settingsViewModel,
                                                onOpenSupportSheet = { showSupportSheet = true },
                                                onOpenBedtimePicker = { showBedtimePickerDialog = true },
                                                onPreviewGateway = { showMindfulGatewayDialog = true },
                                                onOpenWalkthrough = {
                                                    walkthroughStep = 0
                                                    showAppWalkthrough = true
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    NmrBottomNavigationBar(
                        currentTab = currentTab,
                        onTabSelected = { currentTabName = it.name }
                    )
                }
            }
        }

        // ====================================================================
        // DİYALOGLAR VE ALT PANELLER
        // ====================================================================

        if (showCustomCountDialog) {
            val currentLimit = protectionViewModel.uiState.value.dailyCountLimit
            CustomNumberInputDialog(
                title = currentStrings.customCountTitle,
                description = currentStrings.customCountDesc,
                initialValue = currentLimit,
                onDismiss = { showCustomCountDialog = false },
                onConfirm = { count ->
                    protectionViewModel.setDailyCountLimit(count)
                    showCustomCountDialog = false
                }
            )
        }

        if (showCustomTimeDialog) {
            val currentTimeLimit = protectionViewModel.uiState.value.dailyTimeLimitMinutes
            CustomNumberInputDialog(
                title = currentStrings.customTimeTitle,
                description = currentStrings.customTimeDesc,
                initialValue = currentTimeLimit,
                onDismiss = { showCustomTimeDialog = false },
                onConfirm = { mins ->
                    protectionViewModel.setDailyTimeLimitMinutes(mins)
                    showCustomTimeDialog = false
                }
            )
        }

        if (showSnoozeDialog) {
            SnoozeSelectionDialog(
                onDismiss = { showSnoozeDialog = false },
                onSelectMinutes = { minutes ->
                    showSnoozeDialog = false
                    todayViewModel.startSnooze(minutes, "")
                }
            )
        }

        if (showMindfulGatewayDialog) {
            MindfulGatewayDialog(
                onDismiss = { showMindfulGatewayDialog = false },
                onApproved = { durationMinutes, microPlan ->
                    showMindfulGatewayDialog = false
                    todayViewModel.startSnooze(durationMinutes, microPlan)
                    Toast.makeText(
                        context,
                        if (isEnglish) "Mindful window started ($durationMinutes min)" else "Bilinçli pencere başladı ($durationMinutes dk)",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }

        if (showSupportSheet) {
            SupportBottomSheet(
                isSupporter = isSupporter,
                donationCount = supporterDonationCount,
                donationItems = donationItems,
                isEnglish = isEnglish,
                onDismiss = { showSupportSheet = false },
                onTipClick = { productId ->
                    val activity = context as? Activity
                    if (activity != null) {
                        donationManager?.launchDonationFlow(activity, productId)
                    }
                },
                onVerifySupporterCode = { code ->
                    donationManager?.verifySupporterCode(context, code) ?: false
                }
            )
        }

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
                "DM" -> protectionViewModel.uiState.value.dmMode
                "EXPLORE" -> protectionViewModel.uiState.value.exploreMode
                "FEED" -> protectionViewModel.uiState.value.feedMode
                "PROFILE" -> protectionViewModel.uiState.value.profileMode
                "REELS_TAB" -> protectionViewModel.uiState.value.reelsTabMode
                else -> DmReelMode.FOLLOW_GLOBAL
            }

            RegionModeBottomSheet(
                regionTitle = regionTitle,
                currentMode = currentSelectedMode,
                onDismiss = { regionBeingEdited = null },
                onSelectMode = { selectedMode ->
                    protectionViewModel.setRegionMode(regionKey, selectedMode)
                    regionBeingEdited = null
                }
            )
        }

        if (showBedtimePickerDialog) {
            val sState = settingsViewModel.uiState.value
            BedtimeTimePickerDialog(
                startH = sState.bedtimeStartHour,
                startM = sState.bedtimeStartMinute,
                endH = sState.bedtimeEndHour,
                endM = sState.bedtimeEndMinute,
                onDismiss = { showBedtimePickerDialog = false },
                onConfirm = { sH, sM, eH, eM ->
                    settingsViewModel.setBedtimeWindow(sH, sM, eH, eM)
                    showBedtimePickerDialog = false
                }
            )
        }
    }
}
