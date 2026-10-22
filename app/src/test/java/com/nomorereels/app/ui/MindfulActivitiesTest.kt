package com.nomorereels.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MindfulActivitiesTest {

    @Test
    fun testTotalActivitiesCountIs250() {
        assertEquals("Expected exactly 250 activities", 250, MindfulActivities.activities.size)
    }

    @Test
    fun testEveryBracketHasExactly50Activities() {
        for (bracket in TimeBracket.values()) {
            val list = MindfulActivities.getActivitiesForBracket(bracket)
            assertEquals("Bracket ${bracket.name} should have 50 activities", 50, list.size)
        }
    }

    @Test
    fun testAllActivitiesHaveValidFields() {
        for ((i, act) in MindfulActivities.activities.withIndex()) {
            assertTrue("Activity $i titleTr is blank", act.titleTr.isNotBlank())
            assertTrue("Activity $i titleEn is blank", act.titleEn.isNotBlank())
            assertTrue("Activity $i categoryTr is blank", act.categoryTr.isNotBlank())
            assertTrue("Activity $i categoryEn is blank", act.categoryEn.isNotBlank())
            assertTrue("Activity $i descTr is blank", act.descTr.isNotBlank())
            assertTrue("Activity $i descEn is blank", act.descEn.isNotBlank())
        }
    }

    @Test
    fun testTimeBracketMapping() {
        assertEquals(TimeBracket.MIN_1_3, TimeBracket.forMinutes(0))
        assertEquals(TimeBracket.MIN_1_3, TimeBracket.forMinutes(2))
        assertEquals(TimeBracket.MIN_5, TimeBracket.forMinutes(4))
        assertEquals(TimeBracket.MIN_5, TimeBracket.forMinutes(5))
        assertEquals(TimeBracket.MIN_15, TimeBracket.forMinutes(10))
        assertEquals(TimeBracket.MIN_15, TimeBracket.forMinutes(15))
        assertEquals(TimeBracket.MIN_30, TimeBracket.forMinutes(30))
        assertEquals(TimeBracket.MIN_60_PLUS, TimeBracket.forMinutes(60))
        assertEquals(TimeBracket.MIN_60_PLUS, TimeBracket.forMinutes(120))
    }

    @Test
    fun testGetActivityForToday() {
        for (bracket in TimeBracket.values()) {
            val act = MindfulActivities.getActivityForToday(bracket, savedMinutes = 10)
            assertNotNull(act)
            assertEquals(bracket, act.bracket)
        }
    }
}
