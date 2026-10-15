package com.nomorereels.app.detection

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
              "version": 2,
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
        assertEquals(2, ruleSet.version)
        assertEquals(listOf("custom_feed_tab"), ruleSet.feed.tabIds)
        assertEquals(listOf("CustomHome"), ruleSet.feed.tabDescriptions)
        assertNotNull(ruleSet.reelsTab)
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
    fun `main feed with embedded reels video and feed_tab selected should classify as MAIN_FEED`() {
        val rootNode = NodeSnapshot(
            className = "android.widget.FrameLayout",
            children = listOf(
                NodeSnapshot(
                    viewId = "com.instagram.android:id/clips_video_container",
                    contentDescription = "Reel by sample_user",
                    isSelected = false
                ),
                NodeSnapshot(
                    className = "android.widget.LinearLayout",
                    contentDescription = "Reels",
                    isSelected = true
                ),
                NodeSnapshot(
                    viewId = "com.instagram.android:id/feed_tab",
                    contentDescription = "Home",
                    isSelected = true
                ),
                NodeSnapshot(
                    viewId = "com.instagram.android:id/clips_tab",
                    contentDescription = "Reels",
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
    fun `explore tab selected should classify as EXPLORE`() {
        val rootNode = NodeSnapshot(
            className = "android.widget.FrameLayout",
            children = listOf(
                NodeSnapshot(
                    viewId = "com.instagram.android:id/search_tab",
                    contentDescription = "Ara ve Keşfet",
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
        assertEquals(ScreenType.EXPLORE, result)
        assertFalse(result.isReels)
    }

    @Test
    fun `profile tab selected should classify as PROFILE`() {
        val rootNode = NodeSnapshot(
            className = "android.widget.FrameLayout",
            children = listOf(
                NodeSnapshot(
                    viewId = "com.instagram.android:id/profile_tab",
                    contentDescription = "Profil",
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
        assertEquals(ScreenType.PROFILE, result)
        assertFalse(result.isReels)
    }

    @Test
    fun `direct messages should classify as DIRECT_MESSAGES`() {
        val rootNode = NodeSnapshot(
            className = "android.widget.FrameLayout",
            children = listOf(
                NodeSnapshot(
                    viewId = "com.instagram.android:id/direct_message_list_recycler_view"
                )
            )
        )

        val snapshot = ScreenSnapshot(
            packageName = "com.instagram.android",
            currentActivity = "com.instagram.direct.messagelist.DirectMessageListActivity",
            rootNode = rootNode
        )

        val result = classifier.classify(snapshot)
        assertEquals(ScreenType.DIRECT_MESSAGES, result)
        assertFalse(result.isReels)
    }

    @Test
    fun `clips viewer in modal activity without feed_tab should classify as REELS_VIEWER`() {
        val rootNode = NodeSnapshot(
            className = "android.widget.FrameLayout",
            children = listOf(
                NodeSnapshot(
                    viewId = "com.instagram.android:id/clips_video_container"
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
    fun `story viewer should NOT classify as REELS`() {
        val rootNode = NodeSnapshot(
            className = "android.widget.FrameLayout",
            children = listOf(
                NodeSnapshot(
                    viewId = "com.instagram.android:id/reel_viewer_layout"
                )
            )
        )

        val snapshot = ScreenSnapshot(
            packageName = "com.instagram.android",
            currentActivity = "com.instagram.modal.ReelViewerActivity",
            rootNode = rootNode
        )

        val result = classifier.classify(snapshot)
        assertEquals(ScreenType.MAIN_FEED, result)
        assertFalse(result.isReels)
    }

    @Test
    fun `empty snapshot should classify as UNKNOWN`() {
        val snapshot = ScreenSnapshot(
            packageName = "com.instagram.android",
            rootNode = null
        )

        val result = classifier.classify(snapshot)
        assertEquals(ScreenType.UNKNOWN, result)
    }
}
