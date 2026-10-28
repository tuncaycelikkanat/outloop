package com.nomorereels.app.billing

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SupporterCodeValidatorTest {

    @Test
    fun validCodes_shouldReturnTrue() {
        assertTrue(SupporterCodeValidator.isValid("OUTLOOP-HERO"))
        assertTrue(SupporterCodeValidator.isValid("outloop-hero"))
        assertTrue(SupporterCodeValidator.isValid("  OUTLOOP-HERO  "))
        assertTrue(SupporterCodeValidator.isValid("OUTLOOPHERO"))
        assertTrue(SupporterCodeValidator.isValid("outloop hero"))

        assertTrue(SupporterCodeValidator.isValid("OUTLOOP-KREOSUS"))
        assertTrue(SupporterCodeValidator.isValid("OUTLOOP-COFFEE"))
        assertTrue(SupporterCodeValidator.isValid("OUTLOOP-SPONSOR"))
        assertTrue(SupporterCodeValidator.isValid("OUTLOOP-2026"))
        assertTrue(SupporterCodeValidator.isValid("OUTLOOP-STAR"))
    }

    @Test
    fun invalidCodes_shouldReturnFalse() {
        assertFalse(SupporterCodeValidator.isValid(""))
        assertFalse(SupporterCodeValidator.isValid("   "))
        assertFalse(SupporterCodeValidator.isValid("RANDOM-CODE"))
        assertFalse(SupporterCodeValidator.isValid("OUTLOOP-FAKE"))
        assertFalse(SupporterCodeValidator.isValid("123456"))
    }
}
