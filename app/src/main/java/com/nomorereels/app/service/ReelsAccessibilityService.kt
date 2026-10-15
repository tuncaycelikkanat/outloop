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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ReelsAccessibilityService : AccessibilityService() {

    private val classifier = ScreenClassifier()

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
        val screenType = classifier.classify(snapshot)
        _lastDetectedType.value = screenType

        Log.d(TAG, "Instagram Olayı: ${event.eventType}, Ekran: $screenType, Activity: $activityName")

        // 3. Reels algılandıysa engelle / yönlendir
        if (screenType.isReels) {
            Log.w(TAG, "Reels tespit edildi! Eylem çalıştırılıyor (Ekran: $screenType)...")
            val handled = actionExecutor.executeBlock(screenType, rootNode)
            DetectionLogStore.log(
                screenType = screenType,
                details = "Activity: ${activityName?.substringAfterLast('.')}",
                actionTaken = if (handled) "Engellendi (Ana Sayfaya Geçildi)" else "Beklemede (Cooldown)"
            )
        } else if (screenType != ScreenType.UNKNOWN) {
            DetectionLogStore.log(
                screenType = screenType,
                details = "Activity: ${activityName?.substringAfterLast('.')}",
                actionTaken = null
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

        private val _lastDetectedType = MutableStateFlow(ScreenType.UNKNOWN)
        val lastDetectedType: StateFlow<ScreenType> = _lastDetectedType.asStateFlow()

        private val _ruleVersion = MutableStateFlow(1)
        val ruleVersion: StateFlow<Int> = _ruleVersion.asStateFlow()
    }
}
