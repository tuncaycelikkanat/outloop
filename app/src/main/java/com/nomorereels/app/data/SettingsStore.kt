package com.nomorereels.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nomorereels.app.policy.DmReelMode
import com.nomorereels.app.policy.ProtectionLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "nomorereels_settings")

enum class AppLanguage(val code: String, val titleTr: String, val titleEn: String) {
    TR("tr", "Türkçe 🇹🇷", "Turkish 🇹🇷"),
    EN("en", "İngilizce 🇬🇧", "English 🇬🇧");

    companion object {
        fun fromCode(code: String?): AppLanguage = entries.firstOrNull { it.code == code } ?: TR
    }
}

enum class AppThemeMode(val code: String, val titleTr: String, val titleEn: String) {
    SYSTEM("system", "Sistem Varsayılanı", "System Default"),
    LIGHT("light", "Açık Tema", "Light Theme"),
    DARK("dark", "Karanlık (AMOLED)", "Dark (AMOLED)");

    companion object {
        fun fromCode(code: String?): AppThemeMode = entries.firstOrNull { it.code == code } ?: SYSTEM
    }
}

/**
 * Kullanıcı tercihlerini (DM Reel Modu, Engelleme Ekranı, Hazır Seviyeler, Gece Odak Saati vb.)
 * hem bellekte reaktif StateFlow olarak tutan hem de DataStore'a kalıcı olarak kaydeden depo.
 */
object SettingsStore {

    private val KEY_GENERAL_MODE = stringPreferencesKey("general_mode")
    private val KEY_DM_REEL_MODE = stringPreferencesKey("dm_reel_mode")
    private val KEY_EXPLORE_REEL_MODE = stringPreferencesKey("explore_reel_mode")
    private val KEY_FEED_REEL_MODE = stringPreferencesKey("feed_reel_mode")
    private val KEY_PROFILE_REEL_MODE = stringPreferencesKey("profile_reel_mode")
    private val KEY_REELS_TAB_MODE = stringPreferencesKey("reels_tab_mode")
    private val KEY_OVERLAY_FEEDBACK_ENABLED = booleanPreferencesKey("overlay_feedback_enabled")
    private val KEY_CUSTOM_REEL_OVERLAY_ENABLED = booleanPreferencesKey("custom_reel_overlay_enabled")
    private val KEY_HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
    private val KEY_BEDTIME_MODE_ENABLED = booleanPreferencesKey("bedtime_mode_enabled")
    private val KEY_BEDTIME_START_HOUR = intPreferencesKey("bedtime_start_hour")
    private val KEY_BEDTIME_START_MINUTE = intPreferencesKey("bedtime_start_minute")
    private val KEY_BEDTIME_END_HOUR = intPreferencesKey("bedtime_end_hour")
    private val KEY_BEDTIME_END_MINUTE = intPreferencesKey("bedtime_end_minute")
    private val KEY_STRICT_MODE_ENABLED = booleanPreferencesKey("strict_mode_enabled")
    private val KEY_APP_LANGUAGE = stringPreferencesKey("app_language")
    private val KEY_APP_THEME_MODE = stringPreferencesKey("app_theme_mode")
    private val KEY_PUNCH_HOLE_ALERT_ENABLED = booleanPreferencesKey("punch_hole_alert_enabled")

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var isInitialized = false

    private val _generalMode = MutableStateFlow(DmReelMode.SINGLE_REEL)
    val generalMode: StateFlow<DmReelMode> = _generalMode.asStateFlow()

    private val _dmReelMode = MutableStateFlow(DmReelMode.SINGLE_REEL)
    val dmReelMode: StateFlow<DmReelMode> = _dmReelMode.asStateFlow()

    private val _exploreReelMode = MutableStateFlow(DmReelMode.FOLLOW_GLOBAL)
    val exploreReelMode: StateFlow<DmReelMode> = _exploreReelMode.asStateFlow()

    private val _feedReelMode = MutableStateFlow(DmReelMode.BLOCK_ALL)
    val feedReelMode: StateFlow<DmReelMode> = _feedReelMode.asStateFlow()

    private val _profileReelMode = MutableStateFlow(DmReelMode.FOLLOW_GLOBAL)
    val profileReelMode: StateFlow<DmReelMode> = _profileReelMode.asStateFlow()

