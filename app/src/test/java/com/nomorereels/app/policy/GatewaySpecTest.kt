package com.nomorereels.app.policy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GatewaySpecTest {

    @Test
    fun testComputeInitialWaitSecondsScalesProgressively() {
        assertEquals(10, GatewaySpec.computeInitialWaitSeconds(0))
        assertEquals(20, GatewaySpec.computeInitialWaitSeconds(1))
        assertEquals(40, GatewaySpec.computeInitialWaitSeconds(2))
        assertEquals(40, GatewaySpec.computeInitialWaitSeconds(5))
    }

    @Test
    fun testCanApproveSnoozeRespectsBedtimeAndDailyLimit() {
        assertTrue(GatewaySpec.canApproveSnooze(isBedtime = false, snoozeCountToday = 0))
        assertTrue(GatewaySpec.canApproveSnooze(isBedtime = false, snoozeCountToday = 2))
        assertFalse(GatewaySpec.canApproveSnooze(isBedtime = false, snoozeCountToday = 3))
        assertFalse(GatewaySpec.canApproveSnooze(isBedtime = true, snoozeCountToday = 0))
    }

    @Test
    fun testDurationOptionsAndQuickChips() {
        assertEquals(listOf(3, 5, 10), GatewaySpec.DURATION_OPTIONS)
        assertTrue(GatewaySpec.intentOptions(isEnglish = false).size >= 4)
        assertTrue(GatewaySpec.intentOptions(isEnglish = true).size >= 4)
    }

    @Test
    fun testBreathingPhaseText() {
        assertEquals("Nefes al...", GatewaySpec.breathingPhaseText(isInhale = true, isEnglish = false))
        assertEquals("Nefes ver...", GatewaySpec.breathingPhaseText(isInhale = false, isEnglish = false))
        assertEquals("Inhale...", GatewaySpec.breathingPhaseText(isInhale = true, isEnglish = true))
        assertEquals("Exhale...", GatewaySpec.breathingPhaseText(isInhale = false, isEnglish = true))
    }

    @Test
    fun testContextualBadgeTextForAllBlockDecisions() {
        assertEquals(
            "🌿 TEK REEL SINIRI",
            GatewaySpec.contextualBadgeText(PolicyDecision.BLOCK_AFTER_SWIPE, null, isEnglish = false)
        )
        assertEquals(
            "🍃 TEK REEL SÜRESİ DOLDU",
            GatewaySpec.contextualBadgeText(PolicyDecision.BLOCK_AFTER_TIMEOUT, null, isEnglish = false)
        )
        assertEquals(
            "🌙 GECE ODAK SAATİ",
            GatewaySpec.contextualBadgeText(PolicyDecision.BLOCK_BEDTIME, null, isEnglish = false)
        )
    }
}
