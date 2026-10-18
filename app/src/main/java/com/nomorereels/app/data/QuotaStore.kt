package com.nomorereels.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Context.quotaDataStore: DataStore<Preferences> by preferencesDataStore(name = "nomorereels_quota")

/**
 * Günlük Reels adedi, Reels'de geçirilen süre ve kullanıcıya özel kota/uyarı eşiklerini
 * yöneten ve her gece 00:00'da otomatik olarak sıfırlanan sayaç ve tercih deposu.
 */
object QuotaStore {

    private val KEY_LAST_RESET_DATE = stringPreferencesKey("quota_last_reset_date")
    private val KEY_TODAY_REEL_COUNT = intPreferencesKey("quota_today_reel_count")
    private val KEY_TODAY_TIME_SPENT_SECONDS = longPreferencesKey("quota_today_time_spent_seconds")

    private val KEY_DAILY_COUNT_LIMIT_ENABLED = booleanPreferencesKey("daily_count_limit_enabled")
    private val KEY_DAILY_COUNT_LIMIT = intPreferencesKey("daily_count_limit")

    private val KEY_DAILY_TIME_LIMIT_ENABLED = booleanPreferencesKey("daily_time_limit_enabled")
    private val KEY_DAILY_TIME_LIMIT_MINUTES = intPreferencesKey("daily_time_limit_minutes")

    private val KEY_WARNING_PERCENT = intPreferencesKey("quota_warning_percent")
    private val KEY_SINGLE_REEL_MAX_SECONDS = intPreferencesKey("single_reel_max_seconds")

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var isInitialized = false

    private val _todayReelCount = MutableStateFlow(0)
    val todayReelCount: StateFlow<Int> = _todayReelCount.asStateFlow()

    private val _todayTimeSpentSeconds = MutableStateFlow(0L)
    val todayTimeSpentSeconds: StateFlow<Long> = _todayTimeSpentSeconds.asStateFlow()

    private val _dailyCountLimitEnabled = MutableStateFlow(false)
    val dailyCountLimitEnabled: StateFlow<Boolean> = _dailyCountLimitEnabled.asStateFlow()

    private val _dailyCountLimit = MutableStateFlow(10)
    val dailyCountLimit: StateFlow<Int> = _dailyCountLimit.asStateFlow()

    private val _dailyTimeLimitEnabled = MutableStateFlow(false)
    val dailyTimeLimitEnabled: StateFlow<Boolean> = _dailyTimeLimitEnabled.asStateFlow()

    private val _dailyTimeLimitMinutes = MutableStateFlow(15)
    val dailyTimeLimitMinutes: StateFlow<Int> = _dailyTimeLimitMinutes.asStateFlow()

    private val _warningPercent = MutableStateFlow(80)
    val warningPercent: StateFlow<Int> = _warningPercent.asStateFlow()

    private val _singleReelMaxSeconds = MutableStateFlow(90)
    val singleReelMaxSeconds: StateFlow<Int> = _singleReelMaxSeconds.asStateFlow()

    private var hasWarnedCountToday = false
    private var hasWarnedTimeToday = false

    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true
        val appContext = context.applicationContext

        val todayDate = getTodayDateString()

