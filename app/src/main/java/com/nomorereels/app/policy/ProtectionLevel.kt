package com.nomorereels.app.policy

/**
 * Kullanıcı için hazır koruma seviyeleri / profilleri (Faz 3).
 * Kullanıcı dilerse tek dokunuşla bu hazır profillerden birini seçebilir
 * veya her bölgeyi ayrı ayrı özelleştirebilir.
 */
enum class ProtectionLevel(
    val title: String,
    val subtitle: String,
    val dmMode: DmReelMode,
    val exploreMode: DmReelMode,
    val feedMode: DmReelMode,
    val profileMode: DmReelMode,
    val reelsTabMode: DmReelMode
) {
    STRICT(
        title = "Komple Yasak",
        subtitle = "Tüm bölgelerde (Reels sekmesi, DM, Keşfet) Reels tamamen kilitlidir. Sıfır video.",
        dmMode = DmReelMode.BLOCK_ALL,
        exploreMode = DmReelMode.BLOCK_ALL,
        feedMode = DmReelMode.BLOCK_ALL,
        profileMode = DmReelMode.BLOCK_ALL,
        reelsTabMode = DmReelMode.BLOCK_ALL
    ),
    SINGLE_REEL(
        title = "Sadece Bir Tane",
        subtitle = "Reels sekmesinden veya DM'den açılan ilk video serbest; 2. videoya kaydırınca anında kapatır.",
        dmMode = DmReelMode.SINGLE_REEL,
        exploreMode = DmReelMode.SINGLE_REEL,
        feedMode = DmReelMode.SINGLE_REEL,
        profileMode = DmReelMode.SINGLE_REEL,
        reelsTabMode = DmReelMode.SINGLE_REEL
    ),
    COUNT_QUOTA(
        title = "X Tane İzleme",
        subtitle = "Günlük belirlenen adet (örn. 10 Reel) kadar serbesttir; kotan bitince kilitler.",
        dmMode = DmReelMode.COUNT_QUOTA,
        exploreMode = DmReelMode.COUNT_QUOTA,
        feedMode = DmReelMode.COUNT_QUOTA,
        profileMode = DmReelMode.COUNT_QUOTA,
        reelsTabMode = DmReelMode.COUNT_QUOTA
    ),
    TIME_QUOTA(
        title = "X Dakika İzleme",
        subtitle = "Günlük belirlenen dakika (örn. 15 dk) kadar serbesttir; süren dolunca kilitler.",
        dmMode = DmReelMode.TIME_QUOTA,
        exploreMode = DmReelMode.TIME_QUOTA,
        feedMode = DmReelMode.TIME_QUOTA,
        profileMode = DmReelMode.TIME_QUOTA,
        reelsTabMode = DmReelMode.TIME_QUOTA
    ),
    BALANCED(
        title = "Dengeli",
        subtitle = "DM ve Keşfet'ten tek video serbest (2. videoya kaydırma korumalı), doğrudan Reels sekmesi kilitli.",
        dmMode = DmReelMode.SINGLE_REEL,
        exploreMode = DmReelMode.SINGLE_REEL,
        feedMode = DmReelMode.SINGLE_REEL,
        profileMode = DmReelMode.SINGLE_REEL,
        reelsTabMode = DmReelMode.BLOCK_ALL
    ),
    MINDFUL(
        title = "Bilinçli Sürtünme (5 Sn Onay)",
        subtitle = "DM ve Keşfet videoları için 5 sn basılı tutma onayı gerektirir; akış ve Reels sekmesi kilitlidir.",
        dmMode = DmReelMode.ASK_WITH_FRICTION,
        exploreMode = DmReelMode.ASK_WITH_FRICTION,
        feedMode = DmReelMode.BLOCK_ALL,
        profileMode = DmReelMode.SINGLE_REEL,
        reelsTabMode = DmReelMode.BLOCK_ALL
    );

    fun localizedTitle(isEnglish: Boolean): String = if (!isEnglish) title else when (this) {
        STRICT -> "Block All"
        SINGLE_REEL -> "Just One Reel"
        COUNT_QUOTA -> "Count Quota"
        TIME_QUOTA -> "Time Quota"
        BALANCED -> "Balanced"
        MINDFUL -> "Mindful Friction (5s Hold)"
    }

    fun localizedSubtitle(isEnglish: Boolean): String = if (!isEnglish) subtitle else when (this) {
        STRICT -> "Reels is completely locked across all regions (Reels tab, DM, Explore). Zero videos."
        SINGLE_REEL -> "First video opened from DM or Reels tab is allowed; exits immediately on swipe to 2nd video."
        COUNT_QUOTA -> "Allowed up to your daily count (e.g. 10 Reels); locks once reached."
        TIME_QUOTA -> "Allowed up to your daily time (e.g. 15 min); locks once expired."
        BALANCED -> "Single video allowed from DM and Explore (swipe-protected), direct Reels tab locked."
        MINDFUL -> "Requires a 5s hold confirmation for DM and Explore videos; feed and Reels tab locked."
    }

    fun matches(
        dm: DmReelMode,
        explore: DmReelMode,
        feed: DmReelMode,
        profile: DmReelMode,
        reelsTab: DmReelMode
    ): Boolean {
        return dmMode == dm &&
                exploreMode == explore &&
                feedMode == feed &&
                profileMode == profile &&
                reelsTabMode == reelsTab
    }

    companion object {
        fun findMatchingLevel(
            dm: DmReelMode,
            explore: DmReelMode,
            feed: DmReelMode,
            profile: DmReelMode,
            reelsTab: DmReelMode
        ): ProtectionLevel? {
            return entries.firstOrNull { it.matches(dm, explore, feed, profile, reelsTab) }
        }
    }
}
