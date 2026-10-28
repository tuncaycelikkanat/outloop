package com.nomorereels.app.action

import android.accessibilityservice.AccessibilityService
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.ClipDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.nomorereels.app.R
import com.nomorereels.app.data.AppLanguage
import com.nomorereels.app.data.SettingsStore
import com.nomorereels.app.policy.GatewaySpec
import com.nomorereels.app.policy.PolicyDecision
import com.nomorereels.app.ui.i18n.AppStrings

private fun getSerifBold(context: Context): Typeface =
    try { ResourcesCompat.getFont(context, R.font.plus_jakarta_sans_bold) ?: Typeface.DEFAULT_BOLD }
    catch (_: Exception) { Typeface.DEFAULT_BOLD }

private fun getSansBold(context: Context): Typeface =
    try { ResourcesCompat.getFont(context, R.font.plus_jakarta_sans_bold) ?: Typeface.DEFAULT_BOLD }
    catch (_: Exception) { Typeface.DEFAULT_BOLD }

private fun getSansRegular(context: Context): Typeface =
    try { ResourcesCompat.getFont(context, R.font.plus_jakarta_sans_regular) ?: Typeface.DEFAULT }
    catch (_: Exception) { Typeface.DEFAULT }

// Design System Color Tokens (Quiet Sage & Midnight Forest)
private const val COLOR_LIGHT_CANVAS = "#F7F7F2"
private const val COLOR_LIGHT_SURFACE = "#FFFFFF"
private const val COLOR_LIGHT_PRIMARY = "#52745D"
private const val COLOR_LIGHT_PRESSED = "#3F5D49"
private const val COLOR_LIGHT_ACCENT = "#DCE9DF"
private const val COLOR_LIGHT_TEXT = "#24332A"
private const val COLOR_LIGHT_MUTED = "#707A71"
private const val COLOR_LIGHT_BORDER = "#E4E7DE"

private const val COLOR_DARK_CANVAS = "#000000"
private const val COLOR_DARK_SURFACE = "#101310"
private const val COLOR_DARK_PRIMARY = "#A5C6A9"
private const val COLOR_DARK_ACCENT = "#273B2D"
private const val COLOR_DARK_TEXT = "#F0F3ED"
private const val COLOR_DARK_MUTED = "#A3ADA3"
private const val COLOR_DARK_BORDER = "#2B322C"

/**
 * Fiziksel veya jest tabanlı Geri (Back) tuşunu yakalayıp onay kapısını güvenle kapatan özel FrameLayout.
 * Kullanıcının ekranda hapis kalmasını (trap) önler.
 */
private class BackInterceptingFrameLayout(
    context: Context,
    private val onBackPress: () -> Unit
) : FrameLayout(context) {
    init {
        isFocusableInTouchMode = true
        isFocusable = true
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
            onBackPress()
            return true
        }
        return super.dispatchKeyEvent(event)
    }
}

/**
 * Erişilebilirlik katmanı (TYPE_ACCESSIBILITY_OVERLAY) veya uygulama katmanı
 * (TYPE_APPLICATION_OVERLAY) üzerinden:
 * 1. "Bilinçli Sürtünme (5 Saniye Basılı Tutma)" tam ekran onay kapısını,
 * 2. Engelleme / 2. videoya kaydırma koruması bilgi kartını (banner)
 * yöneten sınıf.
 */
