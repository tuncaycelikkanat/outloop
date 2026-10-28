package com.nomorereels.app.detection

import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo

@Suppress("DEPRECATION")
fun safeRecycle(node: AccessibilityNodeInfo?) {
    if (Build.VERSION.SDK_INT < 34) {
        try {
            node?.recycle()
        } catch (_: Exception) {}
    }
}

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
        private const val MAX_TOTAL_NODES = 450
        private const val HARD_MAX_NODES = 650

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
            // Üst seviye kapsayıcılar (depth <= 5) ve sekme çubuğu (tab_bar / feed_tab / clips_tab)
            // yoğun ekranlarda bile asla budanmaz; böylece ekranın altındaki sekme çubuğu her zaman yakalanır.
            val canTraverseNormal = depth < MAX_TREE_DEPTH && nodeCounter[0] < MAX_TOTAL_NODES
            val canTraverseShallowOrTab = depth <= 5 && nodeCounter[0] < HARD_MAX_NODES

            if (canTraverseNormal || canTraverseShallowOrTab) {
                for (i in 0 until childCount) {
                    if (nodeCounter[0] >= HARD_MAX_NODES) break
                    if (nodeCounter[0] >= MAX_TOTAL_NODES && depth > 5) break

                    val child = node.getChild(i)
                    if (child != null) {
                        try {
                            if (child.isVisibleToUser) {
                                val childId = child.viewIdResourceName ?: ""
                                val isIrrelevantSubtree = childId.contains("row_comment", ignoreCase = true) ||
                                    childId.contains("comment_thread", ignoreCase = true)
                                if (!isIrrelevantSubtree) {
                                    children.add(createNodeSnapshot(child, depth + 1, nodeCounter))
                                }
                            }
                        } finally {
                            safeRecycle(child)
                        }
                    }
                }
            }

            val viewId = node.viewIdResourceName
            val rawText = node.text?.toString()
            val safeText = when {
                rawText.isNullOrBlank() -> null
                isStructuralText(rawText) -> rawText
                isFingerprintNodeId(viewId) -> rawText.take(160)
                else -> null
            }

            return NodeSnapshot(
                viewId = viewId,
                className = node.className?.toString(),
                contentDescription = node.contentDescription?.toString(),
                // Sadece yapısal tespit veya Reel kimlik parmak izi için kullanılır; özel mesajlar asla okunmaz
                text = safeText,
                isSelected = node.isSelected,
                isClickable = node.isClickable,
                children = children
            )
        }

        private fun isFingerprintNodeId(viewId: String?): Boolean {
            if (viewId.isNullOrBlank()) return false
            return viewId.contains("clips_author_username", ignoreCase = true) ||
                viewId.contains("clips_caption", ignoreCase = true) ||
                viewId.contains("clips_viewer_action_bar_title", ignoreCase = true) ||
                viewId.contains("reel_viewer_title", ignoreCase = true)
        }

        private val STRUCTURAL_WORDS = setOf(
            "reels", "reel", "akış", "feed", "ana sayfa", "home", "accueil", "inicio", "startseite",
            "keşfet", "explore", "ara", "search", "ara ve keşfet", "suche", "rechercher", "buscar", "explorar",
            "profil", "profile", "perfil", "gönderiler", "posts", "beiträge", "publications", "publicaciones",
            "profili düzenle", "edit profile", "profil bearbeiten", "modifier le profil", "editar perfil",
            "mesajlar", "messages", "nachrichten", "mensajes", "sohbetler", "chats", "sohbet", "chat",
            "birincil", "primary", "genel", "general", "istekler", "requests",
            "mesaj gönder", "send message", "mesaj yaz", "friends", "arkadaşlar"
        )

        private fun isStructuralText(text: String?): Boolean {
            if (text.isNullOrBlank()) return false
            val lower = text.trim().lowercase()
            return lower in STRUCTURAL_WORDS
        }
    }
}