    private val _reelsTabMode = MutableStateFlow(DmReelMode.BLOCK_ALL)
    val reelsTabMode: StateFlow<DmReelMode> = _reelsTabMode.asStateFlow()

    private val _overlayFeedbackEnabled = MutableStateFlow(true)
    val overlayFeedbackEnabled: StateFlow<Boolean> = _overlayFeedbackEnabled.asStateFlow()

    private val _customReelOverlayEnabled = MutableStateFlow(false)
    val customReelOverlayEnabled: StateFlow<Boolean> = _customReelOverlayEnabled.asStateFlow()

    private val _hasCompletedOnboarding = MutableStateFlow(false)
    val hasCompletedOnboarding: StateFlow<Boolean> = _hasCompletedOnboarding.asStateFlow()

    private val _bedtimeModeEnabled = MutableStateFlow(false)
    val bedtimeModeEnabled: StateFlow<Boolean> = _bedtimeModeEnabled.asStateFlow()

    private val _bedtimeStartHour = MutableStateFlow(23)
    val bedtimeStartHour: StateFlow<Int> = _bedtimeStartHour.asStateFlow()

    private val _bedtimeStartMinute = MutableStateFlow(30)
    val bedtimeStartMinute: StateFlow<Int> = _bedtimeStartMinute.asStateFlow()

    private val _bedtimeEndHour = MutableStateFlow(7)
    val bedtimeEndHour: StateFlow<Int> = _bedtimeEndHour.asStateFlow()

    private val _bedtimeEndMinute = MutableStateFlow(0)
    val bedtimeEndMinute: StateFlow<Int> = _bedtimeEndMinute.asStateFlow()

    private val _strictModeEnabled = MutableStateFlow(false)
    val strictModeEnabled: StateFlow<Boolean> = _strictModeEnabled.asStateFlow()

    private val _appLanguage = MutableStateFlow(AppLanguage.TR)
    val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()

    private val _appThemeMode = MutableStateFlow(AppThemeMode.SYSTEM)
    val appThemeMode: StateFlow<AppThemeMode> = _appThemeMode.asStateFlow()

    private val _punchHoleAlertEnabled = MutableStateFlow(true)
    val punchHoleAlertEnabled: StateFlow<Boolean> = _punchHoleAlertEnabled.asStateFlow()

    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true
        val appContext = context.applicationContext