        scope.launch {
            appContext.quotaDataStore.data.collect { prefs ->
                val lastDate = prefs[KEY_LAST_RESET_DATE] ?: ""
                if (lastDate != todayDate) {
                    // Gün değişmiş; sayaçları sıfırla
                    _todayReelCount.value = 0
                    _todayTimeSpentSeconds.value = 0L
                    hasWarnedCountToday = false
                    hasWarnedTimeToday = false
                    appContext.quotaDataStore.edit { editPrefs ->
                        editPrefs[KEY_LAST_RESET_DATE] = todayDate
                        editPrefs[KEY_TODAY_REEL_COUNT] = 0
                        editPrefs[KEY_TODAY_TIME_SPENT_SECONDS] = 0L
                    }
                } else {
                    _todayReelCount.value = prefs[KEY_TODAY_REEL_COUNT] ?: 0
                    _todayTimeSpentSeconds.value = prefs[KEY_TODAY_TIME_SPENT_SECONDS] ?: 0L
                }

                _dailyCountLimitEnabled.value = prefs[KEY_DAILY_COUNT_LIMIT_ENABLED] ?: false
                _dailyCountLimit.value = prefs[KEY_DAILY_COUNT_LIMIT] ?: 10
                _dailyTimeLimitEnabled.value = prefs[KEY_DAILY_TIME_LIMIT_ENABLED] ?: false
                _dailyTimeLimitMinutes.value = prefs[KEY_DAILY_TIME_LIMIT_MINUTES] ?: 15
                _warningPercent.value = prefs[KEY_WARNING_PERCENT] ?: 80
                _singleReelMaxSeconds.value = prefs[KEY_SINGLE_REEL_MAX_SECONDS] ?: 90
            }
        }
    }

    fun incrementReelCount(context: Context): Int {
        val newCount = _todayReelCount.value + 1
        _todayReelCount.value = newCount
        val appContext = context.applicationContext
        val todayDate = getTodayDateString()

        scope.launch {
            appContext.quotaDataStore.edit { prefs ->
                prefs[KEY_LAST_RESET_DATE] = todayDate
                prefs[KEY_TODAY_REEL_COUNT] = newCount
            }
        }
        return newCount
    }

    fun addTimeSpent(context: Context, secondsToAdd: Long): Long {
        if (secondsToAdd <= 0L) return _todayTimeSpentSeconds.value
        val newTotal = _todayTimeSpentSeconds.value + secondsToAdd
        _todayTimeSpentSeconds.value = newTotal
        val appContext = context.applicationContext
        val todayDate = getTodayDateString()

        scope.launch {
            appContext.quotaDataStore.edit { prefs ->
                prefs[KEY_LAST_RESET_DATE] = todayDate
                prefs[KEY_TODAY_TIME_SPENT_SECONDS] = newTotal
            }
        }
        return newTotal
    }

    fun resetTodayUsage(context: Context) {
        _todayReelCount.value = 0
        _todayTimeSpentSeconds.value = 0L
        hasWarnedCountToday = false
        hasWarnedTimeToday = false
        val appContext = context.applicationContext
        val todayDate = getTodayDateString()

        scope.launch {
            appContext.quotaDataStore.edit { prefs ->
                prefs[KEY_LAST_RESET_DATE] = todayDate
                prefs[KEY_TODAY_REEL_COUNT] = 0
                prefs[KEY_TODAY_TIME_SPENT_SECONDS] = 0L
            }
        }
    }

    fun setDailyCountLimitEnabled(context: Context, enabled: Boolean) {
        _dailyCountLimitEnabled.value = enabled
        val appContext = context.applicationContext
        scope.launch {
            appContext.quotaDataStore.edit { it[KEY_DAILY_COUNT_LIMIT_ENABLED] = enabled }
        }
    }

    fun setDailyCountLimit(context: Context, limit: Int) {
        _dailyCountLimit.value = limit
        val appContext = context.applicationContext
        scope.launch {
            appContext.quotaDataStore.edit { it[KEY_DAILY_COUNT_LIMIT] = limit }
        }
    }

    fun setDailyTimeLimitEnabled(context: Context, enabled: Boolean) {
        _dailyTimeLimitEnabled.value = enabled
        val appContext = context.applicationContext
        scope.launch {
            appContext.quotaDataStore.edit { it[KEY_DAILY_TIME_LIMIT_ENABLED] = enabled }
        }
    }

    fun setDailyTimeLimitMinutes(context: Context, minutes: Int) {
        _dailyTimeLimitMinutes.value = minutes
        val appContext = context.applicationContext
        scope.launch {
            appContext.quotaDataStore.edit { it[KEY_DAILY_TIME_LIMIT_MINUTES] = minutes }
        }
    }

    fun setWarningPercent(context: Context, percent: Int) {
        _warningPercent.value = percent
        val appContext = context.applicationContext
        scope.launch {
            appContext.quotaDataStore.edit { it[KEY_WARNING_PERCENT] = percent }
        }
    }

    fun setSingleReelMaxSeconds(context: Context, seconds: Int) {
        _singleReelMaxSeconds.value = seconds
        val appContext = context.applicationContext
        scope.launch {
            appContext.quotaDataStore.edit { it[KEY_SINGLE_REEL_MAX_SECONDS] = seconds }
        }
    }

    // --- Durum Değerlendirme Fonksiyonları ---

    fun isCountLimitExceeded(): Boolean {
        if (!_dailyCountLimitEnabled.value) return false
        return _todayReelCount.value >= _dailyCountLimit.value
    }

    fun isTimeLimitExceeded(): Boolean {
        if (!_dailyTimeLimitEnabled.value) return false
        val maxSeconds = _dailyTimeLimitMinutes.value * 60L
        return _todayTimeSpentSeconds.value >= maxSeconds
    }

    /**
     * Kullanıcı kota dolmadan önce uyarı eşiğine geldi mi?
     * Örneğin %80 ayarlandıysa ve limit 10 ise, 8. videoda tek seferlik true döner.
     */
    fun checkAndTriggerCountWarning(): Boolean {
        if (!_dailyCountLimitEnabled.value || hasWarnedCountToday) return false
        val threshold = (_dailyCountLimit.value * _warningPercent.value) / 100
        if (_todayReelCount.value >= threshold && !isCountLimitExceeded()) {
            hasWarnedCountToday = true
            return true
        }
        return false
    }

    /**
     * Kullanıcı süre dolmadan önce uyarı eşiğine geldi mi?
     */
    fun checkAndTriggerTimeWarning(): Boolean {
        if (!_dailyTimeLimitEnabled.value || hasWarnedTimeToday) return false
        val maxSeconds = _dailyTimeLimitMinutes.value * 60L
        val thresholdSeconds = (maxSeconds * _warningPercent.value) / 100
        if (_todayTimeSpentSeconds.value >= thresholdSeconds && !isTimeLimitExceeded()) {
            hasWarnedTimeToday = true
            return true
        }
        return false
    }

    fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }
}
