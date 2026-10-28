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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "nomorereels_settings")

enum class AppLanguage(val code: String, val titleTr: String, val titleEn: String) {
    TR("tr", "Türkçe 🇹🇷", "Turkish 🇹🇷"),
    EN("en", "İngilizce 🇬🇧", "English 🇬🇧");

    companion object {
        fun defaultForDevice(): AppLanguage =
            if (java.util.Locale.getDefault().language.equals("tr", ignoreCase = true)) TR else EN

        fun fromCode(code: String?): AppLanguage =
            entries.firstOrNull { it.code == code } ?: defaultForDevice()
    }
}

enum class AppThemeMode(val code: String, val titleTr: String, val titleEn: String) {
    SYSTEM("system", "Sistem Varsayılanı", "System Default"),
    LIGHT("light", "Açık Tema", "Light Theme"),
    DARK("dark", "Karanlık (AMOLED)", "Dark (AMOLED)");

    companion object {
        fun fromCode(code: String?): AppThemeMode = entries.firstOrNull { it.code == code } ?: LIGHT
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
    private val KEY_HAPTIC_FEEDBACK_ENABLED = booleanPreferencesKey("haptic_feedback_enabled")
    private val KEY_HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
    private val KEY_BEDTIME_MODE_ENABLED = booleanPreferencesKey("bedtime_mode_enabled")
    private val KEY_BEDTIME_START_HOUR = intPreferencesKey("bedtime_start_hour")
    private val KEY_BEDTIME_START_MINUTE = intPreferencesKey("bedtime_start_minute")
    private val KEY_BEDTIME_END_HOUR = intPreferencesKey("bedtime_end_hour")
    private val KEY_BEDTIME_END_MINUTE = intPreferencesKey("bedtime_end_minute")
    private val KEY_APP_LANGUAGE = stringPreferencesKey("app_language")
    private val KEY_APP_THEME_MODE = stringPreferencesKey("app_theme_mode")
    private val KEY_SNOOZE_END_TIME_MS = androidx.datastore.preferences.core.longPreferencesKey("snooze_end_time_ms")
    private val KEY_SNOOZE_SAVED_GENERAL_MODE = stringPreferencesKey("snooze_saved_general_mode")
    private val KEY_SNOOZE_SAVED_DM_MODE = stringPreferencesKey("snooze_saved_dm_mode")
    private val KEY_SNOOZE_SAVED_EXPLORE_MODE = stringPreferencesKey("snooze_saved_explore_mode")
    private val KEY_SNOOZE_SAVED_FEED_MODE = stringPreferencesKey("snooze_saved_feed_mode")
    private val KEY_SNOOZE_SAVED_PROFILE_MODE = stringPreferencesKey("snooze_saved_profile_mode")
    private val KEY_SNOOZE_SAVED_REELS_TAB_MODE = stringPreferencesKey("snooze_saved_reels_tab_mode")
    private val KEY_MINDFUL_GATEWAY_ENABLED = booleanPreferencesKey("mindful_gateway_enabled")
    private val KEY_LAST_MICRO_PLAN = stringPreferencesKey("last_micro_plan")
    private val KEY_IS_SUPPORTER = booleanPreferencesKey("is_supporter")
    private val KEY_SUPPORTER_DONATION_COUNT = intPreferencesKey("supporter_donation_count")
    private val KEY_PROTECTION_MASTER_ENABLED = booleanPreferencesKey("protection_master_enabled")
    private val KEY_SNOOZE_COUNT_DAY = stringPreferencesKey("snooze_count_day")
    private val KEY_SNOOZE_COUNT_TODAY = intPreferencesKey("snooze_count_today")

    /** Bir günde Farkındalık Kapısı / uygulama içi moladan verilebilecek en fazla izin sayısı. */
    const val MAX_DAILY_SNOOZES = 3

    private fun todayKey(): String =
        java.time.LocalDate.now().toString()

    private val _snoozeCountToday = MutableStateFlow(0)
    val snoozeCountToday: StateFlow<Int> = _snoozeCountToday.asStateFlow()

    /** Bugün için mola hakkı kaldı mı? (Gece Odak Saati'nde ayrıca hiç izin verilmez.) */
    fun canStartSnoozeNow(): Boolean =
        !isBedtimeActiveNow() && _snoozeCountToday.value < MAX_DAILY_SNOOZES

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1))

    @Volatile
    private var isInitialized = false

    private val _protectionMasterEnabled = MutableStateFlow(true)
    val protectionMasterEnabled: StateFlow<Boolean> = _protectionMasterEnabled.asStateFlow()

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

    private val _hapticFeedbackEnabled = MutableStateFlow(true)
    val hapticFeedbackEnabled: StateFlow<Boolean> = _hapticFeedbackEnabled.asStateFlow()

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

    private val _appLanguage = MutableStateFlow(AppLanguage.defaultForDevice())
    val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()

    private val _appThemeMode = MutableStateFlow(AppThemeMode.LIGHT)
    val appThemeMode: StateFlow<AppThemeMode> = _appThemeMode.asStateFlow()

    private val _snoozeEndTimeMs = MutableStateFlow(0L)
    val snoozeEndTimeMs: StateFlow<Long> = _snoozeEndTimeMs.asStateFlow()

    private val _mindfulGatewayEnabled = MutableStateFlow(true)
    val mindfulGatewayEnabled: StateFlow<Boolean> = _mindfulGatewayEnabled.asStateFlow()

    private val _lastMicroPlan = MutableStateFlow("")
    val lastMicroPlan: StateFlow<String> = _lastMicroPlan.asStateFlow()

    private val _isSupporter = MutableStateFlow(false)
    val isSupporter: StateFlow<Boolean> = _isSupporter.asStateFlow()

    private val _supporterDonationCount = MutableStateFlow(0)
    val supporterDonationCount: StateFlow<Int> = _supporterDonationCount.asStateFlow()

    private fun applyPrefsToMemory(prefs: Preferences) {
        val legacySavedGen = prefs[KEY_SNOOZE_SAVED_GENERAL_MODE]
        _generalMode.value = DmReelMode.fromName(legacySavedGen ?: prefs[KEY_GENERAL_MODE], DmReelMode.SINGLE_REEL)
        _dmReelMode.value = DmReelMode.fromName(prefs[KEY_SNOOZE_SAVED_DM_MODE] ?: prefs[KEY_DM_REEL_MODE], DmReelMode.SINGLE_REEL)
        _exploreReelMode.value = DmReelMode.fromName(prefs[KEY_SNOOZE_SAVED_EXPLORE_MODE] ?: prefs[KEY_EXPLORE_REEL_MODE], DmReelMode.FOLLOW_GLOBAL)
        _feedReelMode.value = DmReelMode.fromName(prefs[KEY_SNOOZE_SAVED_FEED_MODE] ?: prefs[KEY_FEED_REEL_MODE], DmReelMode.BLOCK_ALL)
        _profileReelMode.value = DmReelMode.fromName(prefs[KEY_SNOOZE_SAVED_PROFILE_MODE] ?: prefs[KEY_PROFILE_REEL_MODE], DmReelMode.FOLLOW_GLOBAL)
        _reelsTabMode.value = DmReelMode.fromName(prefs[KEY_SNOOZE_SAVED_REELS_TAB_MODE] ?: prefs[KEY_REELS_TAB_MODE], DmReelMode.BLOCK_ALL)
        _overlayFeedbackEnabled.value = prefs[KEY_OVERLAY_FEEDBACK_ENABLED] ?: true
        _hapticFeedbackEnabled.value = prefs[KEY_HAPTIC_FEEDBACK_ENABLED] ?: true

        _hasCompletedOnboarding.value = prefs[KEY_HAS_COMPLETED_ONBOARDING] ?: false
        _bedtimeModeEnabled.value = prefs[KEY_BEDTIME_MODE_ENABLED] ?: false
        _bedtimeStartHour.value = prefs[KEY_BEDTIME_START_HOUR] ?: 23
        _bedtimeStartMinute.value = prefs[KEY_BEDTIME_START_MINUTE] ?: 30
        _bedtimeEndHour.value = prefs[KEY_BEDTIME_END_HOUR] ?: 7
        _bedtimeEndMinute.value = prefs[KEY_BEDTIME_END_MINUTE] ?: 0
        _appLanguage.value = AppLanguage.fromCode(prefs[KEY_APP_LANGUAGE])
        _appThemeMode.value = AppThemeMode.fromCode(prefs[KEY_APP_THEME_MODE])
        val savedSnooze = prefs[KEY_SNOOZE_END_TIME_MS] ?: 0L
        _snoozeEndTimeMs.value = if (savedSnooze > System.currentTimeMillis()) savedSnooze else 0L

        _mindfulGatewayEnabled.value = prefs[KEY_MINDFUL_GATEWAY_ENABLED] ?: true
        _lastMicroPlan.value = prefs[KEY_LAST_MICRO_PLAN] ?: ""
        _isSupporter.value = prefs[KEY_IS_SUPPORTER] ?: false
        _supporterDonationCount.value = prefs[KEY_SUPPORTER_DONATION_COUNT] ?: 0
        _protectionMasterEnabled.value = prefs[KEY_PROTECTION_MASTER_ENABLED] ?: true
        _snoozeCountToday.value =
            if (prefs[KEY_SNOOZE_COUNT_DAY] == todayKey()) prefs[KEY_SNOOZE_COUNT_TODAY] ?: 0 else 0
    }

    suspend fun awaitInit(context: Context) {
        val appContext = context.applicationContext
        val prefs = appContext.dataStore.data.first()
        applyPrefsToMemory(prefs)
        if (!isInitialized) {
            init(appContext)
        }
    }

    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true
        val appContext = context.applicationContext

        scope.launch {
            // Eski sürümden kalan geçici mola (snooze_saved_*) anahtarları varsa bir kez temizle ve gerçek modları koru
            appContext.dataStore.edit { prefs ->
                val savedGen = prefs[KEY_SNOOZE_SAVED_GENERAL_MODE]
                if (savedGen != null) {
                    prefs[KEY_GENERAL_MODE] = savedGen
                    prefs[KEY_SNOOZE_SAVED_DM_MODE]?.let { prefs[KEY_DM_REEL_MODE] = it }
                    prefs[KEY_SNOOZE_SAVED_EXPLORE_MODE]?.let { prefs[KEY_EXPLORE_REEL_MODE] = it }
                    prefs[KEY_SNOOZE_SAVED_FEED_MODE]?.let { prefs[KEY_FEED_REEL_MODE] = it }
                    prefs[KEY_SNOOZE_SAVED_PROFILE_MODE]?.let { prefs[KEY_PROFILE_REEL_MODE] = it }
                    prefs[KEY_SNOOZE_SAVED_REELS_TAB_MODE]?.let { prefs[KEY_REELS_TAB_MODE] = it }
                    prefs.remove(KEY_SNOOZE_SAVED_GENERAL_MODE)
                    prefs.remove(KEY_SNOOZE_SAVED_DM_MODE)
                    prefs.remove(KEY_SNOOZE_SAVED_EXPLORE_MODE)
                    prefs.remove(KEY_SNOOZE_SAVED_FEED_MODE)
                    prefs.remove(KEY_SNOOZE_SAVED_PROFILE_MODE)
                    prefs.remove(KEY_SNOOZE_SAVED_REELS_TAB_MODE)
                }
            }
            val initialPrefs = appContext.dataStore.data.first()
            applyPrefsToMemory(initialPrefs)
        }
    }

    fun setProtectionMasterEnabled(context: Context, enabled: Boolean) {
        _protectionMasterEnabled.value = enabled
        val appContext = context.applicationContext
        scope.launch {
            appContext.dataStore.edit { prefs ->
                prefs[KEY_PROTECTION_MASTER_ENABLED] = enabled
            }
            com.nomorereels.app.widget.QuotaAppWidgetProvider.updateAllWidgets(appContext)
        }
    }

    fun recordDonation(context: Context) {
        val newCount = _supporterDonationCount.value + 1
        _supporterDonationCount.value = newCount
        _isSupporter.value = true
        val appContext = context.applicationContext
        scope.launch {
            appContext.dataStore.edit { prefs ->
                prefs[KEY_IS_SUPPORTER] = true
                prefs[KEY_SUPPORTER_DONATION_COUNT] = newCount
            }
        }
    }

    fun resetSupporterStatus(context: Context) {
        _supporterDonationCount.value = 0
        _isSupporter.value = false
        val appContext = context.applicationContext
        scope.launch {
            appContext.dataStore.edit { prefs ->
                prefs[KEY_IS_SUPPORTER] = false
                prefs[KEY_SUPPORTER_DONATION_COUNT] = 0
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
            com.nomorereels.app.widget.QuotaAppWidgetProvider.updateAllWidgets(appContext)
        }
    }

    /**
     * Genel modu seçer ve TÜM bölgeleri "Genel moda uy" (FOLLOW_GLOBAL) yapar.
     * Böylece kullanıcının Korumalar ekranında seçtiği mod her bölgede gerçekten uygulanır.
     * Bölge bazlı istisnalar daha sonra Bölgeler sekmesinden yeniden tanımlanabilir.
     */
    fun applyGeneralModeToAllRegions(context: Context, mode: DmReelMode) {
        val follow = DmReelMode.FOLLOW_GLOBAL
        _generalMode.value = mode
        _dmReelMode.value = follow
        _exploreReelMode.value = follow
        _feedReelMode.value = follow
        _profileReelMode.value = follow
        _reelsTabMode.value = follow
        val appContext = context.applicationContext
        scope.launch {
            appContext.dataStore.edit { prefs ->
                prefs[KEY_GENERAL_MODE] = mode.name
                prefs[KEY_DM_REEL_MODE] = follow.name
                prefs[KEY_EXPLORE_REEL_MODE] = follow.name
                prefs[KEY_FEED_REEL_MODE] = follow.name
                prefs[KEY_PROFILE_REEL_MODE] = follow.name
                prefs[KEY_REELS_TAB_MODE] = follow.name
            }
            com.nomorereels.app.widget.QuotaAppWidgetProvider.updateAllWidgets(appContext)
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

    fun setHapticFeedbackEnabled(context: Context, enabled: Boolean) {
        _hapticFeedbackEnabled.value = enabled
        val appContext = context.applicationContext
        scope.launch {
            appContext.dataStore.edit { prefs ->
                prefs[KEY_HAPTIC_FEEDBACK_ENABLED] = enabled
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
            com.nomorereels.app.widget.QuotaAppWidgetProvider.updateAllWidgets(appContext)
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
            com.nomorereels.app.widget.QuotaAppWidgetProvider.updateAllWidgets(appContext)
        }
    }

    fun isDarkThemeActiveNow(context: Context): Boolean {
        val theme = _appThemeMode.value
        if (theme == AppThemeMode.DARK) return true
        if (theme == AppThemeMode.LIGHT) return false
        if (isBedtimeActiveNow()) return true
        val uiMode = context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
        return uiMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
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

    fun setMindfulGatewayEnabled(context: Context, enabled: Boolean) {
        _mindfulGatewayEnabled.value = enabled
        val appContext = context.applicationContext
        scope.launch {
            appContext.dataStore.edit { prefs ->
                prefs[KEY_MINDFUL_GATEWAY_ENABLED] = enabled
            }
        }
    }

    fun setLastMicroPlan(context: Context, plan: String) {
        _lastMicroPlan.value = plan
        val appContext = context.applicationContext
        scope.launch {
            appContext.dataStore.edit { prefs ->
                prefs[KEY_LAST_MICRO_PLAN] = plan
            }
        }
    }

    fun consumeLastMicroPlan(context: Context): String {
        val plan = _lastMicroPlan.value
        if (plan.isNotEmpty()) {
            setLastMicroPlan(context, "")
        }
        return plan
    }

    // --- Süreli Gevşetme (Mola Modu / Snooze) ---

    fun isSnoozeActiveNow(nowMs: Long = System.currentTimeMillis()): Boolean {
        return _snoozeEndTimeMs.value > nowMs
    }

    /** Servisin dikkate aldığı mola: Gece Odak Saati her zaman molayı geçersiz kılar. */
    fun isSnoozeEffective(nowMs: Long = System.currentTimeMillis()): Boolean {
        return isSnoozeActiveNow(nowMs) && !isBedtimeActiveNow()
    }

    fun getSnoozeRemainingMinutes(nowMs: Long = System.currentTimeMillis()): Int {
        val diff = _snoozeEndTimeMs.value - nowMs
        return if (diff > 0) ((diff + 59_999L) / 60_000L).toInt() else 0
    }

    /**
     * Kullanıcıya geçici mola (örn. 5, 10, 15 dk) veya Bilinçli Pencere verir.
     * Bölge modlarını FREE ile ezmez; servis ve arayüz doğrudan [isSnoozeEffective] durumuna bakar.
     * Böylece mola esnasında kullanıcının gerçek bölge ayarları korunur ve yedeklemelerde bozulmaz.
     */
    fun startSnooze(
        context: Context,
        durationMinutes: Int,
        microPlan: String = ""
    ) {
        val now = System.currentTimeMillis()
        val endTime = now + (durationMinutes * 60_000L)

        _snoozeEndTimeMs.value = endTime
        _lastMicroPlan.value = microPlan
        val day = todayKey()
        val newCount = _snoozeCountToday.value + 1
        _snoozeCountToday.value = newCount
        val appContext = context.applicationContext
        scope.launch {
            appContext.dataStore.edit { prefs ->
                prefs[KEY_SNOOZE_END_TIME_MS] = endTime
                prefs[KEY_LAST_MICRO_PLAN] = microPlan
                prefs[KEY_SNOOZE_COUNT_DAY] = day
                prefs[KEY_SNOOZE_COUNT_TODAY] = newCount
            }
            com.nomorereels.app.widget.QuotaAppWidgetProvider.updateAllWidgets(appContext)
        }
    }

    /**
     * Molayı sonlandırır ve korumayı anında devreye alır.
     */
    fun stopSnooze(context: Context) {
        _snoozeEndTimeMs.value = 0L
        val appContext = context.applicationContext
        scope.launch {
            appContext.dataStore.edit { prefs ->
                prefs[KEY_SNOOZE_END_TIME_MS] = 0L
                prefs.remove(KEY_SNOOZE_SAVED_GENERAL_MODE)
                prefs.remove(KEY_SNOOZE_SAVED_DM_MODE)
                prefs.remove(KEY_SNOOZE_SAVED_EXPLORE_MODE)
                prefs.remove(KEY_SNOOZE_SAVED_FEED_MODE)
                prefs.remove(KEY_SNOOZE_SAVED_PROFILE_MODE)
                prefs.remove(KEY_SNOOZE_SAVED_REELS_TAB_MODE)
            }
            com.nomorereels.app.widget.QuotaAppWidgetProvider.updateAllWidgets(appContext)
        }
    }

    /**
     * İstatistikler sıfırlandığında günlük mola sayacını ve aktif molayı da sıfırlar.
     */
    fun resetDailySnoozeCount(context: Context) {
        _snoozeCountToday.value = 0
        _snoozeEndTimeMs.value = 0L
        val appContext = context.applicationContext
        scope.launch {
            appContext.dataStore.edit { prefs ->
                prefs[KEY_SNOOZE_COUNT_TODAY] = 0
                prefs[KEY_SNOOZE_END_TIME_MS] = 0L
            }
            com.nomorereels.app.widget.QuotaAppWidgetProvider.updateAllWidgets(appContext)
        }
    }

    /**
     * Servis ve UI döngüsünde mola süresi doldu mu kontrol eder; dolduysa otomatik sonlandırır
     * ve süresi yeni dolduysa `true` döner.
     */
    fun checkSnoozeExpiration(context: Context, nowMs: Long = System.currentTimeMillis()): Boolean {
        val end = _snoozeEndTimeMs.value
        if (end in 1..nowMs) {
            stopSnooze(context)
            return true
        }
        return false
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
