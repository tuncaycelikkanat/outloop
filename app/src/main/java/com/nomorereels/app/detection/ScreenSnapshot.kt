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
        private const val MAX_TREE_DEPTH = 22
        private const val MAX_TOTAL_NODES = 250

        fun fromNodeInfo(
            packageName: String,
            activityName: String?,
            rootNode: AccessibilityNodeInfo?
        ): ScreenSnapshot {
            val nodeCounter = intArrayOf(0)
            return ScreenSnapshot(
                packageName = packageName,
                currentActivity = activityName,
                rootNode = rootNode?.let { createNodeSnapshot(it, depth = 0, nodeCounter = nodeCounter) }
            )
        }

        private fun createNodeSnapshot(
            node: AccessibilityNodeInfo,
            depth: Int,
            nodeCounter: IntArray
        ): NodeSnapshot {
            nodeCounter[0]++

            val childCount = node.childCount
            val children = ArrayList<NodeSnapshot>(childCount)

            // Derinlik sınırı ve toplam düğüm kotası kontrolü:
            // Instagram ekranındaki tüm yapısal bileşenler (sekmeler, Reels oynatıcı, ızgara vb.)
            // ilk 22 katman ve 250 düğüm içerisindedir. Yorumlar gibi devasa alt listelerin taranması
            // engellenerek Binder IPC yükü ve bellek tahsisi en aza indirilir.
            val shouldTraverseChildren = depth < MAX_TREE_DEPTH && nodeCounter[0] < MAX_TOTAL_NODES

            if (shouldTraverseChildren) {
                for (i in 0 until childCount) {
                    if (nodeCounter[0] >= MAX_TOTAL_NODES) break
                    val child = node.getChild(i)
                    if (child != null) {
                        if (child.isVisibleToUser) {
                            val childId = child.viewIdResourceName ?: ""
                            val isIrrelevantSubtree = childId.contains("row_comment", ignoreCase = true) ||
                                childId.contains("comment_thread", ignoreCase = true)
                            if (!isIrrelevantSubtree) {
                                children.add(createNodeSnapshot(child, depth + 1, nodeCounter))
                            }
                        }
                    }
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
