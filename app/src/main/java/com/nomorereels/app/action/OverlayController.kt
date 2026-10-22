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
import android.provider.Settings
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
import com.nomorereels.app.data.MindfulBarrierType
import com.nomorereels.app.data.SettingsStore
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

    private var bannerView: View? = null
    private var bannerWindowManager: WindowManager? = null
    private var bannerDismissRunnable: Runnable? = null
    private var holdTickRunnable: Runnable? = null
    private var pendingReleaseAudioRunnable: Runnable? = null

    val isFrictionOverlayShowing: Boolean
        get() = frictionView != null || isFrictionOverlayPending

    val isSuppressed: Boolean
        get() = System.currentTimeMillis() < suppressionUntilMs

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
        if (frictionView != null) return true

        val (targetContext, windowType) = resolveOverlayContextAndType(fallbackContext) ?: run {
            Log.w(TAG, "Overlay gösterilemedi: Uygun servis veya SYSTEM_ALERT_WINDOW izni yok.")
            return false
        }

        // Medyayı HEMEN duraklat (post beklemeden)
        pendingReleaseAudioRunnable?.let { mainHandler.removeCallbacks(it) }
        pendingReleaseAudioRunnable = null
        MediaPauseHelper.pauseMedia(targetContext)

        mainHandler.post {
            if (frictionView != null) return@post
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
                }

                wm.addView(view, params)
                frictionView = view
                frictionWindowManager = wm
                Log.i(TAG, "Bilinçli Sürtünme (5 sn basılı tutma) ekranı gösterildi.")
            } catch (e: Exception) {
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
        suppressionUntilMs = System.currentTimeMillis() + 3000L
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

        mainHandler.post {
            holdTickRunnable?.let { mainHandler.removeCallbacks(it) }
            holdTickRunnable = null

            val view = frictionView
            val wm = frictionWindowManager
            frictionView = null
            frictionWindowManager = null

            if (view != null && wm != null) {
                try {
                    wm.removeView(view)
                } catch (e: Exception) {
                    Log.w(TAG, "Friction overlay kaldırılırken hata: ${e.message}")
                }
            }
        }
    }

    /**
     * Farkındalık Kapısı (Mindful Gateway):
     * Otomatik kaydırmayı kesmek için sakin bir nefes ve 10 saniyelik duraklama sunan tam ekran katman.
     */
    fun showMindfulGatewayOverlay(
        fallbackContext: Context? = null,
        barrierType: MindfulBarrierType = MindfulBarrierType.BREATH_AND_INTENT,
        isNightMode: Boolean = false,
        isEnglish: Boolean = false,
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
                val view = buildMindfulGatewayView(
                    context = targetContext,
                    barrierType = barrierType,
                    isNightMode = isNightMode,
                    isEnglish = isEnglish,
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
                }

                wm.addView(view, params)
                frictionView = view
                frictionWindowManager = wm
                isFrictionOverlayPending = false
                Log.i(TAG, "Farkındalık Kapısı (Mindful Gateway) ekranı gösterildi.")
            } catch (e: Exception) {
                isFrictionOverlayPending = false
                Log.e(TAG, "Farkındalık Kapısı eklenirken hata: ${e.message}")
            }
        }
        return true
    }

    /**
     * Bilinçli pencere süresi dolduğunda seçilen niyetle birlikte nazik hatırlatma banner'ı gösterir.
     */
    fun showMindfulWindowExpired(
        microPlan: String,
        isEnglish: Boolean = false,
        fallbackContext: Context? = null
    ) {
        val strings = AppStrings.get(if (isEnglish) AppLanguage.EN else AppLanguage.TR)
        val title = strings.gatewayWindowExpiredTitle
        val sub = strings.gatewayWindowExpiredDesc(microPlan)
        showPunchHolePill(title, sub, Color.parseColor("#274D3E"), fallbackContext, durationMs = 3500L)
    }

    fun showPunchHolePill(
        title: String,
        subtitle: String? = null,
        accentColor: Int,
        fallbackContext: Context? = null,
        durationMs: Long = 2500L
    ): Boolean {
        val (targetContext, windowType) = resolveOverlayContextAndType(fallbackContext) ?: return false

        mainHandler.post {
            removeBannerImmediate()

            try {
                val wm = targetContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                val view = buildPunchHolePillView(targetContext, title, subtitle, accentColor)

                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    windowType,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                    y = if (windowType == WindowManager.LayoutParams.TYPE_APPLICATION) dp(targetContext, 24) else dp(targetContext, 12)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                    }
                }

                view.scaleX = 0.3f
                view.scaleY = 0.3f
                view.alpha = 0f

                wm.addView(view, params)
                bannerView = view
                bannerWindowManager = wm

                view.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .alpha(1.0f)
                    .setDuration(220L)
                    .setInterpolator(android.view.animation.OvershootInterpolator(1.2f))
                    .start()

                val dismissTask = Runnable {
                    view.animate()
                        .scaleX(0.3f)
                        .scaleY(0.3f)
                        .alpha(0f)
                        .setDuration(180L)
                        .withEndAction { removeBannerImmediate() }
                        .start()
                }
                bannerDismissRunnable = dismissTask
                mainHandler.postDelayed(dismissTask, durationMs)
            } catch (e: Exception) {
                Log.w(TAG, "Nokta kamera hap bildirimi gösterilemedi: ${e.message}")
            }
        }
        return true
    }

    /**
     * Reels engellendiğinde veya DM'de 2. videoya kaydırma yakalandığında ekranın üst kısmında
     * dokunmayı engellemeyen (FLAG_NOT_TOUCHABLE) kısa bir farkındalık bildirimi gösterir.
     */
    fun showBlockBanner(
        decision: PolicyDecision,
        fallbackContext: Context? = null,
        durationMs: Long = 2400L
    ) {
        val targetCtx = fallbackContext ?: serviceProvider()
        val (titleText, subText, accentColor) = getBannerStringsAndColor(decision, targetCtx)
        if (SettingsStore.punchHoleAlertEnabled.value) {
            showPunchHolePill(titleText, subText, accentColor, fallbackContext, durationMs)
            return
        }

        val (targetContext, windowType) = resolveOverlayContextAndType(fallbackContext) ?: return

        mainHandler.post {
            removeBannerImmediate()

            try {
                val wm = targetContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                val view = buildCustomBannerView(targetContext, titleText, subText, accentColor)

                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    windowType,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                    y = if (windowType == WindowManager.LayoutParams.TYPE_APPLICATION) dp(targetContext, 24) else dp(targetContext, 56)
                }

                view.scaleX = 0.3f
                view.scaleY = 0.3f
                view.alpha = 0f

                wm.addView(view, params)
                bannerView = view
                bannerWindowManager = wm

                view.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .alpha(1.0f)
                    .setDuration(220L)
                    .setInterpolator(android.view.animation.OvershootInterpolator(1.2f))
                    .start()

                val dismissTask = Runnable {
                    view.animate()
                        .scaleX(0.3f)
                        .scaleY(0.3f)
                        .alpha(0f)
                        .setDuration(180L)
                        .withEndAction { removeBannerImmediate() }
                        .start()
                }
                bannerDismissRunnable = dismissTask
                mainHandler.postDelayed(dismissTask, durationMs)
            } catch (e: Exception) {
                Log.w(TAG, "Engelleme bilgi kartı gösterilemedi: ${e.message}")
            }
        }
    }

    /**
     * Kota eşiğine yaklaşıldığında (örn. %80'e ulaşıldığında veya son 1 video kaldığında)
     * ekranın üstünde dokunmayı engellemeyen nazik bir uyarı kartı gösterir.
     */
    fun showQuotaWarningBanner(
        title: String,
        message: String,
        fallbackContext: Context? = null,
        durationMs: Long = 2800L
    ) {
        if (SettingsStore.punchHoleAlertEnabled.value) {
            showPunchHolePill(title, message, Color.parseColor("#D4973B"), fallbackContext, durationMs)
            return
        }

        val (targetContext, windowType) = resolveOverlayContextAndType(fallbackContext) ?: return

        mainHandler.post {
            removeBannerImmediate()

            try {
                val wm = targetContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                val view = buildCustomBannerView(targetContext, title, message, Color.parseColor("#D4973B"))

                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    windowType,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                    y = dp(targetContext, 64)
                }

                wm.addView(view, params)
                bannerView = view
                bannerWindowManager = wm

                val dismissTask = Runnable { removeBannerImmediate() }
                bannerDismissRunnable = dismissTask
                mainHandler.postDelayed(dismissTask, durationMs)
            } catch (e: Exception) {
                Log.w(TAG, "Kota uyarı kartı gösterilemedi: ${e.message}")
            }
        }
    }

    private fun removeBannerImmediate() {
        bannerDismissRunnable?.let { mainHandler.removeCallbacks(it) }
        bannerDismissRunnable = null

        val view = bannerView
        val wm = bannerWindowManager
        bannerView = null
        bannerWindowManager = null

        if (view != null && wm != null) {
            try {
                wm.removeView(view)
            } catch (_: Exception) {
            }
        }
    }

    fun dismissAll() {
        dismissMindfulFrictionOverlay(releaseAudioImmediately = true)
        mainHandler.post { removeBannerImmediate() }
    }

    private fun resolveOverlayContextAndType(fallbackContext: Context?): Pair<Context, Int>? {
        val service = serviceProvider()
        if (service != null) {
            return service to WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        }
        var ctx = fallbackContext
        while (ctx is android.content.ContextWrapper) {
            if (ctx is android.app.Activity) break
            ctx = ctx.baseContext
        }
        if (ctx is android.app.Activity && !ctx.isFinishing && !ctx.isDestroyed) {
            return ctx to WindowManager.LayoutParams.TYPE_APPLICATION
        }
        if (fallbackContext != null && Settings.canDrawOverlays(fallbackContext)) {
            return fallbackContext.applicationContext to WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
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
        val root = BackInterceptingFrameLayout(context, onCancel).apply {
            setBackgroundColor(Color.parseColor("#E6000000"))
            setPadding(dp(context, 20), dp(context, 24), dp(context, 20), dp(context, 24))
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
            text = "FARKINDALIK KAPISI · DM REEL"
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
            text = "Nefes Al. Otomatik Pilotta mısın?"
            setTextColor(Color.parseColor(titleColor))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
            typeface = getSerifBold(context)
            gravity = Gravity.CENTER
            setPadding(0, dp(context, 16), 0, dp(context, 8))
        }
        card.addView(title)

        val subtitleText = buildString {
            append("Bu Reel bir DM sohbetinden açıldı.")
            if (!reelSummary.isNullOrBlank() && reelSummary != "belirsiz") {
                append(" ($reelSummary)")
            }
            append("\n\nGerçekten izlemek istiyorsan aşağıdaki butona 5 saniye basılı tut. Yalnızca bu videoya izin verilir; 2. videoya kaydırırsan otomatik olarak sohbete dönülür.")
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

        // İlerleme çubuğu (5 saniye dolumu)
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

        // 5 Saniye Basılı Tut Butonu
        val holdButton = Button(context).apply {
            text = "İzlemek İçin 5 Saniye Basılı Tut"
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
                            holdButton.text = String.format("Basılı Tutuluyor... %.1f sn", remainingSec)

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
                        holdButton.text = "Yarıda Bırakıldı · 5 Sn Basılı Tut"
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

        // Vazgeç ve Sohbete Dön Butonu (Ana çıkış aksiyonu)
        val cancelButton = Button(context).apply {
            text = "Vazgeç ve Sohbete Dön"
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
        barrierType: MindfulBarrierType = MindfulBarrierType.BREATH_AND_INTENT,
        isNightMode: Boolean,
        isEnglish: Boolean,
        onApproved: (durationMinutes: Int, microPlan: String) -> Unit,
        onCancel: () -> Unit
    ): View {
        val strings = AppStrings.get(if (isEnglish) AppLanguage.EN else AppLanguage.TR)

        val cardBg = if (isNightMode) COLOR_DARK_SURFACE else COLOR_LIGHT_SURFACE
        val cardBorder = if (isNightMode) COLOR_DARK_BORDER else COLOR_LIGHT_BORDER
        val titleColor = if (isNightMode) COLOR_DARK_TEXT else COLOR_LIGHT_TEXT
        val textColor = if (isNightMode) COLOR_DARK_MUTED else COLOR_LIGHT_MUTED
        val badgeBg = if (isNightMode) COLOR_DARK_ACCENT else COLOR_LIGHT_ACCENT
        val badgeText = if (isNightMode) COLOR_DARK_PRIMARY else COLOR_LIGHT_PRIMARY
        val primaryBtnBg = if (isNightMode) COLOR_DARK_PRIMARY else COLOR_LIGHT_PRIMARY
        val primaryBtnText = if (isNightMode) Color.parseColor(COLOR_DARK_SURFACE) else Color.WHITE
        val breathingCircleBg = if (isNightMode) COLOR_DARK_ACCENT else COLOR_LIGHT_ACCENT
        val breathingCircleStroke = if (isNightMode) COLOR_DARK_PRIMARY else COLOR_LIGHT_PRIMARY

        var countdownRunnable: Runnable? = null
        var breathAnim: ValueAnimator? = null

        fun cleanupAnim() {
            countdownRunnable?.let { mainHandler.removeCallbacks(it) }
            countdownRunnable = null
            breathAnim?.cancel()
            breathAnim = null
        }

        val root = BackInterceptingFrameLayout(context) {
            cleanupAnim()
            onCancel()
        }.apply {
            setBackgroundColor(Color.parseColor("#E60A0F0D"))
            setPadding(dp(context, 20), dp(context, 24), dp(context, 20), dp(context, 24))
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
            setPadding(dp(context, 24), dp(context, 28), dp(context, 24), dp(context, 24))
        }

        // 1. Rozet (Badge)
        val badge = TextView(context).apply {
            text = if (isNightMode) {
                if (isEnglish) "🌙 NIGHT REST MODE" else "🌙 GECE DİNLENME MODU"
            } else {
                if (isEnglish) "🌿 MINDFUL PAUSE" else "🌿 FARKINDALIK KAPISI"
            }
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
            text = if (isNightMode) {
                if (isEnglish) "Time to Rest" else "Gece Dinlenme Zamanı"
            } else {
                if (isEnglish) "Take a Deep Breath" else "Derin Bir Nefes Al"
            }
            setTextColor(Color.parseColor(titleColor))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 23f)
            typeface = getSerifBold(context)
            gravity = Gravity.CENTER
            setPadding(0, dp(context, 16), 0, dp(context, 8))
        }
        card.addView(titleView)

        // 3. Açıklama (Subtitle)
        val subtitleView = TextView(context).apply {
            text = if (isNightMode) {
                if (isEnglish)
                    "Late-night screens disrupt deep sleep. Give your mind permission to recharge."
                else
                    "Gece ekran ışığı derin uyku ritmini bozar. Zihnine dinlenmesi için alan açmak ister misin?"
            } else {
                if (isEnglish)
                    "Notice the automatic scrolling reflex. Give yourself a moment to choose consciously."
                else
                    "Otomatik kaydırma refleksini fark et. Bilinçli bir seçim yapmak için kendine bir an tanı."
            }
            setTextColor(Color.parseColor(textColor))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            typeface = getSansRegular(context)
            gravity = Gravity.CENTER
            setLineSpacing(dp(context, 3).toFloat(), 1.0f)
            setPadding(0, 0, 0, dp(context, 20))
        }
        card.addView(subtitleView)

        // 4. Nefes Çemberi Animasyonu
        val circleContainer = FrameLayout(context).apply {
            setPadding(0, dp(context, 4), 0, dp(context, 24))
        }
        val circleSize = dp(context, 80)
        val circleView = View(context).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor(breathingCircleBg))
                setStroke(dp(context, 3), Color.parseColor(breathingCircleStroke))
            }
        }
        val innerCircleSize = dp(context, 40)
        val innerCircleView = View(context).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor(breathingCircleStroke))
            }
        }
        circleContainer.addView(circleView, FrameLayout.LayoutParams(circleSize, circleSize, Gravity.CENTER))
        circleContainer.addView(innerCircleView, FrameLayout.LayoutParams(innerCircleSize, innerCircleSize, Gravity.CENTER))
        card.addView(circleContainer)

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

        // 5. Birincil Buton: "Vazgeç ve Geri Dön" (Normal, belirgin buton)
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
                cleanupAnim()
                onCancel()
            }
        }
        card.addView(btnCancel, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = dp(context, 4)
            bottomMargin = dp(context, 12)
        })

        // 6. İkincil Eylem: "5 dakika izin ver"
        // (10 saniye kilitli, buton değil düz metin gibi, kendini belli etmeyen)
        var remainingSeconds = 10
        val continueText = TextView(context).apply {
            text = if (isEnglish) "Allow 5 minutes (${remainingSeconds}s)" else "5 dakika izin ver (${remainingSeconds}s)"
            setTextColor(Color.parseColor(textColor).let {
                Color.argb(160, Color.red(it), Color.green(it), Color.blue(it))
            })
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            typeface = getSansRegular(context)
            gravity = Gravity.CENTER
            setPadding(dp(context, 16), dp(context, 10), dp(context, 16), dp(context, 10))
            isClickable = false
            isEnabled = false
        }

        val runnable = object : Runnable {
            override fun run() {
                remainingSeconds--
                if (remainingSeconds > 0) {
                    continueText.text = if (isEnglish)
                        "Allow 5 minutes (${remainingSeconds}s)"
                    else
                        "5 dakika izin ver (${remainingSeconds}s)"
                    mainHandler.postDelayed(this, 1000L)
                } else {
                    continueText.text = if (isEnglish) "Allow 5 minutes" else "5 dakika izin ver"
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
                onApproved(5, if (isEnglish) "Mindful Pause" else "Bilinçli Mola")
            }
        }

        card.addView(continueText, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            bottomMargin = dp(context, 4)
        })

        scroll.addView(card)
        root.addView(scroll, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.CENTER
        ))

        return root
    }

    fun getBannerStringsAndColor(
        decision: PolicyDecision,
        context: Context? = null
    ): Triple<String, String, Int> {
        val isEnglish = SettingsStore.appLanguage.value == AppLanguage.EN
        val isDark = context?.let { SettingsStore.isDarkThemeActiveNow(it) } ?: false
        val calmSage = Color.parseColor(if (isDark) COLOR_DARK_PRIMARY else COLOR_LIGHT_PRIMARY)

        return when (decision) {
            PolicyDecision.BLOCK_AFTER_SWIPE -> Triple(
                if (isEnglish) "Single Reel Limit Reached" else "Tek Reel İznin Bitti",
                if (isEnglish) "Scrolled to 2nd video" else "2. videoya kaydırıldı",
                calmSage
            )
            PolicyDecision.BLOCK_AFTER_TIMEOUT -> Triple(
                if (isEnglish) "Time Limit Reached" else "Süre Sınırı Doldu",
                if (isEnglish) "Single Reel timeout reached" else "Tek Reel süresi aşıldı",
                calmSage
            )
            PolicyDecision.BLOCK_BEDTIME -> Triple(
                if (isEnglish) "Bedtime Focus Active" else "Gece Odak Saati",
                if (isEnglish) "Bedtime lock active" else "Uyku vakti kilidi devrede",
                Color.parseColor(if (isDark) "#9FA8DA" else "#5C6BC0")
            )
            PolicyDecision.BLOCK_DAILY_COUNT_LIMIT -> Triple(
                if (isEnglish) "Daily Reel Limit Reached" else "Reel Kotan Doldu",
                if (isEnglish) "Daily limit reached" else "Günlük limitine ulaştın",
                calmSage
            )
            PolicyDecision.BLOCK_DAILY_TIME_LIMIT -> Triple(
                if (isEnglish) "Daily Time Limit Reached" else "Günlük Süren Doldu",
                if (isEnglish) "Daily Reels target completed" else "Reels hedef süren tamamlandı",
                calmSage
            )
            else -> Triple(
                if (isEnglish) "Reels Blocked" else "Reels Engellendi",
                if (isEnglish) "Your focus was protected" else "Odağın korundu",
                calmSage
            )
        }
    }

    private fun buildPunchHolePillView(
        context: Context,
        title: String,
        subtitle: String?,
        accentColor: Int
    ): View {
        val isDark = SettingsStore.isDarkThemeActiveNow(context)

        val bgColor = if (isDark) Color.parseColor("#F5111512") else Color.parseColor("#FDFEFC")
        val borderColor = if (isDark) Color.parseColor("#2C3A2F") else Color.parseColor("#DCE4DC")
        val titleColor = if (isDark) Color.parseColor("#F0F4ED") else Color.parseColor("#1E2B22")
        val subtitleColor = if (isDark) Color.parseColor("#9CAD9F") else Color.parseColor("#637265")
        val dividerColor = if (isDark) Color.parseColor("#2E3D31") else Color.parseColor("#D4DDD4")
        val haloColor = Color.argb(
            if (isDark) 50 else 38,
            Color.red(accentColor),
            Color.green(accentColor),
            Color.blue(accentColor)
        )

        return LinearLayout(context).apply {
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
            setPadding(dp(context, 10), dp(context, 7), dp(context, 15), dp(context, 7))

            // Glowing dual-layer indicator dot
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

            val titleView = TextView(context).apply {
                text = title
                setTextColor(titleColor)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                typeface = getSansBold(context)
                includeFontPadding = false
            }
            addView(titleView)

            if (!subtitle.isNullOrBlank()) {
                val separator = View(context).apply {
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(dividerColor)
                    }
                }
                addView(separator, LinearLayout.LayoutParams(dp(context, 3), dp(context, 3)).apply {
                    marginStart = dp(context, 8)
                    marginEnd = dp(context, 8)
                    gravity = Gravity.CENTER_VERTICAL
                })

                val subtitleView = TextView(context).apply {
                    text = subtitle
                    setTextColor(subtitleColor)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                    typeface = getSansRegular(context)
                    includeFontPadding = false
                }
                addView(subtitleView)
            }
        }
    }

    private fun buildCustomBannerView(
        context: Context,
        titleText: String,
        subText: String,
        accentColor: Int
    ): View {
        return buildPunchHolePillView(context, titleText, subText, accentColor)
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
