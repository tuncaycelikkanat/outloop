package com.nomorereels.app.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.nomorereels.app.action.ActionExecutor
import com.nomorereels.app.detection.DetectionLogStore
import com.nomorereels.app.detection.RuleSet
import com.nomorereels.app.detection.ScreenClassifier
import com.nomorereels.app.detection.ScreenSnapshot
import com.nomorereels.app.detection.ScreenType
import com.nomorereels.app.policy.NavigationTracker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ReelsAccessibilityService : AccessibilityService() {

    private val classifier = ScreenClassifier()
    private val navigationTracker = NavigationTracker()

    override fun onServiceConnected() {
        super.onServiceConnected()
        currentInstance = this
        _isServiceActive.value = true

        // Dinamik kuralları assets/rules.json dosyasından yükle
        classifier.rules = loadRulesFromAssets()
        _ruleVersion.value = classifier.rules.version

        Log.i(TAG, "ReelsAccessibilityService bağlandı (Kural sürümü: ${classifier.rules.version}).")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val pkgName = event.packageName?.toString() ?: return
        if (pkgName != TARGET_PACKAGE) return

        val activityName = event.className?.toString()
        val rootNode = rootInActiveWindow ?: return

        // 1. Düğüm ağacını saf veri modeline dönüştür
        val snapshot = ScreenSnapshot.fromNodeInfo(
            packageName = pkgName,
            activityName = activityName,
            rootNode = rootNode
        )

        // 2. Ekranı dinamik kural motoruyla sınıflandır
        val detectedScreen = classifier.classify(snapshot)

        // Teşhis için ekrandaki kilit düğümleri topla
        val keyNodes = mutableListOf<String>()
        val selectedNodes = snapshot.findNodes { it.isSelected }
        for (n in selectedNodes.take(3)) {
            val shortId = n.viewId?.substringAfterLast('/') ?: "no-id"
            keyNodes.add("★ SEÇİLİ: id=$shortId, desc='${n.contentDescription}'")
        }
        val tabNodes = snapshot.findNodes { (it.viewId?.contains("tab") == true || it.viewId?.contains("clips") == true) && !it.isSelected }
        for (n in tabNodes.take(3)) {
            val shortId = n.viewId?.substringAfterLast('/') ?: "no-id"
            keyNodes.add("Sekme: id=$shortId, desc='${n.contentDescription}'")
        }

        // 3. Reels algılandıysa derhal engelle
        if (detectedScreen.isReels) {
            Log.w(TAG, "Reels tespit edildi! Eylem çalıştırılıyor (Ekran: $detectedScreen)...")
            val handled = actionExecutor.executeBlock(detectedScreen, rootNode)
            navigationTracker.update(detectedScreen)
            _lastDetectedType.value = detectedScreen

            DetectionLogStore.log(
                screenType = detectedScreen,
                details = "Activity: ${activityName?.substringAfterLast('.')}",
                actionTaken = if (handled) "Engellendi (Ana Sayfaya Geçildi)" else "Beklemede (Cooldown)",
                keyNodes = keyNodes
            )
            return
        }

        // 4. Normal ekran geçişi: YALNIZCA EKRAN DEĞİŞİMİ OLDUĞUNDA VE UNKNOWN DEĞİLSE LOGLA!
        val isScreenChanged = navigationTracker.update(detectedScreen)
        val activeScreen = navigationTracker.currentScreen
        _lastDetectedType.value = activeScreen

        if (isScreenChanged && activeScreen != ScreenType.UNKNOWN) {
            Log.i(TAG, ">>> EKRAN DEĞİŞİMİ: ${navigationTracker.previousScreen} -> $activeScreen <<<")
            DetectionLogStore.log(
                screenType = activeScreen,
                details = "Ekran: ${activeScreen.name} (${activityName?.substringAfterLast('.')})",
                actionTaken = null,
                keyNodes = keyNodes
            )
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "ReelsAccessibilityService kesintiye uğradı (onInterrupt).")
    }

    override fun onDestroy() {
        super.onDestroy()
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

        private val _isServiceActive = MutableStateFlow(false)
        val isServiceActive: StateFlow<Boolean> = _isServiceActive.asStateFlow()

        private val _lastDetectedType = MutableStateFlow(ScreenType.MAIN_FEED)
        val lastDetectedType: StateFlow<ScreenType> = _lastDetectedType.asStateFlow()

        private val _ruleVersion = MutableStateFlow(2)
        val ruleVersion: StateFlow<Int> = _ruleVersion.asStateFlow()
    }
}
