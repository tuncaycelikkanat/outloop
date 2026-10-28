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

    /** Arayüzde çip / etiket olarak kullanılacak kısa ad. */
    val shortTitle: String
        get() = localizedShortTitle(isEnglish = false)

    fun localizedTitle(isEnglish: Boolean): String = if (!isEnglish) title else when (this) {
        FOLLOW_GLOBAL -> "Follow Global Mode"
        BLOCK_ALL -> "Block All"
        SINGLE_REEL -> "Just One Reel"
        COUNT_QUOTA -> "X Reels (Daily Quota)"
        TIME_QUOTA -> "X Minutes (Daily Time)"
        ASK_WITH_FRICTION -> "Mindful Hold (5s)"
        FREE -> "Unrestricted"
    }

    fun localizedDescription(isEnglish: Boolean): String = if (!isEnglish) description else when (this) {
        FOLLOW_GLOBAL -> "Follows the general protection mode on the home screen."
        BLOCK_ALL -> "Never allows Reels to open; exits immediately."
        SINGLE_REEL -> "Allows the first opened video; exits as soon as you swipe to a 2nd Reel."
        COUNT_QUOTA -> "Allows up to your daily count limit; blocks once reached."
        TIME_QUOTA -> "Allows up to your daily minute limit; blocks once reached."
        ASK_WITH_FRICTION -> "Shows a 5-second hold prompt when a Reel opens. Allows a single video if approved."
        FREE -> "Does not intervene in Reels opened from this region."
    }

    fun localizedShortTitle(isEnglish: Boolean): String = if (!isEnglish) {
        when (this) {
            FOLLOW_GLOBAL -> "Genel moda uy"
            BLOCK_ALL -> "Komple Yasak"
            SINGLE_REEL -> "Sadece 1 Tane"
            COUNT_QUOTA -> "X Tane / Süre"
            TIME_QUOTA -> "X Dakika"
            ASK_WITH_FRICTION -> "5 sn Onaylı"
            FREE -> "Serbest"
        }
    } else {
        when (this) {
            FOLLOW_GLOBAL -> "Follow Global"
            BLOCK_ALL -> "Block All"
            SINGLE_REEL -> "Just 1 Reel"
            COUNT_QUOTA -> "Count Quota"
            TIME_QUOTA -> "Time Quota"
            ASK_WITH_FRICTION -> "5s Hold"
            FREE -> "Free"
        }
    }

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

    fun localizedReason(isEnglish: Boolean): String = if (!isEnglish) reasonText else when (this) {
        ALLOW -> "Allowed"
        BLOCK_IMMEDIATELY -> "Blocked (Reels Feed)"
        BLOCK_AFTER_SWIPE -> "Blocked (Swiped to 2nd Reel!)"
        BLOCK_AFTER_TIMEOUT -> "Blocked (Single Reel Time Expired)"
        BLOCK_BEDTIME -> "Blocked (Bedtime Focus · Sleep Time!)"
        BLOCK_DAILY_COUNT_LIMIT -> "Blocked (Daily Reel Count Reached!)"
        BLOCK_DAILY_TIME_LIMIT -> "Blocked (Daily Reel Time Reached!)"
        ASK_MINDFUL_FRICTION -> "Awaiting Mindful Hold (Hold 5s)"
    }

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
        val activeMode = resolveActiveMode(
            detectedScreen = detectedScreen,
            originScreen = tracker.reelOriginScreen,
            dmMode = dmMode,
            exploreMode = exploreMode,
            feedMode = feedMode,
            profileMode = profileMode,
            reelsTabMode = reelsTabMode,
            generalMode = generalMode
        )

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

    companion object {
        /**
         * Reels ekranı ve köken ekranına göre uygulanacak nihai modu döndürür.
         * FOLLOW_GLOBAL burada genel moda çözülür; dönen değer asla FOLLOW_GLOBAL olmaz
         * (genel mod da FOLLOW_GLOBAL ise SINGLE_REEL'e düşer).
         */
        fun resolveActiveMode(
            detectedScreen: ScreenType,
            originScreen: ScreenType?,
            dmMode: DmReelMode,
            exploreMode: DmReelMode,
            feedMode: DmReelMode,
            profileMode: DmReelMode,
            reelsTabMode: DmReelMode,
            generalMode: DmReelMode
        ): DmReelMode {
            val rawMode = if (detectedScreen == ScreenType.REELS_TAB) {
                reelsTabMode
            } else {
                when (originScreen) {
                    ScreenType.DIRECT_MESSAGES -> dmMode
                    ScreenType.EXPLORE -> exploreMode
                    ScreenType.MAIN_FEED -> feedMode
                    ScreenType.PROFILE -> profileMode
                    else -> reelsTabMode
                }
            }
            return resolveFollowGlobal(rawMode, generalMode)
        }

        fun resolveFollowGlobal(mode: DmReelMode, generalMode: DmReelMode): DmReelMode {
            if (mode != DmReelMode.FOLLOW_GLOBAL) return mode
            return if (generalMode == DmReelMode.FOLLOW_GLOBAL) DmReelMode.SINGLE_REEL else generalMode
        }

        /**
         * Modların sertlik sırası (küçük = daha sıkı). İrade Kilidi, bir değişikliğin korumayı
         * gevşetip gevşetmediğine bu sıralamaya bakarak karar verir.
         */
        fun strictnessRank(mode: DmReelMode): Int = when (mode) {
            DmReelMode.BLOCK_ALL -> 0
            DmReelMode.ASK_WITH_FRICTION -> 1
            DmReelMode.SINGLE_REEL -> 2
            DmReelMode.COUNT_QUOTA -> 3
            DmReelMode.TIME_QUOTA -> 3
            DmReelMode.FOLLOW_GLOBAL -> 2
            DmReelMode.FREE -> 5
        }

        /** Eski ve yeni (çözülmüş) bölge modları arasında herhangi bir bölge gevşedi mi? */
        fun isLoosening(oldModes: List<DmReelMode>, newModes: List<DmReelMode>): Boolean {
            return oldModes.zip(newModes).any { (old, new) -> strictnessRank(new) > strictnessRank(old) }
        }
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
