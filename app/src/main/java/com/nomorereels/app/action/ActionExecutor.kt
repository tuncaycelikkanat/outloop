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
import com.nomorereels.app.detection.RuleSet
import com.nomorereels.app.detection.ScreenType

enum class ExitReelsResult {
    PERFORMED,
    ALREADY_EXITING,
    FAILED
}

class ActionExecutor(
    private val serviceProvider: () -> AccessibilityService?,
    private val cooldownMs: Long = 500L
) {
    @Volatile
    var rules: RuleSet = RuleSet.DEFAULT

    private val mainHandler = Handler(Looper.getMainLooper())
    private var lastActionTime: Long = 0L
    private var recentAttempts = 0
    private var recentAttemptWindowStart: Long = 0L

    @Volatile
    private var isExitInProgress: Boolean = false
    @Volatile
    private var lastExitTime: Long = 0L

    val isExiting: Boolean
        get() = isExitInProgress || (System.currentTimeMillis() - lastExitTime < EXIT_IN_FLIGHT_MS)

    /**
     * Instagram Reels ekranından çıkılıp güvenli bir ekrana (Ana Akış, DM, Keşfet, Profil)
     * dönüldüğü an çağrılır. Tüm bekleme (cooldown) ve çıkış kilitlerini sıfırlayarak
     * kullanıcının hemen ardından tekrar Reels'e girmesi durumunda sıfır gecikmeyle
     * yeniden engelleme ve bildirim gösterilmesini sağlar.
     */
    fun onReturnedToNonReels() {
        recentAttempts = 0
        isExitInProgress = false
        lastExitTime = 0L
        lastActionTime = 0L
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
            // 1. Önce normal Cooldown kontrolü (aynı geçişin art arda gelen olayları devre kesiciyi şişirmemeli)
            if (now - lastActionTime < cooldownMs) {
                Log.d(TAG, "İşlem bekleme süresinde (cooldown aktif), atlandı. Ekran: $screenType")
                return false
            }

            // 2. Devre Kesici (Circuit Breaker): 2.5 saniyede 4'ten fazla gerçek deneme olursa kullanıcıyı kilitlememek için dur
            if (now - recentAttemptWindowStart > 2500L) {
                recentAttemptWindowStart = now
                recentAttempts = 0
            }
            recentAttempts++
            if (recentAttempts > 4) {
                Log.w(TAG, "Devre kesici devrede: Çok sık tetikleme oldu ($recentAttempts), kısa süre bekleniyor...")
                return false
            }
        }

        val service = serviceProvider()
        if (service == null) {
            Log.w(TAG, "Erişilebilirlik servisi hazır değil.")
            return false
        }

        val handled = exitReelsWithResult(screenType, null) == ExitReelsResult.PERFORMED

        if (handled) {
            lastActionTime = now
            recordBlockMetricsAndHaptic(service)
        }

        return handled
    }

    fun recordBlockMetricsAndHaptic(context: Context) {
        try {
            com.nomorereels.app.data.QuotaStore.incrementBlockCount(context)
        } catch (_: Exception) {}
        if (SettingsStore.hapticFeedbackEnabled.value) {
            performHapticPulse(context)
        }
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
     * P1-15: Halihazırda çıkış devam ediyorsa ALREADY_EXITING döndürerek çift sayımı önler.
     */
    fun exitReelsWithResult(
        screenType: ScreenType,
        originScreen: ScreenType? = null
    ): ExitReelsResult {
        val now = System.currentTimeMillis()
        if (isExiting) {
            Log.d(TAG, "exitReels: Çıkış işlemi zaten devam ediyor, spam önlendi.")
            return ExitReelsResult.ALREADY_EXITING
        }

        val service = serviceProvider() ?: run {
            Log.w(TAG, "exitReels: Erişilebilirlik servisi hazır değil.")
            return ExitReelsResult.FAILED
        }

        isExitInProgress = true
        lastExitTime = now
        mainHandler.postDelayed({ isExitInProgress = false }, EXIT_IN_FLIGHT_MS)

        Log.i(TAG, ">>> REELS'DEN ÇIKILIYOR (exitReels) - Ekran: $screenType, Köken: $originScreen <<<")

        val igRoot = findInstagramRootNode(service)
        var handled = false

        if (screenType == ScreenType.REELS_VIEWER) {
            if (igRoot != null) {
                handled = clickBackButton(igRoot, service)
            }
            if (!handled) {
                handled = service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
                Log.i(TAG, "REELS_VIEWER: GLOBAL_ACTION_BACK gönderildi, sonuç: $handled")
            }
        } else if (screenType == ScreenType.REELS_TAB) {
            if (igRoot != null) {
                handled = clickHomeTab(igRoot, service)
            }
            if (!handled) {
                handled = dispatchHomeTabTap(service)
            }
        } else {
            if (igRoot != null) {
                handled = clickBackButton(igRoot, service) || clickHomeTab(igRoot, service)
            }
            if (!handled) {
                handled = service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
            }
        }

        igRoot?.let { com.nomorereels.app.detection.safeRecycle(it) }

        return if (handled) ExitReelsResult.PERFORMED else ExitReelsResult.FAILED
    }

    fun exitReels(
        screenType: ScreenType,
        originScreen: ScreenType? = null
    ): Boolean = exitReelsWithResult(screenType, originScreen) == ExitReelsResult.PERFORMED

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
        val backIds = rules.actionTargets.backIds
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

        val screenHeight = service.resources.displayMetrics.heightPixels
        val backDescriptions = rules.actionTargets.backDescriptions
        val backNode = findNodeRecursive(root) { node ->
            val desc = node.contentDescription?.toString()?.trim()?.lowercase() ?: ""
            if (desc.isEmpty() || !backDescriptions.any { desc == it }) return@findNodeRecursive false
            val rect = Rect()
            node.getBoundsInScreen(rect)
            rect.width() > 0 && rect.height() > 0 && rect.centerY() < (screenHeight * 0.30f)
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
        // 1. Bilinen Home view ID'leri ile dene (rules.json'dan)
        val homeIds = rules.actionTargets.homeIds
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

        // 2. P1-12: Tam eşleşme ve ekranın alt %22'lik sekme çubuğu bölgesi kontrolü
        val screenHeight = service.resources.displayMetrics.heightPixels
        val homeDescriptions = rules.actionTargets.homeDescriptions
        val matchingNode = findNodeRecursive(root) { node ->
            val desc = node.contentDescription?.toString()?.trim()?.lowercase() ?: ""
            if (desc.isEmpty() || !homeDescriptions.any { desc == it }) return@findNodeRecursive false
            val rect = Rect()
            node.getBoundsInScreen(rect)
            rect.width() > 0 && rect.height() > 0 && rect.centerY() > (screenHeight * 0.78f)
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

        var bottomSystemInset = 0
        try {
            val wm = service.getSystemService(Context.WINDOW_SERVICE) as? android.view.WindowManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && wm != null) {
                val insets = wm.currentWindowMetrics.windowInsets
                    .getInsetsIgnoringVisibility(android.view.WindowInsets.Type.navigationBars())
                bottomSystemInset = insets.bottom
            } else {
                val resId = service.resources.getIdentifier("navigation_bar_height", "dimen", "android")
                if (resId > 0) {
                    bottomSystemInset = service.resources.getDimensionPixelSize(resId)
                }
            }
        } catch (_: Exception) {}

        val tabBarCenterOffsetPx = 24f * displayMetrics.density
        // 5 sekmeli alt barda en soldaki Home ikonu:
        // X = ekran genişliğinin %10'u
        // Y = sistem 3-tuşlu/jest navigasyon çubuğunun hemen üstündeki Instagram alt barının merkezi
        val x = width * 0.10f
        val y = (height - bottomSystemInset - tabBarCenterOffsetPx).coerceIn(height * 0.82f, height * 0.96f)
        Log.i(TAG, "Alt bar varsayılan Ana Sayfa koordinatına jest gönderiliyor: ($x, $y) [bottomInset=$bottomSystemInset]")
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
        const val EXIT_IN_FLIGHT_MS = 400L
    }
}
