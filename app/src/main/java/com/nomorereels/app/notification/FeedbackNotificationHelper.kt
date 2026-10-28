package com.nomorereels.app.notification

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.nomorereels.app.R
import com.nomorereels.app.data.AppLanguage
import com.nomorereels.app.data.SettingsStore

/**
 * Reels engellendiğinde veya kota uyarılarında ekranın üst kısmından
 * akıcı ve yerel olarak kayan Android Heads-Up (Kafadan) Sistem Bildirimlerini yönetir.
 *
 * - %100 yerel ve kararlıdır (WindowManager overlay hileleri yerine Android'in kendi altyapısını kullanır).
 * - Sessizdir (setSilent=true, müzik veya video sesini kesmez).
 * - Otomatik kaybolur (setTimeoutAfter ile 2.5 saniye sonra bildirim çekmecesinden de silinir).
 */
object FeedbackNotificationHelper {

    private const val TAG = "NoMoreReels"
    const val CHANNEL_ID = "outloop_feedback_v2"
    private const val NOTIFICATION_ID = 2001

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val isEnglish = SettingsStore.appLanguage.value == AppLanguage.EN
            val channelName = if (isEnglish) "Outloop Feedback" else "Outloop Bildirimleri"
            val channelDesc = if (isEnglish)
                "Status and block alerts for Outloop"
            else
                "Outloop Reels engelleme ve kota durum uyarıları"

            val channel = NotificationChannel(
                CHANNEL_ID,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = channelDesc
                setSound(null, null)
                enableVibration(false)
                setShowBadge(false)
            }

            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.createNotificationChannel(channel)
        }
    }

    @SuppressLint("MissingPermission")
    fun showFeedbackNotification(
        context: Context,
        title: String,
        message: String? = null,
        durationMs: Long = 2500L
    ) {
        if (!SettingsStore.overlayFeedbackEnabled.value) {
            return
        }

        try {
            ensureChannel(context)

            val nmCompat = NotificationManagerCompat.from(context)
            if (!nmCompat.areNotificationsEnabled()) {
                Log.d(TAG, "Bildirim izni kapalı, sistem bildirimi atlandı.")
                return
            }

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_outloop)
                .setContentTitle(title)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setAutoCancel(true)
                .setTimeoutAfter(durationMs)
                .setOnlyAlertOnce(true)
                .setSilent(true)

            if (!message.isNullOrBlank()) {
                builder.setContentText(message)
            }

            nmCompat.notify(NOTIFICATION_ID, builder.build())
        } catch (e: Exception) {
            Log.w(TAG, "Feedback bildirimi gösterilemedi: ${e.message}")
        }
    }

    fun cancelFeedbackNotification(context: Context) {
        try {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
        } catch (_: Exception) {}
    }
}
