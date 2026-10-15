package com.nomorereels.app.detection

enum class ScreenType {
    MAIN_FEED,
    REELS_TAB,
    REELS_VIEWER,
    DIRECT_MESSAGES,
    EXPLORE,
    PROFILE,
    UNKNOWN;

    val isReels: Boolean
        get() = this == REELS_TAB || this == REELS_VIEWER
}

class ScreenClassifier(
    var rules: RuleSet = RuleSet.DEFAULT
) {

    /**
     * Düğüm ağacı anlık görüntüsünü (snapshot) dinamik kural kümesine göre analiz ederek
     * mevcut ekran türünü belirler. Saf Kotlin fonksiyonudur.
     */
    fun classify(snapshot: ScreenSnapshot): ScreenType {
        if (snapshot.rootNode == null) {
            return ScreenType.UNKNOWN
        }

        // 1. KONTROL: Alt sekme çubuğundaki gerçek Reels sekmesi mi seçili?
        // En öncelikli kontrol: Reels sekmesine tıklandıysa derhal yakala!
        if (isClipsTabSelected(snapshot)) {
            return ScreenType.REELS_TAB
        }

        // 2. KONTROL: Tam ekran modal Reels Oynatıcısı mı?
        if (isClipsViewer(snapshot)) {
            return ScreenType.REELS_VIEWER
        }

        // 3. KONTROL: DM (Direkt Mesajlar) ekranı mı?
        if (isDirectMessageScreen(snapshot)) {
            return ScreenType.DIRECT_MESSAGES
        }

        // 4. KONTROL: Profil ekranı mı?
        if (isProfile(snapshot)) {
            return ScreenType.PROFILE
        }

        // 5. KONTROL: Keşfet (Explore) ekranı mı?
        if (isExplore(snapshot)) {
            return ScreenType.EXPLORE
        }

        // 6. KONTROL: Hikaye (Story) ekranı mı?
        if (isStoryScreen(snapshot)) {
            return ScreenType.MAIN_FEED
        }

        // 7. KONTROL: Ana Akış (Feed) sekmesi aktif olarak seçili mi?
        if (isFeedTabSelected(snapshot) || isMainFeed(snapshot)) {
            return ScreenType.MAIN_FEED
        }

        return ScreenType.UNKNOWN
    }

    private fun isClipsTabSelected(snapshot: ScreenSnapshot): Boolean {
        return snapshot.findNodes { node ->
            val id = node.viewId ?: ""
            val desc = node.contentDescription ?: ""

            // Hariç tutulacaklar kontrolü (örn. "story", "tray")
            if (rules.reelsTab.excludedDescriptions.any { desc.contains(it, ignoreCase = true) }) {
                return@findNodes false
            }

            val matchesId = rules.reelsTab.tabIds.any { id.contains(it, ignoreCase = true) }
            matchesId && node.isSelected
        }.isNotEmpty()
    }

    private fun isFeedTabSelected(snapshot: ScreenSnapshot): Boolean {
        return snapshot.findNodes { node ->
            val id = node.viewId ?: ""
            val desc = node.contentDescription ?: ""
            val matchesId = rules.feed.tabIds.any { id.contains(it, ignoreCase = true) }
            val matchesDesc = rules.feed.tabDescriptions.any { desc.equals(it, ignoreCase = true) }
            (matchesId || matchesDesc) && node.isSelected
        }.isNotEmpty()
    }

    private fun isExplore(snapshot: ScreenSnapshot): Boolean {
        // Alt sekme çubuğu kontrolü
        val hasExploreTab = snapshot.findNodes { node ->
            val id = node.viewId ?: ""
            val desc = node.contentDescription ?: ""
            val matchesId = rules.explore.tabIds.any { id.contains(it, ignoreCase = true) }
            val matchesDesc = rules.explore.tabDescriptions.any { desc.contains(it, ignoreCase = true) }
            (matchesId || matchesDesc) && node.isSelected
        }.isNotEmpty()

        // Ekran içeriği kontrolü (arama kutusu, keşfet gridi)
        val hasExploreContent = snapshot.findNodes { node ->
            val id = node.viewId ?: ""
            val desc = node.contentDescription ?: ""
            rules.explore.contentIds.any { id.contains(it, ignoreCase = true) } ||
            desc.contains("Arama", ignoreCase = true) ||
            desc.contains("Ara ve Keşfet", ignoreCase = true)
        }.isNotEmpty()

        return hasExploreTab || hasExploreContent
    }

    private fun isProfile(snapshot: ScreenSnapshot): Boolean {
        // Alt sekme çubuğu kontrolü (Profil avatar sekmesi)
        val hasProfileTab = snapshot.findNodes { node ->
            val id = node.viewId ?: ""
            val desc = node.contentDescription ?: ""
            val matchesId = rules.profile.tabIds.any { id.contains(it, ignoreCase = true) }
            val matchesDesc = rules.profile.tabDescriptions.any { desc.contains(it, ignoreCase = true) }
            (matchesId || matchesDesc) && node.isSelected
        }.isNotEmpty()

        // Ekran içeriği kontrolü (Profili düzenle butonu, kullanıcı başlığı, istatistikler)
        val hasProfileContent = snapshot.findNodes { node ->
            val id = node.viewId ?: ""
            val desc = node.contentDescription ?: ""
            rules.profile.contentIds.any { id.contains(it, ignoreCase = true) } ||
            desc.contains("profili düzenle", ignoreCase = true) ||
            desc.contains("edit profile", ignoreCase = true) ||
            desc.contains("gönderi", ignoreCase = true) ||
            desc.contains("takipçi", ignoreCase = true)
        }.isNotEmpty()

        return hasProfileTab || hasProfileContent
    }

    private fun isDirectMessageScreen(snapshot: ScreenSnapshot): Boolean {
        val activity = snapshot.currentActivity ?: ""
        val matchesActivity = rules.directMessages.activityKeywords.any { activity.contains(it, ignoreCase = true) }
        if (matchesActivity) return true

        return snapshot.findNodes { node ->
            val id = node.viewId ?: ""
            rules.directMessages.viewIds.any { id.contains(it, ignoreCase = true) }
        }.isNotEmpty()
    }

    private fun isStoryScreen(snapshot: ScreenSnapshot): Boolean {
        val activity = snapshot.currentActivity ?: ""
        val matchesActivity = rules.story.activities.any { activity.contains(it, ignoreCase = true) }
        if (matchesActivity) return true

        return snapshot.findNodes { node ->
            val id = node.viewId ?: ""
            rules.story.layoutIds.any { id.contains(it, ignoreCase = true) }
        }.isNotEmpty()
    }

    private fun isClipsViewer(snapshot: ScreenSnapshot): Boolean {
        val activity = snapshot.currentActivity ?: ""
        val isModalActivity = rules.reelsViewer.modalActivities.any { activity.contains(it, ignoreCase = true) }

        val hasClipsNode = snapshot.findNodes { node ->
            val id = node.viewId ?: ""
            rules.reelsViewer.viewerIds.any { id.contains(it, ignoreCase = true) }
        }.isNotEmpty()

        val isFeedActive = isFeedTabSelected(snapshot)
        return hasClipsNode && !isFeedActive && (isModalActivity || !snapshot.containsId("feed_tab"))
    }

    private fun isMainFeed(snapshot: ScreenSnapshot): Boolean {
        return snapshot.findNodes { node ->
            val id = node.viewId ?: ""
            rules.feed.containerIds.any { id.contains(it, ignoreCase = true) }
        }.isNotEmpty()
    }
}
