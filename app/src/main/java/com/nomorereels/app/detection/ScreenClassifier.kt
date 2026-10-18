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

/**
 * Oynatılan Reel videosunun kimliğini temsil eden yapısal parmak izi.
 * DM'den açılan tek bir Reel izlenirken kullanıcının parmağını kaydırıp
 * 2. bir Reel videosuna geçip geçmediğini tespit etmek için kullanılır.
 */
data class ReelContentFingerprint(
    val author: String? = null,
    val mediaDesc: String? = null,
    val caption: String? = null
) {
    val isEmpty: Boolean
        get() = author.isNullOrBlank() && mediaDesc.isNullOrBlank() && caption.isNullOrBlank()

    /**
     * İki parmak izinin farklı bir Reel videosuna ait olup olmadığını döner.
     * İlk yükleme anında bazı alanların (örn. caption) 100-200ms geç yüklenmesi
     * yanlış pozitif üretmesin diye yalnızca her iki tarafta da dolu olan alanlar karşılaştırılır.
     */
    fun isDifferentReelFrom(other: ReelContentFingerprint): Boolean {
        if (!author.isNullOrBlank() && !other.author.isNullOrBlank() &&
            !author.equals(other.author, ignoreCase = true)
        ) {
            return true
        }
        if (!mediaDesc.isNullOrBlank() && !other.mediaDesc.isNullOrBlank() &&
            !mediaDesc.equals(other.mediaDesc, ignoreCase = true)
        ) {
            return true
        }
        if (!caption.isNullOrBlank() && !other.caption.isNullOrBlank() &&
            caption != other.caption
        ) {
            return true
        }
        return false
    }

    /**
     * Aynı Reel yüklenirken sonradan gelen eksik alanları (örn. geç yüklenen caption) tamamlar.
     */
    fun mergeWith(newer: ReelContentFingerprint): ReelContentFingerprint {
        return ReelContentFingerprint(
            author = author?.takeIf { it.isNotBlank() } ?: newer.author?.takeIf { it.isNotBlank() },
            mediaDesc = mediaDesc?.takeIf { it.isNotBlank() } ?: newer.mediaDesc?.takeIf { it.isNotBlank() },
            caption = caption?.takeIf { it.isNotBlank() } ?: newer.caption?.takeIf { it.isNotBlank() }
        )
    }

    fun toSummaryString(): String {
        val parts = mutableListOf<String>()
        if (!author.isNullOrBlank()) parts.add("@$author")
        if (!caption.isNullOrBlank()) parts.add("\"${caption.take(20)}\"")
        else if (!mediaDesc.isNullOrBlank()) parts.add("\"${mediaDesc.take(24)}\"")
        return if (parts.isEmpty()) "belirsiz" else parts.joinToString(" ")
    }
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
        if (isClipsTabSelected(snapshot)) {
            return ScreenType.REELS_TAB
        }

        // 2. KONTROL: Ana Akış (Feed) sekmesi aktif olarak seçili mi?
        // Ana Sayfa aktifken arkada önbellekte kalan fragment'lar geri butonu spam'i yapamaz.
        if (isFeedTabSelected(snapshot)) {
            return ScreenType.MAIN_FEED
        }

        // 3. KONTROL: Keşfet (Explore - Mercek ikonu) ızgara ekranı mı?
        if (isExplore(snapshot)) {
            return ScreenType.EXPLORE
        }

        // 4. KONTROL: Profil ekranı veya normal fotoğraf/gönderi detayı mı?
        if (isProfilePostDetail(snapshot) || isProfile(snapshot)) {
            return ScreenType.PROFILE
        }

        // 5. KONTROL: DM (Direkt Mesajlar) sekmesi veya mesajlaşma ekranı mı?
        if (isDirectMessageScreen(snapshot)) {
            return ScreenType.DIRECT_MESSAGES
        }

        // 6. KONTROL: Tam ekran dikey kaydırmalı Reels Oynatıcısı mı?
        // (DM'den, Keşfet'ten, Akış'tan veya Profil'den açılan Reels oynatıcısı)
        if (isClipsViewer(snapshot)) {
            return ScreenType.REELS_VIEWER
        }

        // 7. KONTROL: Hikaye (Story) ekranı mı?
        if (isStoryScreen(snapshot)) {
            return ScreenType.MAIN_FEED
        }

        // 8. KONTROL: Genel Ana Akış belirteçleri
        if (isMainFeed(snapshot)) {
            return ScreenType.MAIN_FEED
        }

        return ScreenType.UNKNOWN
    }

    /**
     * REELS_VIEWER ekranındayken üst bar başlığı veya görünüm yapısından videonun
     * hangi ekrandan açıldığına dair doğrudan bir ipucu çıkarır (örn. Keşfet üst barında 'Explore').
     */
    fun extractReelOriginHint(snapshot: ScreenSnapshot): ScreenType? {
        if (snapshot.rootNode == null) return null

        // 1. Keşfet'ten açılan Reel: Üst barda clips_viewer_action_bar_title 'Explore' / 'Keşfet' yazar
        val hasExploreTitle = snapshot.findNodes { node ->
            val id = node.viewId ?: ""
            val text = node.text ?: ""
            val desc = node.contentDescription ?: ""
            id.contains("clips_viewer_action_bar_title", ignoreCase = true) &&
                (text.equals("Explore", ignoreCase = true) ||
                 text.equals("Keşfet", ignoreCase = true) ||
                 desc.equals("Explore", ignoreCase = true) ||
                 desc.equals("Keşfet", ignoreCase = true))
        }.isNotEmpty()
        if (hasExploreTitle) return ScreenType.EXPLORE

        // 2. DM'den açılan Reel
        val hasDmHint = snapshot.findNodes { node ->
            val id = node.viewId ?: ""
            id.contains("inbox_refreshable_thread_list", ignoreCase = true) ||
            id.contains("message_composer", ignoreCase = true) ||
            id.contains("thread_fragment", ignoreCase = true)
        }.isNotEmpty()
        if (hasDmHint) return ScreenType.DIRECT_MESSAGES

        // 3. Profil'den açılan Reel
        if (isProfileTabSelected(snapshot)) return ScreenType.PROFILE

        // 4. Ana Akış'tan açılan Reel: Üst barda "Reels" / "Friends" sekmesi
        val hasFeedTabs = snapshot.findNodes { node ->
            val id = node.viewId ?: ""
            val desc = node.contentDescription ?: ""
            (id.contains("action_bar_tab_layout", ignoreCase = true) || desc.equals("Friends", ignoreCase = true))
        }.isNotEmpty()
        if (hasFeedTabs) return ScreenType.MAIN_FEED

        return null
    }

    /**
     * REELS_VIEWER ekranındayken aktif videonun yazar, medya açıklaması ve altyazı (caption)
     * bilgilerinden yapısal bir parmak izi çıkarır. Kullanıcı 2. bir Reel'e kaydırdığında
     * bu parmak izi değişir.
     */
    fun extractReelFingerprint(snapshot: ScreenSnapshot): ReelContentFingerprint {
        if (snapshot.rootNode == null) return ReelContentFingerprint()

        val authorNode = snapshot.findNodes { node ->
            val id = node.viewId ?: ""
            id.contains("clips_author_username", ignoreCase = true) ||
            id.contains("clips_author_profile_pic", ignoreCase = true)
        }.firstOrNull { !it.contentDescription.isNullOrBlank() || !it.text.isNullOrBlank() }

        val rawAuthor = authorNode?.contentDescription?.takeIf { it.isNotBlank() }
            ?: authorNode?.text?.takeIf { it.isNotBlank() }
        val normalizedAuthor = rawAuthor
            ?.removePrefix("Profile picture of ")
            ?.removeSuffix("'s profile picture")
            ?.trim()

        val mediaNode = snapshot.findNodes { node ->
            val id = node.viewId ?: ""
            id.contains("clips_media_component", ignoreCase = true) ||
            id.contains("clips_video_container", ignoreCase = true)
        }.firstOrNull { !it.contentDescription.isNullOrBlank() }

        val captionContainer = snapshot.findNodes { node ->
            val id = node.viewId ?: ""
            id.contains("clips_caption_component", ignoreCase = true)
        }.firstOrNull()

        val captionDesc = captionContainer
            ?.find { !it.contentDescription.isNullOrBlank() }
            ?.firstOrNull()
            ?.contentDescription
            ?.trim()

        return ReelContentFingerprint(
            author = normalizedAuthor,
            mediaDesc = mediaNode?.contentDescription?.trim(),
            caption = captionDesc
        )
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

            // Yalnızca gerçek alt bar sekme ID'si (clips_tab) kontrol edilir
            val matchesId = rules.reelsTab.tabIds.any { id.contains(it, ignoreCase = true) }
            matchesId && node.isSelected
        }.isNotEmpty()
    }

    private fun isDirectTabSelected(snapshot: ScreenSnapshot): Boolean {
        return snapshot.findNodes { node ->
            val id = node.viewId ?: ""
            val desc = node.contentDescription ?: ""
            val matchesId = id.contains("direct_tab", ignoreCase = true) ||
                id.contains("tab_direct", ignoreCase = true)
            val matchesDesc = desc.equals("Message", ignoreCase = true) ||
                desc.equals("Mesaj", ignoreCase = true) ||
                desc.contains("Direct", ignoreCase = true)
            (matchesId || matchesDesc) && node.isSelected
        }.isNotEmpty()
    }

    private fun isExploreTabSelected(snapshot: ScreenSnapshot): Boolean {
        return snapshot.findNodes { node ->
            val id = node.viewId ?: ""
            val desc = node.contentDescription ?: ""
            val matchesId = rules.explore.tabIds.any { id.contains(it, ignoreCase = true) }
            val matchesDesc = rules.explore.tabDescriptions.any {
                desc.equals(it, ignoreCase = true) || desc.contains(it, ignoreCase = true)
            }
            (matchesId || matchesDesc) && node.isSelected
        }.isNotEmpty()
    }

    private fun isProfileTabSelected(snapshot: ScreenSnapshot): Boolean {
        return snapshot.findNodes { node ->
            val id = node.viewId ?: ""
            val desc = node.contentDescription ?: ""
            val matchesId = rules.profile.tabIds.any { id.contains(it, ignoreCase = true) }
            val matchesDesc = rules.profile.tabDescriptions.any { desc.equals(it, ignoreCase = true) }
            (matchesId || matchesDesc) && node.isSelected
        }.isNotEmpty()
    }

    private fun isProfilePostDetail(snapshot: ScreenSnapshot): Boolean {
        if (hasClipsViewerContainer(snapshot)) {
            return false
        }

        val hasPostTitle = snapshot.findNodes { node ->
            val text = node.text ?: ""
            val desc = node.contentDescription ?: ""
            text.equals("Gönderiler", ignoreCase = true) ||
            text.equals("Posts", ignoreCase = true) ||
            desc.equals("Gönderiler", ignoreCase = true) ||
            desc.equals("Posts", ignoreCase = true)
        }.isNotEmpty()

        val hasDetailFeedId = snapshot.containsId("feed_detail") ||
            (snapshot.containsId("row_feed_photo") && isProfileTabSelected(snapshot))

        return hasPostTitle || hasDetailFeedId
    }

    private fun isProfile(snapshot: ScreenSnapshot): Boolean {
        val hasProfileContent = snapshot.findNodes { node ->
            val id = node.viewId ?: ""
            val desc = node.contentDescription ?: ""
            if (id.contains("row_feed_", ignoreCase = true)) {
                return@findNodes false
            }
            rules.profile.contentIds.any { id.contains(it, ignoreCase = true) } ||
            desc.contains("profili düzenle", ignoreCase = true) ||
            desc.contains("edit profile", ignoreCase = true)
        }.isNotEmpty()

        if (hasClipsViewerContainer(snapshot) && !hasProfileContent) {
            return false
        }

        return isProfileTabSelected(snapshot) || hasProfileContent
    }

    private fun isExplore(snapshot: ScreenSnapshot): Boolean {
        val hasExploreContent = snapshot.findNodes { node ->
            val id = node.viewId ?: ""
            val desc = node.contentDescription ?: ""
            val text = node.text ?: ""
            if (id.contains("tab", ignoreCase = true)) {
                return@findNodes false
            }
            rules.explore.contentIds.any { id.contains(it, ignoreCase = true) } ||
            desc.equals("Arama", ignoreCase = true) ||
            desc.equals("Search", ignoreCase = true) ||
            text.equals("Arama", ignoreCase = true) ||
            text.equals("Search", ignoreCase = true)
        }.isNotEmpty()

        if (hasClipsViewerContainer(snapshot) && !hasExploreContent) {
            return false
        }

        val hasExploreTab = isExploreTabSelected(snapshot)

        return hasExploreTab || hasExploreContent
    }

    private fun isDirectMessageScreen(snapshot: ScreenSnapshot): Boolean {
        if (hasClipsViewerContainer(snapshot) || snapshot.containsId("reel_viewer_layout")) {
            return false
        }

        if (isDirectTabSelected(snapshot)) {
            return true
        }

        val hasDmComponents = snapshot.findNodes { node ->
            val id = node.viewId ?: ""
            id.contains("inbox_refreshable_thread_list", ignoreCase = true) ||
            id.contains("cf_hub_recycler_view", ignoreCase = true) ||
            id.contains("message_list", ignoreCase = true) ||
            id.contains("message_composer", ignoreCase = true) ||
            id.contains("thread_fragment", ignoreCase = true) ||
            id.contains("direct_quick_snap", ignoreCase = true) ||
            id.contains("row_thread", ignoreCase = true)
        }.isNotEmpty()

        if (hasDmComponents) {
            return true
        }

        if (isFeedTabSelected(snapshot) || isExploreTabSelected(snapshot) || isProfileTabSelected(snapshot)) {
            return false
        }

        val activity = snapshot.currentActivity ?: ""
        if (rules.directMessages.activityKeywords.any { activity.contains(it, ignoreCase = true) }) {
            return true
        }

        val hasDmViewId = snapshot.findNodes { node ->
            val id = node.viewId ?: ""
            if (id.contains("direct_share", ignoreCase = true)) return@findNodes false
            rules.directMessages.viewIds.any { id.contains(it, ignoreCase = true) }
        }.isNotEmpty()

        if (hasDmViewId) return true

        return snapshot.findNodes { node ->
            val text = node.text?.trim() ?: ""
            val desc = node.contentDescription?.trim() ?: ""
            rules.directMessages.textKeywords.any { kw ->
                text.equals(kw, ignoreCase = true) || desc.equals(kw, ignoreCase = true)
            }
        }.isNotEmpty()
    }

    private fun hasClipsViewerContainer(snapshot: ScreenSnapshot): Boolean {
        return snapshot.findNodes { node ->
            val id = node.viewId ?: ""
            rules.reelsViewer.viewerIds.any { id.contains(it, ignoreCase = true) }
        }.isNotEmpty()
    }

    private fun isClipsViewer(snapshot: ScreenSnapshot): Boolean {
        if (isProfilePostDetail(snapshot)) {
            return false
        }

        if (hasClipsViewerContainer(snapshot)) {
            return true
        }

        val activity = snapshot.currentActivity ?: ""
        return rules.reelsViewer.modalActivities.any { activity.contains(it, ignoreCase = true) }
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
