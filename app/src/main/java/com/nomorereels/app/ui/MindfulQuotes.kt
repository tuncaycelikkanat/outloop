package com.nomorereels.app.ui

import android.content.Context
import org.json.JSONArray
import java.io.File
import java.time.LocalDate

data class MindfulQuote(
    val textTr: String,
    val textEn: String,
    val author: String
) {
    fun localized(isEnglish: Boolean): String = if (isEnglish) textEn else textTr
}

object MindfulQuotes {

    @Volatile
    private var cachedQuotes: List<MindfulQuote>? = null

    val quotes: List<MindfulQuote>
        get() = cachedQuotes ?: synchronized(this) {
            cachedQuotes ?: loadQuotesFromRawJson().also { cachedQuotes = it }
        }

    fun init(context: Context) {
        if (cachedQuotes != null) return
        synchronized(this) {
            if (cachedQuotes != null) return
            try {
                val resId = context.resources.getIdentifier("quotes", "raw", context.packageName)
                if (resId != 0) {
                    val jsonText = context.resources.openRawResource(resId).bufferedReader(Charsets.UTF_8).use { it.readText() }
                    val parsed = parseQuotesJson(jsonText)
                    if (parsed.isNotEmpty()) {
                        cachedQuotes = parsed
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private fun loadQuotesFromRawJson(): List<MindfulQuote> {
        val rawStream = MindfulQuotes::class.java.classLoader?.getResourceAsStream("res/raw/quotes.json")
            ?: MindfulQuotes::class.java.classLoader?.getResourceAsStream("quotes.json")
        val jsonText = rawStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            ?: sequenceOf(
                File("src/main/res/raw/quotes.json"),
                File("app/src/main/res/raw/quotes.json")
            ).firstOrNull { it.exists() }?.readText(Charsets.UTF_8)

        if (!jsonText.isNullOrBlank()) {
            val parsed = parseQuotesJson(jsonText)
            if (parsed.isNotEmpty()) return parsed
        }
        return listOf(
            MindfulQuote(
                textTr = "Zamanımız az değil, çoğunu ziyan ediyoruz.",
                textEn = "It is not that we have a short time to live, but that we waste a lot of it.",
                author = "Seneca"
            )
        )
    }

    private fun parseQuotesJson(jsonText: String): List<MindfulQuote> {
        val arr = JSONArray(jsonText)
        val result = ArrayList<MindfulQuote>(arr.length())
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            result.add(
                MindfulQuote(
                    textTr = obj.getString("textTr"),
                    textEn = obj.getString("textEn"),
                    author = obj.getString("author")
                )
            )
        }
        return result
    }

    fun getQuoteForDay(dayOfYear: Int = LocalDate.now().dayOfYear): MindfulQuote {
        val index = Math.floorMod(dayOfYear, quotes.size)
        return quotes[index]
    }

    fun getRandomQuote(currentIndex: Int = -1): Pair<MindfulQuote, Int> {
        val nextIndex = if (quotes.size <= 1) 0 else {
            var candidate = (quotes.indices).random()
            while (candidate == currentIndex) {
                candidate = (quotes.indices).random()
            }
            candidate
        }
        return quotes[nextIndex] to nextIndex
    }
}
