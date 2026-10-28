package com.nomorereels.app.data

import android.content.Context
import android.net.Uri
import com.nomorereels.app.policy.DmReelMode
import org.json.JSONObject

/**
 * Sıfır internet politikasına uygun olarak ayarların yerel dosya (JSON)
 * biçiminde dışa aktarılmasını (export) ve geri yüklenmesini (import) sağlayan yardımcı sınıf.
 */
object SettingsBackupHelper {

    private const val BACKUP_VERSION = 2

    /**
     * Mevcut tüm kullanıcı tercihlerini ve kotalarını JSON dizesine dönüştürür.
     */
    fun exportSettingsToJson(): String {
        val root = JSONObject()
        root.put("version", BACKUP_VERSION)
        root.put("timestamp", System.currentTimeMillis())

        // Ana Koruma ve Bölge Ayarları
        root.put("protectionMasterEnabled", SettingsStore.protectionMasterEnabled.value)
        root.put("generalMode", SettingsStore.generalMode.value.name)
        root.put("dmMode", SettingsStore.dmReelMode.value.name)
        root.put("exploreMode", SettingsStore.exploreReelMode.value.name)
        root.put("feedMode", SettingsStore.feedReelMode.value.name)
        root.put("profileMode", SettingsStore.profileReelMode.value.name)
        root.put("reelsTabMode", SettingsStore.reelsTabMode.value.name)

        // Kota ve Süre Ayarları
        root.put("dailyCountLimit", QuotaStore.dailyCountLimit.value)
        root.put("dailyCountLimitEnabled", QuotaStore.dailyCountLimitEnabled.value)
        root.put("dailyTimeLimitMinutes", QuotaStore.dailyTimeLimitMinutes.value)
        root.put("dailyTimeLimitEnabled", QuotaStore.dailyTimeLimitEnabled.value)
        root.put("warningPercent", QuotaStore.warningPercent.value)
        root.put("singleReelMaxSeconds", QuotaStore.singleReelMaxSeconds.value)

        // Farkındalık Kapısı ve Bildirim Ayarları
        root.put("mindfulGatewayEnabled", SettingsStore.mindfulGatewayEnabled.value)
        root.put("overlayFeedbackEnabled", SettingsStore.overlayFeedbackEnabled.value)
        root.put("hapticFeedbackEnabled", SettingsStore.hapticFeedbackEnabled.value)

        // Odak ve Güvenlik Ayarları
        root.put("bedtimeEnabled", SettingsStore.bedtimeModeEnabled.value)
        root.put("bedtimeStartHour", SettingsStore.bedtimeStartHour.value)
        root.put("bedtimeStartMinute", SettingsStore.bedtimeStartMinute.value)
        root.put("bedtimeEndHour", SettingsStore.bedtimeEndHour.value)
        root.put("bedtimeEndMinute", SettingsStore.bedtimeEndMinute.value)

        // Görünüm Ayarları
        root.put("appThemeMode", SettingsStore.appThemeMode.value.name)
        root.put("appLanguage", SettingsStore.appLanguage.value.name)

        return root.toString(2)
    }

