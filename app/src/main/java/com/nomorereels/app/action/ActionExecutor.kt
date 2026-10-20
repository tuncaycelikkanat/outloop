package com.nomorereels.app.action

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.nomorereels.app.detection.ScreenType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ActionExecutor(
    private val serviceProvider: () -> AccessibilityService?,
    private val cooldownMs: Long = 1200L
) {
    private var lastActionTime: Long = 0L
    private var recentAttempts = 0
    private var recentAttemptWindowStart: Long = 0L

    private val _blockCount = MutableStateFlow(0)
    val blockCount: StateFlow<Int> = _blockCount.asStateFlow()

    /**
     * Reels tespit edildiğinde uygun engelleme eylemini çalıştırır:
     * - REELS_TAB ise: Instagram'dan çıkmak yerine alt bardaki "Ana Sayfa" sekmesine tıklar!
     * - REELS_VIEWER (modal) ise: Modalı kapatmak için GLOBAL_ACTION_BACK tetikler.
     */
    fun executeBlock(
        screenType: ScreenType,
        rootNode: AccessibilityNodeInfo?,
        force: Boolean = false
    ): Boolean {
        val now = System.currentTimeMillis()

        if (!force) {
            // 1. Devre Kesici (Circuit Breaker): 2.5 saniyede 3'ten fazla deneme olursa kullanıcıyı kilitlememek için dur
            if (now - recentAttemptWindowStart > 2500L) {
                recentAttemptWindowStart = now
                recentAttempts = 0
            }
            recentAttempts++
            if (recentAttempts > 2) {
                Log.w(TAG, "Devre kesici devrede: Çok sık tetikleme oldu ($recentAttempts), 2 saniye bekleniyor...")
                return false
            }

            // 2. Normal Cooldown kontrolü
            if (now - lastActionTime < cooldownMs) {
                Log.d(TAG, "İşlem bekleme süresinde (cooldown aktif), atlandı. Ekran: $screenType")
                return false
            }
        }

        val service = serviceProvider()
        if (service == null) {
            Log.w(TAG, "Erişilebilirlik servisi hazır değil.")
            return false
        }

        var handled = false

        if (screenType == ScreenType.REELS_TAB && rootNode != null) {
            // Instagram'dan ÇIKMADAN doğrudan Ana Sayfa sekmesine geçiş yap
            Log.i(TAG, ">>> REELS SEKME TESPİTİ: Ana Sayfa sekmesine yönlendiriliyor... <<<")
            handled = clickHomeTab(rootNode)
            if (!handled) {
                Log.w(TAG, "Ana Sayfa sekmesi doğrudan bulunamadı, yedek olarak Geri (Back) deneniyor.")
                handled = service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
            }
        } else {
            // Modal tam ekran oynatıcı
            Log.i(TAG, ">>> REELS OYNATICI TESPİTİ: GLOBAL_ACTION_BACK tetikleniyor... <<<")
            handled = service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
        }

        if (handled) {
            lastActionTime = now
            _blockCount.value += 1
            service?.let { s ->
                try {
                    com.nomorereels.app.data.QuotaStore.incrementBlockCount(s)
                } catch (_: Exception) {}
            }
        }

        return handled
    }

    private fun clickHomeTab(root: AccessibilityNodeInfo): Boolean {
        // 1. Yol: Türkçe veya İngilizce içerik açıklaması ile Ana Sayfa'yı bul
        val homeDescriptions = listOf("ana sayfa", "home", "akış", "feed")
        val matchingNode = findNodeRecursive(root) { node ->
            val desc = node.contentDescription?.toString()?.lowercase() ?: ""
            homeDescriptions.any { desc == it || desc.startsWith(it) }
        }

        if (matchingNode != null) {
            val clicked = performClick(matchingNode)
            if (clicked) {
                Log.i(TAG, "Ana Sayfa sekmesine tıklandı (contentDescription ile).")
                return true
            }
        }

        // 2. Yol: Bilinen Home view ID'leri
        val homeIds = listOf(
            "com.instagram.android:id/feed_tab",
            "com.instagram.android:id/tab_home",
            "com.instagram.android:id/action_bar_home"
        )
        for (id in homeIds) {
            val nodes = root.findAccessibilityNodeInfosByViewId(id)
            if (!nodes.isNullOrEmpty()) {
                val firstNode = nodes[0]
                val clicked = performClick(firstNode)
                if (clicked) {
                    Log.i(TAG, "Ana Sayfa sekmesine tıklandı (viewId ile: $id).")
                    return true
                }
            }
        }

        return false
    }

    private fun findNodeRecursive(
        node: AccessibilityNodeInfo,
        predicate: (AccessibilityNodeInfo) -> Boolean
    ): AccessibilityNodeInfo? {
        if (predicate(node)) {
            return node
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findNodeRecursive(child, predicate)
            if (found != null) {
                return found
            }
        }
        return null
    }

    private fun performClick(node: AccessibilityNodeInfo): Boolean {
        var current: AccessibilityNodeInfo? = node
        while (current != null) {
            if (current.isClickable) {
                return current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            current = current.parent
        }
        return false
    }

    companion object {
        private const val TAG = "NoMoreReels"
    }
}
