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
}
