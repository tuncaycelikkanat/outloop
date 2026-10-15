package com.nomorereels.app.detection

import android.view.accessibility.AccessibilityNodeInfo

data class NodeSnapshot(
    val viewId: String? = null,
    val className: String? = null,
    val contentDescription: String? = null,
    val text: String? = null,
    val isSelected: Boolean = false,
    val isClickable: Boolean = false,
    val children: List<NodeSnapshot> = emptyList()
) {
    fun find(predicate: (NodeSnapshot) -> Boolean): List<NodeSnapshot> {
        val matches = mutableListOf<NodeSnapshot>()
        fun search(node: NodeSnapshot) {
            if (predicate(node)) matches.add(node)
            node.children.forEach { search(it) }
        }
        search(this)
        return matches
    }
}

data class ScreenSnapshot(
    val packageName: String,
    val currentActivity: String? = null,
    val rootNode: NodeSnapshot? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun findNodes(predicate: (NodeSnapshot) -> Boolean): List<NodeSnapshot> {
        return rootNode?.find(predicate) ?: emptyList()
    }

    fun containsId(idPart: String): Boolean {
        return findNodes { it.viewId?.contains(idPart, ignoreCase = true) == true }.isNotEmpty()
    }

    fun containsContentDescription(descPart: String): Boolean {
        return findNodes { it.contentDescription?.contains(descPart, ignoreCase = true) == true }.isNotEmpty()
    }

    companion object {
        fun fromNodeInfo(
            packageName: String,
            activityName: String?,
            rootNode: AccessibilityNodeInfo?
        ): ScreenSnapshot {
            return ScreenSnapshot(
                packageName = packageName,
                currentActivity = activityName,
                rootNode = rootNode?.let { createNodeSnapshot(it) }
            )
        }

        private fun createNodeSnapshot(node: AccessibilityNodeInfo): NodeSnapshot {
            val childCount = node.childCount
            val children = ArrayList<NodeSnapshot>(childCount)
            for (i in 0 until childCount) {
                val child = node.getChild(i)
                if (child != null) {
                    children.add(createNodeSnapshot(child))
                    // Child node must be recycled if using older APIs, but in modern Android
                    // recycling happens automatically. We don't hold references to child node.
                }
            }

            return NodeSnapshot(
                viewId = node.viewIdResourceName,
                className = node.className?.toString(),
                contentDescription = node.contentDescription?.toString(),
                // Sadece yapısal tespit için kullanılacak, kişisel metinler okunmaz/saklanmaz
                text = if (isStructuralText(node.text?.toString())) node.text?.toString() else null,
                isSelected = node.isSelected,
                isClickable = node.isClickable,
                children = children
            )
        }

        private val STRUCTURAL_WORDS = setOf(
            "reels", "reel", "akış", "feed", "ana sayfa", "home",
            "keşfet", "explore", "ara", "search", "ara ve keşfet",
            "profil", "profile", "gönderiler", "posts", "profili düzenle", "edit profile",
            "mesajlar", "messages", "sohbetler", "chats", "sohbet", "chat",
            "birincil", "primary", "genel", "general", "istekler", "requests",
            "mesaj gönder", "send message", "mesaj yaz"
        )

        private fun isStructuralText(text: String?): Boolean {
            if (text.isNullOrBlank()) return false
            val lower = text.trim().lowercase()
            return STRUCTURAL_WORDS.any { lower == it || lower.startsWith(it) }
        }
    }
}
