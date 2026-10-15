package com.nomorereels.app.detection

import org.json.JSONObject

data class FeedRule(
    val tabIds: List<String> = listOf("feed_tab", "tab_home"),
    val tabDescriptions: List<String> = listOf("Home", "Ana Sayfa", "Akış", "Feed"),
    val containerIds: List<String> = listOf("swipeable_tab_view_pager", "action_bar_home")
)

data class ReelsTabRule(
    val tabIds: List<String> = listOf("clips_tab", "tab_clips", "reels_tab"),
    val tabDescriptions: List<String> = listOf("Reels"),
    val excludedDescriptions: List<String> = listOf("story", "tray")
)

data class ReelsViewerRule(
    val viewerIds: List<String> = listOf(
        "clips_viewer_view_pager",
        "clips_swipe_refresh_container"
    ),
    val modalActivities: List<String> = listOf(
        "TransparentModalActivity"
    )
)

data class StoryRule(
    val activities: List<String> = listOf("ReelViewerActivity"),
    val layoutIds: List<String> = listOf("reel_viewer_layout", "story_viewer")
)

data class DirectMessagesRule(
    val activityKeywords: List<String> = listOf("DirectMainActivity", "DirectThreadActivity", "DirectInboxActivity", "DirectThread", "Direct"),
    val viewIds: List<String> = listOf("direct_", "thread_", "message_composer", "row_thread", "direct_inbox", "thread_list", "notes_tray", "composer"),
    val textKeywords: List<String> = listOf("mesajlar", "messages", "sohbetler", "chats", "birincil", "primary", "genel", "general", "istekler", "requests")
)

data class ExploreRule(
    val tabIds: List<String> = listOf("search_tab", "tab_search", "explore_tab"),
    val tabDescriptions: List<String> = listOf("Search", "Ara", "Ara ve Keşfet", "Explore", "Keşfet"),
    val contentIds: List<String> = listOf("action_bar_search", "search_edit_text", "search_box", "explore_grid")
)

data class ProfileRule(
    val tabIds: List<String> = listOf("profile_tab", "tab_profile", "tab_avatar"),
    val tabDescriptions: List<String> = listOf("Profile", "Profil", "clips"),
    val contentIds: List<String> = listOf("profile_header", "edit_profile", "profile_tabs", "user_profile")
)

data class RuleSet(
    val version: Int = 4,
    val minAppVersion: String = "0.1.0",
    val feed: FeedRule = FeedRule(),
    val reelsTab: ReelsTabRule = ReelsTabRule(),
    val reelsViewer: ReelsViewerRule = ReelsViewerRule(),
    val story: StoryRule = StoryRule(),
    val directMessages: DirectMessagesRule = DirectMessagesRule(),
    val explore: ExploreRule = ExploreRule(),
    val profile: ProfileRule = ProfileRule()
) {
    companion object {
        val DEFAULT = RuleSet()

        fun fromJson(jsonStr: String): RuleSet {
            return try {
                val json = JSONObject(jsonStr)
                val version = json.optInt("version", DEFAULT.version)
                val minAppVersion = json.optString("minAppVersion", "0.1.0")
                val screens = json.optJSONObject("screens") ?: JSONObject()

                val feedObj = screens.optJSONObject("feed")
                val feed = FeedRule(
                    tabIds = feedObj?.optStringList("tabIds") ?: DEFAULT.feed.tabIds,
                    tabDescriptions = feedObj?.optStringList("tabDescriptions") ?: DEFAULT.feed.tabDescriptions,
                    containerIds = feedObj?.optStringList("containerIds") ?: DEFAULT.feed.containerIds
                )

                val reelsTabObj = screens.optJSONObject("reelsTab")
                val reelsTab = ReelsTabRule(
                    tabIds = reelsTabObj?.optStringList("tabIds") ?: DEFAULT.reelsTab.tabIds,
                    tabDescriptions = reelsTabObj?.optStringList("tabDescriptions") ?: DEFAULT.reelsTab.tabDescriptions,
                    excludedDescriptions = reelsTabObj?.optStringList("excludedDescriptions") ?: DEFAULT.reelsTab.excludedDescriptions
                )

                val reelsViewerObj = screens.optJSONObject("reelsViewer")
                val reelsViewer = ReelsViewerRule(
                    viewerIds = reelsViewerObj?.optStringList("viewerIds") ?: DEFAULT.reelsViewer.viewerIds,
                    modalActivities = reelsViewerObj?.optStringList("modalActivities") ?: DEFAULT.reelsViewer.modalActivities
                )

                val storyObj = screens.optJSONObject("story")
                val story = StoryRule(
                    activities = storyObj?.optStringList("activities") ?: DEFAULT.story.activities,
                    layoutIds = storyObj?.optStringList("layoutIds") ?: DEFAULT.story.layoutIds
                )

                val dmObj = screens.optJSONObject("directMessages")
                val dm = DirectMessagesRule(
                    activityKeywords = dmObj?.optStringList("activityKeywords") ?: DEFAULT.directMessages.activityKeywords,
                    viewIds = dmObj?.optStringList("viewIds") ?: DEFAULT.directMessages.viewIds,
                    textKeywords = dmObj?.optStringList("textKeywords") ?: DEFAULT.directMessages.textKeywords
                )

                val exploreObj = screens.optJSONObject("explore")
                val explore = ExploreRule(
                    tabIds = exploreObj?.optStringList("tabIds") ?: DEFAULT.explore.tabIds,
                    tabDescriptions = exploreObj?.optStringList("tabDescriptions") ?: DEFAULT.explore.tabDescriptions,
                    contentIds = exploreObj?.optStringList("contentIds") ?: DEFAULT.explore.contentIds
                )

                val profileObj = screens.optJSONObject("profile")
                val profile = ProfileRule(
                    tabIds = profileObj?.optStringList("tabIds") ?: DEFAULT.profile.tabIds,
                    tabDescriptions = profileObj?.optStringList("tabDescriptions") ?: DEFAULT.profile.tabDescriptions,
                    contentIds = profileObj?.optStringList("contentIds") ?: DEFAULT.profile.contentIds
                )

                RuleSet(
                    version = version,
                    minAppVersion = minAppVersion,
                    feed = feed,
                    reelsTab = reelsTab,
                    reelsViewer = reelsViewer,
                    story = story,
                    directMessages = dm,
                    explore = explore,
                    profile = profile
                )
            } catch (e: Exception) {
                DEFAULT
            }
        }

        private fun JSONObject.optStringList(key: String): List<String> {
            val array = optJSONArray(key) ?: return emptyList()
            val list = mutableListOf<String>()
            for (i in 0 until array.length()) {
                list.add(array.optString(i))
            }
            return list
        }
    }
}
