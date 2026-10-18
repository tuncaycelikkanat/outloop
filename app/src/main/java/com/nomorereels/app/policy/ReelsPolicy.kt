package com.nomorereels.app.policy

import com.nomorereels.app.detection.ScreenType

/**
 * DM'den (Direkt Mesajlar) açılan Reels oynatıcısı için kullanıcı tercih modları.
 */
enum class DmReelMode(
    val title: String,
    val description: String
) {
    FOLLOW_GLOBAL(
        title = "Genel moda uy",
        description = "Ana ekrandaki genel koruma modunu takip eder."
    ),
    BLOCK_ALL(
        title = "Komple Yasak",
        description = "Reels açılmasına asla izin vermez, anında geri döndürür."
    ),
    SINGLE_REEL(
        title = "Sadece Bir Tane",
        description = "Açılan ilk videoyu izletir; parmağınızı kaydırıp 2. videoya geçtiğiniz an geri döndürür."
    ),
    COUNT_QUOTA(
        title = "X Tane İzleme (Günlük Kota)",
        description = "Günlük belirlenen adet kadar izletir; kota dolduğunda engeller."
    ),
    TIME_QUOTA(
        title = "X Dakika İzleme (Günlük Süre)",
        description = "Günlük belirlenen dakika kadar izletir; süre dolduğunda engeller."
    ),
    ASK_WITH_FRICTION(
        title = "Onaylı (5 Sn Bekleme)",
        description = "Reel açıldığında 5 saniye basılı tutma ekranı çıkarır. Onaylarsanız tek video izletir."
    ),
    FREE(
        title = "Serbest",
        description = "Bu bölgeden açılan Reels oynatıcısına müdahale etmez."
    );

    companion object {
        fun fromName(name: String?, defaultMode: DmReelMode = SINGLE_REEL): DmReelMode {
            return entries.firstOrNull { it.name == name } ?: defaultMode
        }
    }
}

/**
 * ReelsPolicy tarafından üretilen nihai eylem kararı.
 */
enum class PolicyDecision(
    val reasonText: String
) {
    ALLOW("İzin Verildi"),
    BLOCK_IMMEDIATELY("Engellendi (Reels Akışı)"),
    BLOCK_AFTER_SWIPE("Engellendi (2. Reel'e Kaydırıldı!)"),
    BLOCK_AFTER_TIMEOUT("Engellendi (Tek Reel Süresi Doldu)"),
    BLOCK_BEDTIME("Engellendi (Gece Odak Saati · Uyku Vakti!)"),
    BLOCK_DAILY_COUNT_LIMIT("Engellendi (Günlük Reel Kotanız Doldu!)"),
    BLOCK_DAILY_TIME_LIMIT("Engellendi (Günlük Reels Süreniz Doldu!)"),
    ASK_MINDFUL_FRICTION("Farkındalık Onayı Bekleniyor (5 Sn Basılı Tut)");

    val shouldBlock: Boolean
        get() = this == BLOCK_IMMEDIATELY ||
                this == BLOCK_AFTER_SWIPE ||
                this == BLOCK_AFTER_TIMEOUT ||
                this == BLOCK_BEDTIME ||
                this == BLOCK_DAILY_COUNT_LIMIT ||
                this == BLOCK_DAILY_TIME_LIMIT

    val shouldShowFriction: Boolean
        get() = this == ASK_MINDFUL_FRICTION
}

/**
 * Ekran türü, gezinme geçmişi (köken ekran, 2. videoya kaydırma durumu) ve her bölge için
 * kullanıcı tercihlerine göre izin / engelle / farkındalık onayı kararı veren saf Kotlin sınıfı.
 */
