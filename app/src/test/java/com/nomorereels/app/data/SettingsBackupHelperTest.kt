package com.nomorereels.app.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsBackupHelperTest {

    @Test
    fun testExportSettingsToJsonProducesValidJson() {
        val jsonStr = SettingsBackupHelper.exportSettingsToJson()
        assertNotNull(jsonStr)
        assertTrue(jsonStr.isNotBlank())

        val json = JSONObject(jsonStr)
        assertEquals(2, json.getInt("version"))
        assertTrue(json.has("timestamp"))
        assertTrue(json.has("generalMode"))
        assertTrue(json.has("dailyCountLimit"))
        assertTrue(json.has("dailyTimeLimitMinutes"))
        assertTrue(json.has("bedtimeEnabled"))
        assertTrue(json.has("protectionMasterEnabled"))
        assertTrue(json.has("mindfulGatewayEnabled"))
        assertTrue(json.has("overlayFeedbackEnabled"))
        assertTrue(json.has("hapticFeedbackEnabled"))
        assertTrue(json.has("appThemeMode"))
        assertTrue(json.has("appLanguage"))
    }
}
