package com.nomorereels.app.data

import com.nomorereels.app.policy.DmReelMode
import com.nomorereels.app.policy.PolicyDecision
import com.nomorereels.app.policy.ProtectionLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuotaEstimationTest {

    @Test
    fun testEstimateSavedSecondsPerBlockIsWithinReasonableBounds() {
        val perBlock = QuotaStore.estimateSavedSecondsPerBlock()
        assertTrue("Expected saved seconds per block in 20..90, got $perBlock", perBlock in 20L..90L)
        assertEquals(0L, QuotaStore.estimateSavedSecondsForBlocks(0))
        assertEquals(perBlock * 4, QuotaStore.estimateSavedSecondsForBlocks(4))
    }

    @Test
    fun testLocalizedPolicyStringsAreNonBlank() {
        for (mode in DmReelMode.entries) {
            assertTrue(mode.localizedTitle(isEnglish = false).isNotBlank())
            assertTrue(mode.localizedTitle(isEnglish = true).isNotBlank())
            assertTrue(mode.localizedDescription(isEnglish = false).isNotBlank())
            assertTrue(mode.localizedDescription(isEnglish = true).isNotBlank())
            assertTrue(mode.localizedShortTitle(isEnglish = false).isNotBlank())
            assertTrue(mode.localizedShortTitle(isEnglish = true).isNotBlank())
        }
        for (decision in PolicyDecision.entries) {
            assertTrue(decision.localizedReason(isEnglish = false).isNotBlank())
            assertTrue(decision.localizedReason(isEnglish = true).isNotBlank())
        }
        for (level in ProtectionLevel.entries) {
            assertTrue(level.localizedTitle(isEnglish = false).isNotBlank())
            assertTrue(level.localizedTitle(isEnglish = true).isNotBlank())
            assertTrue(level.localizedSubtitle(isEnglish = false).isNotBlank())
            assertTrue(level.localizedSubtitle(isEnglish = true).isNotBlank())
        }
    }
}
