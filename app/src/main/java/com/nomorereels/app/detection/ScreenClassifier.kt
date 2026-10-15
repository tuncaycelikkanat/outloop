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

class ScreenClassifier {

    /**
     * Düğüm ağacı anlık görüntüsünü (snapshot) analiz ederek mevcut ekran türünü belirler.
     * Saf fonksiyon mantığında çalışır; cihaz olmadan JVM birim testleriyle tamamen test edilebilir.
     */
    fun classify(snapshot: ScreenSnapshot): ScreenType {
        if (snapshot.rootNode == null) {
            return ScreenType.UNKNOWN
        }

        // 1. KONTROL: Ana Akış (Feed) sekmesi aktif olarak seçili mi?
        // Eğer alt bardaki feed_tab seçiliyse, kullanıcı kesinlikle Ana Sayfa'dadır.
        // Akıştaki Reels videoları, kartlar veya hikayeler bu durumu bozamaz.
        if (isFeedTabSelected(snapshot)) {
            return ScreenType.MAIN_FEED
        }

        // 2. KONTROL: Hikaye (Story) ekranı mı?
        if (isStoryScreen(snapshot)) {
            return ScreenType.MAIN_FEED
        }

        // 3. KONTROL: Alt sekme çubuğundaki gerçek Reels sekmesi mi seçili?
        // Instagram'da alt barın asıl Reels sekmesi 'clips_tab' ID'sine sahiptir.
        if (isClipsTabSelected(snapshot)) {
            return ScreenType.REELS_TAB
        }

        // 4. KONTROL: DM (Direkt Mesajlar) ekranı mı?
        if (isDirectMessageScreen(snapshot)) {
            return ScreenType.DIRECT_MESSAGES
        }

        // 5. KONTROL: Tam ekran modal Reels Oynatıcısı mı?
        // Alt sekme çubuğu yokken veya modal aktivitede açılan Reels izleyicisi.
        if (isClipsViewer(snapshot)) {
            return ScreenType.REELS_VIEWER
        }

        // 6. KONTROL: Genel Ana Akış belirteçleri
        if (isMainFeed(snapshot)) {
            return ScreenType.MAIN_FEED
        }

        return ScreenType.UNKNOWN
    }

    private fun isFeedTabSelected(snapshot: ScreenSnapshot): Boolean {
        return snapshot.findNodes { node ->
            val id = node.viewId ?: ""
            val desc = node.contentDescription ?: ""
            val isHomeId = id.contains("feed_tab", ignoreCase = true) || id.contains("tab_home", ignoreCase = true)
            val isHomeDesc = desc.equals("Home", ignoreCase = true) || desc.equals("Ana Sayfa", ignoreCase = true)
            (isHomeId || isHomeDesc) && node.isSelected
        }.isNotEmpty()
    }

    private fun isClipsTabSelected(snapshot: ScreenSnapshot): Boolean {
        return snapshot.findNodes { node ->
            val id = node.viewId ?: ""
            val isClipsTabId = id.contains("clips_tab", ignoreCase = true) || id.contains("tab_clips", ignoreCase = true)
            isClipsTabId && node.isSelected
        }.isNotEmpty()
    }

    private fun isStoryScreen(snapshot: ScreenSnapshot): Boolean {
        val activity = snapshot.currentActivity ?: ""
        if (activity.contains("ReelViewerActivity", ignoreCase = true)) {
            return true
        }
        return snapshot.containsId("reel_viewer_layout") || snapshot.containsId("story_viewer")
    }

    private fun isClipsViewer(snapshot: ScreenSnapshot): Boolean {
        val activity = snapshot.currentActivity ?: ""
        val isModalActivity = activity.contains("ModalActivity", ignoreCase = true) ||
                              activity.contains("TransparentModalActivity", ignoreCase = true)

        val knownClipsViewerIds = listOf(
            "clips_viewer_view_pager",
            "clips_video_container",
            "clips_media_interactivity_container"
        )

        val hasClipsNode = snapshot.findNodes { node ->
            val id = node.viewId ?: ""
            knownClipsViewerIds.any { id.contains(it, ignoreCase = true) }
        }.isNotEmpty()

        // Alt bar feed_tab'ı görünür ve seçiliyken asla bağımsız Clips Viewer sayılmaz
        val isFeedActive = isFeedTabSelected(snapshot)
        return hasClipsNode && !isFeedActive && (isModalActivity || !snapshot.containsId("feed_tab"))
    }

    private fun isDirectMessageScreen(snapshot: ScreenSnapshot): Boolean {
        return snapshot.containsId("direct_") ||
               snapshot.currentActivity?.contains("Direct", ignoreCase = true) == true
    }

    private fun isMainFeed(snapshot: ScreenSnapshot): Boolean {
        return snapshot.containsId("feed") ||
               snapshot.containsId("action_bar_home") ||
               snapshot.containsId("swipeable_tab_view_pager")
    }
}
