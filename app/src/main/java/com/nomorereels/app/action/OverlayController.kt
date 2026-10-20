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
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
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

    private var bannerView: View? = null
    private var bannerWindowManager: WindowManager? = null
    private var bannerDismissRunnable: Runnable? = null

    private var holdTickRunnable: Runnable? = null

    val isFrictionOverlayShowing: Boolean
        get() = frictionView != null

    /**
     * DM'den açılan Reel için 5 saniye basılı tutma gerektiren "Bilinçli Sürtünme"
     * ekranını gösterir. Zaten açıksa tekrar açmaz.
     */
    fun showMindfulFrictionOverlay(
        fallbackContext: Context? = null,
        reelSummary: String? = null,
        holdDurationMs: Long = DEFAULT_HOLD_DURATION_MS,
        onApproved: () -> Unit,
        onCancelAndGoBack: () -> Unit
    ): Boolean {
        if (frictionView != null) return true

        val (targetContext, windowType) = resolveOverlayContextAndType(fallbackContext) ?: run {
            Log.w(TAG, "Overlay gösterilemedi: Uygun servis veya SYSTEM_ALERT_WINDOW izni yok.")
            return false
        }

        mainHandler.post {
            if (frictionView != null) return@post
            try {
                val wm = targetContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                val view = buildFrictionOverlayView(
                    context = targetContext,
                    reelSummary = reelSummary,
                    holdDurationMs = holdDurationMs,
                    onApproved = {
                        dismissMindfulFrictionOverlay()
                        onApproved()
                    },
                    onCancel = {
                        dismissMindfulFrictionOverlay()
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
     * Açık olan Bilinçli Sürtünme ekranını kapatır.
     */
    fun dismissMindfulFrictionOverlay() {
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
     * Otomatik kaydırmayı kesmek için nefes, niyet aynası veya zihin egzersizi sunan tam ekran katman.
     */
    fun showMindfulGatewayOverlay(
        fallbackContext: Context? = null,
        barrierType: MindfulBarrierType = MindfulBarrierType.BREATH_AND_INTENT,
        isNightMode: Boolean = false,
        isEnglish: Boolean = false,
        onApproved: (durationMinutes: Int, microPlan: String) -> Unit,
        onCancelAndGoBack: () -> Unit
    ): Boolean {
        if (frictionView != null) return true

        val (targetContext, windowType) = resolveOverlayContextAndType(fallbackContext) ?: run {
            Log.w(TAG, "Overlay gösterilemedi: Uygun servis veya SYSTEM_ALERT_WINDOW izni yok.")
            return false
        }

        mainHandler.post {
            if (frictionView != null) return@post
            try {
                val wm = targetContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                val view = buildMindfulGatewayView(
                    context = targetContext,
                    barrierType = barrierType,
                    isNightMode = isNightMode,
                    isEnglish = isEnglish,
                    onApproved = { duration, plan ->
                        dismissMindfulFrictionOverlay()
                        onApproved(duration, plan)
                    },
                    onCancel = {
                        dismissMindfulFrictionOverlay()
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
                Log.i(TAG, "Farkındalık Kapısı (Mindful Gateway) ekranı gösterildi.")
            } catch (e: Exception) {
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
                    y = dp(targetContext, 12)
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
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
        val (titleText, subText, accentColor) = getBannerStringsAndColor(decision)
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
                    y = dp(targetContext, 64)
                }

                wm.addView(view, params)
                bannerView = view
                bannerWindowManager = wm

                val dismissTask = Runnable { removeBannerImmediate() }
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
        dismissMindfulFrictionOverlay()
        mainHandler.post { removeBannerImmediate() }
    }

    private fun resolveOverlayContextAndType(fallbackContext: Context?): Pair<Context, Int>? {
        val service = serviceProvider()
        if (service != null) {
            return service to WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
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
        onApproved: () -> Unit,
        onCancel: () -> Unit
    ): View {
        val root = FrameLayout(context).apply {
            setBackgroundColor(Color.parseColor("#E6000000"))
            setPadding(dp(context, 20), dp(context, 24), dp(context, 20), dp(context, 24))
        }

        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor(COLOR_LIGHT_SURFACE))
                cornerRadius = dp(context, 22).toFloat()
                setStroke(dp(context, 1), Color.parseColor(COLOR_LIGHT_BORDER))
            }
            setPadding(dp(context, 24), dp(context, 28), dp(context, 24), dp(context, 24))
        }

        val badge = TextView(context).apply {
            text = "FARKINDALIK KAPISI · DM REEL"
            setTextColor(Color.parseColor(COLOR_LIGHT_PRIMARY))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            typeface = getSansBold(context)
            background = GradientDrawable().apply {
                setColor(Color.parseColor(COLOR_LIGHT_ACCENT))
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
            setTextColor(Color.parseColor(COLOR_LIGHT_TEXT))
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
            setTextColor(Color.parseColor(COLOR_LIGHT_MUTED))
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
                setColor(Color.parseColor(COLOR_LIGHT_CANVAS))
                cornerRadius = dp(context, 8).toFloat()
            }
            val fgShape = GradientDrawable().apply {
                setColor(Color.parseColor(COLOR_LIGHT_PRIMARY))
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
            setTextColor(Color.parseColor(COLOR_LIGHT_PRIMARY))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            typeface = getSansBold(context)
            background = GradientDrawable().apply {
                setColor(Color.parseColor(COLOR_LIGHT_ACCENT))
                cornerRadius = dp(context, 22).toFloat()
                setStroke(dp(context, 1), Color.parseColor(COLOR_LIGHT_ACCENT))
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
                    (holdButton.background as? GradientDrawable)?.setColor(Color.parseColor(COLOR_LIGHT_PRIMARY))
                    holdButton.setTextColor(Color.WHITE)

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
                        (holdButton.background as? GradientDrawable)?.setColor(Color.parseColor(COLOR_LIGHT_ACCENT))
                        holdButton.setTextColor(Color.parseColor(COLOR_LIGHT_PRIMARY))
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
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            typeface = getSansBold(context)
            background = GradientDrawable().apply {
                setColor(Color.parseColor(COLOR_LIGHT_PRIMARY))
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
        barrierType: MindfulBarrierType,
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
        val secondaryBtnBg = if (isNightMode) COLOR_DARK_ACCENT else COLOR_LIGHT_ACCENT
        val secondaryBtnText = if (isNightMode) COLOR_DARK_PRIMARY else COLOR_LIGHT_PRIMARY
        val breathingCircleBg = if (isNightMode) COLOR_DARK_ACCENT else COLOR_LIGHT_ACCENT
        val breathingCircleStroke = if (isNightMode) COLOR_DARK_PRIMARY else COLOR_LIGHT_PRIMARY

        val root = FrameLayout(context).apply {
            setBackgroundColor(Color.parseColor("#E6000000"))
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
                cornerRadius = dp(context, 22).toFloat()
                setStroke(dp(context, 1), Color.parseColor(cardBorder))
            }
            setPadding(dp(context, 24), dp(context, 26), dp(context, 24), dp(context, 26))
        }

        // 1. Badge
        val badge = TextView(context).apply {
            text = if (isNightMode) "GECE KORUMA · NIGHT REST" else strings.mindfulGatewayHeader
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

        // 2. Title View
        val titleView = TextView(context).apply {
            setTextColor(Color.parseColor(titleColor))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
            typeface = getSerifBold(context)
            gravity = Gravity.CENTER
            setPadding(0, dp(context, 16), 0, dp(context, 8))
        }
        card.addView(titleView)

        // 3. Subtitle View
        val subtitleView = TextView(context).apply {
            setTextColor(Color.parseColor(textColor))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            typeface = getSansRegular(context)
            gravity = Gravity.CENTER
            setLineSpacing(dp(context, 3).toFloat(), 1.0f)
            setPadding(0, 0, 0, dp(context, 20))
        }
        card.addView(subtitleView)

        // 4. Dynamic Content Area
        val dynamicArea = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }
        card.addView(dynamicArea, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        // Helper to show Window Selection + Micro-Plan
        fun showWindowSelection(windowMinutes: Int? = null) {
            dynamicArea.removeAllViews()
            titleView.text = if (isEnglish) "Set a Mindful Window" else "Seçim Senin"
            subtitleView.text = if (isEnglish)
                "How would you like to continue right now?"
            else
                "Şu an nasıl devam etmek istersin?"

            if (windowMinutes == null) {
                // Show choice: Put phone down vs 5 min vs 10 min
                val putDownButton = Button(context).apply {
                    text = "📱 ${strings.gatewayPutPhoneDown}"
                    isAllCaps = false
                    setTextColor(primaryBtnText)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                    typeface = getSansBold(context)
                    background = GradientDrawable().apply {
                        setColor(Color.parseColor(primaryBtnBg))
                        cornerRadius = dp(context, 22).toFloat()
                    }
                    setPadding(dp(context, 16), dp(context, 14), dp(context, 16), dp(context, 14))
                    setOnClickListener { onCancel() }
                }
                dynamicArea.addView(putDownButton, LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dp(context, 12) })

                val windowsRow = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                }

                val btn5Min = Button(context).apply {
                    text = "⏱️ ${strings.gatewayMindful5Min}"
                    isAllCaps = false
                    setTextColor(Color.parseColor(secondaryBtnText))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                    typeface = getSansBold(context)
                    background = GradientDrawable().apply {
                        setColor(Color.parseColor(secondaryBtnBg))
                        cornerRadius = dp(context, 22).toFloat()
                    }
                    setOnClickListener { showWindowSelection(5) }
                }

                val btn10Min = Button(context).apply {
                    text = "⏱️ ${strings.gatewayMindful10Min}"
                    isAllCaps = false
                    setTextColor(Color.parseColor(secondaryBtnText))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                    typeface = getSansBold(context)
                    background = GradientDrawable().apply {
                        setColor(Color.parseColor(secondaryBtnBg))
                        cornerRadius = dp(context, 22).toFloat()
                    }
                    setOnClickListener { showWindowSelection(10) }
                }

                windowsRow.addView(btn5Min, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginEnd = dp(context, 6)
                })
                windowsRow.addView(btn10Min, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = dp(context, 6)
                })

                dynamicArea.addView(windowsRow, LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dp(context, 12) })
            } else {
                // User picked windowMinutes (5 or 10 min), ask for Micro-Plan (If-Then)
                titleView.text = if (isEnglish) "$windowMinutes Min Mindful Window" else "$windowMinutes Dk Bilinçli Pencere"
                subtitleView.text = strings.gatewayMicroPlanQuestion

                val plans = listOf(
                    "💧 ${strings.gatewayMicroPlanWater}",
                    "💼 ${strings.gatewayMicroPlanWork}",
                    "🛌 ${strings.gatewayMicroPlanSleep}",
                    "🚶 ${strings.gatewayMicroPlanWalk}"
                )

                for (plan in plans) {
                    val planBtn = Button(context).apply {
                        text = plan
                        isAllCaps = false
                        setTextColor(Color.parseColor(secondaryBtnText))
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                        typeface = getSansBold(context)
                        background = GradientDrawable().apply {
                            setColor(Color.parseColor(secondaryBtnBg))
                            cornerRadius = dp(context, 22).toFloat()
                        }
                        setPadding(dp(context, 16), dp(context, 12), dp(context, 16), dp(context, 12))
                        setOnClickListener {
                            val cleanPlan = plan.substringAfter(" ").trim()
                            onApproved(windowMinutes, cleanPlan)
                        }
                    }
                    dynamicArea.addView(planBtn, LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { bottomMargin = dp(context, 8) })
                }
            }

            // Quick Exit
            val exitBtn = TextView(context).apply {
                text = strings.gatewayExitImmediate
                setTextColor(Color.parseColor(textColor))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                typeface = getSansRegular(context)
                gravity = Gravity.CENTER
                setPadding(dp(context, 12), dp(context, 10), dp(context, 12), dp(context, 10))
                setOnClickListener { onCancel() }
            }
            dynamicArea.addView(exitBtn)
        }

        // Helper to show Intent Selection Phase
        fun showIntentPhase() {
            dynamicArea.removeAllViews()
            titleView.text = strings.gatewayIntentQuestion
            subtitleView.text = if (isEnglish) "Notice your state without judgment." else "Yargılamadan sadece fark et."

            // Option 1: Autopilot
            val btnAutopilot = Button(context).apply {
                text = "🍃 ${strings.gatewayIntentAutopilot}"
                isAllCaps = false
                setTextColor(Color.parseColor(secondaryBtnText))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                typeface = getSansBold(context)
                background = GradientDrawable().apply {
                    setColor(Color.parseColor(secondaryBtnBg))
                    cornerRadius = dp(context, 22).toFloat()
                }
                setPadding(dp(context, 16), dp(context, 12), dp(context, 16), dp(context, 12))
                setOnClickListener {
                    dynamicArea.removeAllViews()
                    titleView.text = if (isEnglish) "Awareness Achieved" else "Farkındalık"
                    subtitleView.text = strings.gatewayAutopilotAck
                    mainHandler.postDelayed({ onCancel() }, 1400L)
                }
            }
            dynamicArea.addView(btnAutopilot, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(context, 10) })

            // Option 2: Quick Break
            val btnBreak = Button(context).apply {
                text = "☕ ${strings.gatewayIntentBreak}"
                isAllCaps = false
                setTextColor(Color.parseColor(secondaryBtnText))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                typeface = getSansBold(context)
                background = GradientDrawable().apply {
                    setColor(Color.parseColor(secondaryBtnBg))
                    cornerRadius = dp(context, 22).toFloat()
                }
                setPadding(dp(context, 16), dp(context, 12), dp(context, 16), dp(context, 12))
                setOnClickListener { showWindowSelection() }
            }
            dynamicArea.addView(btnBreak, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(context, 10) })

            // Option 3: Specific Task
            val btnSpecific = Button(context).apply {
                text = "🔍 ${strings.gatewayIntentSpecific}"
                isAllCaps = false
                setTextColor(Color.parseColor(secondaryBtnText))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                typeface = getSansBold(context)
                background = GradientDrawable().apply {
                    setColor(Color.parseColor(secondaryBtnBg))
                    cornerRadius = dp(context, 22).toFloat()
                }
                setPadding(dp(context, 16), dp(context, 12), dp(context, 16), dp(context, 12))
                setOnClickListener { showWindowSelection() }
            }
            dynamicArea.addView(btnSpecific, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(context, 14) })

            // Exit
            val exitBtn = TextView(context).apply {
                text = strings.gatewayExitImmediate
                setTextColor(Color.parseColor(textColor))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                typeface = getSansRegular(context)
                gravity = Gravity.CENTER
                setPadding(dp(context, 12), dp(context, 8), dp(context, 12), dp(context, 8))
                setOnClickListener { onCancel() }
            }
            dynamicArea.addView(exitBtn)
        }

        // Branching according to mode
        if (isNightMode) {
            titleView.text = strings.gatewayNightTitle
            subtitleView.text = strings.gatewayNightSub

            val sleepBtn = Button(context).apply {
                text = strings.gatewayNightSleep
                isAllCaps = false
                setTextColor(primaryBtnText)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                typeface = getSansBold(context)
                background = GradientDrawable().apply {
                    setColor(Color.parseColor(primaryBtnBg))
                    cornerRadius = dp(context, 22).toFloat()
                }
                setPadding(dp(context, 16), dp(context, 14), dp(context, 16), dp(context, 14))
                setOnClickListener { onCancel() }
            }
            dynamicArea.addView(sleepBtn, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(context, 10) })

            val watch5MinBtn = Button(context).apply {
                text = strings.gatewayNight5Min
                isAllCaps = false
                setTextColor(Color.parseColor(secondaryBtnText))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                typeface = getSansBold(context)
                background = GradientDrawable().apply {
                    setColor(Color.parseColor(secondaryBtnBg))
                    cornerRadius = dp(context, 22).toFloat()
                }
                setPadding(dp(context, 14), dp(context, 12), dp(context, 14), dp(context, 12))
                setOnClickListener { onApproved(5, strings.gatewayMicroPlanSleep) }
            }
            dynamicArea.addView(watch5MinBtn, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(context, 12) })

            val exitBtn = TextView(context).apply {
                text = strings.gatewayExitImmediate
                setTextColor(Color.parseColor(textColor))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                typeface = getSansRegular(context)
                gravity = Gravity.CENTER
                setPadding(dp(context, 12), dp(context, 8), dp(context, 12), dp(context, 8))
                setOnClickListener { onCancel() }
            }
            dynamicArea.addView(exitBtn)

        } else if (barrierType == MindfulBarrierType.COGNITIVE_MATH) {
            titleView.text = strings.barrierTypeMath
            subtitleView.text = strings.gatewayMathPrompt

            val qText = TextView(context).apply {
                text = "14 + 19 = ?"
                setTextColor(Color.parseColor(primaryBtnBg))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 26f)
                typeface = getSerifBold(context)
                gravity = Gravity.CENTER
                setPadding(0, dp(context, 6), 0, dp(context, 16))
            }
            dynamicArea.addView(qText)

            val answersRow = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
            val options = listOf(23 to false, 33 to true, 35 to false)
            for ((num, isCorrect) in options) {
                val optBtn = Button(context).apply {
                    text = "$num"
                    isAllCaps = false
                    setTextColor(Color.parseColor(secondaryBtnText))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                    typeface = getSansBold(context)
                    background = GradientDrawable().apply {
                        setColor(Color.parseColor(secondaryBtnBg))
                        cornerRadius = dp(context, 22).toFloat()
                    }
                    setOnClickListener {
                        if (isCorrect) {
                            showWindowSelection()
                        } else {
                            (background as? GradientDrawable)?.setColor(Color.parseColor("#FBECE9"))
                            setTextColor(Color.parseColor("#A84838"))
                        }
                    }
                }
                answersRow.addView(optBtn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginEnd = dp(context, 4)
                    marginStart = dp(context, 4)
                })
            }
            dynamicArea.addView(answersRow, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(context, 16) })

            val exitBtn = TextView(context).apply {
                text = strings.gatewayExitImmediate
                setTextColor(Color.parseColor(textColor))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                typeface = getSansRegular(context)
                gravity = Gravity.CENTER
                setPadding(dp(context, 12), dp(context, 8), dp(context, 12), dp(context, 8))
                setOnClickListener { onCancel() }
            }
            dynamicArea.addView(exitBtn)

        } else if (barrierType == MindfulBarrierType.MINDFUL_TYPING) {
            titleView.text = strings.barrierTypeTyping
            subtitleView.text = strings.gatewayTypingPrompt

            val targetSentence = strings.gatewayTypingSentence
            val quoteView = TextView(context).apply {
                text = "\"$targetSentence\""
                setTextColor(Color.parseColor(primaryBtnBg))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                typeface = getSansRegular(context)
                gravity = Gravity.CENTER
                setPadding(dp(context, 8), dp(context, 4), dp(context, 8), dp(context, 12))
            }
            dynamicArea.addView(quoteView)

            val input = EditText(context).apply {
                hint = if (isEnglish) "Type sentence here..." else "Cümleyi buraya yaz..."
                setHintTextColor(Color.parseColor(textColor))
                setTextColor(Color.parseColor(titleColor))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                typeface = getSansRegular(context)
                background = GradientDrawable().apply {
                    setColor(Color.parseColor(if (isNightMode) COLOR_DARK_SURFACE else COLOR_LIGHT_CANVAS))
                    cornerRadius = dp(context, 16).toFloat()
                    setStroke(dp(context, 1), Color.parseColor(cardBorder))
                }
                setPadding(dp(context, 14), dp(context, 12), dp(context, 14), dp(context, 12))
            }
            dynamicArea.addView(input, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(context, 12) })

            val continueBtn = Button(context).apply {
                text = if (isEnglish) "Continue" else "Devam Et"
                isAllCaps = false
                isEnabled = false
                setTextColor(Color.parseColor(textColor))
                typeface = getSansBold(context)
                background = GradientDrawable().apply {
                    setColor(Color.parseColor(secondaryBtnBg))
                    cornerRadius = dp(context, 22).toFloat()
                }
                setOnClickListener { showWindowSelection() }
            }
            dynamicArea.addView(continueBtn, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(context, 12) })

            input.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    val typed = s?.toString()?.trim() ?: ""
                    val isMatch = typed.equals(targetSentence.trim(), ignoreCase = true)
                    continueBtn.isEnabled = isMatch
                    if (isMatch) {
                        continueBtn.setTextColor(primaryBtnText)
                        (continueBtn.background as? GradientDrawable)?.setColor(Color.parseColor(primaryBtnBg))
                    } else {
                        continueBtn.setTextColor(Color.parseColor(textColor))
                        (continueBtn.background as? GradientDrawable)?.setColor(Color.parseColor(secondaryBtnBg))
                    }
                }
                override fun afterTextChanged(s: Editable?) {}
            })

            val exitBtn = TextView(context).apply {
                text = strings.gatewayExitImmediate
                setTextColor(Color.parseColor(textColor))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                typeface = getSansRegular(context)
                gravity = Gravity.CENTER
                setPadding(dp(context, 12), dp(context, 8), dp(context, 12), dp(context, 8))
                setOnClickListener { onCancel() }
            }
            dynamicArea.addView(exitBtn)

        } else {
            // Default: BREATH_AND_INTENT
            titleView.text = strings.gatewayPauseTitle
            subtitleView.text = strings.gatewayBreathPrompt

            // Breathing pulsing circle
            val circleSize = dp(context, 76)
            val circleView = View(context).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor(breathingCircleBg))
                    setStroke(dp(context, 3), Color.parseColor(breathingCircleStroke))
                }
            }
            dynamicArea.addView(circleView, LinearLayout.LayoutParams(circleSize, circleSize).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = dp(context, 8)
                bottomMargin = dp(context, 20)
            })

            val breathAnim = ValueAnimator.ofFloat(0.72f, 1.15f).apply {
                duration = 2000L
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

            val proceedBtn = Button(context).apply {
                text = if (isEnglish) "Notice My Intention ➔" else "Niyetimi Fark Et ➔"
                isAllCaps = false
                setTextColor(primaryBtnText)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                typeface = getSansBold(context)
                background = GradientDrawable().apply {
                    setColor(Color.parseColor(primaryBtnBg))
                    cornerRadius = dp(context, 22).toFloat()
                }
                setPadding(dp(context, 16), dp(context, 12), dp(context, 16), dp(context, 12))
                setOnClickListener {
                    breathAnim.cancel()
                    showIntentPhase()
                }
            }
            dynamicArea.addView(proceedBtn, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(context, 12) })

            // Instant Exit
            val exitBtn = TextView(context).apply {
                text = strings.gatewayExitImmediate
                setTextColor(Color.parseColor(textColor))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                typeface = getSansRegular(context)
                gravity = Gravity.CENTER
                setPadding(dp(context, 12), dp(context, 8), dp(context, 12), dp(context, 8))
                setOnClickListener {
                    breathAnim.cancel()
                    onCancel()
                }
            }
            dynamicArea.addView(exitBtn)
        }

        scroll.addView(card)
        root.addView(scroll, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.CENTER
        ))

        return root
    }

    fun getBannerStringsAndColor(decision: PolicyDecision): Triple<String, String, Int> {
        val calmSage = Color.parseColor(COLOR_LIGHT_PRIMARY)
        return when (decision) {
            PolicyDecision.BLOCK_AFTER_SWIPE -> Triple(
                "Tek Reel İznin Bitti",
                "2. videoya kaydırıldı",
                calmSage
            )
            PolicyDecision.BLOCK_AFTER_TIMEOUT -> Triple(
                "Süre Sınırı Doldu",
                "Tek Reel süresi aşıldı",
                calmSage
            )
            PolicyDecision.BLOCK_BEDTIME -> Triple(
                "Gece Odak Saati 🌙",
                "Uyku vakti kilidi devrede",
                Color.parseColor(COLOR_DARK_PRIMARY)
            )
            PolicyDecision.BLOCK_DAILY_COUNT_LIMIT -> Triple(
                "Reel Kotan Doldu 🎯",
                "Günlük limitine ulaştın",
                calmSage
            )
            PolicyDecision.BLOCK_DAILY_TIME_LIMIT -> Triple(
                "Günlük Süren Doldu ⏱️",
                "Reels hedef süren tamamlandı",
                calmSage
            )
            else -> Triple(
                "Reels Engellendi 🛑",
                "Odağın korundu",
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
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#F2101310"))
                cornerRadius = dp(context, 999).toFloat()
                setStroke(dp(context, 1), Color.parseColor(COLOR_DARK_BORDER))
            }
            setPadding(dp(context, 12), dp(context, 7), dp(context, 16), dp(context, 7))

            val cameraDot = View(context).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(accentColor)
                }
            }
            addView(cameraDot, LinearLayout.LayoutParams(dp(context, 8), dp(context, 8)).apply {
                marginEnd = dp(context, 8)
            })

            val textColumn = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val titleView = TextView(context).apply {
                text = title
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                typeface = getSansBold(context)
            }
            textColumn.addView(titleView)

            if (!subtitle.isNullOrBlank()) {
                val subtitleView = TextView(context).apply {
                    text = " · $subtitle"
                    setTextColor(Color.parseColor(COLOR_DARK_MUTED))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                    typeface = getSansRegular(context)
                }
                textColumn.addView(subtitleView)
            }

            addView(textColumn)
        }
    }

    private fun buildCustomBannerView(
        context: Context,
        titleText: String,
        subText: String,
        accentColor: Int
    ): View {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#F2101310"))
                cornerRadius = dp(context, 16).toFloat()
                setStroke(dp(context, 1), Color.parseColor(COLOR_DARK_BORDER))
            }
            setPadding(dp(context, 20), dp(context, 12), dp(context, 20), dp(context, 12))

            addView(TextView(context).apply {
                text = titleText
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                typeface = getSansBold(context)
                gravity = Gravity.CENTER
            })

            addView(TextView(context).apply {
                text = subText
                setTextColor(Color.parseColor(COLOR_DARK_MUTED))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                typeface = getSansRegular(context)
                gravity = Gravity.CENTER
                setPadding(0, dp(context, 2), 0, 0)
            })
        }
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
