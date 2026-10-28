package com.nomorereels.app.policy

import com.nomorereels.app.data.SettingsStore
import com.nomorereels.app.ui.MindfulActivities

/**
 * Farkındalık Kapısı (Mindful Gateway) için hem Android View (OverlayController)
 * hem de Jetpack Compose (MindfulGatewayDialog) katmanlarının ortak kullandığı
 * iş kuralları, metinler ve durum hesaplamaları (4.3-1 .. 4.3-9).
 */
object GatewaySpec {

    val DURATION_OPTIONS = listOf(3, 5, 10)
    const val DEFAULT_DURATION_MINUTES = 5

    /** Kademeli bekleme süresi: 1. mola 10 sn, 2. mola 20 sn, 3. mola 40 sn (4.3-2). */
    fun computeInitialWaitSeconds(snoozeCountToday: Int): Int = when {
        snoozeCountToday <= 0 -> 10
        snoozeCountToday == 1 -> 20
        else -> 40
    }

    fun remainingDailySnoozes(snoozeCountToday: Int): Int =
        (SettingsStore.MAX_DAILY_SNOOZES - snoozeCountToday).coerceAtLeast(0)

    fun canApproveSnooze(isBedtime: Boolean, snoozeCountToday: Int): Boolean =
        !isBedtime && remainingDailySnoozes(snoozeCountToday) > 0

    fun lockReasonText(
        isBedtime: Boolean,
        snoozeCountToday: Int,
        isEnglish: Boolean
    ): String? {
        return when {
            isBedtime -> if (isEnglish) {
                "🌙 Bedtime Focus Hours are active. Breaks are locked tonight to protect your rest."
            } else {
                "🌙 Gece Odak Saati aktif. Uyku düzenini korumak için bu saatlerde mola izni kapalıdır."
            }
            remainingDailySnoozes(snoozeCountToday) <= 0 -> if (isEnglish) {
                "🍃 All ${SettingsStore.MAX_DAILY_SNOOZES} daily mindful windows have been used today."
            } else {
                "🍃 Bugünkü ${SettingsStore.MAX_DAILY_SNOOZES} bilinçli mola hakkının tamamı kullanıldı."
            }
            else -> null
        }
    }

    /** Kapının neden açıldığını gösteren bağlamsal rozet metni (4.3-4). */
    fun contextualBadgeText(
        decision: PolicyDecision?,
        originLabel: String?,
        isEnglish: Boolean
    ): String {
        return when (decision) {
            PolicyDecision.BLOCK_BEDTIME ->
                if (isEnglish) "🌙 BEDTIME FOCUS" else "🌙 GECE ODAK SAATİ"
            PolicyDecision.BLOCK_DAILY_COUNT_LIMIT ->
                if (isEnglish) "✨ DAILY REEL QUOTA REACHED" else "✨ GÜNLÜK REEL KOTASI DOLDU"
            PolicyDecision.BLOCK_DAILY_TIME_LIMIT ->
                if (isEnglish) "⏳ DAILY TIME QUOTA REACHED" else "⏳ GÜNLÜK SÜRE KOTASI DOLDU"
            PolicyDecision.BLOCK_AFTER_SWIPE ->
                if (isEnglish) "🌿 SINGLE REEL LIMIT" else "🌿 TEK REEL SINIRI"
            PolicyDecision.BLOCK_AFTER_TIMEOUT ->
                if (isEnglish) "🍃 SINGLE REEL TIME UP" else "🍃 TEK REEL SÜRESİ DOLDU"
            PolicyDecision.ASK_MINDFUL_FRICTION ->
                if (isEnglish) "💬 DM REEL PAUSE" else "💬 DM REEL DURAKLAMASI"
            PolicyDecision.BLOCK_IMMEDIATELY -> {
                if (originLabel == "DIRECT_MESSAGES") {
                    if (isEnglish) "💬 DM REEL PAUSE" else "💬 DM REEL DURAKLAMASI"
                } else {
                    if (isEnglish) "🌿 MINDFUL GATEWAY" else "🌿 FARKINDALIK KAPISI"
                }
            }
            else -> if (isEnglish) "🌿 MINDFUL GATEWAY" else "🌿 FARKINDALIK KAPISI"
        }
    }

    /** Mola bitiminde hatırlatılacak mikro-niyet seçenekleri (4.3-1). */
    fun intentOptions(isEnglish: Boolean): List<Pair<String, String>> {
        return if (isEnglish) {
            listOf(
                "💧 Drink water" to "Drink a glass of water",
                "💬 Check DM only" to "Just check the message & exit",
                "🧘 Quick breather" to "Take a short mental break",
                "💼 Back to focus" to "Return to work / study"
            )
        } else {
            listOf(
                "💧 Su içeceğim" to "Bir bardak su içeceğim",
                "💬 Sadece mesaj" to "Mesaja bakıp çıkacağım",
                "🧘 Kısa mola" to "Kısa bir zihin molası",
                "💼 İşe döneceğim" to "İşime / dersime döneceğim"
            )
        }
    }

    fun breathingPhaseText(isInhale: Boolean, isEnglish: Boolean): String {
        return if (isEnglish) {
            if (isInhale) "Inhale..." else "Exhale..."
        } else {
            if (isInhale) "Nefes al..." else "Nefes ver..."
        }
    }

    fun allowActionText(
        durationMinutes: Int,
        remainingSeconds: Int,
        remainingSnoozes: Int,
        isEnglish: Boolean
    ): String {
        return if (remainingSeconds > 0) {
            if (isEnglish) {
                "Allow $durationMinutes min (${remainingSeconds}s) · $remainingSnoozes/${SettingsStore.MAX_DAILY_SNOOZES} left"
            } else {
                "$durationMinutes dk izin ver (${remainingSeconds} sn) · Kalan hak: $remainingSnoozes/${SettingsStore.MAX_DAILY_SNOOZES}"
            }
        } else {
            if (isEnglish) {
                "Allow $durationMinutes minutes ($remainingSnoozes/${SettingsStore.MAX_DAILY_SNOOZES} left today)"
            } else {
                "$durationMinutes dakika izin ver (Bugün kalan: $remainingSnoozes/${SettingsStore.MAX_DAILY_SNOOZES})"
            }
        }
    }

    fun randomQuickActivityText(isEnglish: Boolean): String {
        val list = MindfulActivities.getActivitiesForBracket(com.nomorereels.app.ui.TimeBracket.MIN_1_3)
        val activity = if (list.isNotEmpty()) list.random() else MindfulActivities.activities.first()
        return "✨ ${activity.localizedTitle(isEnglish)}"
    }
}
