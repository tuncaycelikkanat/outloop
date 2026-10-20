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

    private const val BACKUP_VERSION = 1

    /**
     * Mevcut tüm kullanıcı tercihlerini ve kotalarını JSON dizesine dönüştürür.
     */
    fun exportSettingsToJson(): String {
        val root = JSONObject()
        root.put("version", BACKUP_VERSION)
        root.put("timestamp", System.currentTimeMillis())

        // Bölge ve Genel Mod Ayarları
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

        // Odak ve Güvenlik Ayarları
        root.put("bedtimeEnabled", SettingsStore.bedtimeModeEnabled.value)
        root.put("bedtimeStartHour", SettingsStore.bedtimeStartHour.value)
        root.put("bedtimeStartMinute", SettingsStore.bedtimeStartMinute.value)
        root.put("bedtimeEndHour", SettingsStore.bedtimeEndHour.value)
        root.put("bedtimeEndMinute", SettingsStore.bedtimeEndMinute.value)
        root.put("strictModeEnabled", SettingsStore.strictModeEnabled.value)
        root.put("punchHoleAlertEnabled", SettingsStore.punchHoleAlertEnabled.value)

        // Görünüm Ayarları
        root.put("appThemeMode", SettingsStore.appThemeMode.value.name)
        root.put("appLanguage", SettingsStore.appLanguage.value.name)

        return root.toString(2)
    }

    /**
     * JSON dizesini ayrıştırıp ayarları günceller.
     * Geçerli bir JSON değilse veya sürüm uyumsuzsa false döner.
     */
    fun importSettingsFromJson(context: Context, jsonStr: String): Boolean {
        return try {
            val root = JSONObject(jsonStr)
            val version = root.optInt("version", -1)
            if (version < 1) return false

            // Modları oku ve uygula
            if (root.has("generalMode")) {
                SettingsStore.setGeneralMode(context, DmReelMode.valueOf(root.getString("generalMode")))
            }
            if (root.has("dmMode")) {
                SettingsStore.setDmReelMode(context, DmReelMode.valueOf(root.getString("dmMode")))
            }
            if (root.has("exploreMode")) {
                SettingsStore.setExploreReelMode(context, DmReelMode.valueOf(root.getString("exploreMode")))
            }
            if (root.has("feedMode")) {
                SettingsStore.setFeedReelMode(context, DmReelMode.valueOf(root.getString("feedMode")))
            }
            if (root.has("profileMode")) {
                SettingsStore.setProfileReelMode(context, DmReelMode.valueOf(root.getString("profileMode")))
            }
            if (root.has("reelsTabMode")) {
                SettingsStore.setReelsTabMode(context, DmReelMode.valueOf(root.getString("reelsTabMode")))
            }

            // Kota ayarları
            if (root.has("dailyCountLimit")) {
                QuotaStore.setDailyCountLimit(context, root.getInt("dailyCountLimit"))
            }
            if (root.has("dailyCountLimitEnabled")) {
                QuotaStore.setDailyCountLimitEnabled(context, root.getBoolean("dailyCountLimitEnabled"))
            }
            if (root.has("dailyTimeLimitMinutes")) {
                QuotaStore.setDailyTimeLimitMinutes(context, root.getInt("dailyTimeLimitMinutes"))
            }
            if (root.has("dailyTimeLimitEnabled")) {
                QuotaStore.setDailyTimeLimitEnabled(context, root.getBoolean("dailyTimeLimitEnabled"))
            }
            if (root.has("warningPercent")) {
                QuotaStore.setWarningPercent(context, root.getInt("warningPercent"))
            }
            if (root.has("singleReelMaxSeconds")) {
                QuotaStore.setSingleReelMaxSeconds(context, root.getInt("singleReelMaxSeconds"))
            }

            // Gece Odak & İrade
            if (root.has("bedtimeEnabled")) {
                SettingsStore.setBedtimeModeEnabled(context, root.getBoolean("bedtimeEnabled"))
            }
            if (root.has("bedtimeStartHour") && root.has("bedtimeEndHour")) {
                SettingsStore.setBedtimeWindow(
                    context,
                    root.getInt("bedtimeStartHour"),
                    root.optInt("bedtimeStartMinute", 0),
                    root.getInt("bedtimeEndHour"),
                    root.optInt("bedtimeEndMinute", 0)
                )
            }
            if (root.has("strictModeEnabled")) {
                SettingsStore.setStrictModeEnabled(context, root.getBoolean("strictModeEnabled"))
            }
            if (root.has("punchHoleAlertEnabled")) {
                SettingsStore.setPunchHoleAlertEnabled(context, root.getBoolean("punchHoleAlertEnabled"))
            }

            // Tema & Dil
            if (root.has("appThemeMode")) {
                SettingsStore.setAppThemeMode(context, AppThemeMode.valueOf(root.getString("appThemeMode")))
            }
            if (root.has("appLanguage")) {
                SettingsStore.setAppLanguage(context, AppLanguage.valueOf(root.getString("appLanguage")))
            }

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
