package com.nomorereels.app.policy

/**
 * Kullanıcının seçtiği genel mod, bölge modları (DM, Keşfet, Feed, Profil, Alt Bar),
 * kota limitleri ve Gece Odak Saati / Mola durumlarına bakarak, "Instagram'da ne olacak?"
 * sorusuna seçili dilde (TR veya EN) tek bir doğal cümleyle canlı özet üreten yardımcı sınıf.
 */
object PolicySummaryHelper {

    fun getSummarySentence(
        generalMode: DmReelMode,
        dmMode: DmReelMode,
        exploreMode: DmReelMode,
        feedMode: DmReelMode,
        profileMode: DmReelMode,
        reelsTabMode: DmReelMode,
        isBedtimeActive: Boolean,
        isSnoozeActive: Boolean,
        snoozeRemainingMinutes: Int,
        dailyCountLimit: Int,
        dailyTimeLimitMinutes: Int,
        isEnglish: Boolean = false,
        isProtectionEnabled: Boolean = true
    ): String {
        // 0. Koruma Kapalıysa
        if (!isProtectionEnabled) {
            return if (isEnglish) {
                "Protection is off · Reels can be watched freely."
            } else {
                "Koruma şu an kapalı · Reels videoları serbestçe izlenebilir."
            }
        }

        // 1. Mola Modu (Snooze) Aktifse
        if (isSnoozeActive) {
            return if (isEnglish) {
                val minText = if (snoozeRemainingMinutes > 0) "$snoozeRemainingMinutes min remaining" else "Expiring soon"
                "$minText break active · Protection restores automatically."
            } else {
                val minText = if (snoozeRemainingMinutes > 0) "$snoozeRemainingMinutes dk kaldı" else "Süre dolmak üzere"
                "$minText mola devrede · Süre bitince koruma otomatik kilitlenecek."
            }
        }

        // 2. Gece Odak Saati Aktifse
        if (isBedtimeActive) {
            return if (isEnglish) {
                "Bedtime Focus active · All Reels feeds locked unconditionally until morning."
            } else {
                "Gece Odak Saati devrede · Sabaha kadar tüm Reels akışları kilitlidir."
            }
        }

        // 3. Bölgelerin çözümlenmiş halleri
        val resolvedDm = ReelsPolicy.resolveFollowGlobal(dmMode, generalMode)
        val resolvedExplore = ReelsPolicy.resolveFollowGlobal(exploreMode, generalMode)
        val resolvedFeed = ReelsPolicy.resolveFollowGlobal(feedMode, generalMode)
        val resolvedProfile = ReelsPolicy.resolveFollowGlobal(profileMode, generalMode)
        val resolvedTab = ReelsPolicy.resolveFollowGlobal(reelsTabMode, generalMode)

        val allSame = resolvedDm == resolvedExplore &&
                resolvedExplore == resolvedFeed &&
                resolvedFeed == resolvedProfile &&
                resolvedProfile == resolvedTab

        // 4. Tüm bölgeler aynı moddaysa tek cümle
        if (allSame) {
            return if (isEnglish) {
                when (resolvedDm) {
                    DmReelMode.BLOCK_ALL -> "All Instagram Reels feeds completely locked (Zero videos)."
                    DmReelMode.SINGLE_REEL -> "First video allowed; closes immediately upon scrolling to next."
                    DmReelMode.COUNT_QUOTA -> "Daily limit of $dailyCountLimit Reels active; locks when reached."
                    DmReelMode.TIME_QUOTA -> "Daily limit of $dailyTimeLimitMinutes minutes active; locks when reached."
                    DmReelMode.ASK_WITH_FRICTION -> "5-second mindful hold required to open Reels."
                    DmReelMode.FREE -> "All Reels feeds free; protection inactive."
                    DmReelMode.FOLLOW_GLOBAL -> "First video allowed, closes on scroll."
                }
            } else {
                when (resolvedDm) {
                    DmReelMode.BLOCK_ALL -> "Tüm Instagram Reels akışları tamamen kilitli (Sıfır video)."
                    DmReelMode.SINGLE_REEL -> "Açılan ilk video serbest; 2. videoya kaydırınca anında kapatılır."
                    DmReelMode.COUNT_QUOTA -> "Günlük $dailyCountLimit Reel kotası devrede; bitince kilitlenir."
                    DmReelMode.TIME_QUOTA -> "Günlük $dailyTimeLimitMinutes dakika süresi devrede; dolunca kilitlenir."
                    DmReelMode.ASK_WITH_FRICTION -> "Tüm Reels videoları için 5 saniye basılı tutma onayı gerekir."
                    DmReelMode.FREE -> "Tüm Reels akışları serbest; koruma devre dışı."
                    DmReelMode.FOLLOW_GLOBAL -> "İlk video serbest, kaydırınca kapatılır."
                }
            }
        }

        // 5. Dengeli veya Bölge Bazlı Özel Kurallar Varsa
        if (isEnglish) {
            val tabText = when (resolvedTab) {
                DmReelMode.BLOCK_ALL -> "Reels tab locked"
                DmReelMode.SINGLE_REEL -> "single video on Reels tab"
                DmReelMode.FREE -> "Reels tab open"
                else -> "Reels tab protected"
            }
            val dmText = when (resolvedDm) {
                DmReelMode.BLOCK_ALL -> "DM locked"
                DmReelMode.SINGLE_REEL -> "single video in DM"
                DmReelMode.ASK_WITH_FRICTION -> "DM 5s delay"
                DmReelMode.FREE -> "DM open"
                else -> "DM quota"
            }
            val quotaText = if (generalMode == DmReelMode.COUNT_QUOTA) {
                ", daily $dailyCountLimit Reels quota"
            } else if (generalMode == DmReelMode.TIME_QUOTA) {
                ", daily $dailyTimeLimitMinutes min limit"
            } else ""

            return "On Instagram: $dmText, $tabText$quotaText."
        } else {
            val tabText = when (resolvedTab) {
                DmReelMode.BLOCK_ALL -> "Reels sekmesi kilitli"
                DmReelMode.SINGLE_REEL -> "Reels sekmesinde tek video"
                DmReelMode.FREE -> "Reels sekmesi serbest"
                else -> "Reels sekmesi korumalı"
            }
            val dmText = when (resolvedDm) {
                DmReelMode.BLOCK_ALL -> "DM kapalı"
                DmReelMode.SINGLE_REEL -> "DM'den tek video serbest"
                DmReelMode.ASK_WITH_FRICTION -> "DM 5 sn onaylı"
                DmReelMode.FREE -> "DM serbest"
                else -> "DM kotalı"
            }
            val quotaText = if (generalMode == DmReelMode.COUNT_QUOTA) {
                ", günlük $dailyCountLimit Reel kotası"
            } else if (generalMode == DmReelMode.TIME_QUOTA) {
                ", günlük $dailyTimeLimitMinutes dk süre"
            } else ""

            return "Instagram'da: $dmText, $tabText$quotaText."
        }
    }
}
