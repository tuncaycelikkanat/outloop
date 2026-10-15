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

        // Teşhis: Seçili düğümleri ve Reels ile ilgili düğümleri logla
        val selectedNodes = snapshot.findNodes { it.isSelected }
        for (node in selectedNodes) {
            Log.d(TAG, "--> SEÇİLİ DÜĞÜM: id=${node.viewId}, desc='${node.contentDescription}', class=${node.className}")
        }

        val reelsNodes = snapshot.findNodes { 
            val desc = it.contentDescription ?: ""
            val id = it.viewId ?: ""
            desc.contains("reels", ignoreCase = true) || id.contains("clips", ignoreCase = true)
        }
        for (node in reelsNodes) {
            Log.d(TAG, "--> REELS DÜĞÜMÜ: id=${node.viewId}, desc='${node.contentDescription}', isSelected=${node.isSelected}")
        }

        // 3. Reels algılandıysa engelle / yönlendir
        if (screenType.isReels) {
            Log.w(TAG, "Reels tespit edildi! Eylem çalıştırılıyor (Ekran: $screenType)...")
            actionExecutor.executeBlock(screenType, rootNode)
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