class ReelsPolicy(
    private val singleReelMaxDurationMs: Long = NavigationTracker.DEFAULT_SINGLE_REEL_TIMEOUT_MS
) {

    fun evaluate(
        detectedScreen: ScreenType,
        tracker: NavigationTracker,
        dmMode: DmReelMode,
        exploreMode: DmReelMode = DmReelMode.BLOCK_ALL,
        feedMode: DmReelMode = DmReelMode.BLOCK_ALL,
        profileMode: DmReelMode = DmReelMode.BLOCK_ALL,
        reelsTabMode: DmReelMode = DmReelMode.BLOCK_ALL,
        generalMode: DmReelMode = DmReelMode.SINGLE_REEL,
        isBedtimeActive: Boolean = false,
        isCountLimitExceeded: Boolean = false,
        isTimeLimitExceeded: Boolean = false,
        customSingleReelMaxDurationMs: Long? = null,
        nowMs: Long = System.currentTimeMillis()
    ): PolicyDecision {
        // 1. Reels olmayan ekranlara her zaman izin ver
        if (!detectedScreen.isReels) {
            return PolicyDecision.ALLOW
        }

        // 1b. Gece Odak Saati aktifse tüm Reels ekranlarını istisnasız engelle
        if (isBedtimeActive) {
            return PolicyDecision.BLOCK_BEDTIME
        }

        // 2. Ekran türüne (REELS_TAB veya REELS_VIEWER kökenine) göre aktif modu seç:
        val rawMode = if (detectedScreen == ScreenType.REELS_TAB) {
            reelsTabMode
        } else {
            when (tracker.reelOriginScreen) {
                ScreenType.DIRECT_MESSAGES -> dmMode
                ScreenType.EXPLORE -> exploreMode
                ScreenType.MAIN_FEED -> feedMode
                ScreenType.PROFILE -> profileMode
                else -> reelsTabMode
            }
        }

        val activeMode = if (rawMode == DmReelMode.FOLLOW_GLOBAL) generalMode else rawMode

        val effectiveSingleReelDuration = customSingleReelMaxDurationMs ?: singleReelMaxDurationMs

        // 3. Seçili bölge/sekme modunu uygula:
        return evaluateMode(
            mode = activeMode,
            tracker = tracker,
            isCountLimitExceeded = isCountLimitExceeded,
            isTimeLimitExceeded = isTimeLimitExceeded,
            maxDurationMs = effectiveSingleReelDuration,
            nowMs = nowMs
        )
    }

    private fun evaluateMode(
        mode: DmReelMode,
        tracker: NavigationTracker,
        isCountLimitExceeded: Boolean,
        isTimeLimitExceeded: Boolean,
        maxDurationMs: Long,
        nowMs: Long
    ): PolicyDecision {
        return when (mode) {
            DmReelMode.FOLLOW_GLOBAL -> {
                evaluateSingleReelGuard(tracker, maxDurationMs, nowMs)
            }

            DmReelMode.BLOCK_ALL -> {
                PolicyDecision.BLOCK_IMMEDIATELY
            }

            DmReelMode.SINGLE_REEL -> {
                evaluateSingleReelGuard(tracker, maxDurationMs, nowMs)
            }

            DmReelMode.COUNT_QUOTA -> {
                if (isCountLimitExceeded) {
                    PolicyDecision.BLOCK_DAILY_COUNT_LIMIT
                } else {
                    PolicyDecision.ALLOW
                }
            }

            DmReelMode.TIME_QUOTA -> {
                if (isTimeLimitExceeded) {
                    PolicyDecision.BLOCK_DAILY_TIME_LIMIT
                } else {
                    PolicyDecision.ALLOW
                }
            }

            DmReelMode.FREE -> {
                PolicyDecision.ALLOW
            }

            DmReelMode.ASK_WITH_FRICTION -> {
                // Önce 2. videoya kaydırılmış mı kontrol et
                if (tracker.hasSwipedToNextReel) {
                    PolicyDecision.BLOCK_AFTER_SWIPE
                } else if (!tracker.isCurrentDmReelApproved) {
                    // Henüz 5 saniyelik bilinçli sürtünme onayı verilmediyse onay ekranı göster
                    PolicyDecision.ASK_MINDFUL_FRICTION
                } else {
                    // Onay verilmiş; tek Reel kurallarını (kaydırma ve süre) denetle
                    evaluateSingleReelGuard(tracker, maxDurationMs, nowMs)
                }
            }
        }
    }

    private fun evaluateSingleReelGuard(
        tracker: NavigationTracker,
        maxDurationMs: Long,
        nowMs: Long
    ): PolicyDecision {
        if (tracker.hasSwipedToNextReel) {
            return PolicyDecision.BLOCK_AFTER_SWIPE
        }
        if (tracker.isSingleReelTimeoutExpired(maxDurationMs, nowMs)) {
            return PolicyDecision.BLOCK_AFTER_TIMEOUT
        }
        return PolicyDecision.ALLOW
    }
}
