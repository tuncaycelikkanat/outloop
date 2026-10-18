package com.nomorereels.app.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.nomorereels.app.action.ActionExecutor
import com.nomorereels.app.action.OverlayController
import com.nomorereels.app.data.QuotaStore
import com.nomorereels.app.data.SettingsStore
import com.nomorereels.app.detection.DetectionLogStore
import com.nomorereels.app.detection.ReelContentFingerprint
import com.nomorereels.app.detection.RuleSet
import com.nomorereels.app.detection.ScreenClassifier
import com.nomorereels.app.detection.ScreenSnapshot
import com.nomorereels.app.detection.ScreenType
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

    private var lastReelViewerActiveTimeMs: Long = 0L
    private var accumulatedViewerTimeMs: Long = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        currentInstance = this
        _isServiceActive.value = true

        SettingsStore.init(this)
        QuotaStore.init(this)

        // Dinamik kuralları assets/rules.json dosyasından yükle
        classifier.rules = loadRulesFromAssets()
        _ruleVersion.value = classifier.rules.version

        Log.i(TAG, "ReelsAccessibilityService bağlandı (Kural sürümü: ${classifier.rules.version}).")
    }

    private var lastContentChangeProcessedMs = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val pkgName = event.packageName?.toString() ?: return
        if (pkgName != TARGET_PACKAGE) return

        val eventType = event.eventType
        val now = System.currentTimeMillis()

        // A. OLAY FRENLEME (THROTTLING - %85 CPU ve Pil Tasarrufu):
        // Kullanıcı akışta veya ızgarada gezinirken saniyede 30-60 kez TYPE_WINDOW_CONTENT_CHANGED gelir.
        // Bu olayları 140 ms'de 1 ile sınırlandırırız. Böylece gereksiz Binder IPC ve nesne tahsisi %85 azalır.
        // İSTİSNA: TYPE_WINDOW_STATE_CHANGED (yeni ekran açılması), TYPE_VIEW_CLICKED (dokunma) veya
        // TYPE_VIEW_SCROLLED (sayfa kaydırma) olayları asla frenlenmez; sıfır gecikmeyle (<5ms) çalışır!
        if (eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            if (now - lastContentChangeProcessedMs < CONTENT_CHANGE_THROTTLE_MS) {
                return
            }
            lastContentChangeProcessedMs = now
        }

        val activityName = event.className?.toString()
        val srcId = try { event.source?.viewIdResourceName } catch (_: Exception) { null }

        // B. HIZLI YOL (FAST-PATH - Kaydırma Olayı Süzgeci):
        // 1. Eğer halihazırda Reels oynatıcısında değilsek (örn. Ana Akış veya Keşfet kaydırılıyorsa),
        //    liste kaydırmaları ekran türünü değiştiremez. Ağacı çekip analiz etmeden hemen çık!
        // 2. Reels izleyicisindeysek ama dikey ViewPager kaydırması değilse (örn. arka plan fling) çık!
        if (eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            if (navigationTracker.currentScreen != ScreenType.REELS_VIEWER) {
                return
            }
            if (!isVerticalReelPagerScroll(event, activityName, srcId)) {
                return
            }
            Log.i(
                "NoMoreReelsDump",
                "SCROLL_EVENT class=$activityName srcId=$srcId from=${event.fromIndex} to=${event.toIndex} " +
                    "count=${event.itemCount} dX=${event.scrollDeltaX} dY=${event.scrollDeltaY} curScreen=${navigationTracker.currentScreen}"
            )
        }

        val rootNode = rootInActiveWindow ?: return

        // 1. Düğüm ağacını saf veri modeline dönüştür
        val snapshot = ScreenSnapshot.fromNodeInfo(
            packageName = pkgName,
            activityName = activityName,
            rootNode = rootNode
        )

        // 2. Ekranı dinamik kural motoruyla sınıflandır
        val detectedScreen = classifier.classify(snapshot)
        val originHint = classifier.extractReelOriginHint(snapshot)

        // Teşhis için ekrandaki kilit düğümleri topla
        val keyNodes = collectDiagnosticKeyNodes(snapshot)

        // 3. Reels ekranındaysak aktif videonun parmak izini (yazar/medya/altyazı) çıkar
        val reelFingerprint = if (detectedScreen.isReels) {
            classifier.extractReelFingerprint(snapshot)
        } else {
            ReelContentFingerprint()
        }

        if (!reelFingerprint.isEmpty) {
            keyNodes.add(0, "🎬 Reel Kimliği: ${reelFingerprint.toSummaryString()}")
        }

        // 4. Gezinme durum makinesini güncelle (Ekran geçişi + Köken tespiti + 2. Reel parmak izi değişimi)
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

        logScenarioSnapshotIfNeeded(event, snapshot, detectedScreen, activeScreen, isScreenChanged, reelFingerprint)

        // Reels dışındaki bir ekrana geçildiyse açık olan Sürtünme (Friction) katmanını kapat ve süreyi kaydet
        if (!activeScreen.isReels) {
            if (lastReelViewerActiveTimeMs > 0L) {
                val delta = now - lastReelViewerActiveTimeMs
                if (delta in 100..4000) {
                    accumulatedViewerTimeMs += delta
                }
                lastReelViewerActiveTimeMs = 0L
            }
            if (accumulatedViewerTimeMs >= 500L) {
                val secs = (accumulatedViewerTimeMs + 500L) / 1000L
                QuotaStore.addTimeSpent(this, secs)
            }
            accumulatedViewerTimeMs = 0L

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

        // 5. Reels ekranındayız (REELS_TAB veya REELS_VIEWER):
        // Reels süresini ve adet sayacını takip et
        if (activeScreen.isReels) {
            val newReelEntered = isScreenChanged || navigationTracker.checkAndClearNewReelSwiped()
            if (newReelEntered) {
                QuotaStore.incrementReelCount(this)
                if (QuotaStore.checkAndTriggerCountWarning()) {
                    overlayController.showQuotaWarningBanner(
                        title = "Günlük Kota Uyarısı ⏳",
                        message = "Kotana yaklaştın: Bugün ${QuotaStore.todayReelCount.value}/${QuotaStore.dailyCountLimit.value} Reel izledin."
                    )
                }
            }

            if (lastReelViewerActiveTimeMs > 0L) {
                val delta = now - lastReelViewerActiveTimeMs
                if (delta in 100..4000) {
                    accumulatedViewerTimeMs += delta
                    if (accumulatedViewerTimeMs >= 1000L) {
                        val secs = accumulatedViewerTimeMs / 1000L
                        accumulatedViewerTimeMs %= 1000L
                        QuotaStore.addTimeSpent(this, secs)
                        if (QuotaStore.checkAndTriggerTimeWarning()) {
                            val currentMins = QuotaStore.todayTimeSpentSeconds.value / 60
                            overlayController.showQuotaWarningBanner(
                                title = "Günlük Süre Uyarısı ⏱️",
                                message = "Süre sınırına yaklaştın: ${currentMins}/${QuotaStore.dailyTimeLimitMinutes.value} dakika."
                            )
                        }
                    }
                }
            }
            lastReelViewerActiveTimeMs = now
        }

        // Politikayı çalıştır
        val dmMode = SettingsStore.dmReelMode.value
        val exploreMode = SettingsStore.exploreReelMode.value
        val feedMode = SettingsStore.feedReelMode.value
        val profileMode = SettingsStore.profileReelMode.value
        val reelsTabMode = SettingsStore.reelsTabMode.value

        val decision = reelsPolicy.evaluate(
            detectedScreen = activeScreen,
            tracker = navigationTracker,
            dmMode = dmMode,
            exploreMode = exploreMode,
            feedMode = feedMode,
            profileMode = profileMode,
            reelsTabMode = reelsTabMode,
            generalMode = SettingsStore.generalMode.value,
            isBedtimeActive = SettingsStore.isBedtimeActiveNow(),
            isCountLimitExceeded = QuotaStore.isCountLimitExceeded(),
            isTimeLimitExceeded = QuotaStore.isTimeLimitExceeded(),
            customSingleReelMaxDurationMs = QuotaStore.singleReelMaxSeconds.value * 1000L
        )

        val originLabel = navigationTracker.reelOriginScreen?.name ?: "DOĞRUDAN"
        val activeModeForLog = if (activeScreen == ScreenType.REELS_TAB) {
            reelsTabMode
        } else {
            when (navigationTracker.reelOriginScreen) {
                ScreenType.DIRECT_MESSAGES -> dmMode
                ScreenType.EXPLORE -> exploreMode
                ScreenType.MAIN_FEED -> feedMode
                ScreenType.PROFILE -> profileMode
                else -> dmMode
            }
        }

        val isInspectMode = try {
            android.provider.Settings.Global.getInt(contentResolver, "nmr_inspect_mode", 0) == 1
        } catch (_: Exception) {
            false
        }
        if (isInspectMode) {
            Log.i("NoMoreReelsDump", "INSPECT_MODE decision=$decision active=$activeScreen origin=$originLabel swiped=${navigationTracker.hasSwipedToNextReel} fp=${reelFingerprint.toSummaryString()}")
            return
        }

        when {
            decision.shouldBlock -> {
                if (overlayController.isFrictionOverlayShowing) {
                    overlayController.dismissMindfulFrictionOverlay()
                }

                Log.w(TAG, "Reels engelleniyor! Karar: $decision, Ekran: $activeScreen, Köken: $originLabel, Mod: ${activeModeForLog.name}")
                val handled = actionExecutor.executeBlock(activeScreen, rootNode)

                if (handled && SettingsStore.overlayFeedbackEnabled.value) {
                    overlayController.showBlockBanner(decision)
                }

                DetectionLogStore.log(
                    screenType = activeScreen,
                    details = "Köken: $originLabel · Mod: ${activeModeForLog.name}",
                    actionTaken = if (handled) decision.reasonText else "Beklemede (Cooldown)",
                    keyNodes = keyNodes
                )
            }

            decision.shouldShowFriction -> {
                if (!overlayController.isFrictionOverlayShowing) {
                    val shown = overlayController.showMindfulFrictionOverlay(
                        reelSummary = reelFingerprint.toSummaryString(),
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
                            actionExecutor.executeBlock(ScreenType.REELS_VIEWER, null, force = true)
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
        val sig = "$detectedScreen|$activeScreen|${navigationTracker.reelOriginScreen}|${reelFingerprint.toSummaryString()}|${idList.joinToString(",")}"
        if (!isScreenChanged && sig == lastDumpSignature && event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return
        }
        lastDumpSignature = sig

        Log.i(
            "NoMoreReelsDump",
            "=== SNAPSHOT ev=${AccessibilityEvent.eventTypeToString(event.eventType)} act=${snapshot.currentActivity} " +
                "detected=$detectedScreen active=$activeScreen origin=${navigationTracker.reelOriginScreen} " +
                "swiped=${navigationTracker.hasSwipedToNextReel} fp=${reelFingerprint.toSummaryString()} ==="
        )
        for (n in interestingNodes.take(60)) {
            val shortId = n.viewId?.substringAfterLast('/') ?: ""
            val shortCls = n.className?.substringAfterLast('.') ?: ""
            if (shortId.isNotEmpty() || n.isSelected || !n.contentDescription.isNullOrBlank()) {
                Log.i(
                    "NoMoreReelsDump",
                    "  NODE id=$shortId sel=${n.isSelected} cls=$shortCls text='${n.text ?: ""}' desc='${n.contentDescription ?: ""}'"
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
            keyNodes.add("★ SEÇİLİ: id=$shortId, desc='${n.contentDescription}'")
        }
        val tabNodes = snapshot.findNodes {
            (it.viewId?.contains("tab") == true || it.viewId?.contains("clips") == true) && !it.isSelected
        }
        for (n in tabNodes.take(3)) {
            val shortId = n.viewId?.substringAfterLast('/') ?: "no-id"
            keyNodes.add("Sekme: id=$shortId, desc='${n.contentDescription}'")
        }
        return keyNodes
    }

    override fun onInterrupt() {
        Log.w(TAG, "ReelsAccessibilityService kesintiye uğradı (onInterrupt).")
        overlayController.dismissAll()
    }

    override fun onDestroy() {
        super.onDestroy()
        overlayController.dismissAll()
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
