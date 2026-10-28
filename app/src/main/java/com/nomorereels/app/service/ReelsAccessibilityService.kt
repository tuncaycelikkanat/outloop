package com.nomorereels.app.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.nomorereels.app.action.ActionExecutor
import com.nomorereels.app.action.MediaPauseHelper
import com.nomorereels.app.action.OverlayController
import com.nomorereels.app.data.HeatmapStore
import com.nomorereels.app.data.QuotaStore
import com.nomorereels.app.data.SettingsStore
import com.nomorereels.app.detection.DetectionLogStore
import com.nomorereels.app.detection.ReelContentFingerprint
import com.nomorereels.app.detection.RuleSet
import com.nomorereels.app.detection.ScreenClassifier
import com.nomorereels.app.detection.ScreenSnapshot
import com.nomorereels.app.detection.ScreenType
import com.nomorereels.app.policy.DmReelMode
import com.nomorereels.app.policy.NavigationTracker
import com.nomorereels.app.policy.PolicyDecision
import com.nomorereels.app.policy.ReelsPolicy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ReelsAccessibilityService : AccessibilityService() {

    private val classifier = ScreenClassifier()
    private val navigationTracker = NavigationTracker()
    private val reelsPolicy = ReelsPolicy()
    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())

    private var lastReelViewerActiveTimeMs: Long = 0L
    private var accumulatedViewerTimeMs: Long = 0L
    private var lastEventClassificationMs: Long = 0L
    private var isWatchdogScheduled: Boolean = false

    private fun accumulateAndRecordViewerTime(
        now: Long,
        activeMode: DmReelMode? = null,
        flushRemainder: Boolean = false
    ) {
        if (lastReelViewerActiveTimeMs > 0L) {
            val delta = now - lastReelViewerActiveTimeMs
            if (delta in 10L..4000L) {
                accumulatedViewerTimeMs += delta
            }
            lastReelViewerActiveTimeMs = if (flushRemainder) 0L else now
        } else if (!flushRemainder) {
            lastReelViewerActiveTimeMs = now
        }

        if (flushRemainder) {
            if (accumulatedViewerTimeMs >= 500L) {
                val secs = (accumulatedViewerTimeMs + 500L) / 1000L
                QuotaStore.addTimeSpent(this, secs)
            }
            accumulatedViewerTimeMs = 0L
        } else if (accumulatedViewerTimeMs >= 1000L) {
            val secs = accumulatedViewerTimeMs / 1000L
            accumulatedViewerTimeMs %= 1000L
            QuotaStore.addTimeSpent(this, secs)
            if (activeMode == DmReelMode.TIME_QUOTA && QuotaStore.checkAndTriggerTimeWarning()) {
                showLocalizedTimeQuotaWarning()
            }
        }
    }

    /**
     * Kullanıcı Reels ekranında eller serbest (dokunmadan) video izlerken
     * saniyede 1 kez çalışarak:
     * 1. Süre kotası ve %80 uyarı bildirimini,
     * 2. Tek Reel izleme süresi sınırını (timeout),
     * 3. Bilinçli Pencere (Snooze) bitiş bildirimini ve
     * 4. Gece Odak Saati başlangıcını
     * saniyesi saniyesine yakalayan hafif zamanlayıcı.
     */
    private fun obtainInstagramRootNode(): android.view.accessibility.AccessibilityNodeInfo? {
        val active = rootInActiveWindow
        if (active != null) {
            if (active.packageName?.toString() == TARGET_PACKAGE) {
                return active
            }
            com.nomorereels.app.detection.safeRecycle(active)
        }
        try {
            val windowList = windows
            if (!windowList.isNullOrEmpty()) {
                for (window in windowList) {
                    val root = window.root ?: continue
                    if (root.packageName?.toString() == TARGET_PACKAGE) {
                        return root
                    }
                    com.nomorereels.app.detection.safeRecycle(root)
                }
            }
        } catch (_: Exception) {}
        return null
    }

    private val reelWatchdogRunnable = object : Runnable {
        override fun run() {
            isWatchdogScheduled = false
            if (!navigationTracker.currentScreen.isReels) return

            val now = System.currentTimeMillis()

            // 1. Mola (Bilinçli Pencere) süresi doldu mu?
            if (SettingsStore.checkSnoozeExpiration(this@ReelsAccessibilityService, now)) {
                val plan = SettingsStore.consumeLastMicroPlan(this@ReelsAccessibilityService)
                val isEnglish = SettingsStore.appLanguage.value == com.nomorereels.app.data.AppLanguage.EN
                overlayController.showMindfulWindowExpired(plan, isEnglish)
            }

            if (overlayController.isFrictionOverlayShowing || actionExecutor.isExiting || overlayController.isSuppressed) {
                lastReelViewerActiveTimeMs = 0L
                val igRoot = obtainInstagramRootNode()
                if (igRoot == null && !actionExecutor.isExiting) {
                    // Kullanıcı Farkındalık Kapısı açıkken Ana Ekran tuşuyla Instagram'dan çıktıysa katmanı kapat
                    overlayController.dismissMindfulFrictionOverlay(releaseAudioImmediately = true)
                    accumulateAndRecordViewerTime(now, flushRemainder = true)
                    navigationTracker.update(navigationTracker.lastNonReelsScreen, ReelContentFingerprint(), null, now)
                    _lastDetectedType.value = navigationTracker.currentScreen
                    _lastOriginScreen.value = null
                    actionExecutor.onReturnedToNonReels()
                    overlayController.onReturnedToNonReels()
                    QuotaStore.flush(this@ReelsAccessibilityService)
                    return
                }
                com.nomorereels.app.detection.safeRecycle(igRoot)
                scheduleReelWatchdog()
                return
            }

            val root = obtainInstagramRootNode()
            if (root == null) {
                // Instagram'dan çıkılmış: birikmiş süreyi yaz, durum makinesini sıfırla ve kaydet
                accumulateAndRecordViewerTime(now, flushRemainder = true)
                navigationTracker.update(navigationTracker.lastNonReelsScreen, ReelContentFingerprint(), null, now)
                _lastDetectedType.value = navigationTracker.currentScreen
                _lastOriginScreen.value = null
                actionExecutor.onReturnedToNonReels()
                overlayController.onReturnedToNonReels()
                QuotaStore.flush(this@ReelsAccessibilityService)
                return
            }

            try {
                // P1-8: Son 900ms içinde olay tabanlı sınıflandırma yapıldıysa tam ağaç taramasını atla
                if (now - lastEventClassificationMs >= 900L) {
                    val snapshot = ScreenSnapshot.fromNodeInfo(
                        packageName = TARGET_PACKAGE,
                        activityName = null,
                        rootNode = root
                    )
                    val detected = classifier.classify(snapshot)
                    if (detected != ScreenType.UNKNOWN) {
                        val fp = if (detected.isReels) classifier.extractReelFingerprint(snapshot) else ReelContentFingerprint()
                        val originHint = if (detected.isReels) classifier.extractReelOriginHint(snapshot) else null
                        navigationTracker.update(detected, fp, originHint, now)
                    }
                }

                val activeScreen = navigationTracker.currentScreen
                if (!activeScreen.isReels) {
                    accumulateAndRecordViewerTime(now, flushRemainder = true)
                    actionExecutor.onReturnedToNonReels()
                    overlayController.onReturnedToNonReels()
                    QuotaStore.flush(this@ReelsAccessibilityService)
                    return
                }

                if (!SettingsStore.protectionMasterEnabled.value) {
                    lastReelViewerActiveTimeMs = 0L
                    scheduleReelWatchdog()
                    return
                }

                QuotaStore.ensureToday(this@ReelsAccessibilityService)

                val activeMode = ReelsPolicy.resolveActiveMode(
                    detectedScreen = activeScreen,
                    originScreen = navigationTracker.reelOriginScreen,
                    dmMode = SettingsStore.dmReelMode.value,
                    exploreMode = SettingsStore.exploreReelMode.value,
                    feedMode = SettingsStore.feedReelMode.value,
                    profileMode = SettingsStore.profileReelMode.value,
                    reelsTabMode = SettingsStore.reelsTabMode.value,
                    generalMode = SettingsStore.generalMode.value
                )

                // 2. Aktif izleme süresini ilerlet (Snooze sırasında da gerçek kullanım sayılır — P0-5)
                accumulateAndRecordViewerTime(now, activeMode = activeMode, flushRemainder = false)

                if (SettingsStore.isSnoozeEffective(now)) {
                    scheduleReelWatchdog()
                    return
                }

                // 3. Süre sınırı, Gece Odak Saati veya yorum penceresi sonrası emniyet kontrolü
                val decision = reelsPolicy.evaluate(
                    detectedScreen = activeScreen,
                    tracker = navigationTracker,
                    dmMode = SettingsStore.dmReelMode.value,
                    exploreMode = SettingsStore.exploreReelMode.value,
                    feedMode = SettingsStore.feedReelMode.value,
                    profileMode = SettingsStore.profileReelMode.value,
                    reelsTabMode = SettingsStore.reelsTabMode.value,
                    generalMode = SettingsStore.generalMode.value,
                    isBedtimeActive = SettingsStore.isBedtimeActiveNow(),
                    isCountLimitExceeded = QuotaStore.isCountLimitExceeded(enteringNewReel = false),
                    isTimeLimitExceeded = QuotaStore.isTimeLimitExceeded(),
                    customSingleReelMaxDurationMs = QuotaStore.singleReelMaxSeconds.value * 1000L,
                    nowMs = now
                )

                if (decision.shouldBlock) {
                    lastReelViewerActiveTimeMs = 0L
                    handleBlockDecision(
                        decision = decision,
                        activeScreen = activeScreen,
                        activeMode = activeMode,
                        originLabel = navigationTracker.reelOriginScreen?.name ?: "DOĞRUDAN",
                        rootNode = root,
                        isScreenChanged = false,
                        keyNodes = emptyList()
                    )
                    if (navigationTracker.currentScreen.isReels) {
                        scheduleReelWatchdog()
                    }
                    return
                }

                scheduleReelWatchdog()
            } finally {
                com.nomorereels.app.detection.safeRecycle(root)
            }
        }
    }

    private fun scheduleReelWatchdog() {
        if (!isWatchdogScheduled) {
            isWatchdogScheduled = true
            mainHandler.postDelayed(reelWatchdogRunnable, 1000L)
        }
    }

    private fun stopReelWatchdog() {
        if (isWatchdogScheduled) {
            mainHandler.removeCallbacks(reelWatchdogRunnable)
            isWatchdogScheduled = false
        }
    }

    private fun showLocalizedCountQuotaWarning() {
        val isEnglish = SettingsStore.appLanguage.value == com.nomorereels.app.data.AppLanguage.EN
        overlayController.showQuotaWarningBanner(
            title = if (isEnglish) "Last few Reels left ⏳" else "Son birkaç Reel kaldı ⏳",
            message = null
        )
    }

    private fun showLocalizedTimeQuotaWarning() {
        val isEnglish = SettingsStore.appLanguage.value == com.nomorereels.app.data.AppLanguage.EN
        overlayController.showQuotaWarningBanner(
            title = if (isEnglish) "Time's almost up ⏳" else "Süren dolmak üzere ⏳",
            message = null
        )
    }

    /** Debug derlemede ayrıntılı ağaç dökümü (NoMoreReelsDump) yapılır; release'te tamamen kapalı. */
    private var isDebuggableBuild = false

    private var cachedInspectMode = false
    private var inspectModeCheckedAtMs = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        currentInstance = this
        _isServiceActive.value = true

        isDebuggableBuild = (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0

        SettingsStore.init(this)
        QuotaStore.init(this)
        HeatmapStore.init(this)

        // Dinamik kuralları assets/rules.json dosyasından yükle
        classifier.rules = loadRulesFromAssets()
        actionExecutor.rules = classifier.rules
        _ruleVersion.value = classifier.rules.version

        Log.i(TAG, "ReelsAccessibilityService bağlandı (Kural sürümü: ${classifier.rules.version}).")
    }

    private fun isInspectModeEnabled(now: Long): Boolean {
        if (!isDebuggableBuild) return false
        if (now - inspectModeCheckedAtMs > 5_000L) {
            inspectModeCheckedAtMs = now
            cachedInspectMode = try {
                android.provider.Settings.Global.getInt(contentResolver, "nmr_inspect_mode", 0) == 1
            } catch (_: Exception) {
                false
            }
        }
        return cachedInspectMode
    }

    private var lastContentChangeProcessedMs = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val pkgName = event.packageName?.toString() ?: return
        if (pkgName != TARGET_PACKAGE) return

        // Farkındalık Kapısı / 5 sn basılı tutma katmanı açıkken arka plan olaylarının oturumu bozmasını önle
        if (overlayController.isFrictionOverlayShowing) {
            return
        }

        val eventType = event.eventType
        val now = System.currentTimeMillis()

        // A. OLAY FRENLEME (THROTTLING - %85 CPU ve Pil Tasarrufu):
        // Çıkış işlemi (isExiting) devam ederken geri dönüş ekranını anında yakalamak için frenleme uygulanmaz.
        if (eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED && !actionExecutor.isExiting) {
            if (now - lastContentChangeProcessedMs < CONTENT_CHANGE_THROTTLE_MS) {
                return
            }
            lastContentChangeProcessedMs = now
        }

        val activityName = event.className?.toString()
        val srcId = if (eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            val sourceNode = try { event.source } catch (_: Exception) { null }
            try {
                sourceNode?.viewIdResourceName
            } finally {
                com.nomorereels.app.detection.safeRecycle(sourceNode)
            }
        } else {
            null
        }

        // B. HIZLI YOL (FAST-PATH - Kaydırma Olayı Süzgeci):
        if (eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            if (!navigationTracker.currentScreen.isReels) {
                return
            }
            if (!isVerticalReelPagerScroll(event, activityName, srcId)) {
                return
            }
            if (isDebuggableBuild) {
                Log.i(
                    "NoMoreReelsDump",
                    "SCROLL_EVENT class=$activityName srcId=$srcId from=${event.fromIndex} to=${event.toIndex} " +
                        "count=${event.itemCount} dX=${event.scrollDeltaX} dY=${event.scrollDeltaY} curScreen=${navigationTracker.currentScreen}"
                )
            }
        }

        val rootNode = obtainInstagramRootNode() ?: return
        try {
            // 1. Düğüm ağacını saf veri modeline dönüştür
            val snapshot = ScreenSnapshot.fromNodeInfo(
                packageName = pkgName,
                activityName = activityName,
                rootNode = rootNode
            )

            // 1b. Mola (Bilinçli Pencere) süresi doldu mu kontrol et
            if (SettingsStore.checkSnoozeExpiration(this, now)) {
                val plan = SettingsStore.consumeLastMicroPlan(this)
                val isEnglish = SettingsStore.appLanguage.value == com.nomorereels.app.data.AppLanguage.EN
                overlayController.showMindfulWindowExpired(plan, isEnglish)
            }

            // 2. Ekranı dinamik kural motoruyla sınıflandır
            val detectedScreen = classifier.classify(snapshot)
            com.nomorereels.app.data.LocalDiagnosticsStore.recordInstagramEvent(
                nowMs = now,
                isReelsScreen = detectedScreen.isReels || navigationTracker.currentScreen.isReels
            )

            val originHint = if (detectedScreen.isReels) classifier.extractReelOriginHint(snapshot) else null

            // 3. Reels ekranındaysak aktif videonun geçiş kimliğini yalnızca bellekte anlık karşılaştırma için çıkar
            val reelFingerprint = if (detectedScreen.isReels) {
                classifier.extractReelFingerprint(snapshot)
            } else {
                ReelContentFingerprint()
            }

            val keyNodes: List<String> by lazy {
                val nodes = collectDiagnosticKeyNodes(snapshot)
                if (!reelFingerprint.isEmpty) {
                    nodes.add(0, "🎬 Reel Yapısı Algılandı")
                }
                nodes
            }

            // 4. Gezinme durum makinesini güncelle
            lastEventClassificationMs = now
            val isScreenChanged = navigationTracker.update(
                newScreen = detectedScreen,
                reelFingerprint = reelFingerprint,
                originHint = originHint
            )
            val activeScreen = navigationTracker.currentScreen
            _lastDetectedType.value = activeScreen
            _lastOriginScreen.value = navigationTracker.reelOriginScreen

            // 4b. Eğer mevcut ekran Reels ise ve dikey sayfa kaydırma olayı geldiyse 2. videoya geçişi kaydet
            if (event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED &&
                activeScreen.isReels &&
                isVerticalReelPagerScroll(event, activityName, srcId)
            ) {
                val swiped = navigationTracker.onReelPagerScrolled()
                if (swiped) {
                    Log.w(TAG, ">>> REEL KAYDIRMA TESPİTİ (TYPE_VIEW_SCROLLED): 2. videoya geçildi! <<<")
                }
            }

            if (isDebuggableBuild) {
                logScenarioSnapshotIfNeeded(event, snapshot, detectedScreen, activeScreen, isScreenChanged, reelFingerprint)
            }

            // Reels dışındaki bir ekrana geçildiyse: tüm çıkış/bastırma kilitlerini sıfırla ve süreyi kaydet
            if (!activeScreen.isReels) {
                stopReelWatchdog()
                actionExecutor.onReturnedToNonReels()
                overlayController.onReturnedToNonReels()

                accumulateAndRecordViewerTime(now, flushRemainder = true)
                QuotaStore.flush(this)

                if (overlayController.isFrictionOverlayShowing) {
                    overlayController.dismissMindfulFrictionOverlay()
                }

                if (isScreenChanged && activeScreen != ScreenType.UNKNOWN) {
                    Log.i(TAG, ">>> EKRAN DEĞİŞİMİ: ${navigationTracker.previousScreen} -> $activeScreen <<<")
                    DetectionLogStore.log(
                        screenType = activeScreen,
                        details = "Ekran: ${activeScreen.name} (${activityName?.substringAfterLast('.')})",
                        actionTaken = null,
                        keyNodes = keyNodes
                    )
                }
                return
            }

            // 5. Reels ekranındayız (REELS_TAB veya REELS_VIEWER)
            scheduleReelWatchdog()

            if (actionExecutor.isExiting || overlayController.isSuppressed) {
                return
            }

            val isProtectionMasterEnabled = SettingsStore.protectionMasterEnabled.value
            if (!isProtectionMasterEnabled) {
                if (overlayController.isFrictionOverlayShowing) {
                    overlayController.dismissMindfulFrictionOverlay()
                }
                _lastDetectedType.value = activeScreen
                _lastOriginScreen.value = navigationTracker.reelOriginScreen
                lastReelViewerActiveTimeMs = 0L
                return
            }

            QuotaStore.ensureToday(this)
            val newReelEntered = isScreenChanged || navigationTracker.checkAndClearNewReelSwiped()
            // P1-4: Isı haritasına yalnızca Reels oturumu ilk başladığında (ekran değişiminde) dürtü kaydet
            if (isScreenChanged) {
                HeatmapStore.recordAttempt(this, now)
            }

            val dmMode = SettingsStore.dmReelMode.value
            val exploreMode = SettingsStore.exploreReelMode.value
            val feedMode = SettingsStore.feedReelMode.value
            val profileMode = SettingsStore.profileReelMode.value
            val reelsTabMode = SettingsStore.reelsTabMode.value
            val generalMode = SettingsStore.generalMode.value

            val activeMode = ReelsPolicy.resolveActiveMode(
                detectedScreen = activeScreen,
                originScreen = navigationTracker.reelOriginScreen,
                dmMode = dmMode,
                exploreMode = exploreMode,
                feedMode = feedMode,
                profileMode = profileMode,
                reelsTabMode = reelsTabMode,
                generalMode = generalMode
            )

            // P0-5: Snooze etkinken (Gece Odak Saati dışında) engelleme yapılmaz ancak izlenen Reel ve süre sayılır
            if (SettingsStore.isSnoozeEffective(now)) {
                if (overlayController.isFrictionOverlayShowing) {
                    overlayController.dismissMindfulFrictionOverlay()
                }
                if (newReelEntered) {
                    QuotaStore.incrementReelCount(this)
                }
                accumulateAndRecordViewerTime(now, activeMode = activeMode, flushRemainder = false)
                _lastDetectedType.value = activeScreen
                _lastOriginScreen.value = navigationTracker.reelOriginScreen
                return
            }

            // Politikayı çalıştır
            val decision = reelsPolicy.evaluate(
                detectedScreen = activeScreen,
                tracker = navigationTracker,
                dmMode = dmMode,
                exploreMode = exploreMode,
                feedMode = feedMode,
                profileMode = profileMode,
                reelsTabMode = reelsTabMode,
                generalMode = generalMode,
                isBedtimeActive = SettingsStore.isBedtimeActiveNow(),
                isCountLimitExceeded = QuotaStore.isCountLimitExceeded(enteringNewReel = newReelEntered),
                isTimeLimitExceeded = QuotaStore.isTimeLimitExceeded(),
                customSingleReelMaxDurationMs = QuotaStore.singleReelMaxSeconds.value * 1000L
            )

            val originLabel = navigationTracker.reelOriginScreen?.name ?: "DOĞRUDAN"
            val activeModeForLog = activeMode

            if (isInspectModeEnabled(now)) {
                Log.i("NoMoreReelsDump", "INSPECT_MODE decision=$decision active=$activeScreen origin=$originLabel mode=$activeMode swiped=${navigationTracker.hasSwipedToNextReel}")
                return
            }

            // 6. Kullanım muhasebesi: yalnızca gerçekten izin verilen (izlenen) Reel'ler sayılır.
            if (decision == PolicyDecision.ALLOW) {
                if (newReelEntered) {
                    QuotaStore.incrementReelCount(this)
                    if (activeMode == DmReelMode.COUNT_QUOTA && QuotaStore.checkAndTriggerCountWarning()) {
                        showLocalizedCountQuotaWarning()
                    }
                }
                accumulateAndRecordViewerTime(now, activeMode = activeMode, flushRemainder = false)
            } else {
                // Engellenen veya onay bekleyen Reel'de geçen süre izleme süresine eklenmez
                lastReelViewerActiveTimeMs = 0L
            }

            when {
                decision.shouldBlock -> {
                    handleBlockDecision(
                        decision = decision,
                        activeScreen = activeScreen,
                        activeMode = activeModeForLog,
                        originLabel = originLabel,
                        rootNode = rootNode,
                        isScreenChanged = isScreenChanged,
                        keyNodes = keyNodes
                    )
                }

                decision.shouldShowFriction -> {
                    if (!overlayController.isFrictionOverlayShowing && !overlayController.isSuppressed && !actionExecutor.isExiting) {
                        // Medyayı HEMEN duraklat
                        MediaPauseHelper.pauseMedia(this)

                        val isEnglish = SettingsStore.appLanguage.value == com.nomorereels.app.data.AppLanguage.EN
                        val isDarkTheme = SettingsStore.isDarkThemeActiveNow(this)

                        if (SettingsStore.mindfulGatewayEnabled.value) {
                            overlayController.showMindfulGatewayOverlay(
                                isEnglish = isEnglish,
                                decision = decision,
                                originLabel = originLabel,
                                onApproved = { durationMinutes, microPlan ->
                                    navigationTracker.approveCurrentDmReel()
                                    SettingsStore.startSnooze(
                                        context = this,
                                        durationMinutes = durationMinutes,
                                        microPlan = microPlan
                                    )
                                    DetectionLogStore.log(
                                        screenType = ScreenType.REELS_VIEWER,
                                        details = "Farkındalık Kapısı (DM): $durationMinutes dk ($microPlan)",
                                        actionTaken = "Onaylandı",
                                        keyNodes = keyNodes
                                    )
                                },
                                onCancelAndGoBack = {
                                    QuotaStore.incrementGatewayDismissCount(this)
                                    navigationTracker.markExitInitiated()
                                    overlayController.suppress(400L)
                                    val exited = actionExecutor.exitReels(
                                        screenType = ScreenType.REELS_VIEWER,
                                        originScreen = navigationTracker.reelOriginScreen ?: ScreenType.DIRECT_MESSAGES
                                    )
                                    if (exited) {
                                        // Kullanıcı Farkındalık Kapısı'nı zaten gördü; ek bildirim gösterme
                                        actionExecutor.recordBlockMetricsAndHaptic(this)
                                    }
                                    DetectionLogStore.log(
                                        screenType = ScreenType.REELS_VIEWER,
                                        details = "Farkındalık Kapısı (DM): Vazgeçildi",
                                        actionTaken = "Geri Dönüldü",
                                        keyNodes = keyNodes
                                    )
                                }
                            )
                        } else {
                            val shown = overlayController.showMindfulFrictionOverlay(
                                reelSummary = null,
                                isNightMode = isDarkTheme,
                                onApproved = {
                                    navigationTracker.approveCurrentDmReel()
                                    DetectionLogStore.log(
                                        screenType = ScreenType.REELS_VIEWER,
                                        details = "Köken: $originLabel · 5 Sn Basılı Tutma Tamamlandı",
                                        actionTaken = "Onaylandı (Tek Reel İzleniyor)",
                                        keyNodes = keyNodes
                                    )
                                },
                                onCancelAndGoBack = {
                                    navigationTracker.markExitInitiated()
                                    overlayController.suppress(400L)
                                    val exited = actionExecutor.exitReels(
                                        screenType = ScreenType.REELS_VIEWER,
                                        originScreen = navigationTracker.reelOriginScreen ?: ScreenType.DIRECT_MESSAGES
                                    )
                                    if (exited) {
                                        actionExecutor.recordBlockMetricsAndHaptic(this)
                                        if (SettingsStore.overlayFeedbackEnabled.value) {
                                            overlayController.showBlockBanner(decision)
                                        }
                                    }
                                    DetectionLogStore.log(
                                        screenType = ScreenType.REELS_VIEWER,
                                        details = "Köken: $originLabel · Kullanıcı Vazgeçti",
                                        actionTaken = "Geri Dönüldü",
                                        keyNodes = keyNodes
                                    )
                                }
                            )

                            if (isScreenChanged && shown) {
                                DetectionLogStore.log(
                                    screenType = activeScreen,
                                    details = "Köken: $originLabel · 5 Sn Onay Kapısı Açıldı",
                                    actionTaken = decision.reasonText,
                                    keyNodes = keyNodes
                                )
                            }
                        }
                    }
                }

                decision == PolicyDecision.ALLOW -> {
                    // Tekil veya serbest Reel'e izin verildi; yalnızca ilk girişte günlüğe yaz
                    if (isScreenChanged) {
                        Log.i(TAG, ">>> REEL İZİN VERİLDİ (Köken: $originLabel, Mod: ${activeModeForLog.name}) <<<")
                        DetectionLogStore.log(
                            screenType = activeScreen,
                            details = "Köken: $originLabel · Mod: ${activeModeForLog.title}",
                            actionTaken = "İzin Verildi (${activeModeForLog.title})",
                            keyNodes = keyNodes
                        )
                    }
                }
            }
        } finally {
            com.nomorereels.app.detection.safeRecycle(rootNode)
        }
    }

    private fun handleBlockDecision(
        decision: PolicyDecision,
        activeScreen: ScreenType,
        activeMode: DmReelMode,
        originLabel: String,
        rootNode: android.view.accessibility.AccessibilityNodeInfo?,
        isScreenChanged: Boolean,
        keyNodes: List<String>
    ) {
        if (overlayController.isFrictionOverlayShowing || overlayController.isSuppressed || actionExecutor.isExiting) {
            return
        }

        val gatewayPath = SettingsStore.mindfulGatewayEnabled.value
        var gatewayFailed = false
        if (gatewayPath) {
            // P1-13: Yalnızca Farkındalık Kapısı açılırken medyayı duraklat
            MediaPauseHelper.pauseMedia(this)
            val isEnglish = SettingsStore.appLanguage.value == com.nomorereels.app.data.AppLanguage.EN
            val shown = overlayController.showMindfulGatewayOverlay(
                isEnglish = isEnglish,
                decision = decision,
                originLabel = originLabel,
                onApproved = { durationMinutes, microPlan ->
                    SettingsStore.startSnooze(
                        context = this,
                        durationMinutes = durationMinutes,
                        microPlan = microPlan
                    )
                    DetectionLogStore.log(
                        screenType = activeScreen,
                        details = "Farkındalık Kapısı: $durationMinutes dk Bilinçli Pencere ($microPlan)",
                        actionTaken = "Bilinçli İzin Verildi",
                        keyNodes = keyNodes
                    )
                },
                onCancelAndGoBack = {
                    QuotaStore.incrementGatewayDismissCount(this)
                    navigationTracker.markExitInitiated()
                    overlayController.suppress(400L)
                    val exited = actionExecutor.exitReels(
                        screenType = activeScreen,
                        originScreen = navigationTracker.reelOriginScreen
                    )
                    if (exited) {
                        // Kullanıcı Farkındalık Kapısı'nı zaten gördü; ek bildirim gösterme
                        actionExecutor.recordBlockMetricsAndHaptic(this)
                    }
                    DetectionLogStore.log(
                        screenType = activeScreen,
                        details = "Farkındalık Kapısı: Çıkış / Vazgeçme",
                        actionTaken = "Geri Dönüldü",
                        keyNodes = emptyList()
                    )
                }
            )

            if (isScreenChanged && shown) {
                DetectionLogStore.log(
                    screenType = activeScreen,
                    details = "Farkındalık Kapısı Açıldı · Mod: ${activeMode.name}",
                    actionTaken = decision.reasonText,
                    keyNodes = keyNodes
                )
            }
            gatewayFailed = !shown && !overlayController.isSuppressed
        }
        if (!gatewayPath || gatewayFailed) {
            Log.w(TAG, "Reels engelleniyor! Karar: $decision, Ekran: $activeScreen, Köken: $originLabel, Mod: ${activeMode.name}")
            val handled = actionExecutor.executeBlock(activeScreen, rootNode)

            if (handled) {
                navigationTracker.markExitInitiated()
                overlayController.suppress(400L)
                if (SettingsStore.overlayFeedbackEnabled.value) {
                    overlayController.showBlockBanner(decision)
                }
            }

            DetectionLogStore.log(
                screenType = activeScreen,
                details = "Köken: $originLabel · Mod: ${activeMode.name}",
                actionTaken = if (handled) decision.reasonText else "Beklemede (Cooldown)",
                keyNodes = keyNodes
            )
        }
    }

    private var lastDumpSignature: String = ""

    private fun logScenarioSnapshotIfNeeded(
        event: AccessibilityEvent,
        snapshot: ScreenSnapshot,
        detectedScreen: ScreenType,
        activeScreen: ScreenType,
        isScreenChanged: Boolean,
        reelFingerprint: ReelContentFingerprint
    ) {
        val interestingNodes = snapshot.findNodes { node ->
            !node.viewId.isNullOrBlank() ||
                node.isSelected ||
                !node.contentDescription.isNullOrBlank() ||
                !node.text.isNullOrBlank()
        }
        val idList = interestingNodes.mapNotNull { it.viewId?.substringAfterLast('/') }.distinct()
        val sig = "$detectedScreen|$activeScreen|${navigationTracker.reelOriginScreen}|${idList.joinToString(",")}"
        if (!isScreenChanged && sig == lastDumpSignature && event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return
        }
        lastDumpSignature = sig

        Log.i(
            "NoMoreReelsDump",
            "=== SNAPSHOT ev=${AccessibilityEvent.eventTypeToString(event.eventType)} act=${snapshot.currentActivity} " +
                "detected=$detectedScreen active=$activeScreen origin=${navigationTracker.reelOriginScreen} " +
                "swiped=${navigationTracker.hasSwipedToNextReel} ==="
        )
        for (n in interestingNodes.take(60)) {
            val shortId = n.viewId?.substringAfterLast('/') ?: ""
            val shortCls = n.className?.substringAfterLast('.') ?: ""
            if (shortId.isNotEmpty() || n.isSelected) {
                Log.i(
                    "NoMoreReelsDump",
                    "  NODE id=$shortId sel=${n.isSelected} cls=$shortCls"
                )
            }
        }
    }

    private fun isVerticalReelPagerScroll(
        event: AccessibilityEvent,
        className: String?,
        srcId: String?
    ): Boolean {
        // 1. RecyclerView veya ScrollView kaydırmaları ASLA dikey Reel pager kaydırması değildir!
        // Instagram'da dikey Reels kaydırma bileşeni clips_viewer_view_pager (ViewPager)'dır.
        // Ana akışın veya Keşfet ızgarasının arka planda kalan RecyclerView'ı sahte olay üretmemelidir.
        if (className?.contains("RecyclerView", ignoreCase = true) == true ||
            className?.contains("ScrollView", ignoreCase = true) == true
        ) {
            return false
        }

        // 2. Sıfır veya çok küçük delta değerleri (layout geçişleri veya mikro titreşimler) hariç tutulur
        val deltaY = kotlin.math.abs(event.scrollDeltaY)
        if (deltaY < 50) {
            return false
        }

        // 3. Kaynak ID'si varsa clips_viewer_view_pager veya view_pager içermelidir
        if (!srcId.isNullOrBlank()) {
            return srcId.contains("clips_viewer_view_pager", ignoreCase = true) ||
                   srcId.contains("view_pager", ignoreCase = true)
        }

        // 4. Kaynak ID'si yoksa sınıf adı ViewPager olmalıdır
        return className?.contains("ViewPager", ignoreCase = true) == true
    }

    private fun collectDiagnosticKeyNodes(snapshot: ScreenSnapshot): MutableList<String> {
        val keyNodes = mutableListOf<String>()
        val selectedNodes = snapshot.findNodes { it.isSelected }
        for (n in selectedNodes.take(3)) {
            val shortId = n.viewId?.substringAfterLast('/') ?: "no-id"
            keyNodes.add("★ SEÇİLİ: id=$shortId")
        }
        val tabNodes = snapshot.findNodes {
            (it.viewId?.contains("tab") == true || it.viewId?.contains("clips") == true) && !it.isSelected
        }
        for (n in tabNodes.take(3)) {
            val shortId = n.viewId?.substringAfterLast('/') ?: "no-id"
            keyNodes.add("Sekme: id=$shortId")
        }
        return keyNodes
    }

    override fun onInterrupt() {
        Log.w(TAG, "ReelsAccessibilityService kesintiye uğradı (onInterrupt).")
        if (overlayController.isFrictionOverlayShowing) {
            overlayController.dismissMindfulFrictionOverlay(releaseAudioImmediately = true)
        }
        overlayController.release()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopReelWatchdog()
        accumulateAndRecordViewerTime(System.currentTimeMillis(), flushRemainder = true)
        QuotaStore.flush(this)
        actionExecutor.onReturnedToNonReels()
        overlayController.dismissAll()
        overlayController.release()
        currentInstance = null
        _isServiceActive.value = false
        navigationTracker.reset()
        Log.i(TAG, "ReelsAccessibilityService sonlandırıldı.")
    }

    private fun loadRulesFromAssets(): RuleSet {
        return try {
            val jsonStr = assets.open("rules.json").bufferedReader().use { it.readText() }
            val loadedRules = RuleSet.fromJson(jsonStr)
            Log.i(TAG, "Kurallar assets/rules.json dosyasından başarıyla yüklendi (v${loadedRules.version}).")
            loadedRules
        } catch (e: Exception) {
            Log.w(TAG, "rules.json okunamadı, varsayılan kurallar devrede: ${e.message}")
            RuleSet.DEFAULT
        }
    }

    companion object {
        const val TAG = "NoMoreReels"
        const val TARGET_PACKAGE = "com.instagram.android"

        @Volatile
        private var currentInstance: ReelsAccessibilityService? = null

        val actionExecutor = ActionExecutor(serviceProvider = { currentInstance })
        val overlayController = OverlayController(serviceProvider = { currentInstance })

        private val _isServiceActive = MutableStateFlow(false)
        val isServiceActive: StateFlow<Boolean> = _isServiceActive.asStateFlow()

        private val _lastDetectedType = MutableStateFlow(ScreenType.MAIN_FEED)
        val lastDetectedType: StateFlow<ScreenType> = _lastDetectedType.asStateFlow()

        private val _lastOriginScreen = MutableStateFlow<ScreenType?>(null)
        val lastOriginScreen: StateFlow<ScreenType?> = _lastOriginScreen.asStateFlow()

        private val _ruleVersion = MutableStateFlow(4)
        val ruleVersion: StateFlow<Int> = _ruleVersion.asStateFlow()

        const val CONTENT_CHANGE_THROTTLE_MS = 140L
    }
}
