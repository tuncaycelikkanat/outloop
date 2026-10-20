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
import com.nomorereels.app.data.QuotaStore
import com.nomorereels.app.data.SettingsStore
import com.nomorereels.app.policy.DmReelMode
import com.nomorereels.app.service.ReelsAccessibilityService
import com.nomorereels.app.ui.MainActivity

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
        SettingsStore.init(context)
        QuotaStore.init(context)

        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_quota)

        val isServiceActive = ReelsAccessibilityService.isServiceActive.value
        val isSnoozeActive = SettingsStore.isSnoozeActiveNow()
        val generalMode = SettingsStore.generalMode.value
        val blockCount = maxOf(
            ReelsAccessibilityService.actionExecutor.blockCount.value,
            QuotaStore.todayBlockCount.value
        )
        val todayCount = QuotaStore.todayReelCount.value
        val countLimit = QuotaStore.dailyCountLimit.value
        val todayMinutes = (QuotaStore.todayTimeSpentSeconds.value / 60).toInt()
        val timeLimitMinutes = QuotaStore.dailyTimeLimitMinutes.value

        when {
            !isServiceActive -> {
                views.setTextViewText(R.id.tv_widget_badge, "KORUMA KAPALI")
                views.setTextColor(R.id.tv_widget_badge, Color.parseColor("#CF4040"))
                views.setTextViewText(R.id.tv_widget_primary, "Servis Kapalı")
                views.setTextViewText(R.id.tv_widget_secondary, "Açmak için dokunun")
            }
            isSnoozeActive -> {
                val remMins = SettingsStore.getSnoozeRemainingMinutes()
                views.setTextViewText(R.id.tv_widget_badge, "MOLA DEVREDE")
                views.setTextColor(R.id.tv_widget_badge, Color.parseColor("#E08A1E"))
                views.setTextViewText(R.id.tv_widget_primary, "Kalan: $remMins dk")
                views.setTextViewText(R.id.tv_widget_secondary, "Süre bitince koruma geri döner")
            }
            generalMode == DmReelMode.BLOCK_ALL -> {
                views.setTextViewText(R.id.tv_widget_badge, "KORUMA AKTİF")
                views.setTextColor(R.id.tv_widget_badge, Color.parseColor("#2E9E5B"))
                views.setTextViewText(R.id.tv_widget_primary, "Komple Yasak")
                views.setTextViewText(R.id.tv_widget_secondary, "Bugün $blockCount Reel engellendi")
            }
            generalMode == DmReelMode.SINGLE_REEL -> {
                views.setTextViewText(R.id.tv_widget_badge, "KORUMA AKTİF")
                views.setTextColor(R.id.tv_widget_badge, Color.parseColor("#2E9E5B"))
                views.setTextViewText(R.id.tv_widget_primary, "Sadece 1 Tane")
                views.setTextViewText(R.id.tv_widget_secondary, "İlk serbest · $blockCount engellendi")
            }
            generalMode == DmReelMode.COUNT_QUOTA -> {
                views.setTextViewText(R.id.tv_widget_badge, "KOTA DEVREDE")
                views.setTextColor(R.id.tv_widget_badge, Color.parseColor("#2E9E5B"))
                views.setTextViewText(R.id.tv_widget_primary, "$todayCount / $countLimit Reel")
                views.setTextViewText(R.id.tv_widget_secondary, "Bugün $blockCount Reel engellendi")
            }
            generalMode == DmReelMode.TIME_QUOTA -> {
                views.setTextViewText(R.id.tv_widget_badge, "SÜRE KOTASI")
                views.setTextColor(R.id.tv_widget_badge, Color.parseColor("#2E9E5B"))
                views.setTextViewText(R.id.tv_widget_primary, "$todayMinutes / $timeLimitMinutes dk")
                views.setTextViewText(R.id.tv_widget_secondary, "Bugün $blockCount Reel engellendi")
            }
            else -> {
                views.setTextViewText(R.id.tv_widget_badge, "KORUMA AKTİF")
                views.setTextColor(R.id.tv_widget_badge, Color.parseColor("#2E9E5B"))
                views.setTextViewText(R.id.tv_widget_primary, generalMode.title)
                views.setTextViewText(R.id.tv_widget_secondary, "Bugün $blockCount Reel engellendi")
            }
        }

        // Dokunulduğunda uygulamayı aç
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
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