        scope.launch {
            appContext.dataStore.data.collect { prefs ->
                _generalMode.value = DmReelMode.fromName(prefs[KEY_GENERAL_MODE], DmReelMode.SINGLE_REEL)
                _dmReelMode.value = DmReelMode.fromName(prefs[KEY_DM_REEL_MODE], DmReelMode.SINGLE_REEL)
                _exploreReelMode.value = DmReelMode.fromName(prefs[KEY_EXPLORE_REEL_MODE], DmReelMode.FOLLOW_GLOBAL)
                _feedReelMode.value = DmReelMode.fromName(prefs[KEY_FEED_REEL_MODE], DmReelMode.BLOCK_ALL)
                _profileReelMode.value = DmReelMode.fromName(prefs[KEY_PROFILE_REEL_MODE], DmReelMode.FOLLOW_GLOBAL)
                _reelsTabMode.value = DmReelMode.fromName(prefs[KEY_REELS_TAB_MODE], DmReelMode.BLOCK_ALL)
                _overlayFeedbackEnabled.value = prefs[KEY_OVERLAY_FEEDBACK_ENABLED] ?: true
                _customReelOverlayEnabled.value = prefs[KEY_CUSTOM_REEL_OVERLAY_ENABLED] ?: false

                _hasCompletedOnboarding.value = prefs[KEY_HAS_COMPLETED_ONBOARDING] ?: false
                _bedtimeModeEnabled.value = prefs[KEY_BEDTIME_MODE_ENABLED] ?: false
                _bedtimeStartHour.value = prefs[KEY_BEDTIME_START_HOUR] ?: 23
                _bedtimeStartMinute.value = prefs[KEY_BEDTIME_START_MINUTE] ?: 30
                _bedtimeEndHour.value = prefs[KEY_BEDTIME_END_HOUR] ?: 7
                _bedtimeEndMinute.value = prefs[KEY_BEDTIME_END_MINUTE] ?: 0
                _strictModeEnabled.value = prefs[KEY_STRICT_MODE_ENABLED] ?: false
                _appLanguage.value = AppLanguage.fromCode(prefs[KEY_APP_LANGUAGE])
                _appThemeMode.value = AppThemeMode.fromCode(prefs[KEY_APP_THEME_MODE])
                _punchHoleAlertEnabled.value = prefs[KEY_PUNCH_HOLE_ALERT_ENABLED] ?: true
            }
        }
    }

    fun setGeneralMode(context: Context, mode: DmReelMode) {
        _generalMode.value = mode
        val appContext = context.applicationContext
        scope.launch {
            appContext.dataStore.edit { prefs ->
                prefs[KEY_GENERAL_MODE] = mode.name
            }
        }
    }

    fun setDmReelMode(context: Context, mode: DmReelMode) {
        _dmReelMode.value = mode
        val appContext = context.applicationContext
        scope.launch {
            appContext.dataStore.edit { prefs ->
                prefs[KEY_DM_REEL_MODE] = mode.name
            }
        }
    }

    fun setExploreReelMode(context: Context, mode: DmReelMode) {
        _exploreReelMode.value = mode
        val appContext = context.applicationContext
        scope.launch {
            appContext.dataStore.edit { prefs ->
                prefs[KEY_EXPLORE_REEL_MODE] = mode.name
            }
        }
    }

    fun setFeedReelMode(context: Context, mode: DmReelMode) {
        _feedReelMode.value = mode
        val appContext = context.applicationContext
        scope.launch {
            appContext.dataStore.edit { prefs ->
                prefs[KEY_FEED_REEL_MODE] = mode.name
            }
        }
    }

    fun setProfileReelMode(context: Context, mode: DmReelMode) {
        _profileReelMode.value = mode
        val appContext = context.applicationContext
        scope.launch {
            appContext.dataStore.edit { prefs ->
                prefs[KEY_PROFILE_REEL_MODE] = mode.name
            }
        }
    }

    fun setReelsTabMode(context: Context, mode: DmReelMode) {
        _reelsTabMode.value = mode
        val appContext = context.applicationContext
        scope.launch {
            appContext.dataStore.edit { prefs ->
                prefs[KEY_REELS_TAB_MODE] = mode.name
            }
        }
    }

    fun setOverlayFeedbackEnabled(context: Context, enabled: Boolean) {
        _overlayFeedbackEnabled.value = enabled
        val appContext = context.applicationContext
        scope.launch {
            appContext.dataStore.edit { prefs ->
                prefs[KEY_OVERLAY_FEEDBACK_ENABLED] = enabled
            }
        }
    }

    fun setCustomReelOverlayEnabled(context: Context, enabled: Boolean) {
        _customReelOverlayEnabled.value = enabled
        val appContext = context.applicationContext
        scope.launch {
            appContext.dataStore.edit { prefs ->
                prefs[KEY_CUSTOM_REEL_OVERLAY_ENABLED] = enabled
            }
        }
    }

    fun applyProtectionLevel(context: Context, level: ProtectionLevel) {
        _dmReelMode.value = level.dmMode
        _exploreReelMode.value = level.exploreMode
        _feedReelMode.value = level.feedMode
        _profileReelMode.value = level.profileMode
        _reelsTabMode.value = level.reelsTabMode
        val appContext = context.applicationContext
        scope.launch {
            appContext.dataStore.edit { prefs ->
                prefs[KEY_DM_REEL_MODE] = level.dmMode.name
                prefs[KEY_EXPLORE_REEL_MODE] = level.exploreMode.name
                prefs[KEY_FEED_REEL_MODE] = level.feedMode.name
                prefs[KEY_PROFILE_REEL_MODE] = level.profileMode.name
                prefs[KEY_REELS_TAB_MODE] = level.reelsTabMode.name
            }
        }
        when (level) {
            ProtectionLevel.COUNT_QUOTA -> {
                QuotaStore.setDailyCountLimitEnabled(context, true)
                QuotaStore.setDailyTimeLimitEnabled(context, false)
            }
            ProtectionLevel.TIME_QUOTA -> {
                QuotaStore.setDailyTimeLimitEnabled(context, true)
                QuotaStore.setDailyCountLimitEnabled(context, false)
            }
            ProtectionLevel.STRICT,
            ProtectionLevel.SINGLE_REEL,
            ProtectionLevel.BALANCED,
            ProtectionLevel.MINDFUL -> {
                QuotaStore.setDailyCountLimitEnabled(context, false)
                QuotaStore.setDailyTimeLimitEnabled(context, false)
            }
        }
    }

    fun setHasCompletedOnboarding(context: Context, completed: Boolean) {
        _hasCompletedOnboarding.value = completed
        val appContext = context.applicationContext
        scope.launch {
            appContext.dataStore.edit { prefs ->
                prefs[KEY_HAS_COMPLETED_ONBOARDING] = completed
            }
        }
    }

    fun setBedtimeModeEnabled(context: Context, enabled: Boolean) {
        _bedtimeModeEnabled.value = enabled
        val appContext = context.applicationContext
        scope.launch {
            appContext.dataStore.edit { prefs ->
                prefs[KEY_BEDTIME_MODE_ENABLED] = enabled
            }
        }
    }

    fun setBedtimeWindow(
        context: Context,
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int
    ) {
        _bedtimeStartHour.value = startHour
        _bedtimeStartMinute.value = startMinute
        _bedtimeEndHour.value = endHour
        _bedtimeEndMinute.value = endMinute
        val appContext = context.applicationContext
        scope.launch {
            appContext.dataStore.edit { prefs ->
                prefs[KEY_BEDTIME_START_HOUR] = startHour
                prefs[KEY_BEDTIME_START_MINUTE] = startMinute
                prefs[KEY_BEDTIME_END_HOUR] = endHour
                prefs[KEY_BEDTIME_END_MINUTE] = endMinute
            }
        }
    }

    fun setStrictModeEnabled(context: Context, enabled: Boolean) {
        _strictModeEnabled.value = enabled
        val appContext = context.applicationContext
        scope.launch {
            appContext.dataStore.edit { prefs ->
                prefs[KEY_STRICT_MODE_ENABLED] = enabled
            }
        }
    }

    fun isBedtimeActiveNow(
        overrideHour: Int? = null,
        overrideMinute: Int? = null
    ): Boolean {
        val calendar = Calendar.getInstance()
        val hour = overrideHour ?: calendar.get(Calendar.HOUR_OF_DAY)
        val minute = overrideMinute ?: calendar.get(Calendar.MINUTE)

        return BedtimeCalculator.isBedtimeActive(
            enabled = _bedtimeModeEnabled.value,
            startHour = _bedtimeStartHour.value,
            startMinute = _bedtimeStartMinute.value,
            endHour = _bedtimeEndHour.value,
            endMinute = _bedtimeEndMinute.value,
            currentHour = hour,
            currentMinute = minute
        )
    }

    fun setAppLanguage(context: Context, language: AppLanguage) {
        _appLanguage.value = language
        val appContext = context.applicationContext
        scope.launch {
            appContext.dataStore.edit { prefs ->
                prefs[KEY_APP_LANGUAGE] = language.code
            }
        }
    }

    fun setAppThemeMode(context: Context, themeMode: AppThemeMode) {
        _appThemeMode.value = themeMode
        val appContext = context.applicationContext
        scope.launch {
            appContext.dataStore.edit { prefs ->
                prefs[KEY_APP_THEME_MODE] = themeMode.code
            }
        }
    }

    fun setPunchHoleAlertEnabled(context: Context, enabled: Boolean) {
        _punchHoleAlertEnabled.value = enabled
        val appContext = context.applicationContext
        scope.launch {
            appContext.dataStore.edit { prefs ->
                prefs[KEY_PUNCH_HOLE_ALERT_ENABLED] = enabled
            }
        }
    }
}

/**
 * Gece Odak Saati zaman aralığı hesaplamasını saf Kotlin olarak gerçekleştiren ve
 * gece yarısını aşan (örn: 23:30 - 07:00) saat pencerelerini hatasız denetleyen hesaplayıcı.
 */
object BedtimeCalculator {
    fun isBedtimeActive(
        enabled: Boolean,
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int,
        currentHour: Int,
        currentMinute: Int
    ): Boolean {
        if (!enabled) return false

        val currentMinutes = currentHour * 60 + currentMinute
        val startMinutes = startHour * 60 + startMinute
        val endMinutes = endHour * 60 + endMinute

        return if (startMinutes <= endMinutes) {
            currentMinutes in startMinutes until endMinutes
        } else {
            // Gece yarısını aşan aralık (örn. 23:30 - 07:00)
            currentMinutes >= startMinutes || currentMinutes < endMinutes
        }
    }
}
