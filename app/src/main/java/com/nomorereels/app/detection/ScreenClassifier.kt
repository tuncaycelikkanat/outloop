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
     * Saf fonksiyon mantığında çalışır; Android Framework bağımlılığı olmadığı için
     * cihaz olmadan JVM birim testleriyle tamamen test edilebilir.
     */
    fun classify(snapshot: ScreenSnapshot): ScreenType {
        if (snapshot.rootNode == null) {
            return ScreenType.UNKNOWN
        }

        // ÖNEMLİ INSTAGRAM NOTU:
        // Instagram iç kodunda "Reel" kelimesi HİKAYELER (Stories) için kullanılır:
        // - "reels tray container" = Üstteki hikayeler çubuğu
        // - "ReelViewerActivity" = Hikaye izleyicisi
        // Kısa videolar (Reels) ise Instagram içinde "Clips" olarak adlandırılır:
        // - "clips_tab" = Reels sekmesi
        // - "clips_viewer" = Reels tam ekran video oynatıcısı

        // 1. Kontrol: Hikaye (Story) ekranı mı? (Yanlış pozitifleri hemen önle)
        if (isStoryScreen(snapshot)) {
            return ScreenType.MAIN_FEED
        }

        // 2. Kontrol: Reels Sekmesi aktif olarak seçili mi?
        if (isReelsTabSelected(snapshot)) {
            return ScreenType.REELS_TAB
        }

        // 3. Kontrol: Tam ekran Reels Oynatıcısı (Clips Viewer) açık mı?
        if (isClipsViewer(snapshot)) {
            return ScreenType.REELS_VIEWER
        }

        // 4. Kontrol: DM (Direkt Mesajlar) ekranı mı?
        if (isDirectMessageScreen(snapshot)) {
            return ScreenType.DIRECT_MESSAGES
        }

        // 5. Kontrol: Ana Akış (Feed) mı?
        if (isMainFeed(snapshot)) {
            return ScreenType.MAIN_FEED
        }

        return ScreenType.UNKNOWN
    }

    private fun isStoryScreen(snapshot: ScreenSnapshot): Boolean {
        val activity = snapshot.currentActivity ?: ""
        if (activity.contains("ReelViewerActivity", ignoreCase = true)) {
            return true
        }
        return snapshot.containsId("reel_viewer_layout") ||
               snapshot.containsId("story_viewer")
    }

    private fun isReelsTabSelected(snapshot: ScreenSnapshot): Boolean {
        val matchingNodes = snapshot.findNodes { node ->
            val desc = node.contentDescription ?: ""
            // Hikayeleri kesinlikle ayıkla
            if (desc.contains("story", ignoreCase = true) || desc.contains("tray", ignoreCase = true)) {
                return@findNodes false
            }

            // Tam olarak "Reels" sekmesi kontrolü
            val isReelsDesc = desc.equals("Reels", ignoreCase = true)
            val isClipsTabId = node.viewId?.let { id ->
                id.contains("clips_tab", ignoreCase = true) ||
                id.contains("tab_clips", ignoreCase = true)
            } == true

            (isReelsDesc || isClipsTabId) && node.isSelected
        }

        return matchingNodes.isNotEmpty()
    }

    private fun isClipsViewer(snapshot: ScreenSnapshot): Boolean {
        val knownClipsViewerIds = listOf(
            "clips_viewer",
            "clips_video_container",
            "clips_media_interactivity_container",
            "clips_swipe_refresh_container",
            "clips_video_layout"
        )

        val hasClipsNode = snapshot.findNodes { node ->
            val id = node.viewId ?: ""
            knownClipsViewerIds.any { id.contains(it, ignoreCase = true) }
        }.isNotEmpty()

        return hasClipsNode
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
