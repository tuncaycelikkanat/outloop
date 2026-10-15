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

        // 1. KONTROL: Ana Akış (Feed) sekmesi aktif olarak seçili mi?
        // Alt bardaki feed_tab seçiliyse akıştaki diğer öğeler görmezden gelinir.
        if (isFeedTabSelected(snapshot)) {
            return ScreenType.MAIN_FEED
        }

        // 2. KONTROL: Hikaye (Story) ekranı mı?
        if (isStoryScreen(snapshot)) {
            return ScreenType.MAIN_FEED
        }

        // 3. KONTROL: Alt sekme çubuğundaki gerçek Reels sekmesi mi seçili?
        if (isClipsTabSelected(snapshot)) {
            return ScreenType.REELS_TAB
        }

        // 4. KONTROL: DM (Direkt Mesajlar) ekranı mı?
        if (isDirectMessageScreen(snapshot)) {
            return ScreenType.DIRECT_MESSAGES
        }

        // 5. KONTROL: Keşfet (Explore) sekmesi mi seçili?
        if (isExploreSelected(snapshot)) {
            return ScreenType.EXPLORE
        }

        // 6. KONTROL: Profil sekmesi mi seçili?
        if (isProfileSelected(snapshot)) {
            return ScreenType.PROFILE
        }

        // 7. KONTROL: Tam ekran modal Reels Oynatıcısı mı?
        if (isClipsViewer(snapshot)) {
            return ScreenType.REELS_VIEWER
        }

        // 8. KONTROL: Genel Ana Akış belirteçleri
        if (isMainFeed(snapshot)) {
            return ScreenType.MAIN_FEED
        }

        return ScreenType.UNKNOWN
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

    private fun isStoryScreen(snapshot: ScreenSnapshot): Boolean {
        val activity = snapshot.currentActivity ?: ""
        val matchesActivity = rules.story.activities.any { activity.contains(it, ignoreCase = true) }
        if (matchesActivity) return true

        return snapshot.findNodes { node ->
            val id = node.viewId ?: ""
            rules.story.layoutIds.any { id.contains(it, ignoreCase = true) }
        }.isNotEmpty()
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

    private fun isExploreSelected(snapshot: ScreenSnapshot): Boolean {
        return snapshot.findNodes { node ->
            val id = node.viewId ?: ""
            val desc = node.contentDescription ?: ""
            val matchesId = rules.explore.tabIds.any { id.contains(it, ignoreCase = true) }
            val matchesDesc = rules.explore.tabDescriptions.any { desc.equals(it, ignoreCase = true) }
            (matchesId || matchesDesc) && node.isSelected
        }.isNotEmpty()
    }

    private fun isProfileSelected(snapshot: ScreenSnapshot): Boolean {
        return snapshot.findNodes { node ->
            val id = node.viewId ?: ""
            val desc = node.contentDescription ?: ""
            val matchesId = rules.profile.tabIds.any { id.contains(it, ignoreCase = true) }
            val matchesDesc = rules.profile.tabDescriptions.any { desc.equals(it, ignoreCase = true) }
            (matchesId || matchesDesc) && node.isSelected
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
