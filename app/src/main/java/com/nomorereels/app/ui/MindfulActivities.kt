package com.nomorereels.app.ui

import android.content.Context
import org.json.JSONArray
import java.io.File
import java.util.Calendar

enum class TimeBracket(
    val id: String,
    val labelTr: String,
    val labelEn: String,
    val rangeDescTr: String,
    val rangeDescEn: String
) {
    MIN_1_3("1_3", "1-3 dk", "1-3 min", "Mikro Duraklamalar", "Micro-Pauses"),
    MIN_5("5", "5 dk", "5 min", "Hızlı Tazelenme", "Quick Reset"),
    MIN_15("15", "10-15 dk", "10-15 min", "Odaklanmış Mola", "Focused Break"),
    MIN_30("30", "30 dk", "30 min", "Derin Zaman", "Deep Value Block"),
    MIN_60_PLUS("60+", "60+ dk", "60+ min", "Hayat Değiştiren Alan", "Life-Changing Space");

    companion object {
        fun forMinutes(minutes: Long): TimeBracket = when {
            minutes >= 50 -> MIN_60_PLUS
            minutes >= 25 -> MIN_30
            minutes >= 8 -> MIN_15
            minutes >= 4 -> MIN_5
            else -> MIN_1_3
        }
    }
}

data class MindfulActivity(
    val categoryTr: String,
    val categoryEn: String,
    val titleTr: String,
    val titleEn: String,
    val descTr: String,
    val descEn: String,
    val bracket: TimeBracket
) {
    fun localizedCategory(isEnglish: Boolean): String = if (isEnglish) categoryEn else categoryTr
    fun localizedTitle(isEnglish: Boolean): String = if (isEnglish) titleEn else titleTr
    fun localizedDesc(isEnglish: Boolean): String = if (isEnglish) descEn else descTr

    val isNightFriendly: Boolean
        get() {
            val lowerTitle = titleTr.lowercase()
            val lowerCat = categoryTr.lowercase()
            val outdoorOrLoudKeywords = listOf(
                "balkon", "dışarı", "yürüyüş", "koşu", "squat", "şınav",
                "temizlik", "süpür", "bahçe", "market", "güneş"
            )
            if (outdoorOrLoudKeywords.any { lowerTitle.contains(it) || descTr.lowercase().contains(it) }) {
                return false
            }
            val calmCategories = listOf(
                "nefes", "sinir sistemi", "zihin", "göz", "farkındalık",
                "gerilim", "meditasyon", "şükran", "okuma", "uyku", "topraklanma"
            )
            return calmCategories.any { lowerCat.contains(it) } || true
        }
}

object MindfulActivities {

    @Volatile
    private var cachedActivities: List<MindfulActivity>? = null

    val activities: List<MindfulActivity>
        get() = cachedActivities ?: synchronized(this) {
            cachedActivities ?: loadActivitiesFromRawJson().also { cachedActivities = it }
        }

    fun init(context: Context) {
        if (cachedActivities != null) return
        synchronized(this) {
            if (cachedActivities != null) return
            try {
                val resId = context.resources.getIdentifier("activities", "raw", context.packageName)
                if (resId != 0) {
                    val jsonText = context.resources.openRawResource(resId).bufferedReader(Charsets.UTF_8).use { it.readText() }
                    val parsed = parseActivitiesJson(jsonText)
                    if (parsed.isNotEmpty()) {
                        cachedActivities = parsed
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private fun loadActivitiesFromRawJson(): List<MindfulActivity> {
        val rawStream = MindfulActivities::class.java.classLoader?.getResourceAsStream("res/raw/activities.json")
            ?: MindfulActivities::class.java.classLoader?.getResourceAsStream("activities.json")
        val jsonText = rawStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            ?: sequenceOf(
                File("src/main/res/raw/activities.json"),
                File("app/src/main/res/raw/activities.json")
            ).firstOrNull { it.exists() }?.readText(Charsets.UTF_8)

        if (!jsonText.isNullOrBlank()) {
            val parsed = parseActivitiesJson(jsonText)
            if (parsed.isNotEmpty()) return parsed
        }
        return listOf(
            MindfulActivity(
                categoryTr = "Nefes",
                categoryEn = "Breathing",
                titleTr = "3 Derin Diyafram Nefesi",
                titleEn = "3 Deep Diaphragmatic Breaths",
                descTr = "Burnundan 4 saniyede al, 4 saniye tut, 6 saniyede ver.",
                descEn = "Inhale for 4 seconds, hold for 4, exhale for 6.",
                bracket = TimeBracket.MIN_1_3
            )
        )
    }

    private fun parseActivitiesJson(jsonText: String): List<MindfulActivity> {
        val arr = JSONArray(jsonText)
        val result = ArrayList<MindfulActivity>(arr.length())
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val bracketName = obj.optString("bracket", "MIN_1_3")
            val bracket = TimeBracket.entries.firstOrNull { it.name == bracketName } ?: TimeBracket.MIN_1_3
            result.add(
                MindfulActivity(
                    categoryTr = obj.getString("categoryTr"),
                    categoryEn = obj.getString("categoryEn"),
                    titleTr = obj.getString("titleTr"),
                    titleEn = obj.getString("titleEn"),
                    descTr = obj.getString("descTr"),
                    descEn = obj.getString("descEn"),
                    bracket = bracket
                )
            )
        }
        return result
    }

    fun getActivitiesForBracket(bracket: TimeBracket): List<MindfulActivity> {
        return activities.filter { it.bracket == bracket }
    }

    /**
     * Kazanılan süreye, günün saatine (gece ise sakinleştirici/iç mekan aktiviteleri öncelikli)
     * ve kullanıcının "Başka öner" (shuffleOffset) tercihine göre aktivite seçer.
     */
    fun getActivityForToday(
        bracket: TimeBracket,
        savedMinutes: Long,
        shuffleOffset: Int = 0,
        hourOfDay: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    ): MindfulActivity {
        val baseList = getActivitiesForBracket(bracket)
        if (baseList.isEmpty()) return activities.first()

        val isNightHours = hourOfDay >= 21 || hourOfDay < 6
        val pool = if (isNightHours) {
            baseList.filter { it.isNightFriendly }.ifEmpty { baseList }
        } else {
            baseList
        }

        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val index = Math.floorMod(dayOfYear * 17 + (savedMinutes / 5).toInt() + shuffleOffset, pool.size)
        return pool[index]
    }
}
