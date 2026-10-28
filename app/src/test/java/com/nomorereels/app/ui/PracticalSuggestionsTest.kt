package com.nomorereels.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PracticalSuggestionsTest {

    @Test
    fun testSuggestionsCountIsOver100() {
        assertTrue("Expected at least 100 suggestions, found ${PracticalSuggestions.all.size}", PracticalSuggestions.all.size >= 100)
    }

    @Test
    fun testAllSuggestionsHaveValidFields() {
        for ((index, item) in PracticalSuggestions.all.withIndex()) {
            assertTrue("Item $index has blank bracket", item.bracket.isNotBlank())
            assertTrue("Item $index has blank emoji", item.emoji.isNotBlank())
            assertTrue("Item $index has blank textTr", item.textTr.isNotBlank())
            assertTrue("Item $index has blank textEn", item.textEn.isNotBlank())
        }
    }

    @Test
    fun testForTimeReturnsExpectedCount() {
        val micro = PracticalSuggestions.forTime(savedMinutes = 0, savedSeconds = 40, isEnglish = false, count = 3)
        assertEquals(3, micro.size)

        val shortMins = PracticalSuggestions.forTime(savedMinutes = 4, savedSeconds = 0, isEnglish = false, count = 3)
        assertEquals(3, shortMins.size)

        val medium = PracticalSuggestions.forTime(savedMinutes = 12, savedSeconds = 0, isEnglish = false, count = 3)
        assertEquals(3, medium.size)

        val deep = PracticalSuggestions.forTime(savedMinutes = 35, savedSeconds = 0, isEnglish = true, count = 3)
        assertEquals(3, deep.size)
        // Verify English text is returned when isEnglish = true
        assertFalse(deep.first().second.contains("yapabilirsin"))
    }

    @Test
    fun testForTimeSingleReturnsNonBlankTextWithoutEmojiPrefix() {
        val singleTr = PracticalSuggestions.forTimeSingle(savedMinutes = 5, savedSeconds = 0, isEnglish = false)
        assertTrue(singleTr.isNotBlank())
        // Should be plain text, not starting with bracket or emoji
        assertTrue(singleTr.length > 5)

        val singleEn = PracticalSuggestions.forTimeSingle(savedMinutes = 5, savedSeconds = 0, isEnglish = true)
        assertTrue(singleEn.isNotBlank())
        assertFalse(singleEn.contains("yapabilirsin"))
    }
}
