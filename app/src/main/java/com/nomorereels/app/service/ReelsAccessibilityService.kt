package com.nomorereels.app.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.nomorereels.app.action.ActionExecutor
import com.nomorereels.app.detection.NodeSnapshot
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
        Log.i(TAG, "ReelsAccessibilityService başarıyla bağlandı ve dinlemeye başladı.")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val pkgName = event.packageName?.toString() ?: return
        if (pkgName != TARGET_PACKAGE) return

        val activityName = event.className?.toString()
        val rootNode = rootInActiveWindow ?: return

        // 1. Düğüm ağacını güvenli, hafif saf veri modeline dönüştür
        val snapshot = ScreenSnapshot.fromNodeInfo(
            packageName = pkgName,
            activityName = activityName,
            rootNode = rootNode
        )

        // 2. Ekranı sınıflandır
        val screenType = classifier.classify(snapshot)
        _lastDetectedType.value = screenType

        Log.d(TAG, "Instagram Olayı: ${event.eventType}, Ekran: $screenType, Activity: $activityName")

        // 3. Reels algılandıysa engelle / geri dön
        if (screenType.isReels) {
            Log.w(TAG, "Reels tespit edildi! Geri dönme tetikleniyor...")
            actionExecutor.triggerBackAction(reason = "Tespit: $screenType")
            logNodeTreeSummary(snapshot.rootNode)
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

    private fun logNodeTreeSummary(root: NodeSnapshot?) {
        if (root == null) return
        val nodesWithId = root.find { it.viewId != null || it.contentDescription != null }
        Log.d(TAG, "--- Reels Ekran Düğüm Özeti (Toplam ${nodesWithId.size} önemli düğüm) ---")
        nodesWithId.take(15).forEach { node ->
            Log.d(TAG, "Node: id=${node.viewId}, desc=${node.contentDescription}, selected=${node.isSelected}")
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
    }
}
