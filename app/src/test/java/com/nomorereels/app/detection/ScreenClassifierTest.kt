package com.nomorereels.app.detection

import com.nomorereels.app.policy.NavigationTracker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ScreenClassifierTest {

    private lateinit var classifier: ScreenClassifier

    @Before
    fun setUp() {
        classifier = ScreenClassifier(RuleSet.DEFAULT)
    }

    @Test
    fun `RuleSet should parse correctly from JSON string`() {
        val json = """
            {
              "version": 3,
              "minAppVersion": "0.1.0",
              "screens": {
                "feed": {
                  "tabIds": ["custom_feed_tab"],
                  "tabDescriptions": ["CustomHome"],
                  "containerIds": ["custom_container"]
                }
              }
            }
        """.trimIndent()

        val ruleSet = RuleSet.fromJson(json)
        assertEquals(3, ruleSet.version)
        assertEquals(listOf("custom_feed_tab"), ruleSet.feed.tabIds)
        assertEquals(listOf("CustomHome"), ruleSet.feed.tabDescriptions)
        assertNotNull(ruleSet.reelsTab)
    }

    @Test
    fun `home screen with direct message icon should classify as MAIN_FEED not DIRECT_MESSAGES`() {
        val rootNode = NodeSnapshot(
            className = "android.widget.FrameLayout",
            children = listOf(
                NodeSnapshot(
                    viewId = "com.instagram.android:id/feed_tab",
                    contentDescription = "Home",
                    isSelected = true
                ),
                NodeSnapshot(
                    viewId = "com.instagram.android:id/direct_inbox_button",
                    contentDescription = "Direct Messages",
                    isSelected = false
                )
            )
        )

        val snapshot = ScreenSnapshot(
            packageName = "com.instagram.android",
            currentActivity = "com.instagram.mainactivity.InstagramMainActivity",
            rootNode = rootNode
        )

        val result = classifier.classify(snapshot)
        assertEquals(ScreenType.MAIN_FEED, result)
        assertFalse(result.isReels)
    }

    @Test
    fun `profile post detail should classify as PROFILE not REELS_VIEWER`() {
        val rootNode = NodeSnapshot(
            className = "android.widget.FrameLayout",
            children = listOf(
                NodeSnapshot(
                    text = "Gönderiler",
                    contentDescription = "Gönderiler"
                ),
                NodeSnapshot(
                    viewId = "com.instagram.android:id/row_feed_photo_profile_imageview"
                )
            )
        )

        val snapshot = ScreenSnapshot(
            packageName = "com.instagram.android",
            currentActivity = "com.instagram.mainactivity.InstagramMainActivity",
            rootNode = rootNode
        )

        val result = classifier.classify(snapshot)
        assertEquals(ScreenType.PROFILE, result)
        assertFalse(result.isReels)
    }

    @Test
    fun `NavigationTracker should return true only on actual screen change and filter noise`() {
        val tracker = NavigationTracker()
        assertEquals(ScreenType.MAIN_FEED, tracker.currentScreen)

        // Aynı ekranda kalma -> false dönmeli
        val duplicate = tracker.update(ScreenType.MAIN_FEED)
        assertFalse(duplicate)
        assertEquals(ScreenType.MAIN_FEED, tracker.currentScreen)

        // Yeni ekrana geçiş -> true dönmeli
        val changed = tracker.update(ScreenType.EXPLORE)
        assertTrue(changed)
        assertEquals(ScreenType.EXPLORE, tracker.currentScreen)
        assertEquals(ScreenType.MAIN_FEED, tracker.previousScreen)

        // UNKNOWN olayı geldiğinde mevcut ekranı KORUMALI!
        val unknownResult = tracker.update(ScreenType.UNKNOWN)
        assertFalse(unknownResult)
        assertEquals(ScreenType.EXPLORE, tracker.currentScreen)
    }

    @Test
    fun `clips tab selected should classify as REELS_TAB`() {
        val rootNode = NodeSnapshot(
            className = "android.widget.FrameLayout",
            children = listOf(
                NodeSnapshot(
                    viewId = "com.instagram.android:id/feed_tab",
                    contentDescription = "Home",
                    isSelected = false
                ),
                NodeSnapshot(
                    viewId = "com.instagram.android:id/clips_tab",
                    contentDescription = "Reels",
                    isSelected = true
                )
            )
        )

        val snapshot = ScreenSnapshot(
            packageName = "com.instagram.android",
            currentActivity = "com.instagram.mainactivity.InstagramMainActivity",
            rootNode = rootNode
        )

        val result = classifier.classify(snapshot)
        assertEquals(ScreenType.REELS_TAB, result)
        assertTrue(result.isReels)
    }

    @Test
    fun `clips viewer in modal activity without feed_tab should classify as REELS_VIEWER`() {
        val rootNode = NodeSnapshot(
            className = "android.widget.FrameLayout",
            children = listOf(
                NodeSnapshot(
                    viewId = "com.instagram.android:id/clips_viewer_view_pager"
                )
            )
        )

        val snapshot = ScreenSnapshot(
            packageName = "com.instagram.android",
            currentActivity = "com.instagram.modal.TransparentModalActivity",
            rootNode = rootNode
        )

        val result = classifier.classify(snapshot)
        assertEquals(ScreenType.REELS_VIEWER, result)
        assertTrue(result.isReels)
    }

    @Test
    fun `direct message inbox without feed_tab should classify as DIRECT_MESSAGES`() {
        val rootNode = NodeSnapshot(
            className = "android.widget.FrameLayout",
            children = listOf(
                NodeSnapshot(
                    viewId = "com.instagram.android:id/action_bar_title",
                    text = "Mesajlar"
                ),
                NodeSnapshot(
                    viewId = "com.instagram.android:id/direct_inbox_filter_button",
                    contentDescription = "Sohbetler"
                ),
                NodeSnapshot(
                    viewId = "com.instagram.android:id/thread_list_item"
                )
            )
        )

        val snapshot = ScreenSnapshot(
            packageName = "com.instagram.android",
            currentActivity = "com.instagram.mainactivity.InstagramMainActivity",
            rootNode = rootNode
        )

        val result = classifier.classify(snapshot)
        assertEquals(ScreenType.DIRECT_MESSAGES, result)
        assertFalse(result.isReels)
    }

    @Test
    fun `direct message chat thread with composer should classify as DIRECT_MESSAGES`() {
        val rootNode = NodeSnapshot(
            className = "android.widget.FrameLayout",
            children = listOf(
                NodeSnapshot(
                    viewId = "com.instagram.android:id/row_thread_composer_edittext",
                    text = "Mesaj gönder..."
                ),
                NodeSnapshot(
                    viewId = "com.instagram.android:id/row_thread_composer_button_send"
                )
            )
        )

        val snapshot = ScreenSnapshot(
            packageName = "com.instagram.android",
            currentActivity = "com.instagram.mainactivity.InstagramMainActivity",
            rootNode = rootNode
        )

        val result = classifier.classify(snapshot)
        assertEquals(ScreenType.DIRECT_MESSAGES, result)
        assertFalse(result.isReels)
    }

    @Test
    fun `reels viewer containing direct_share_button must classify as REELS_VIEWER not DIRECT_MESSAGES`() {
        val rootNode = NodeSnapshot(
            className = "android.widget.FrameLayout",
            children = listOf(
                NodeSnapshot(
                    viewId = "com.instagram.android:id/clips_viewer_view_pager"
                ),
                NodeSnapshot(
                    viewId = "com.instagram.android:id/direct_share_button",
                    contentDescription = "Share"
                )
            )
        )

        val snapshot = ScreenSnapshot(
            packageName = "com.instagram.android",
            currentActivity = "com.instagram.mainactivity.InstagramMainActivity",
            rootNode = rootNode
        )

        val result = classifier.classify(snapshot)
        assertEquals(ScreenType.REELS_VIEWER, result)
        assertTrue(result.isReels)
    }

    @Test
    fun `explore grid with post thumbnails must classify as EXPLORE not PROFILE`() {
        val rootNode = NodeSnapshot(
            className = "android.widget.FrameLayout",
            children = listOf(
                NodeSnapshot(
                    viewId = "com.instagram.android:id/search_tab",
                    contentDescription = "Ara ve Keşfet",
                    isSelected = true
                ),
                NodeSnapshot(
                    viewId = "com.instagram.android:id/media_set_row_content_identifier",
                    children = listOf(
                        NodeSnapshot(
                            contentDescription = "kullanici adlı kişinin gönderisi, 1. satır, 1. sütun"
                        )
                    )
                )
            )
        )

        val snapshot = ScreenSnapshot(
            packageName = "com.instagram.android",
            currentActivity = "com.instagram.mainactivity.InstagramMainActivity",
            rootNode = rootNode
        )

        val result = classifier.classify(snapshot)
        assertEquals(ScreenType.EXPLORE, result)
        assertFalse(result.isReels)
    }

    @Test
    fun `explore grid with search_tab and action_bar_search even with cached clips_viewer_view_pager must classify as EXPLORE`() {
        val rootNode = NodeSnapshot(
            className = "android.widget.FrameLayout",
            children = listOf(
                NodeSnapshot(
                    viewId = "com.instagram.android:id/search_tab",
                    contentDescription = "Search and explore",
                    isSelected = true
                ),
                NodeSnapshot(
                    viewId = "com.instagram.android:id/action_bar_search",
                    contentDescription = "Arama"
                ),
                NodeSnapshot(
                    viewId = "com.instagram.android:id/clips_viewer_view_pager"
                )
            )
        )

        val snapshot = ScreenSnapshot(
            packageName = "com.instagram.android",
            currentActivity = "com.instagram.mainactivity.InstagramMainActivity",
            rootNode = rootNode
        )

        val result = classifier.classify(snapshot)
        assertEquals(ScreenType.EXPLORE, result)
        assertFalse(result.isReels)
    }

    @Test
    fun `reel opened from explore with search_tab and cached row_feed_profile_header must classify as REELS_VIEWER not PROFILE or EXPLORE`() {
        val rootNode = NodeSnapshot(
            className = "android.widget.FrameLayout",
            children = listOf(
                NodeSnapshot(
                    viewId = "com.instagram.android:id/search_tab",
                    contentDescription = "Search and explore",
                    isSelected = true
                ),
                NodeSnapshot(
                    viewId = "com.instagram.android:id/row_feed_profile_header"
                ),
                NodeSnapshot(
                    viewId = "com.instagram.android:id/row_feed_photo_imageview"
                ),
                NodeSnapshot(
                    viewId = "com.instagram.android:id/clips_viewer_view_pager"
                )
            )
        )

        val snapshot = ScreenSnapshot(
            packageName = "com.instagram.android",
            currentActivity = "com.instagram.mainactivity.InstagramMainActivity",
            rootNode = rootNode
        )

        val result = classifier.classify(snapshot)
        assertEquals(ScreenType.REELS_VIEWER, result)
        assertTrue(result.isReels)
    }

    @Test
    fun `home feed with cached clips_viewer_view_pager or selected Reels container must classify as MAIN_FEED`() {
        val rootNode = NodeSnapshot(
            className = "android.widget.FrameLayout",
            children = listOf(
                NodeSnapshot(
                    viewId = "com.instagram.android:id/feed_tab",
                    contentDescription = "Home",
                    isSelected = true
                ),
                NodeSnapshot(
                    viewId = "com.instagram.android:id/clips_viewer_view_pager"
                ),
                NodeSnapshot(
                    className = "android.widget.LinearLayout",
                    contentDescription = "Reels",
                    isSelected = true
                )
            )
        )

        val snapshot = ScreenSnapshot(
            packageName = "com.instagram.android",
            currentActivity = "com.instagram.mainactivity.InstagramMainActivity",
            rootNode = rootNode
        )

        val result = classifier.classify(snapshot)
        assertEquals(ScreenType.MAIN_FEED, result)
        assertFalse(result.isReels)
    }

    @Test
    fun `direct_tab selected in bottom navigation bar must classify as DIRECT_MESSAGES`() {
        val rootNode = NodeSnapshot(
            className = "android.widget.FrameLayout",
            children = listOf(
                NodeSnapshot(
                    viewId = "com.instagram.android:id/feed_tab",
                    contentDescription = "Home",
                    isSelected = false
                ),
                NodeSnapshot(
                    viewId = "com.instagram.android:id/direct_tab",
                    contentDescription = "Message",
                    isSelected = true
                ),
                NodeSnapshot(
                    viewId = "com.instagram.android:id/inbox_refreshable_thread_list_recyclerview"
                )
            )
        )

        val snapshot = ScreenSnapshot(
            packageName = "com.instagram.android",
            currentActivity = "com.instagram.mainactivity.InstagramMainActivity",
            rootNode = rootNode
        )

        val result = classifier.classify(snapshot)
        assertEquals(ScreenType.DIRECT_MESSAGES, result)
    }

    @Test
    fun `clips_viewer_action_bar_title with Explore or Kesfet must extract origin hint as EXPLORE`() {
        val rootNode = NodeSnapshot(
            className = "android.widget.FrameLayout",
            children = listOf(
                NodeSnapshot(
                    viewId = "com.instagram.android:id/clips_viewer_view_pager"
                ),
                NodeSnapshot(
                    viewId = "com.instagram.android:id/clips_viewer_action_bar_title",
                    text = "Explore"
                )
            )
        )

        val snapshot = ScreenSnapshot(
            packageName = "com.instagram.android",
            currentActivity = "com.instagram.modal.TransparentModalActivity",
            rootNode = rootNode
        )

        val originHint = classifier.extractReelOriginHint(snapshot)
        assertEquals(ScreenType.EXPLORE, originHint)
    }

    @Test
    fun `explore grid with grid_card_layout_container and explore_action_bar when scrolled must classify as EXPLORE`() {
        val rootNode = NodeSnapshot(
            className = "android.widget.FrameLayout",
            children = listOf(
                NodeSnapshot(
                    viewId = "com.instagram.android:id/explore_action_bar"
                ),
                NodeSnapshot(
                    viewId = "com.instagram.android:id/grid_card_layout_container"
                ),
                NodeSnapshot(
                    viewId = "com.instagram.android:id/swipeable_tab_view_pager"
                )
            )
        )

        val snapshot = ScreenSnapshot(
            packageName = "com.instagram.android",
            currentActivity = "com.instagram.mainactivity.InstagramMainActivity",
            rootNode = rootNode
        )

        val result = classifier.classify(snapshot)
        assertEquals(ScreenType.EXPLORE, result)
    }

    @Test
    fun `NavigationTracker initial fingerprint change within settle window updates initial fingerprint without marking swipe`() {
        val tracker = NavigationTracker()
        tracker.update(ScreenType.EXPLORE)

        val initialFp = ReelContentFingerprint(author = "old_cached_author")
        tracker.update(ScreenType.REELS_VIEWER, initialFp, originHint = ScreenType.EXPLORE, nowMs = 1000L)

        assertEquals(ScreenType.EXPLORE, tracker.reelOriginScreen)
        assertEquals("old_cached_author", tracker.initialReelFingerprint.author)
        assertFalse(tracker.hasSwipedToNextReel)

        // 300ms sonra (settle window içinde) yeni Reel'in gerçek parmak izi geldi
        val actualFp = ReelContentFingerprint(author = "actual_new_author")
        tracker.update(ScreenType.REELS_VIEWER, actualFp, originHint = ScreenType.EXPLORE, nowMs = 1300L)

        assertEquals("actual_new_author", tracker.initialReelFingerprint.author)
        assertFalse("Oturumun ilk 800ms'sindeki parmak izi değişimi sahte kaydırma üretmemeli", tracker.hasSwipedToNextReel)

        // 2000ms sonra kullanıcı gerçekten 2. Reel'e kaydırdı
        val swipedFp = ReelContentFingerprint(author = "second_reel_author")
        tracker.update(ScreenType.REELS_VIEWER, swipedFp, originHint = ScreenType.EXPLORE, nowMs = 3000L)

        assertTrue("Gerçek kaydırmada hasSwipedToNextReel true olmalı", tracker.hasSwipedToNextReel)
    }

    @Test
    fun `NavigationTracker resets reel session on return to explore and cleanly allows opening a 2nd reel`() {
        val tracker = NavigationTracker()
        tracker.update(ScreenType.EXPLORE)

        // 1. Reel açıldı
        val fp1 = ReelContentFingerprint(author = "reel_one")
        tracker.update(ScreenType.REELS_VIEWER, fp1, originHint = ScreenType.EXPLORE, nowMs = 1000L)
        assertEquals(ScreenType.EXPLORE, tracker.reelOriginScreen)
        assertFalse(tracker.hasSwipedToNextReel)

        // 1. Reel'den kaydırıldı -> engellendi
        tracker.onReelPagerScrolled(nowMs = 2500L)
        assertTrue(tracker.hasSwipedToNextReel)

        // Geri tuşuyla Keşfet ızgarasına dönüldü
        tracker.update(ScreenType.EXPLORE, nowMs = 3000L)
        assertEquals(ScreenType.EXPLORE, tracker.currentScreen)
        assertEquals(ScreenType.EXPLORE, tracker.lastNonReelsScreen)
        assertFalse("Keşfete dönünce kaydırma durumu sıfırlanmalı", tracker.hasSwipedToNextReel)

        // Keşfetten 2. Reel'e tıklandı -> Temiz bir oturum olarak açılmalı ve hemen geri atmamalı!
        val fp2 = ReelContentFingerprint(author = "reel_two")
        tracker.update(ScreenType.REELS_VIEWER, fp2, originHint = ScreenType.EXPLORE, nowMs = 3500L)
        assertEquals(ScreenType.EXPLORE, tracker.reelOriginScreen)
        assertEquals("reel_two", tracker.initialReelFingerprint.author)
        assertFalse("2. Reel yeni oturumda engellenmeden izlenebilmeli", tracker.hasSwipedToNextReel)
    }
}