    /**
     * JSON dizesini önce tüm alanlarıyla doğrular; yalnızca tüm alanlar geçerliyse
     * ayarları atomik olarak günceller (kısmi bozulmayı önler).
     * Geçerli bir JSON değilse veya sürüm uyumsuzsa false döner.
     */
    fun importSettingsFromJson(context: Context, jsonStr: String): Boolean {
        return try {
            val root = JSONObject(jsonStr)
            val version = root.optInt("version", -1)
            if (version < 1) return false

            // 1. Önce tüm alanları doğrula ve yerel değişkenlere oku (hiçbir store'u değiştirme)
            val protectionMaster = if (root.has("protectionMasterEnabled")) root.getBoolean("protectionMasterEnabled") else null
            val generalMode = if (root.has("generalMode")) DmReelMode.fromName(root.getString("generalMode"), SettingsStore.generalMode.value) else null
            val dmMode = if (root.has("dmMode")) DmReelMode.fromName(root.getString("dmMode"), SettingsStore.dmReelMode.value) else null
            val exploreMode = if (root.has("exploreMode")) DmReelMode.fromName(root.getString("exploreMode"), SettingsStore.exploreReelMode.value) else null
            val feedMode = if (root.has("feedMode")) DmReelMode.fromName(root.getString("feedMode"), SettingsStore.feedReelMode.value) else null
            val profileMode = if (root.has("profileMode")) DmReelMode.fromName(root.getString("profileMode"), SettingsStore.profileReelMode.value) else null
            val reelsTabMode = if (root.has("reelsTabMode")) DmReelMode.fromName(root.getString("reelsTabMode"), SettingsStore.reelsTabMode.value) else null

            val dailyCountLimit = if (root.has("dailyCountLimit")) root.getInt("dailyCountLimit").coerceIn(1, 9999) else null
            val dailyCountLimitEnabled = if (root.has("dailyCountLimitEnabled")) root.getBoolean("dailyCountLimitEnabled") else null
            val dailyTimeLimitMinutes = if (root.has("dailyTimeLimitMinutes")) root.getInt("dailyTimeLimitMinutes").coerceIn(1, 1440) else null
            val dailyTimeLimitEnabled = if (root.has("dailyTimeLimitEnabled")) root.getBoolean("dailyTimeLimitEnabled") else null
            val warningPercent = if (root.has("warningPercent")) root.getInt("warningPercent").coerceIn(10, 95) else null
            val singleReelMaxSeconds = if (root.has("singleReelMaxSeconds")) root.getInt("singleReelMaxSeconds").coerceIn(15, 600) else null

            val mindfulGatewayEnabled = if (root.has("mindfulGatewayEnabled")) root.getBoolean("mindfulGatewayEnabled") else null
            val overlayFeedbackEnabled = if (root.has("overlayFeedbackEnabled")) root.getBoolean("overlayFeedbackEnabled") else null
            val hapticFeedbackEnabled = if (root.has("hapticFeedbackEnabled")) root.getBoolean("hapticFeedbackEnabled") else null

            val bedtimeEnabled = if (root.has("bedtimeEnabled")) root.getBoolean("bedtimeEnabled") else null
            val hasBedtimeWindow = root.has("bedtimeStartHour") && root.has("bedtimeEndHour")
            val bedtimeStartH = if (hasBedtimeWindow) root.getInt("bedtimeStartHour").coerceIn(0, 23) else 0
            val bedtimeStartM = if (hasBedtimeWindow) root.optInt("bedtimeStartMinute", 0).coerceIn(0, 59) else 0
            val bedtimeEndH = if (hasBedtimeWindow) root.getInt("bedtimeEndHour").coerceIn(0, 23) else 0
            val bedtimeEndM = if (hasBedtimeWindow) root.optInt("bedtimeEndMinute", 0).coerceIn(0, 59) else 0

            val parsedTheme = if (root.has("appThemeMode")) {
                val rawTheme = root.getString("appThemeMode")
                AppThemeMode.entries.firstOrNull {
                    it.name.equals(rawTheme, ignoreCase = true) || it.code.equals(rawTheme, ignoreCase = true)
                } ?: SettingsStore.appThemeMode.value
            } else null

            val parsedLang = if (root.has("appLanguage")) {
                val rawLang = root.getString("appLanguage")
                AppLanguage.entries.firstOrNull {
                    it.name.equals(rawLang, ignoreCase = true) || it.code.equals(rawLang, ignoreCase = true)
                } ?: SettingsStore.appLanguage.value
            } else null

            // 2. Doğrulama başarılı; şimdi tüm değerleri uygula
            protectionMaster?.let { SettingsStore.setProtectionMasterEnabled(context, it) }
            generalMode?.let { SettingsStore.setGeneralMode(context, it) }
            dmMode?.let { SettingsStore.setDmReelMode(context, it) }
            exploreMode?.let { SettingsStore.setExploreReelMode(context, it) }
            feedMode?.let { SettingsStore.setFeedReelMode(context, it) }
            profileMode?.let { SettingsStore.setProfileReelMode(context, it) }
            reelsTabMode?.let { SettingsStore.setReelsTabMode(context, it) }

            dailyCountLimit?.let { QuotaStore.setDailyCountLimit(context, it) }
            dailyCountLimitEnabled?.let { QuotaStore.setDailyCountLimitEnabled(context, it) }
            dailyTimeLimitMinutes?.let { QuotaStore.setDailyTimeLimitMinutes(context, it) }
            dailyTimeLimitEnabled?.let { QuotaStore.setDailyTimeLimitEnabled(context, it) }
            warningPercent?.let { QuotaStore.setWarningPercent(context, it) }
            singleReelMaxSeconds?.let { QuotaStore.setSingleReelMaxSeconds(context, it) }

            mindfulGatewayEnabled?.let { SettingsStore.setMindfulGatewayEnabled(context, it) }
            overlayFeedbackEnabled?.let { SettingsStore.setOverlayFeedbackEnabled(context, it) }
            hapticFeedbackEnabled?.let { SettingsStore.setHapticFeedbackEnabled(context, it) }

            bedtimeEnabled?.let { SettingsStore.setBedtimeModeEnabled(context, it) }
            if (hasBedtimeWindow) {
                SettingsStore.setBedtimeWindow(context, bedtimeStartH, bedtimeStartM, bedtimeEndH, bedtimeEndM)
            }

            parsedTheme?.let { SettingsStore.setAppThemeMode(context, it) }
            parsedLang?.let { SettingsStore.setAppLanguage(context, it) }

            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * SAF (Storage Access Framework) URI'sine JSON içeriğini yazar.
     */
    fun exportToUri(context: Context, uri: Uri): Boolean {
        return try {
            val json = exportSettingsToJson()
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(json.toByteArray(Charsets.UTF_8))
                out.flush()
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * SAF URI'sinden JSON içeriğini okur ve ayarları geri yükler.
     */
    fun importFromUri(context: Context, uri: Uri): Boolean {
        return try {
            val content = context.contentResolver.openInputStream(uri)?.use { input ->
                input.bufferedReader(Charsets.UTF_8).readText()
            } ?: return false
            importSettingsFromJson(context, content)
        } catch (_: Exception) {
            false
        }
    }
}
