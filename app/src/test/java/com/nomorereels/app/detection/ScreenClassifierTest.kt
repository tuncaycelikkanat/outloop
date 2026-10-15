package com.nomorereels.app.detection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ScreenClassifierTest {

    private lateinit var classifier: ScreenClassifier

    @Before
    fun setUp() {
        classifier = ScreenClassifier()
    }

    @Test
    fun `reels tab selected should classify as REELS_TAB`() {
        val rootNode = NodeSnapshot(
            className = "android.widget.FrameLayout",
            children = listOf(
                NodeSnapshot(
                    viewId = "com.instagram.android:id/tab_bar",
                    children = listOf(
                        NodeSnapshot(
                            contentDescription = "Ana Akış",
                            isSelected = false
                        ),
                        NodeSnapshot(
                            contentDescription = "Reels",
                            isSelected = true
                        )
                    )
                )
            )
        )

        val snapshot = ScreenSnapshot(
            packageName = "com.instagram.android",
            currentActivity = "com.instagram.mainactivity.MainActivity",
            rootNode = rootNode
        )

        val result = classifier.classify(snapshot)
        assertEquals(ScreenType.REELS_TAB, result)
        assertTrue(result.isReels)
    }

    @Test
    fun `stories tray in main feed should NOT classify as REELS`() {
        val rootNode = NodeSnapshot(
            className = "android.widget.FrameLayout",
            children = listOf(
                NodeSnapshot(
                    viewId = "com.instagram.android:id/swipeable_tab_view_pager",
                    children = listOf(
                        NodeSnapshot(
                            contentDescription = "reels tray container",
                            isSelected = false
                        ),
                        NodeSnapshot(
                            viewId = "com.instagram.android:id/avatar_image_view",
                            contentDescription = "user's story, 0 of 10, Unseen.",
                            isSelected = false
                        )
                    )
                ),
                NodeSnapshot(
                    viewId = "com.instagram.android:id/tab_bar",
                    children = listOf(
                        NodeSnapshot(
                            contentDescription = "Ana Sayfa",
                            isSelected = true
                        ),
                        NodeSnapshot(
                            contentDescription = "Reels",
                            isSelected = false
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
        assertEquals(ScreenType.MAIN_FEED, result)
        assertFalse(result.isReels)
    }

    @Test
    fun `clips viewer in modal activity should classify as REELS_VIEWER`() {
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
