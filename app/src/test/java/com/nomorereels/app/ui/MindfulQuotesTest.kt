package com.nomorereels.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MindfulQuotesTest {

    @Test
    fun testQuotesCountIsExactly500() {
        assertEquals(500, MindfulQuotes.quotes.size)
    }

    @Test
    fun testAllQuotesHaveValidFields() {
        for ((index, quote) in MindfulQuotes.quotes.withIndex()) {
            assertTrue("Quote $index has blank textTr", quote.textTr.isNotBlank())
            assertTrue("Quote $index has blank textEn", quote.textEn.isNotBlank())
            assertTrue("Quote $index has blank author", quote.author.isNotBlank())
        }
    }

    @Test
    fun testAllQuotesAreUnique() {
        val trSet = MindfulQuotes.quotes.map { it.textTr }.toSet()
        val enSet = MindfulQuotes.quotes.map { it.textEn }.toSet()
        assertEquals("Duplicate Turkish quotes found", 500, trSet.size)
        assertEquals("Duplicate English quotes found", 500, enSet.size)
    }

    @Test
    fun testGetQuoteForDayReturnsValidQuote() {
        val testDays = listOf(-10, 0, 1, 100, 365, 366, 499, 500, 730, 1000)
        for (day in testDays) {
            val quote = MindfulQuotes.getQuoteForDay(day)
            assertNotNull(quote)
            assertTrue(quote.textTr.isNotBlank())
            assertTrue(quote.textEn.isNotBlank())
            assertTrue(quote.author.isNotBlank())
        }
    }

    @Test
    fun testLocalization() {
        val sample = MindfulQuotes.quotes.first()
        assertEquals(sample.textEn, sample.localized(isEnglish = true))
        assertEquals(sample.textTr, sample.localized(isEnglish = false))
    }

    @Test
    fun testGetRandomQuote() {
        val (quote, index) = MindfulQuotes.getRandomQuote()
        assertTrue(index in MindfulQuotes.quotes.indices)
        assertEquals(MindfulQuotes.quotes[index], quote)

        val (nextQuote, nextIndex) = MindfulQuotes.getRandomQuote(currentIndex = index)
        assertFalse(nextIndex == index)
        assertEquals(MindfulQuotes.quotes[nextIndex], nextQuote)
    }
}
