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

        // 1. Kontrol: Reels Sekmesi aktif olarak seçili mi?
        if (isReelsTabSelected(snapshot)) {
            return ScreenType.REELS_TAB
        }

        // 2. Kontrol: Tam ekran Reels Oynatıcısı (Clips Viewer) açık mı?
        if (isReelsViewer(snapshot)) {
            return ScreenType.REELS_VIEWER
        }

        // 3. Kontrol: DM (Direkt Mesajlar) ekranı mı?
        if (isDirectMessageScreen(snapshot)) {
            return ScreenType.DIRECT_MESSAGES
        }

        // 4. Kontrol: Ana Akış (Feed) mı?
        if (isMainFeed(snapshot)) {
            return ScreenType.MAIN_FEED
        }

        return ScreenType.UNKNOWN
    }

    private fun isReelsTabSelected(snapshot: ScreenSnapshot): Boolean {
        // Alt sekme çubuğunda "Reels" açıklamasında veya ID'sinde seçili (selected) bir düğüm var mı?
        val reelsTabNodes = snapshot.findNodes { node ->
            val isReelsDesc = node.contentDescription?.equals("Reels", ignoreCase = true) == true
            val isReelsId = node.viewId?.let { id ->
                id.contains("clips_tab", ignoreCase = true) ||
                id.contains("reels_tab", ignoreCase = true) ||
                id.contains("tab_clips", ignoreCase = true)
            } == true

            (isReelsDesc || isReelsId) && (node.isSelected || node.isClickable)
        }

        // Eğer seçili durumda bir reels tabı varsa
        return reelsTabNodes.any { it.isSelected }
    }

    private fun isReelsViewer(snapshot: ScreenSnapshot): Boolean {
        // Instagram Reels tam ekran video oynatıcı göstergeleri
        val knownViewerIds = listOf(
            "clips_viewer",
            "clips_video_container",
            "reel_viewer",
            "clips_media_interactivity_container",
            "clips_swipe_refresh_container",
            "clips_video_layout",
            "clips_action_bar_button"
        )

        // Modal activity veya viewer ID'leri
        val activity = snapshot.currentActivity ?: ""
        val isModalActivity = activity.contains("ModalActivity", ignoreCase = true) ||
                              activity.contains("TransparentModalActivity", ignoreCase = true)

        val hasViewerNode = snapshot.findNodes { node ->
            val id = node.viewId ?: ""
            knownViewerIds.any { id.contains(it, ignoreCase = true) }
        }.isNotEmpty()

        return hasViewerNode || (isModalActivity && snapshot.containsContentDescription("Reels"))
    }

    private fun isDirectMessageScreen(snapshot: ScreenSnapshot): Boolean {
        return snapshot.containsId("direct_") ||
               snapshot.currentActivity?.contains("Direct", ignoreCase = true) == true
    }

    private fun isMainFeed(snapshot: ScreenSnapshot): Boolean {
        return snapshot.containsId("feed") ||
               snapshot.containsId("action_bar_home")
    }
}
