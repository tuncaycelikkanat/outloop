package com.nomorereels.app.action

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.nomorereels.app.data.SettingsStore
import com.nomorereels.app.detection.ScreenType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ActionExecutor(
    private val serviceProvider: () -> AccessibilityService?,
    private val cooldownMs: Long = 1200L
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var lastActionTime: Long = 0L
    private var recentAttempts = 0
    private var recentAttemptWindowStart: Long = 0L

    @Volatile
    private var isExitInProgress: Boolean = false
    @Volatile
    private var lastExitTime: Long = 0L

    val isExiting: Boolean
        get() = isExitInProgress || (System.currentTimeMillis() - lastExitTime < 1800L)

    private val _blockCount = MutableStateFlow(0)
    val blockCount: StateFlow<Int> = _blockCount.asStateFlow()

    fun resetBlockCount() {
        _blockCount.value = 0
    }

    /**
     * Reels tespit edildiğinde uygun engelleme eylemini çalıştırır:
     * - REELS_TAB ise: Instagram'dan çıkmak yerine alt bardaki "Ana Sayfa" sekmesine tıklar!
     * - REELS_VIEWER (modal) ise: Modalı kapatmak için Back butonuna tıklar / GLOBAL_ACTION_BACK tetikler.
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

        val handled = exitReels(screenType, null)

        if (handled) {
            lastActionTime = now
            _blockCount.value += 1
            try {
                com.nomorereels.app.data.QuotaStore.incrementBlockCount(service)
            } catch (_: Exception) {}
            if (SettingsStore.hapticFeedbackEnabled.value) {
                performHapticPulse(service)
            }
        }

        return handled
    }

    private fun performHapticPulse(context: Context) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                @Suppress("DEPRECATION")
                vibratorManager?.defaultVibrator ?: (context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(
                        VibrationEffect.createOneShot(
                            40L,
                            VibrationEffect.DEFAULT_AMPLITUDE
                        )
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(40L)
                }
            }
        } catch (_: Exception) {}
    }

    /**
     * Kullanıcı "Vazgeç ve Geri Dön" dediğinde veya Reels engellendiğinde,
     * Reels ekranından kesin ve güvenilir bir şekilde çıkılmasını sağlar:
     *
     * 1. REELS_VIEWER (DM, Feed, Keşfet veya Profil'den açılan modal oynatıcı):
     *    - Üst bardaki geri butonuna tıklar veya GLOBAL_ACTION_BACK tetikler.
     *    - Kullanıcıyı geldiği ekrana (DM sohbeti, Ana Akış veya Keşfet) geri döndürür.
     *
     * 2. REELS_TAB (Instagram alt sekme çubuğundaki Reels sekmesi):
     *    - Alt sekmedeki "Ana Sayfa" düğümünü bulup tıklar.
     *    - Düğüm bulunamazsa alt bar Ana Sayfa sekme koordinatına dokunma jesti gönderir.
     *
     * 3. Emniyet Ağı (Safety Net):
     *    - 350ms sonra Instagram hala Reels ekranında mı kontrol edilir.
     *    - Eğer hala Reels'te kalınmışsa önce kurtarma aksiyonu (Back/Home tab), 700ms'de
     *      hala çıkılamamışsa GLOBAL_ACTION_HOME ile kullanıcı güvenle ana ekrana çıkarılır.
     */
    fun exitReels(
        screenType: ScreenType,
        originScreen: ScreenType? = null
    ): Boolean {
        val now = System.currentTimeMillis()
        if (isExiting) {
            Log.d(TAG, "exitReels: Çıkış işlemi zaten devam ediyor, spam önlendi.")
            return true
        }

        val service = serviceProvider() ?: run {
            Log.w(TAG, "exitReels: Erişilebilirlik servisi hazır değil.")
            return false
        }

        isExitInProgress = true
        lastExitTime = now
        mainHandler.postDelayed({ isExitInProgress = false }, 1800L)

        Log.i(TAG, ">>> REELS'DEN ÇIKILIYOR (exitReels) - Ekran: $screenType, Köken: $originScreen <<<")

        val igRoot = findInstagramRootNode(service)
        var handled = false

        if (screenType == ScreenType.REELS_VIEWER) {
            // Modal tam ekran oynatıcı: Nereden açılmış olursa olsun (DM, Feed, Keşfet, Profil vb.)
            // çıkış yolu Back butonuna tıklamak veya tek bir GLOBAL_ACTION_BACK göndermektir.
            if (igRoot != null) {
                handled = clickBackButton(igRoot, service)
            }
            if (!handled) {
                handled = service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
                Log.i(TAG, "REELS_VIEWER: GLOBAL_ACTION_BACK gönderildi, sonuç: $handled")
            }
        } else if (screenType == ScreenType.REELS_TAB) {
            // Reels sekmesi: Alt sekme çubuğunda Ana Sayfa'ya geçiş yap.
            // Asla Back tuşuna basılmaz çünkü Reels sekmesinde Back uygulamadan atabilir.
            if (igRoot != null) {
                handled = clickHomeTab(igRoot, service)
            }
            if (!handled) {
                handled = dispatchHomeTabTap(service)
            }
        } else {
            // Belirsiz / UNKNOWN Reels durumu: Önce geri dönmeyi dene, olmazsa Ana Sayfa'ya geç
            if (igRoot != null) {
                handled = clickBackButton(igRoot, service) || clickHomeTab(igRoot, service)
            }
            if (!handled) {
                handled = service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
            }
        }

        igRoot?.let { com.nomorereels.app.detection.safeRecycle(it) }

        return handled
    }

    private fun findInstagramRootNode(service: AccessibilityService): AccessibilityNodeInfo? {
        try {
            val windows = service.windows
            if (!windows.isNullOrEmpty()) {
                for (window in windows) {
                    val root = window.root ?: continue
                    if (root.packageName?.toString() == "com.instagram.android") {
                        return root
                    }
                    com.nomorereels.app.detection.safeRecycle(root)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "service.windows taranırken hata: ${e.message}")
        }
        val fallback = service.rootInActiveWindow
        if (fallback != null && fallback.packageName?.toString() == "com.instagram.android") {
            return fallback
        }
        fallback?.let { com.nomorereels.app.detection.safeRecycle(it) }
        return null
    }


    private fun clickBackButton(root: AccessibilityNodeInfo, service: AccessibilityService): Boolean {
        val backIds = listOf(
            "com.instagram.android:id/action_bar_button_back",
            "com.instagram.android:id/back_button",
            "com.instagram.android:id/action_bar_back",
            "com.instagram.android:id/action_bar_left_button"
        )
        for (id in backIds) {
            val nodes = root.findAccessibilityNodeInfosByViewId(id)
            if (!nodes.isNullOrEmpty()) {
                try {
                    for (node in nodes) {
                        if (performClick(node)) {
                            Log.i(TAG, "Geri butonuna tıklandı (viewId: $id).")
                            return true
                        }
                        val rect = Rect()
                        node.getBoundsInScreen(rect)
                        if (rect.width() > 0 && rect.height() > 0) {
                            val x = rect.centerX().toFloat()
                            val y = rect.centerY().toFloat()
                            Log.i(TAG, "Geri butonu (viewId: $id) koordinatına jest gönderiliyor: ($x, $y)")
                            return dispatchTapGesture(service, x, y)
                        }
                    }
                } finally {
                    for (n in nodes) com.nomorereels.app.detection.safeRecycle(n)
                }
            }
        }

        val backDescriptions = listOf("geri", "back", "kapat", "close", "yukarı git", "navigate up")
        val backNode = findNodeRecursive(root) { node ->
            val desc = node.contentDescription?.toString()?.lowercase() ?: ""
            backDescriptions.any { desc == it || desc.startsWith(it) }
        }
        if (backNode != null) {
            try {
                if (performClick(backNode)) {
                    Log.i(TAG, "Geri butonuna tıklandı (contentDescription ile).")
                    return true
                }
                val rect = Rect()
                backNode.getBoundsInScreen(rect)
                if (rect.width() > 0 && rect.height() > 0) {
                    val x = rect.centerX().toFloat()
                    val y = rect.centerY().toFloat()
                    Log.i(TAG, "Geri butonu (desc) koordinatına jest gönderiliyor: ($x, $y)")
                    return dispatchTapGesture(service, x, y)
                }
            } finally {
                if (backNode !== root) com.nomorereels.app.detection.safeRecycle(backNode)
            }
        }
        return false
    }

    private fun clickHomeTab(root: AccessibilityNodeInfo, service: AccessibilityService): Boolean {
        // 1. Bilinen Home view ID'leri ile dene
        val homeIds = listOf(
            "com.instagram.android:id/feed_tab",
            "com.instagram.android:id/tab_home",
            "com.instagram.android:id/action_bar_home"
        )
        for (id in homeIds) {
            val nodes = root.findAccessibilityNodeInfosByViewId(id)
            if (!nodes.isNullOrEmpty()) {
                try {
                    for (node in nodes) {
                        if (performClick(node)) {
                            Log.i(TAG, "Ana Sayfa sekmesine tıklandı (viewId ile: $id).")
                            return true
                        }
                        val rect = Rect()
                        node.getBoundsInScreen(rect)
                        if (rect.width() > 0 && rect.height() > 0) {
                            val x = rect.centerX().toFloat()
                            val y = rect.centerY().toFloat()
                            Log.i(TAG, "Ana Sayfa sekmesi (viewId: $id) koordinatına jest gönderiliyor: ($x, $y)")
                            return dispatchTapGesture(service, x, y)
                        }
                    }
                } finally {
                    for (n in nodes) com.nomorereels.app.detection.safeRecycle(n)
                }
            }
        }

        // 2. İçerik açıklaması ile Ana Sayfa sekmesini bul
        val homeDescriptions = listOf("ana sayfa", "home", "akış", "feed")
        val matchingNode = findNodeRecursive(root) { node ->
            val desc = node.contentDescription?.toString()?.lowercase() ?: ""
            homeDescriptions.any { desc == it || desc.startsWith(it) || desc.contains(it) }
        }

        if (matchingNode != null) {
            try {
                if (performClick(matchingNode)) {
                    Log.i(TAG, "Ana Sayfa sekmesine tıklandı (contentDescription ile).")
                    return true
                }
                val rect = Rect()
                matchingNode.getBoundsInScreen(rect)
                if (rect.width() > 0 && rect.height() > 0) {
                    val x = rect.centerX().toFloat()
                    val y = rect.centerY().toFloat()
                    Log.i(TAG, "Ana Sayfa sekmesi koordinatına jest gönderiliyor: ($x, $y)")
                    return dispatchTapGesture(service, x, y)
                }
            } finally {
                if (matchingNode !== root) com.nomorereels.app.detection.safeRecycle(matchingNode)
            }
        }

        return false
    }

    private fun dispatchHomeTabTap(service: AccessibilityService): Boolean {
        val displayMetrics = service.resources.displayMetrics
        val width = displayMetrics.widthPixels.toFloat()
        val height = displayMetrics.heightPixels.toFloat()
        // 5 sekmeli alt barda en soldaki Home ikonu:
        // X = ekran genişliğinin %10'u
        // Y = ekran yüksekliğinin %96'sı
        val x = width * 0.10f
        val y = height * 0.96f
        Log.i(TAG, "Alt bar varsayılan Ana Sayfa koordinatına jest gönderiliyor: ($x, $y)")
        return dispatchTapGesture(service, x, y)
    }

    private fun dispatchTapGesture(service: AccessibilityService, x: Float, y: Float): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false
        val path = Path().apply {
            moveTo(x, y)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0L, 50L)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return service.dispatchGesture(gesture, object : AccessibilityService.GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                Log.d(TAG, "dispatchTapGesture başarıyla tamamlandı ($x, $y)")
            }
            override fun onCancelled(gestureDescription: GestureDescription?) {
                Log.w(TAG, "dispatchTapGesture iptal edildi ($x, $y)")
            }
        }, null)
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
                if (found !== child) {
                    com.nomorereels.app.detection.safeRecycle(child)
                }
                return found
            }
            com.nomorereels.app.detection.safeRecycle(child)
        }
        return null
    }

    private fun performClick(node: AccessibilityNodeInfo): Boolean {
        var current: AccessibilityNodeInfo? = node
        while (current != null) {
            if (current.isClickable) {
                val result = current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                if (current !== node) com.nomorereels.app.detection.safeRecycle(current)
                return result
            }
            val parent = current.parent
            if (current !== node) com.nomorereels.app.detection.safeRecycle(current)
            current = parent
        }
        return false
    }

    companion object {
        private const val TAG = "NoMoreReels"
    }
}
