package com.nomorereels.app.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BedtimeCalculatorTest {

    @Test
    fun testDaytimeWindow() {
        val enabled = true
        val startH = 14
        val startM = 0
        val endH = 18
        val endM = 0

        assertTrue(BedtimeCalculator.isBedtimeActive(enabled, startH, startM, endH, endM, 14, 0))
        assertTrue(BedtimeCalculator.isBedtimeActive(enabled, startH, startM, endH, endM, 15, 30))
        assertFalse(BedtimeCalculator.isBedtimeActive(enabled, startH, startM, endH, endM, 13, 59))
        assertFalse(BedtimeCalculator.isBedtimeActive(enabled, startH, startM, endH, endM, 18, 0))
    }

    @Test
    fun testOvernightWindow() {
        val enabled = true
        val startH = 23
        val startM = 30
        val endH = 7
        val endM = 0

        // Gece yarısı öncesi
        assertTrue(BedtimeCalculator.isBedtimeActive(enabled, startH, startM, endH, endM, 23, 30))
        assertTrue(BedtimeCalculator.isBedtimeActive(enabled, startH, startM, endH, endM, 23, 45))
        assertFalse(BedtimeCalculator.isBedtimeActive(enabled, startH, startM, endH, endM, 23, 29))

        // Gece yarısı sonrası (sabah erken saatler)
        assertTrue(BedtimeCalculator.isBedtimeActive(enabled, startH, startM, endH, endM, 0, 0))
        assertTrue(BedtimeCalculator.isBedtimeActive(enabled, startH, startM, endH, endM, 2, 15))
        assertTrue(BedtimeCalculator.isBedtimeActive(enabled, startH, startM, endH, endM, 6, 59))
        assertFalse(BedtimeCalculator.isBedtimeActive(enabled, startH, startM, endH, endM, 7, 0))
        assertFalse(BedtimeCalculator.isBedtimeActive(enabled, startH, startM, endH, endM, 12, 0))
    }

    @Test
    fun testDisabledReturnsFalse() {
        assertFalse(BedtimeCalculator.isBedtimeActive(false, 23, 0, 7, 0, 2, 0))
    }
}
