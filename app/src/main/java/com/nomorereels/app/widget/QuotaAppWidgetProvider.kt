package com.nomorereels.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import com.nomorereels.app.R
import com.nomorereels.app.data.AppLanguage
import com.nomorereels.app.data.QuotaStore
import com.nomorereels.app.data.SettingsStore
import com.nomorereels.app.policy.DmReelMode
import com.nomorereels.app.service.ReelsAccessibilityService
import com.nomorereels.app.ui.MainActivity
import com.nomorereels.app.ui.i18n.AppStrings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Ana ekran için sıfır dokunuşla kalan kotayı, aktif modu ve engellenen Reel sayısını
 * gösteren hafif ve pil dostu AppWidget sağlayıcısı.
 */
class QuotaAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val appContext = context.applicationContext
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                SettingsStore.awaitInit(appContext)
                QuotaStore.awaitInit(appContext)
                for (appWidgetId in appWidgetIds) {
                    updateWidget(appContext, appWidgetManager, appWidgetId)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_quota)

        val isServiceActive =
            ReelsAccessibilityService.isServiceActive.value ||
                com.nomorereels.app.service.ServiceHealthMonitor.isAccessibilityPermissionGranted(context)
        val isMasterEnabled = SettingsStore.protectionMasterEnabled.value
        val isSnoozeActive = SettingsStore.isSnoozeEffective()
        val generalMode = SettingsStore.generalMode.value
        val isEnglish = SettingsStore.appLanguage.value == AppLanguage.EN
        val strings = AppStrings.get(SettingsStore.appLanguage.value)

        val blockCount = QuotaStore.todayBlockCount.value
        val todayCount = QuotaStore.todayReelCount.value
        val countLimit = QuotaStore.dailyCountLimit.value
        val todayMinutes = (QuotaStore.todayTimeSpentSeconds.value / 60).toInt()
        val timeLimitMinutes = QuotaStore.dailyTimeLimitMinutes.value
        val blockedText = if (isEnglish) "$blockCount Reels blocked today" else "Bugün $blockCount Reel engellendi"

        when {
            !isServiceActive -> {
                views.setTextViewText(R.id.tv_widget_badge, if (isEnglish) "PROTECTION OFF" else "KORUMA KAPALI")
                views.setTextColor(R.id.tv_widget_badge, Color.parseColor("#CF4040"))
                views.setTextViewText(R.id.tv_widget_primary, if (isEnglish) "Service Inactive" else "Servis Kapalı")
                views.setTextViewText(R.id.tv_widget_secondary, if (isEnglish) "Tap to enable" else "Açmak için dokunun")
            }
            !isMasterEnabled -> {
                views.setTextViewText(R.id.tv_widget_badge, if (isEnglish) "PAUSED" else "DURAKLATILDI")
                views.setTextColor(R.id.tv_widget_badge, Color.parseColor("#E08A1E"))
                views.setTextViewText(R.id.tv_widget_primary, if (isEnglish) "Protection Paused" else "Koruma Duraklatıldı")
                views.setTextViewText(R.id.tv_widget_secondary, if (isEnglish) "Tap to resume" else "Devam etmek için dokunun")
            }
            isSnoozeActive -> {
                val remMins = SettingsStore.getSnoozeRemainingMinutes()
                views.setTextViewText(R.id.tv_widget_badge, if (isEnglish) "SNOOZE ACTIVE" else "MOLA DEVREDE")
                views.setTextColor(R.id.tv_widget_badge, Color.parseColor("#E08A1E"))
                views.setTextViewText(
                    R.id.tv_widget_primary,
                    if (isEnglish) "Remaining: $remMins min" else "Kalan: $remMins dk"
                )
                views.setTextViewText(
                    R.id.tv_widget_secondary,
                    if (isEnglish) "Protection resumes automatically" else "Süre bitince koruma geri döner"
                )
            }
            generalMode == DmReelMode.BLOCK_ALL -> {
                views.setTextViewText(R.id.tv_widget_badge, if (isEnglish) "PROTECTION ACTIVE" else "KORUMA AKTİF")
                views.setTextColor(R.id.tv_widget_badge, Color.parseColor("#2E9E5B"))
                views.setTextViewText(R.id.tv_widget_primary, strings.blockAllTitle)
                views.setTextViewText(R.id.tv_widget_secondary, blockedText)
            }
            generalMode == DmReelMode.SINGLE_REEL -> {
                views.setTextViewText(R.id.tv_widget_badge, if (isEnglish) "PROTECTION ACTIVE" else "KORUMA AKTİF")
                views.setTextColor(R.id.tv_widget_badge, Color.parseColor("#2E9E5B"))
                views.setTextViewText(R.id.tv_widget_primary, strings.singleReelTitle)
                views.setTextViewText(
                    R.id.tv_widget_secondary,
                    if (isEnglish) "1st allowed · $blockCount blocked" else "İlk serbest · $blockCount engellendi"
                )
            }
            generalMode == DmReelMode.COUNT_QUOTA -> {
                views.setTextViewText(R.id.tv_widget_badge, if (isEnglish) "QUOTA ACTIVE" else "KOTA DEVREDE")
                views.setTextColor(R.id.tv_widget_badge, Color.parseColor("#2E9E5B"))
                views.setTextViewText(R.id.tv_widget_primary, "$todayCount / $countLimit Reel")
                views.setTextViewText(R.id.tv_widget_secondary, blockedText)
            }
            generalMode == DmReelMode.TIME_QUOTA -> {
                views.setTextViewText(R.id.tv_widget_badge, if (isEnglish) "TIME QUOTA" else "SÜRE KOTASI")
                views.setTextColor(R.id.tv_widget_badge, Color.parseColor("#2E9E5B"))
                views.setTextViewText(
                    R.id.tv_widget_primary,
                    if (isEnglish) "$todayMinutes / $timeLimitMinutes min" else "$todayMinutes / $timeLimitMinutes dk"
                )
                views.setTextViewText(R.id.tv_widget_secondary, blockedText)
            }
            else -> {
                views.setTextViewText(R.id.tv_widget_badge, if (isEnglish) "PROTECTION ACTIVE" else "KORUMA AKTİF")
                views.setTextColor(R.id.tv_widget_badge, Color.parseColor("#2E9E5B"))
                views.setTextViewText(R.id.tv_widget_primary, strings.modeTitle(generalMode))
                views.setTextViewText(R.id.tv_widget_secondary, blockedText)
            }
        }

        // Servis kapalıysa doğrudan Erişilebilirlik Ayarları'nı, açıksa uygulamayı aç
        val intent = if (!isServiceActive) {
            Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        } else {
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            appWidgetId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    companion object {
        /**
         * Kotada veya ayarlarda değişiklik olduğunda tüm aktif widget'ları günceller.
         */
        fun updateAllWidgets(context: Context) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
                val component = ComponentName(context, QuotaAppWidgetProvider::class.java)
                val ids = appWidgetManager.getAppWidgetIds(component)
                if (ids != null && ids.isNotEmpty()) {
                    val intent = Intent(context, QuotaAppWidgetProvider::class.java).apply {
                        action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                    }
                    context.sendBroadcast(intent)
                }
            } catch (_: Exception) {}
        }
    }
}