class OverlayController(
    private val serviceProvider: () -> AccessibilityService?
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    private var frictionView: View? = null
    private var frictionWindowManager: WindowManager? = null
    @Volatile
    private var isFrictionOverlayPending: Boolean = false
    @Volatile
    private var suppressionUntilMs: Long = 0L
    private var holdTickRunnable: Runnable? = null
    private var pendingReleaseAudioRunnable: Runnable? = null

    val isFrictionOverlayShowing: Boolean
        get() = frictionView != null || isFrictionOverlayPending

    val isSuppressed: Boolean
        get() = System.currentTimeMillis() < suppressionUntilMs

    /**
     * Reels dışındaki bir ekrana dönüldüğünde sürtünme katmanı bastırma süresini derhal sıfırlar.
     */
    fun onReturnedToNonReels() {
        suppressionUntilMs = 0L
    }

    /**
     * Belirtilen süre boyunca bildirim/overlay olaylarını bastırır.
     */
    fun suppress(durationMs: Long) {
        val target = System.currentTimeMillis() + durationMs
        if (target > suppressionUntilMs) {
            suppressionUntilMs = target
        }
    }

    /**
     * DM'den açılan Reel için 5 saniye basılı tutma gerektiren "Bilinçli Sürtünme"
     * ekranını gösterir. Zaten açıksa tekrar açmaz.
     */
    fun showMindfulFrictionOverlay(
        fallbackContext: Context? = null,
        reelSummary: String? = null,
        holdDurationMs: Long = DEFAULT_HOLD_DURATION_MS,
        isNightMode: Boolean = false,
        onApproved: () -> Unit,
        onCancelAndGoBack: () -> Unit
    ): Boolean {
        if (isSuppressed) {
            Log.d(TAG, "Bilinçli Sürtünme bastırıldı (cooldown aktif).")
            return false
        }
        if (frictionView != null || isFrictionOverlayPending) return true

        val (targetContext, windowType) = resolveOverlayContextAndType(fallbackContext) ?: run {
            Log.w(TAG, "Overlay gösterilemedi: Uygun servis veya SYSTEM_ALERT_WINDOW izni yok.")
            return false
        }

        // Medyayı HEMEN duraklat (post beklemeden)
        pendingReleaseAudioRunnable?.let { mainHandler.removeCallbacks(it) }
        pendingReleaseAudioRunnable = null
        MediaPauseHelper.pauseMedia(targetContext)

        isFrictionOverlayPending = true

        mainHandler.post {
            if (frictionView != null) {
                isFrictionOverlayPending = false
                return@post
            }
            try {
                val wm = targetContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                val view = buildFrictionOverlayView(
                    context = targetContext,
                    reelSummary = reelSummary,
                    holdDurationMs = holdDurationMs,
                    isNightMode = isNightMode,
                    onApproved = {
                        dismissMindfulFrictionOverlay(releaseAudioImmediately = true)
                        onApproved()
                    },
                    onCancel = {
                        dismissMindfulFrictionOverlay(releaseAudioImmediately = false)
                        onCancelAndGoBack()
                    }
                )

                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    windowType,
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.CENTER
                    if (windowType == WindowManager.LayoutParams.TYPE_APPLICATION) {
                        findActivity(fallbackContext)?.window?.decorView?.windowToken?.let { token = it }
                    }
                }

                wm.addView(view, params)
                frictionView = view
                frictionWindowManager = wm
                isFrictionOverlayPending = false
                Log.i(TAG, "Bilinçli Sürtünme (5 sn basılı tutma) ekranı gösterildi.")
            } catch (e: Exception) {
                isFrictionOverlayPending = false
                MediaPauseHelper.releaseAudioFocus(targetContext)
                Log.e(TAG, "Bilinçli Sürtünme ekranı eklenirken hata: ${e.message}")
            }
        }
        return true
    }

    /**
     * Açık olan Bilinçli Sürtünme / Farkındalık Kapısı ekranını kapatır.
     * @param releaseAudioImmediately true ise ses odağı derhal bırakılır (onaylanıp Reels izlenmeye başlandığında).
     * false ise vazgeçildiğinde Reels'in arkada hemen tekrar çalmasını önlemek için 1.5 sn gecikmeyle bırakılır.
     */
    fun dismissMindfulFrictionOverlay(releaseAudioImmediately: Boolean = false) {
        // Onaylandığında (releaseAudioImmediately = true) bastırma uygulanmaz ki 2. videoya kaydırma anında yakalansın.
        suppressionUntilMs = if (releaseAudioImmediately) 0L else System.currentTimeMillis() + 800L
        isFrictionOverlayPending = false

        pendingReleaseAudioRunnable?.let { mainHandler.removeCallbacks(it) }
        pendingReleaseAudioRunnable = null

        val ctx = serviceProvider() ?: frictionView?.context
        if (ctx != null) {
            if (releaseAudioImmediately) {
                MediaPauseHelper.releaseAudioFocus(ctx)
            } else {
                val r = Runnable {
                    MediaPauseHelper.releaseAudioFocus(ctx)
                    pendingReleaseAudioRunnable = null
                }
                pendingReleaseAudioRunnable = r
                mainHandler.postDelayed(r, 1500L)
            }
        }

        val removeTask = Runnable {
            holdTickRunnable?.let { mainHandler.removeCallbacks(it) }
            holdTickRunnable = null

            val view = frictionView
            val wm = frictionWindowManager
            frictionView = null
            frictionWindowManager = null

            if (view != null && wm != null) {
                try {
                    wm.removeViewImmediate(view)
                } catch (_: Exception) {
                    try {
                        wm.removeView(view)
                    } catch (e: Exception) {
                        Log.w(TAG, "Friction overlay kaldırılırken hata: ${e.message}")
                    }
                }
            }
        }

        if (Looper.myLooper() == Looper.getMainLooper()) {
            removeTask.run()
        } else {
            mainHandler.post(removeTask)
        }
    }

    /**
     * Farkındalık Kapısı (Mindful Gateway):
     * Otomatik kaydırmayı kesmek için sakin bir nefes ve 10 saniyelik duraklama sunan tam ekran katman.
     */
    fun showMindfulGatewayOverlay(
        fallbackContext: Context? = null,
        isEnglish: Boolean = false,
        decision: PolicyDecision? = null,
        originLabel: String? = null,
        onApproved: (durationMinutes: Int, microPlan: String) -> Unit,
        onCancelAndGoBack: () -> Unit
    ): Boolean {
        val now = System.currentTimeMillis()
        if (now < suppressionUntilMs) {
            Log.d(TAG, "Farkındalık Kapısı bastırıldı (cooldown aktif).")
            return false
        }
        if (frictionView != null || isFrictionOverlayPending) return true

        val (targetContext, windowType) = resolveOverlayContextAndType(fallbackContext) ?: run {
            Log.w(TAG, "Overlay gösterilemedi: Uygun servis veya pencere bağlamı yok.")
            return false
        }

        // Medyayı HEMEN duraklat (post beklemeden)
        pendingReleaseAudioRunnable?.let { mainHandler.removeCallbacks(it) }
        pendingReleaseAudioRunnable = null
        MediaPauseHelper.pauseMedia(targetContext)

        isFrictionOverlayPending = true

        mainHandler.post {
            if (frictionView != null) {
                isFrictionOverlayPending = false
                return@post
            }
            try {
                val wm = targetContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                val view = buildMindfulGatewayView(
                    context = targetContext,
                    isEnglish = isEnglish,
                    decision = decision,
                    originLabel = originLabel,
                    onApproved = { duration, plan ->
                        dismissMindfulFrictionOverlay(releaseAudioImmediately = true)
                        onApproved(duration, plan)
                    },
                    onCancel = {
                        dismissMindfulFrictionOverlay(releaseAudioImmediately = false)
                        onCancelAndGoBack()
                    }
                )

                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    windowType,
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.CENTER
                    if (windowType == WindowManager.LayoutParams.TYPE_APPLICATION) {
                        findActivity(fallbackContext)?.window?.decorView?.windowToken?.let { token = it }
                    }
                }

                wm.addView(view, params)
                frictionView = view
                frictionWindowManager = wm
                isFrictionOverlayPending = false
                Log.i(TAG, "Farkındalık Kapısı (Mindful Gateway) ekranı gösterildi.")
            } catch (e: Exception) {
                isFrictionOverlayPending = false
                MediaPauseHelper.releaseAudioFocus(targetContext)
                Log.e(TAG, "Farkındalık Kapısı eklenirken hata: ${e.message}")
            }
        }
        return true
    }

    /**
     * Bilinçli pencere süresi dolduğunda nazik ve kısa hatırlatma hapı gösterir (microPlan alt metniyle).
     */
    fun showMindfulWindowExpired(
        microPlan: String,
        isEnglish: Boolean = false,
        fallbackContext: Context? = null
    ) {
        if (!SettingsStore.overlayFeedbackEnabled.value && fallbackContext == null) return
        val title = if (isEnglish) "Break's over 🌱" else "Mola bitti, devam 🌱"
        val sub = microPlan.trim().takeIf { it.isNotEmpty() }
        showPunchHolePill(title, sub, Color.parseColor("#274D3E"), fallbackContext, durationMs = 3000L)
    }

    private var bannerView: View? = null
    private var bannerPillView: View? = null
    private var bannerTitleView: TextView? = null
    private var bannerAnimator: ValueAnimator? = null
    private var bannerWindowManager: WindowManager? = null
    private var bannerParentGroup: android.view.ViewGroup? = null
    private var isBannerDismissing: Boolean = false
    private var pendingBannerShowRunnable: Runnable? = null
    private var bannerDismissRunnable: Runnable? = null
    private var bannerWatchdog: Runnable? = null
    private var bannerGen: Int = 0
    private var lastBannerShownAtMs: Long = 0L
    private var lastBannerSignature: String = ""

    /**
     * Servis veya Activity sonlandırılırken / kesintiye uğrarken tüm banner kaynaklarını derhal temizler.
     */
    fun release() {
        mainHandler.post { removeBannerImmediate() }
    }

    private fun scheduleDismiss(pill: View, gen: Int, durationMs: Long) {
        bannerDismissRunnable?.let { mainHandler.removeCallbacks(it) }
        bannerWatchdog?.let { mainHandler.removeCallbacks(it) }

        val dismiss = Runnable {
            if (gen == bannerGen) {
                startExit(pill, gen)
            }
        }
        bannerDismissRunnable = dismiss
        mainHandler.postDelayed(dismiss, durationMs)

        // Ne olursa olsun pencereyi kesin olarak kaldıran son çare güvenlik ağı
        val watchdog = Runnable {
            if (gen == bannerGen) {
                removeBannerImmediate()
            }
        }
        bannerWatchdog = watchdog
        mainHandler.postDelayed(watchdog, durationMs + 600L)
    }

    private fun startExit(pill: View, gen: Int) {
        isBannerDismissing = true
        bannerAnimator?.cancel()
        val endOffsetPx = -dp(pill.context, 8).toFloat()
        var cancelled = false
        val exitAnim = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 170L
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { anim ->
                val f = anim.animatedValue as Float
                pill.translationY = endOffsetPx * f
                val s = 1f - 0.08f * f
                pill.scaleX = s
                pill.scaleY = s
                pill.alpha = (1f - f).coerceIn(0f, 1f)
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationCancel(animation: android.animation.Animator) {
                    cancelled = true
                }
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    if (!cancelled && gen == bannerGen) {
                        removeBannerImmediate()
                    }
                }
            })
        }
        bannerAnimator = exitAnim
        exitAnim.start()
    }

    /**
     * Kamera deliği (punch-hole) ve durum çubuğunun (status bar) tam altında,
     * hiçbir zaman fiziksel kameranın altında kalmayan güvenli, kısa ve tatlı üst hap bildirimini gösterir.
     */
    fun showPunchHolePill(
        title: String,
        subtitle: String? = null,
        accentColor: Int,
        fallbackContext: Context? = null,
        durationMs: Long = 2200L,
        showDelayMs: Long = 0L
    ): Boolean {
        val resolved = resolveOverlayContextAndType(fallbackContext)
        val activityFallback = findActivity(fallbackContext)
        if (resolved == null && activityFallback == null) {
            Log.w(TAG, "Üst hap bildirimi gösterilemedi: Aktif servis veya Activity bulunamadı.")
            return false
        }

        pendingBannerShowRunnable?.let { mainHandler.removeCallbacks(it) }
        val task = Runnable {
            pendingBannerShowRunnable = null
            val now = System.currentTimeMillis()
            val displayText = if (!subtitle.isNullOrBlank()) "$title · $subtitle" else title

            // P0: 150ms içinde gelen tamamen aynı metinli çağrıyı yok say (seri tetikleme titremesini engelle)
            if (displayText == lastBannerSignature && (now - lastBannerShownAtMs) < 150L && bannerView != null) {
                return@Runnable
            }

            // 1. HIZLI YOL: Halihazırda ekranda sağlıklı bir pencere varsa (ve kapanmıyorsa),
            // pencereyi WindowManager'da yıkıp baştan kurma gecikmesini önle!
            val existingRoot = bannerView
            val existingPill = bannerPillView
            if (fallbackContext == null && existingRoot != null && existingPill != null
                && existingRoot.isAttachedToWindow && !isBannerDismissing) {
                bannerAnimator?.cancel()
                isBannerDismissing = false

                bannerTitleView?.text = displayText
                lastBannerSignature = displayText
                lastBannerShownAtMs = now

                // Canlı yenileme animasyonu: hafif pop (0.93 -> 1.0)
                existingPill.scaleX = 0.93f
                existingPill.scaleY = 0.93f
                existingPill.alpha = 0.7f
                existingPill.translationY = 0f
                val refreshAnim = ValueAnimator.ofFloat(0f, 1f).apply {
                    duration = 180L
                    interpolator = android.view.animation.OvershootInterpolator(1.1f)
                    addUpdateListener { anim ->
                        val f = anim.animatedValue as Float
                        val s = 0.93f + 0.07f * f
                        existingPill.scaleX = s
                        existingPill.scaleY = s
                        existingPill.alpha = (0.7f + 0.3f * f).coerceIn(0f, 1f)
                    }
                }
                bannerAnimator = refreshAnim
                refreshAnim.start()

                scheduleDismiss(existingPill, bannerGen, durationMs)
                return@Runnable
            }

            removeBannerImmediate()

            val renderContext = resolved?.first ?: activityFallback ?: return@Runnable
            val wm = renderContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val outerPadTop = dp(renderContext, 12)
            val outerPadBottom = dp(renderContext, 16)
            val outerPadHorizontal = dp(renderContext, 16)
            val safeTopPx = (computeSafeTopOffsetPx(renderContext, wm) - outerPadTop).coerceAtLeast(0)

            val (outerContainer, pillView, titleTextView) = buildPunchHolePillWrapper(
                context = renderContext,
                text = displayText,
                accentColor = accentColor,
                padHorizontal = outerPadHorizontal,
                padTop = outerPadTop,
                padBottom = outerPadBottom
            )

            var addedToWindow = false

            // 1. Öncelik: Erişilebilirlik katmanı (TYPE_ACCESSIBILITY_OVERLAY) veya Activity alt penceresi (TYPE_APPLICATION)
            if (resolved != null) {
                try {
                    val params = buildBannerLayoutParams(resolved.second, safeTopPx).apply {
                        if (resolved.second == WindowManager.LayoutParams.TYPE_APPLICATION) {
                            activityFallback?.window?.decorView?.windowToken?.let { token = it }
                        }
                    }
                    wm.addView(outerContainer, params)
                    bannerView = outerContainer
                    bannerPillView = pillView
                    bannerTitleView = titleTextView
                    bannerWindowManager = wm
                    addedToWindow = true
                } catch (e: Exception) {
                    Log.w(TAG, "Birincil WindowManager üst bildirim hatası (${resolved.second}): ${e.message}")
                }
            }

            // 2. Yedek: Uygulama içi önizleme veya servis kapalıyken Activity decorView üzerine doğrudan ekle
            if (!addedToWindow && activityFallback != null && !activityFallback.isFinishing && !activityFallback.isDestroyed) {
                try {
                    val decorGroup = activityFallback.window?.decorView as? android.view.ViewGroup
                    if (decorGroup != null) {
                        val lp = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.WRAP_CONTENT,
                            FrameLayout.LayoutParams.WRAP_CONTENT,
                            Gravity.TOP or Gravity.CENTER_HORIZONTAL
                        ).apply {
                            topMargin = safeTopPx
                        }
                        outerContainer.isClickable = false
                        outerContainer.isFocusable = false
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            outerContainer.elevation = dp(renderContext, 24).toFloat()
                        }
                        decorGroup.addView(outerContainer, lp)
                        bannerView = outerContainer
                        bannerPillView = pillView
                        bannerTitleView = titleTextView
                        bannerParentGroup = decorGroup
                        addedToWindow = true
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Activity decorView üst bildirim ekleme hatası: ${e.message}")
                }
            }

            if (!addedToWindow) return@Runnable

            val currentGen = bannerGen
            isBannerDismissing = false
            lastBannerSignature = displayText
            lastBannerShownAtMs = now

            // Dismiss zamanlayıcısını ve watchdog'unu hemen kur (attach veya post gecikmesine bağlama!)
            scheduleDismiss(pillView, currentGen, durationMs)

            val startOffsetPx = -dp(renderContext, 10).toFloat()
            pillView.translationY = startOffsetPx
            pillView.scaleX = 0.92f
            pillView.scaleY = 0.92f
            pillView.alpha = 0f

            outerContainer.post {
                if (currentGen == bannerGen && bannerPillView === pillView) {
                    val enterAnim = ValueAnimator.ofFloat(0f, 1f).apply {
                        duration = 200L
                        interpolator = android.view.animation.OvershootInterpolator(1.05f)
                        addUpdateListener { anim ->
                            val f = anim.animatedValue as Float
                            pillView.translationY = startOffsetPx * (1f - f)
                            val s = 0.92f + 0.08f * f
                            pillView.scaleX = s
                            pillView.scaleY = s
                            pillView.alpha = f.coerceIn(0f, 1f)
                        }
                    }
                    bannerAnimator = enterAnim
                    enterAnim.start()
                }
            }
        }

        if (showDelayMs > 0L) {
            pendingBannerShowRunnable = task
            mainHandler.postDelayed(task, showDelayMs)
        } else {
            mainHandler.post(task)
        }
        return true
    }

    private fun buildBannerLayoutParams(windowType: Int, safeTopPx: Int): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = safeTopPx
            // Sistem pencere animasyonunu kapat: Instagram'ın geri geçişi sürerken WindowManager
            // bu animasyonu erteleyip pencereyi yarı saydam bekletiyordu. Animasyonu kendimiz yapıyoruz.
            windowAnimations = 0
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            }
        }
    }

    /**
     * Cihazın ön kamera deliği (displayCutout) ve durum çubuğu (statusBars) yüksekliğini
     * hesaplayarak bildirimin ASLA kameranın altında kalmamasını sağlayan güvenli üst boşluğu döndürür.
     */
    private fun computeSafeTopOffsetPx(context: Context, wm: WindowManager): Int {
        var safeTop = 0

        // 1. Android 11+ (API 30+): WindowMetrics üzerinden hem statusBar hem displayCutout yüksekliği
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val windowInsets = wm.currentWindowMetrics.windowInsets
                val insets = windowInsets.getInsetsIgnoringVisibility(
                    android.view.WindowInsets.Type.statusBars() or
                        android.view.WindowInsets.Type.displayCutout()
                )
                safeTop = maxOf(safeTop, insets.top)
                windowInsets.displayCutout?.safeInsetTop?.let {
                    safeTop = maxOf(safeTop, it)
                }
            } catch (_: Exception) {}
        }

        // 2. Android 10+ (API 29+): Display.getCutout yedek kontrolü
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                @Suppress("DEPRECATION")
                wm.defaultDisplay?.cutout?.safeInsetTop?.let {
                    safeTop = maxOf(safeTop, it)
                }
            } catch (_: Exception) {}
        }

        // 3. Klasik Android status_bar_height sistem kaynağı
        try {
            val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
            if (resourceId > 0) {
                safeTop = maxOf(safeTop, context.resources.getDimensionPixelSize(resourceId))
            }
        } catch (_: Exception) {}

        // 4. Kamera deliği olan modern ekranlar için en az 38dp taban + 10dp ferahlık payı (~48dp)
        val minCameraCutoutHeight = dp(context, 38)
        return maxOf(safeTop, minCameraCutoutHeight) + dp(context, 10)
    }

    /**
     * Reels engellendiğinde veya 2. videoya kaydırıldığında
     * ekranın üst kısmında kısa, tatlı bir hap bildirimi gösterir.
     */
    fun showBlockBanner(
        decision: PolicyDecision,
        fallbackContext: Context? = null,
        durationMs: Long = 2200L,
        showDelayMs: Long = 0L
    ) {
        if (fallbackContext == null && !SettingsStore.overlayFeedbackEnabled.value) {
            return
        }
        val targetCtx = fallbackContext ?: serviceProvider()
        val (titleText, subText, accentColor) = getBannerStringsAndColor(decision, targetCtx)
        showPunchHolePill(titleText, subText, accentColor, fallbackContext, durationMs, showDelayMs)
    }

    /**
     * Kota eşiğine yaklaşıldığında ekranın üstünde dokunmayı engellemeyen kısa bir uyarı hapı gösterir.
     */
    fun showQuotaWarningBanner(
        title: String,
        message: String? = null,
        fallbackContext: Context? = null,
        durationMs: Long = 2400L
    ) {
        if (!SettingsStore.overlayFeedbackEnabled.value && fallbackContext == null) return
        showPunchHolePill(title, message, Color.parseColor("#D4973B"), fallbackContext, durationMs)
    }

    private fun removeBannerImmediate() {
        bannerGen++ // Eski tüm bekleyen callback ve animasyonları derhal geçersiz kılar
        pendingBannerShowRunnable?.let { mainHandler.removeCallbacks(it) }
        pendingBannerShowRunnable = null
        bannerDismissRunnable?.let { mainHandler.removeCallbacks(it) }
        bannerDismissRunnable = null
        bannerWatchdog?.let { mainHandler.removeCallbacks(it) }
        bannerWatchdog = null
        isBannerDismissing = false

        val anim = bannerAnimator
        bannerAnimator = null
        if (anim != null) {
            try {
                anim.removeAllListeners()
                anim.cancel()
            } catch (_: Exception) {}
        }

        val view = bannerView
        val wm = bannerWindowManager
        val parentGroup = bannerParentGroup
        bannerView = null
        bannerPillView = null
        bannerTitleView = null
        bannerWindowManager = null
        bannerParentGroup = null

        if (view != null) {
            if (wm != null) {
                try {
                    wm.removeViewImmediate(view)
                } catch (e: Exception) {
                    Log.w(TAG, "removeViewImmediate başarısız: ${e.message}")
                    try { wm.removeView(view) } catch (_: Exception) {}
                }
            }
            if (parentGroup != null) {
                try {
                    parentGroup.removeView(view)
                } catch (_: Exception) {}
            }
        }
    }

    fun dismissAll() {
        dismissMindfulFrictionOverlay(releaseAudioImmediately = true)
        mainHandler.post { removeBannerImmediate() }
    }

    private fun findActivity(context: Context?): android.app.Activity? {
        var ctx = context
        while (ctx is android.content.ContextWrapper) {
            if (ctx is android.app.Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }

    private fun resolveOverlayContextAndType(fallbackContext: Context?): Pair<Context, Int>? {
        val service = serviceProvider()
        if (service != null) {
            return service to WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        }
        val act = findActivity(fallbackContext)
        if (act != null && !act.isFinishing && !act.isDestroyed) {
            return act to WindowManager.LayoutParams.TYPE_APPLICATION
        }
        return null
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun buildFrictionOverlayView(
        context: Context,
        reelSummary: String?,
        holdDurationMs: Long,
        isNightMode: Boolean = false,
        onApproved: () -> Unit,
        onCancel: () -> Unit
    ): View {
        val isEnglish = SettingsStore.appLanguage.value == AppLanguage.EN
        val root = BackInterceptingFrameLayout(context, onCancel).apply {
            setBackgroundColor(Color.parseColor("#E6000000"))
            setPadding(dp(context, 20), dp(context, 24), dp(context, 20), dp(context, 24))
            addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View) {}
                override fun onViewDetachedFromWindow(v: View) {
                    holdTickRunnable?.let { mainHandler.removeCallbacks(it) }
                    holdTickRunnable = null
                }
            })
        }

        val cardBg = if (isNightMode) COLOR_DARK_SURFACE else COLOR_LIGHT_SURFACE
        val cardBorder = if (isNightMode) COLOR_DARK_BORDER else COLOR_LIGHT_BORDER
        val titleColor = if (isNightMode) COLOR_DARK_TEXT else COLOR_LIGHT_TEXT
        val textColor = if (isNightMode) COLOR_DARK_MUTED else COLOR_LIGHT_MUTED
        val badgeBg = if (isNightMode) COLOR_DARK_ACCENT else COLOR_LIGHT_ACCENT
        val badgeText = if (isNightMode) COLOR_DARK_PRIMARY else COLOR_LIGHT_PRIMARY
        val primaryBtnBg = if (isNightMode) COLOR_DARK_PRIMARY else COLOR_LIGHT_PRIMARY
        val primaryBtnText = if (isNightMode) Color.parseColor(COLOR_DARK_SURFACE) else Color.WHITE
        val secondaryBtnBg = if (isNightMode) COLOR_DARK_ACCENT else COLOR_LIGHT_ACCENT
        val secondaryBtnText = if (isNightMode) COLOR_DARK_PRIMARY else COLOR_LIGHT_PRIMARY

        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor(cardBg))
                cornerRadius = dp(context, 22).toFloat()
                setStroke(dp(context, 1), Color.parseColor(cardBorder))
            }
            setPadding(dp(context, 24), dp(context, 28), dp(context, 24), dp(context, 24))
        }

        val badge = TextView(context).apply {
            text = if (isEnglish) "MINDFUL GATEWAY · DM REEL" else "FARKINDALIK KAPISI · DM REEL"
            setTextColor(Color.parseColor(badgeText))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            typeface = getSansBold(context)
            background = GradientDrawable().apply {
                setColor(Color.parseColor(badgeBg))
                cornerRadius = dp(context, 16).toFloat()
            }
            setPadding(dp(context, 12), dp(context, 6), dp(context, 12), dp(context, 6))
        }
        card.addView(badge, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        val title = TextView(context).apply {
            text = if (isEnglish) "Take a Breath. On Autopilot?" else "Nefes Al. Otomatik Pilotta mısın?"
            setTextColor(Color.parseColor(titleColor))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
            typeface = getSerifBold(context)
            gravity = Gravity.CENTER
            setPadding(0, dp(context, 16), 0, dp(context, 8))
        }
        card.addView(title)

        val subtitleText = if (isEnglish) {
            "This Reel was opened from a DM conversation.\n\nIf you truly want to watch it, hold the button below for 5 seconds. Only this single video is allowed; swiping to the 2nd video will return to chat."
        } else {
            "Bu Reel bir DM sohbetinden açıldı.\n\nGerçekten izlemek istiyorsan aşağıdaki butona 5 saniye basılı tut. Yalnızca bu videoya izin verilir; 2. videoya kaydırırsan otomatik olarak sohbete dönülür."
        }

        val subtitle = TextView(context).apply {
            text = subtitleText
            setTextColor(Color.parseColor(textColor))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            typeface = getSansRegular(context)
            gravity = Gravity.CENTER
            setLineSpacing(dp(context, 3).toFloat(), 1.0f)
            setPadding(0, 0, 0, dp(context, 20))
        }
        card.addView(subtitle)

        val progressBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 1000
            progress = 0
            val bgDrawable = GradientDrawable().apply {
                setColor(Color.parseColor(if (isNightMode) COLOR_DARK_CANVAS else COLOR_LIGHT_CANVAS))
                cornerRadius = dp(context, 8).toFloat()
            }
            val fgShape = GradientDrawable().apply {
                setColor(Color.parseColor(primaryBtnBg))
                cornerRadius = dp(context, 8).toFloat()
            }
            val clip = ClipDrawable(fgShape, Gravity.START, ClipDrawable.HORIZONTAL)
            progressDrawable = LayerDrawable(arrayOf(bgDrawable, clip)).apply {
                setId(0, android.R.id.background)
                setId(1, android.R.id.progress)
            }
        }
        card.addView(progressBar, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(context, 10)
        ).apply {
            bottomMargin = dp(context, 16)
        })

        val holdButton = Button(context).apply {
            text = if (isEnglish) "Hold 5 Seconds to Watch" else "İzlemek İçin 5 Saniye Basılı Tut"
            isAllCaps = false
            setTextColor(Color.parseColor(secondaryBtnText))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            typeface = getSansBold(context)
            background = GradientDrawable().apply {
                setColor(Color.parseColor(secondaryBtnBg))
                cornerRadius = dp(context, 22).toFloat()
                setStroke(dp(context, 1), Color.parseColor(secondaryBtnBg))
            }
            setPadding(dp(context, 16), dp(context, 14), dp(context, 16), dp(context, 14))
        }

        var holdStartTime = 0L
        var isHolding = false

        holdButton.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    isHolding = true
                    holdStartTime = System.currentTimeMillis()
                    v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    (holdButton.background as? GradientDrawable)?.setColor(Color.parseColor(primaryBtnBg))
                    holdButton.setTextColor(primaryBtnText)

                    val tick = object : Runnable {
                        override fun run() {
                            if (!isHolding) return
                            val elapsed = System.currentTimeMillis() - holdStartTime
                            val ratio = (elapsed.toFloat() / holdDurationMs.toFloat()).coerceIn(0f, 1f)
                            progressBar.progress = (ratio * 1000).toInt()

                            val remainingSec = ((holdDurationMs - elapsed).coerceAtLeast(0L)) / 1000f
                            holdButton.text = if (isEnglish) {
                                String.format("Holding... %.1f s", remainingSec)
                            } else {
                                String.format("Basılı Tutuluyor... %.1f sn", remainingSec)
                            }

                            if (elapsed >= holdDurationMs) {
                                isHolding = false
                                v.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                onApproved()
                            } else {
                                mainHandler.postDelayed(this, 50L)
                            }
                        }
                    }
                    holdTickRunnable = tick
                    mainHandler.post(tick)
                    true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (isHolding) {
                        isHolding = false
                        holdTickRunnable?.let { mainHandler.removeCallbacks(it) }
                        holdTickRunnable = null
                        progressBar.progress = 0
                        holdButton.text = if (isEnglish) "Released Early · Hold 5s" else "Yarıda Bırakıldı · 5 Sn Basılı Tut"
                        (holdButton.background as? GradientDrawable)?.setColor(Color.parseColor(secondaryBtnBg))
                        holdButton.setTextColor(Color.parseColor(secondaryBtnText))
                    }
                    true
                }

                else -> false
            }
        }

        card.addView(holdButton, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = dp(context, 12)
        })

        val cancelButton = Button(context).apply {
            text = if (isEnglish) "Cancel & Return to Chat" else "Vazgeç ve Sohbete Dön"
            isAllCaps = false
            setTextColor(primaryBtnText)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            typeface = getSansBold(context)
            background = GradientDrawable().apply {
                setColor(Color.parseColor(primaryBtnBg))
                cornerRadius = dp(context, 22).toFloat()
            }
            setPadding(dp(context, 16), dp(context, 14), dp(context, 16), dp(context, 14))
            setOnClickListener {
                isEnabled = false
                onCancel()
            }
        }

        card.addView(cancelButton, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        root.addView(card, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.CENTER
        ))

        return root
    }

    @SuppressLint("SetTextI18n")
    private fun buildMindfulGatewayView(
        context: Context,
        isEnglish: Boolean,
        decision: PolicyDecision? = null,
        originLabel: String? = null,
        onApproved: (durationMinutes: Int, microPlan: String) -> Unit,
        onCancel: () -> Unit
    ): View {
        val isDark = SettingsStore.isDarkThemeActiveNow(context)

        val cardBg = if (isDark) COLOR_DARK_SURFACE else COLOR_LIGHT_SURFACE
        val cardBorder = if (isDark) COLOR_DARK_BORDER else COLOR_LIGHT_BORDER
        val titleColor = if (isDark) COLOR_DARK_TEXT else COLOR_LIGHT_TEXT
        val textColor = if (isDark) COLOR_DARK_MUTED else COLOR_LIGHT_MUTED
        val badgeBg = if (isDark) COLOR_DARK_ACCENT else COLOR_LIGHT_ACCENT
        val badgeText = if (isDark) COLOR_DARK_PRIMARY else COLOR_LIGHT_PRIMARY
        val primaryBtnBg = if (isDark) COLOR_DARK_PRIMARY else COLOR_LIGHT_PRIMARY
        val primaryBtnText = if (isDark) Color.parseColor(COLOR_DARK_SURFACE) else Color.WHITE
        val breathingCircleBg = if (isDark) COLOR_DARK_ACCENT else COLOR_LIGHT_ACCENT
        val breathingCircleStroke = if (isDark) COLOR_DARK_PRIMARY else COLOR_LIGHT_PRIMARY

        val snoozeCountToday = SettingsStore.snoozeCountToday.value
        val isBedtime = SettingsStore.isBedtimeActiveNow()
        val canApprove = GatewaySpec.canApproveSnooze(isBedtime, snoozeCountToday)
        val remainingSnoozes = GatewaySpec.remainingDailySnoozes(snoozeCountToday)

        var countdownRunnable: Runnable? = null
        var breathPhaseRunnable: Runnable? = null
        var breathAnim: ValueAnimator? = null

        fun cleanupAnim() {
            countdownRunnable?.let { mainHandler.removeCallbacks(it) }
            countdownRunnable = null
            breathPhaseRunnable?.let { mainHandler.removeCallbacks(it) }
            breathPhaseRunnable = null
            breathAnim?.cancel()
            breathAnim = null
        }

        val root = BackInterceptingFrameLayout(context) {
            cleanupAnim()
            onCancel()
        }.apply {
            setBackgroundColor(Color.parseColor("#E60A0F0D"))
            setPadding(dp(context, 20), dp(context, 24), dp(context, 20), dp(context, 24))
            contentDescription = if (isEnglish) "Mindful Gateway Pause" else "Farkındalık Kapısı Duraklaması"
            addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View) {
                    v.announceForAccessibility(contentDescription)
                }
                override fun onViewDetachedFromWindow(v: View) {
                    cleanupAnim()
                }
            })
        }

        val scroll = ScrollView(context).apply {
            isFillViewport = true
        }

        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor(cardBg))
                cornerRadius = dp(context, 24).toFloat()
                setStroke(dp(context, 1), Color.parseColor(cardBorder))
            }
            setPadding(dp(context, 24), dp(context, 24), dp(context, 24), dp(context, 22))
        }

        // 1. Bağlamsal Rozet (4.3-4)
        val badge = TextView(context).apply {
            text = GatewaySpec.contextualBadgeText(decision, originLabel, isEnglish)
            setTextColor(Color.parseColor(badgeText))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            typeface = getSansBold(context)
            background = GradientDrawable().apply {
                setColor(Color.parseColor(badgeBg))
                cornerRadius = dp(context, 16).toFloat()
            }
            setPadding(dp(context, 12), dp(context, 6), dp(context, 12), dp(context, 6))
        }
        card.addView(badge, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        // 2. Başlık (Title)
        val titleView = TextView(context).apply {
            text = if (isEnglish) "Take a Deep Breath" else "Derin Bir Nefes Al"
            setTextColor(Color.parseColor(titleColor))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            typeface = getSerifBold(context)
            gravity = Gravity.CENTER
            setPadding(0, dp(context, 14), 0, dp(context, 6))
        }
        card.addView(titleView)

        // 3. Açıklama (Subtitle)
        val subtitleView = TextView(context).apply {
            text = if (isEnglish)
                "Notice the automatic scrolling reflex. Give yourself a moment to choose consciously."
            else
                "Otomatik kaydırma refleksini fark et. Bilinçli bir seçim yapmak için kendine bir an tanı."
            setTextColor(Color.parseColor(textColor))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
            typeface = getSansRegular(context)
            gravity = Gravity.CENTER
            setLineSpacing(dp(context, 3).toFloat(), 1.0f)
            setPadding(0, 0, 0, dp(context, 16))
        }
        card.addView(subtitleView)

        // 4. Nefes Çemberi Animasyonu + Nefes Ritmi Metni (4.3-5)
        val circleContainer = FrameLayout(context).apply {
            setPadding(0, dp(context, 4), 0, dp(context, 6))
        }
        val circleSize = dp(context, 74)
        val circleView = View(context).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor(breathingCircleBg))
                setStroke(dp(context, 3), Color.parseColor(breathingCircleStroke))
            }
        }
        val innerCircleSize = dp(context, 36)
        val innerCircleView = View(context).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor(breathingCircleStroke))
            }
        }
        circleContainer.addView(circleView, FrameLayout.LayoutParams(circleSize, circleSize, Gravity.CENTER))
        circleContainer.addView(innerCircleView, FrameLayout.LayoutParams(innerCircleSize, innerCircleSize, Gravity.CENTER))
        card.addView(circleContainer)

        var isInhale = true
        val breathPhaseLabel = TextView(context).apply {
            text = GatewaySpec.breathingPhaseText(isInhale, isEnglish)
            setTextColor(Color.parseColor(badgeText))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = getSansBold(context)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(context, 12))
        }
        card.addView(breathPhaseLabel)

        val phaseTask = object : Runnable {
            override fun run() {
                isInhale = !isInhale
                breathPhaseLabel.text = GatewaySpec.breathingPhaseText(isInhale, isEnglish)
                mainHandler.postDelayed(this, 2200L)
            }
        }
        breathPhaseRunnable = phaseTask
        mainHandler.postDelayed(phaseTask, 2200L)

        val animatorsEnabled = try { ValueAnimator.areAnimatorsEnabled() } catch (_: Throwable) { true }
        if (animatorsEnabled) {
            breathAnim = ValueAnimator.ofFloat(0.75f, 1.15f).apply {
                duration = 2200L
                repeatMode = ValueAnimator.REVERSE
                repeatCount = ValueAnimator.INFINITE
                interpolator = AccelerateDecelerateInterpolator()
                addUpdateListener { anim ->
                    val scale = anim.animatedValue as Float
                    circleView.scaleX = scale
                    circleView.scaleY = scale
                }
                start()
            }
        }

        // 5. Birincil Buton: "Vazgeç ve Geri Dön"
        val btnCancel = Button(context).apply {
            text = if (isEnglish) "Put Phone Down & Return" else "Vazgeç ve Geri Dön"
            isAllCaps = false
            setTextColor(primaryBtnText)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            typeface = getSansBold(context)
            background = GradientDrawable().apply {
                setColor(Color.parseColor(primaryBtnBg))
                cornerRadius = dp(context, 22).toFloat()
            }
            setPadding(dp(context, 18), dp(context, 14), dp(context, 18), dp(context, 14))
            setOnClickListener {
                isEnabled = false
                cleanupAnim()
                onCancel()
            }
        }
        card.addView(btnCancel, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = dp(context, 8)
            bottomMargin = dp(context, 14)
        })

        // 6. İkincil Eylem: Bedtime / Günlük Sınır Kontrolü + Süre Seçimi & 3 Hak Sayacı
        val lockReason = GatewaySpec.lockReasonText(isBedtime, snoozeCountToday, isEnglish)
        if (!canApprove || lockReason != null) {
            val lockView = TextView(context).apply {
                text = lockReason ?: ""
                setTextColor(Color.parseColor(textColor))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
                typeface = getSansRegular(context)
                gravity = Gravity.CENTER
                setPadding(dp(context, 8), dp(context, 6), dp(context, 8), dp(context, 4))
            }
            card.addView(lockView)
        } else {
            var selectedDuration = GatewaySpec.DEFAULT_DURATION_MINUTES
            var remainingSeconds = GatewaySpec.computeInitialWaitSeconds(snoozeCountToday)

            lateinit var continueText: TextView

            fun updateContinueLabel() {
                continueText.text = GatewaySpec.allowActionText(
                    durationMinutes = selectedDuration,
                    remainingSeconds = remainingSeconds,
                    remainingSnoozes = remainingSnoozes,
                    isEnglish = isEnglish
                )
            }

            // Süre seçimi satırı (3 / 5 / 10 dk)
            val durationRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }
            val durationChipViews = mutableListOf<Pair<Int, TextView>>()
            fun refreshDurationChips() {
                durationChipViews.forEach { (mins, tv) ->
                    val selected = mins == selectedDuration
                    tv.background = GradientDrawable().apply {
                        setColor(if (selected) Color.parseColor(badgeBg) else Color.TRANSPARENT)
                        cornerRadius = dp(context, 12).toFloat()
                        setStroke(dp(context, 1), Color.parseColor(if (selected) primaryBtnBg else cardBorder))
                    }
                    tv.setTextColor(Color.parseColor(if (selected) badgeText else textColor))
                    tv.typeface = if (selected) getSansBold(context) else getSansRegular(context)
                }
            }
            GatewaySpec.DURATION_OPTIONS.forEach { mins ->
                val chip = TextView(context).apply {
                    text = if (isEnglish) "$mins min" else "$mins dk"
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                    setPadding(dp(context, 12), dp(context, 6), dp(context, 12), dp(context, 6))
                    setOnClickListener {
                        selectedDuration = mins
                        refreshDurationChips()
                        updateContinueLabel()
                    }
                }
                durationChipViews.add(mins to chip)
                durationRow.addView(chip, LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginStart = dp(context, 4)
                    marginEnd = dp(context, 4)
                })
            }
            refreshDurationChips()
            card.addView(durationRow, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(context, 8)
            })

            continueText = TextView(context).apply {
                setTextColor(Color.parseColor(textColor).let {
                    Color.argb(160, Color.red(it), Color.green(it), Color.blue(it))
                })
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                typeface = getSansRegular(context)
                gravity = Gravity.CENTER
                setPadding(dp(context, 16), dp(context, 8), dp(context, 16), dp(context, 8))
                isClickable = false
                isEnabled = false
            }
            updateContinueLabel()

            val runnable = object : Runnable {
                override fun run() {
                    remainingSeconds--
                    if (remainingSeconds > 0) {
                        updateContinueLabel()
                        mainHandler.postDelayed(this, 1000L)
                    } else {
                        updateContinueLabel()
                        continueText.isEnabled = true
                        continueText.isClickable = true
                        continueText.setTextColor(Color.parseColor(primaryBtnBg))
                        continueText.paintFlags = continueText.paintFlags or android.graphics.Paint.UNDERLINE_TEXT_FLAG
                    }
                }
            }
            countdownRunnable = runnable
            mainHandler.postDelayed(runnable, 1000L)

            continueText.setOnClickListener {
                if (remainingSeconds <= 0) {
                    cleanupAnim()
                    onApproved(selectedDuration, "")
                }
            }

            card.addView(continueText, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = dp(context, 4)
            })
        }

        scroll.addView(card)
        root.addView(scroll, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.CENTER
        ))

        return root
    }

    data class NotificationVariant(val tr: String, val en: String)

    private val swipeVariants = listOf(
        NotificationVariant("Bir tane yeter 🌿", "One is enough 🌿"),
        NotificationVariant("Odağın sende kalsın ✨", "Keep your focus ✨"),
        NotificationVariant("Burada durabilirsin 🍃", "You can stop right here 🍃"),
        NotificationVariant("Geri dönmeye ne dersin? 🌿", "How about heading back? 🌿"),
        NotificationVariant("Döngüyü kırdın 🌱", "Loop broken 🌱"),
        NotificationVariant("Derin bir nefes al 🌬️", "Take a deep breath 🌬️"),
        NotificationVariant("Farkındalık kazandı 🌿", "Mindfulness won 🌿"),
        NotificationVariant("Küçük bir mola yetti ☕", "A short pause was enough ☕"),
        NotificationVariant("Gerçek dünya seni bekliyor 🌤️", "The real world awaits 🌤️"),
        NotificationVariant("Ana dönme vakti 🌿", "Time to return to the present 🌿"),
        NotificationVariant("Zamanın sana kalsın ✨", "Keep your time for yourself ✨"),
        NotificationVariant("Otopilot durduruldu 🛑", "Autopilot stopped 🛑"),
        NotificationVariant("Kendine alan açtın 🍃", "You made space for yourself 🍃"),
        NotificationVariant("Bugünlük bu video yetti 🌿", "That reel was enough 🌿"),
        NotificationVariant("Akıştan çıkma cesareti 🌱", "Courage to step out 🌱")
    )

    private val timeoutVariants = listOf(
        NotificationVariant("Bu kadar mola yeter 🍃", "That's enough for now 🍃"),
        NotificationVariant("Zaman doldu, odağa devam 🌱", "Time's up, back to focus 🌱"),
        NotificationVariant("Mola tamamlandı 🌿", "Break completed 🌿"),
        NotificationVariant("Küçük bir mola iyi geldi ✨", "A nice little reset ✨"),
        NotificationVariant("Şimdi odaklanma zamanı 🎯", "Time to get focused 🎯"),
        NotificationVariant("Nefes aldın, şimdi devam 🌬️", "Recharged, time to continue 🌬️")
    )

    private val bedtimeVariants = listOf(
        NotificationVariant("İyi geceler, dinlen 🌙", "Rest well tonight 🌙"),
        NotificationVariant("Yarın yeni bir gün 🌟", "Tomorrow is a new day 🌟"),
        NotificationVariant("Gözlerini dinlendirme vakti 😴", "Time to rest your eyes 😴"),
        NotificationVariant("Huzurlu bir uyku çek 🌙", "Sleep peacefully 🌙"),
        NotificationVariant("Gece odağı devrede 🌌", "Night focus active 🌌")
    )

    private val quotaVariants = listOf(
        NotificationVariant("Bugünlük bu kadar ✨", "All done for today ✨"),
        NotificationVariant("Günlük hedefini korudun 🌿", "You protected today's goal 🌿"),
        NotificationVariant("Harika bir sınır çizdin 🌱", "Great boundary set today 🌱"),
        NotificationVariant("Bugünkü kotan doldu 🎯", "Daily quota met 🎯"),
        NotificationVariant("Kendine verdiğin sözü tuttun 🍃", "You kept your promise 🍃")
    )

    private val defaultVariants = listOf(
        NotificationVariant("Odağın sende 🌿", "Focus stays yours 🌿"),
        NotificationVariant("Döngüyü kırdın 🌱", "Loop broken 🌱"),
        NotificationVariant("Farkındalıkla devam et 🍃", "Move forward mindfully 🍃")
    )

    private var lastSelectedSwipeIndex: Int = -1
    private var lastSelectedTimeoutIndex: Int = -1
    private var lastSelectedBedtimeIndex: Int = -1
    private var lastSelectedQuotaIndex: Int = -1
    private var lastSelectedDefaultIndex: Int = -1

    private fun pickRandomVariant(
        list: List<NotificationVariant>,
        lastIndex: Int,
        onIndexSelected: (Int) -> Unit
    ): NotificationVariant {
        if (list.isEmpty()) return NotificationVariant("Bir tane yeter 🌿", "One is enough 🌿")
        if (list.size == 1) return list[0]
        var newIndex = java.util.concurrent.ThreadLocalRandom.current().nextInt(list.size)
        if (newIndex == lastIndex) {
            newIndex = (newIndex + 1 + java.util.concurrent.ThreadLocalRandom.current().nextInt(list.size - 1)) % list.size
        }
        onIndexSelected(newIndex)
        return list[newIndex]
    }

    fun getBannerStringsAndColor(
        decision: PolicyDecision,
        context: Context? = null
    ): Triple<String, String?, Int> {
        val isEnglish = SettingsStore.appLanguage.value == AppLanguage.EN
        val isDark = context?.let { SettingsStore.isDarkThemeActiveNow(it) } ?: false
        val calmSage = Color.parseColor(if (isDark) COLOR_DARK_PRIMARY else COLOR_LIGHT_PRIMARY)

        val variant = when (decision) {
            PolicyDecision.BLOCK_AFTER_SWIPE -> pickRandomVariant(swipeVariants, lastSelectedSwipeIndex) {
                lastSelectedSwipeIndex = it
            }
            PolicyDecision.BLOCK_AFTER_TIMEOUT -> pickRandomVariant(timeoutVariants, lastSelectedTimeoutIndex) {
                lastSelectedTimeoutIndex = it
            }
            PolicyDecision.BLOCK_BEDTIME -> pickRandomVariant(bedtimeVariants, lastSelectedBedtimeIndex) {
                lastSelectedBedtimeIndex = it
            }
            PolicyDecision.BLOCK_DAILY_COUNT_LIMIT,
            PolicyDecision.BLOCK_DAILY_TIME_LIMIT -> pickRandomVariant(quotaVariants, lastSelectedQuotaIndex) {
                lastSelectedQuotaIndex = it
            }
            else -> pickRandomVariant(defaultVariants, lastSelectedDefaultIndex) {
                lastSelectedDefaultIndex = it
            }
        }

        val titleText = if (isEnglish) variant.en else variant.tr
        val accentColor = if (decision == PolicyDecision.BLOCK_BEDTIME) {
            Color.parseColor(if (isDark) "#9FA8DA" else "#5C6BC0")
        } else {
            calmSage
        }

        return Triple(titleText, null, accentColor)
    }

    private fun buildPunchHolePillWrapper(
        context: Context,
        text: String,
        accentColor: Int,
        padHorizontal: Int,
        padTop: Int,
        padBottom: Int
    ): Triple<FrameLayout, View, TextView> {
        val isDark = SettingsStore.isDarkThemeActiveNow(context)

        val bgColor = if (isDark) Color.parseColor("#FA111613") else Color.parseColor("#FAFCFCFA")
        val borderColor = if (isDark) Color.parseColor("#324436") else Color.parseColor("#D6E0D7")
        val titleColor = if (isDark) Color.parseColor("#F0F4ED") else Color.parseColor("#1B2820")
        val haloColor = Color.argb(
            if (isDark) 58 else 44,
            Color.red(accentColor),
            Color.green(accentColor),
            Color.blue(accentColor)
        )

        val screenWidth = context.resources.displayMetrics.widthPixels
        val maxTextWidth = (screenWidth - dp(context, 96)).coerceAtLeast(dp(context, 160))

        lateinit var titleTextView: TextView

        val pillView = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply {
                setColor(bgColor)
                cornerRadius = dp(context, 999).toFloat()
                setStroke(dp(context, 1), borderColor)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                elevation = dp(context, 6).toFloat()
            }
            setPadding(dp(context, 14), dp(context, 9), dp(context, 16), dp(context, 9))

            // Parlayan çift katmanlı nokta göstergesi
            val dotContainer = FrameLayout(context).apply {
                addView(View(context).apply {
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(haloColor)
                    }
                }, FrameLayout.LayoutParams(dp(context, 14), dp(context, 14), Gravity.CENTER))

                addView(View(context).apply {
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(accentColor)
                    }
                }, FrameLayout.LayoutParams(dp(context, 7), dp(context, 7), Gravity.CENTER))
            }

            addView(dotContainer, LinearLayout.LayoutParams(dp(context, 14), dp(context, 14)).apply {
                marginEnd = dp(context, 9)
                gravity = Gravity.CENTER_VERTICAL
            })

            titleTextView = TextView(context).apply {
                this.text = text
                setTextColor(titleColor)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
                typeface = getSansBold(context)
                includeFontPadding = false
                maxLines = 1
                maxWidth = maxTextWidth
                ellipsize = android.text.TextUtils.TruncateAt.END
            }
            addView(titleTextView)
        }

        val outerContainer = FrameLayout(context).apply {
            clipChildren = false
            clipToPadding = false
            setPadding(padHorizontal, padTop, padHorizontal, padBottom)
            addView(
                pillView,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER
                )
            )
        }

        return Triple(outerContainer, pillView, titleTextView)
    }

    private fun dp(context: Context, value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }

    companion object {
        private const val TAG = "NoMoreReels"
        const val DEFAULT_HOLD_DURATION_MS = 5000L
    }
}
