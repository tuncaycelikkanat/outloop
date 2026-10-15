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
        // En yüksek öncelik: Kullanıcı alt bardaki Reels butonuna bastığında anında yakala!
        if (isClipsTabSelected(snapshot)) {
            return ScreenType.REELS_TAB
        }

        // 2. KONTROL: Ana Akış (Feed) sekmesi aktif olarak seçili mi?
        // ÇOK ÖNEMLİ: DM kontrolünden ÖNCE çalışır! Sağ üstteki DM ikonu ana akışı DM yapamaz!
        if (isFeedTabSelected(snapshot)) {
            return ScreenType.MAIN_FEED
        }

        // 3. KONTROL: Profilde açılmış bir gönderi (Fotoğraf, Video postu) detayı mı?
        // Kendi postunu açtığında Reels sanıp çıkmayı önler.
        if (isProfilePostDetail(snapshot)) {
            return ScreenType.PROFILE
        }

        // 4. KONTROL: Profil ana ekranı mı?
        if (isProfile(snapshot)) {
            return ScreenType.PROFILE
        }

        // 5. KONTROL: Keşfet (Explore) ekranı mı?
        if (isExplore(snapshot)) {
            return ScreenType.EXPLORE
        }

        // 6. KONTROL: Gerçek DM (Direkt Mesajlar) ekranı mı?
        // (Sadece DM aktivitesi veya mesaj kutusu fragment'ı açıkken geçerli)
        if (isDirectMessageScreen(snapshot)) {
            return ScreenType.DIRECT_MESSAGES
        }

        // 7. KONTROL: Tam ekran dikey kaydırmalı Reels Oynatıcısı mı?
        // (Sadece clips_viewer_view_pager olduğunda ve normal gönderi detayı değilken)
        if (isClipsViewer(snapshot)) {
            return ScreenType.REELS_VIEWER
        }

        // 8. KONTROL: Hikaye (Story) ekranı mı?
        if (isStoryScreen(snapshot)) {
            return ScreenType.MAIN_FEED
        }

        // 9. KONTROL: Genel Ana Akış belirteçleri
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

    private fun isProfilePostDetail(snapshot: ScreenSnapshot): Boolean {
        // Profildeyken bir gönderiye tıklandığında:
        // Üst barda "Gönderiler" / "Posts" başlığı veya geri butonu yer alır.
        val hasPostTitle = snapshot.findNodes { node ->
            val text = node.text ?: ""
            val desc = node.contentDescription ?: ""
            text.equals("Gönderiler", ignoreCase = true) ||
            text.equals("Posts", ignoreCase = true) ||
            desc.equals("Gönderiler", ignoreCase = true) ||
            desc.equals("Posts", ignoreCase = true) ||
            desc.contains("gönderi", ignoreCase = true)
        }.isNotEmpty()

        val hasDetailFeedId = snapshot.containsId("feed_detail") ||
                              snapshot.containsId("media_set") ||
                              snapshot.containsId("row_feed")

        return hasPostTitle || hasDetailFeedId
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
            desc.contains("takipçi", ignoreCase = true)
        }.isNotEmpty()

        return hasProfileTab || hasProfileContent
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

    private fun isDirectMessageScreen(snapshot: ScreenSnapshot): Boolean {
        // 1. Ana Sayfa, Keşfet veya Profil alt çubuğu mevcutsa veya Ana Sayfa sekmesi seçiliyse ASLA DM ekranı olamaz!
        if (snapshot.containsId("feed_tab") || isFeedTabSelected(snapshot)) {
            return false
        }

        // 2. Reels oynatıcı veya hikaye ekranındaysak DM sayılmaz
        if (snapshot.containsId("clips_viewer_view_pager") || snapshot.containsId("reel_viewer_layout")) {
            return false
        }

        // 3. Aktivite adı kontrolü
        val activity = snapshot.currentActivity ?: ""
        if (rules.directMessages.activityKeywords.any { activity.contains(it, ignoreCase = true) }) {
            return true
        }

        // 4. Görünüm ID'leri kontrolü (direct_, thread_, message_composer, notes_tray vb.)
        val hasDmViewId = snapshot.findNodes { node ->
            val id = node.viewId ?: ""
            if (id.contains("direct_share", ignoreCase = true)) return@findNodes false
            rules.directMessages.viewIds.any { id.contains(it, ignoreCase = true) }
        }.isNotEmpty()

        if (hasDmViewId) return true

        // 5. Metin ve İçerik Açıklaması Kontrolleri (Mesajlar, Sohbetler vb.)
        return snapshot.findNodes { node ->
            val text = node.text ?: ""
            val desc = node.contentDescription ?: ""
            rules.directMessages.textKeywords.any { kw ->
                text.contains(kw, ignoreCase = true) || desc.contains(kw, ignoreCase = true)
            }
        }.isNotEmpty()
    }

    private fun isClipsViewer(snapshot: ScreenSnapshot): Boolean {
        // Normal gönderi detayındaysak veya Ana Sayfa aktifse ASLA Reels Viewer sayılmaz!
        if (isProfilePostDetail(snapshot) || isFeedTabSelected(snapshot)) {
            return false
        }

        val activity = snapshot.currentActivity ?: ""
        val isModalActivity = rules.reelsViewer.modalActivities.any { activity.contains(it, ignoreCase = true) }

        // Gerçek dikey Reels izleyicisi göstergeleri
        val hasClipsViewerPager = snapshot.findNodes { node ->
            val id = node.viewId ?: ""
            rules.reelsViewer.viewerIds.any { id.contains(it, ignoreCase = true) }
        }.isNotEmpty()

        return hasClipsViewerPager || isModalActivity
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

    private fun isMainFeed(snapshot: ScreenSnapshot): Boolean {
        return snapshot.findNodes { node ->
            val id = node.viewId ?: ""
            rules.feed.containerIds.any { id.contains(it, ignoreCase = true) }
        }.isNotEmpty()
    }
}
