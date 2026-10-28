package com.nomorereels.app.ui

import android.content.Context
import org.json.JSONArray
import java.io.File
import java.util.Calendar

data class PracticalSuggestion(
    val bracket: String,
    val emoji: String,
    val textTr: String,
    val textEn: String
) {
    fun localizedText(isEnglish: Boolean): String = if (isEnglish) textEn else textTr
}

object PracticalSuggestions {

    @Volatile
    private var cachedList: List<PracticalSuggestion>? = null

    val all: List<PracticalSuggestion>
        get() = cachedList ?: synchronized(this) {
            cachedList ?: loadFromRawJson().also { cachedList = it }
        }

    fun init(context: Context) {
        if (cachedList != null) return
        synchronized(this) {
            if (cachedList != null) return
            try {
                val resId = context.resources.getIdentifier("practical_suggestions", "raw", context.packageName)
                if (resId != 0) {
                    val jsonText = context.resources.openRawResource(resId).bufferedReader(Charsets.UTF_8).use { it.readText() }
                    val parsed = parseJson(jsonText)
                    if (parsed.isNotEmpty()) {
                        cachedList = parsed
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private fun loadFromRawJson(): List<PracticalSuggestion> {
        val rawStream = PracticalSuggestions::class.java.classLoader?.getResourceAsStream("res/raw/practical_suggestions.json")
            ?: PracticalSuggestions::class.java.classLoader?.getResourceAsStream("practical_suggestions.json")
        val jsonText = rawStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            ?: sequenceOf(
                File("src/main/res/raw/practical_suggestions.json"),
                File("app/src/main/res/raw/practical_suggestions.json")
            ).firstOrNull { it.exists() }?.readText(Charsets.UTF_8)

        if (!jsonText.isNullOrBlank()) {
            val parsed = parseJson(jsonText)
            if (parsed.isNotEmpty()) return parsed
        }
        return listOf(
            PracticalSuggestion("1_3", "💧", "Bir bardak taze su içip kendini toparlayabilirsin.", "Drink a fresh glass of water to reset."),
            PracticalSuggestion("1_3", "🪟", "Pencereyi açıp birkaç dakika temiz hava alabilirsin.", "Open a window and get a few minutes of fresh air."),
            PracticalSuggestion("1_3", "🌬️", "Omuzlarını gevşetip 5 yavaş, derin nefes alabilirsin.", "Relax your shoulders and take 5 slow, deep breaths.")
        )
    }

    private fun parseJson(jsonText: String): List<PracticalSuggestion> {
        val arr = JSONArray(jsonText)
        val result = ArrayList<PracticalSuggestion>(arr.length())
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            result.add(
                PracticalSuggestion(
                    bracket = obj.getString("bracket"),
                    emoji = obj.getString("emoji"),
                    textTr = obj.getString("textTr"),
                    textEn = obj.getString("textEn")
                )
            )
        }
        return result
    }

    fun forTime(
        savedMinutes: Long,
        savedSeconds: Long,
        isEnglish: Boolean,
        count: Int = 3,
        dayOfYear: Int = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
    ): List<Pair<String, String>> {
        val targetBracket = when {
            savedMinutes >= 30 -> "30_plus"
            savedMinutes >= 15 -> "15_20"
            savedMinutes >= 10 -> "10_15"
            savedMinutes >= 5 -> "5_10"
            savedMinutes >= 3 -> "3_5"
            else -> "1_3"
        }

        val pool = all.filter { it.bracket == targetBracket }.ifEmpty { all }
        val startIndex = Math.floorMod(dayOfYear * 7 + (savedMinutes / 2).toInt(), pool.size)

        val result = ArrayList<Pair<String, String>>(count)
        for (i in 0 until minOf(count, pool.size)) {
            val item = pool[(startIndex + i) % pool.size]
            result.add(item.emoji to item.localizedText(isEnglish))
        }
        return result
    }

    fun forTimeSingle(
        savedMinutes: Long,
        savedSeconds: Long,
        isEnglish: Boolean,
        dayOfYear: Int = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
    ): String {
        val targetBracket = when {
            savedMinutes >= 30 -> "30_plus"
            savedMinutes >= 15 -> "15_20"
            savedMinutes >= 10 -> "10_15"
            savedMinutes >= 5 -> "5_10"
            savedMinutes >= 3 -> "3_5"
            else -> "1_3"
        }

        val pool = all.filter { it.bracket == targetBracket }.ifEmpty { all }
        val index = Math.floorMod(dayOfYear * 7 + (savedMinutes / 2).toInt(), pool.size)
        return pool[index].localizedText(isEnglish)
    }
}
