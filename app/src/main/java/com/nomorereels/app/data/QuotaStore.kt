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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Context.quotaDataStore: DataStore<Preferences> by preferencesDataStore(name = "nomorereels_quota")

/**
 * Günlük Reels adedi, Reels'de geçirilen süre, engellenen Reel sayısı ve kullanıcıya özel
 * kota/uyarı eşiklerini yöneten ve her gece 00:00'da otomatik olarak sıfırlanan sayaç ve tercih deposu.
 *
 * Notlar:
 * - Gün dönümü yalnızca uygulama açılışında değil, servis çalışırken de ([ensureToday]) algılanır.
 * - Kota "aşıldı mı" kontrolleri artık ayrı bir "enabled" bayrağına bağlı değildir; kotanın
 *   uygulanıp uygulanmayacağına aktif bölge modu (COUNT_QUOTA / TIME_QUOTA) karar verir.
 * - Süre sayacı bellekte saniye saniye güncellenir ancak diske en fazla [TIME_PERSIST_INTERVAL_SEC]
 *   saniyede bir (veya [flush] çağrıldığında) yazılır.
 */
object QuotaStore {

    private val KEY_LAST_RESET_DATE = stringPreferencesKey("quota_last_reset_date")
    private val KEY_TODAY_REEL_COUNT = intPreferencesKey("quota_today_reel_count")
    private val KEY_TODAY_TIME_SPENT_SECONDS = longPreferencesKey("quota_today_time_spent_seconds")
    private val KEY_TODAY_BLOCK_COUNT = intPreferencesKey("quota_today_block_count")
    private val KEY_TOTAL_BLOCK_COUNT = longPreferencesKey("quota_total_block_count")

    private val KEY_DAILY_COUNT_LIMIT_ENABLED = booleanPreferencesKey("daily_count_limit_enabled")
    private val KEY_DAILY_COUNT_LIMIT = intPreferencesKey("daily_count_limit")

    private val KEY_DAILY_TIME_LIMIT_ENABLED = booleanPreferencesKey("daily_time_limit_enabled")
    private val KEY_DAILY_TIME_LIMIT_MINUTES = intPreferencesKey("daily_time_limit_minutes")

    private val KEY_WARNING_PERCENT = intPreferencesKey("quota_warning_percent")
    private val KEY_SINGLE_REEL_MAX_SECONDS = intPreferencesKey("single_reel_max_seconds")

    private const val TIME_PERSIST_INTERVAL_SEC = 10L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var isInitialized = false

    @Volatile
    private var currentDay: String = ""

    private var unsavedTimeSeconds = 0L

    private val _todayReelCount = MutableStateFlow(0)
    val todayReelCount: StateFlow<Int> = _todayReelCount.asStateFlow()

    private val _todayTimeSpentSeconds = MutableStateFlow(0L)
    val todayTimeSpentSeconds: StateFlow<Long> = _todayTimeSpentSeconds.asStateFlow()

    private val _todayBlockCount = MutableStateFlow(0)
    val todayBlockCount: StateFlow<Int> = _todayBlockCount.asStateFlow()

    private val _totalBlockCount = MutableStateFlow(0L)
    val totalBlockCount: StateFlow<Long> = _totalBlockCount.asStateFlow()

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

        scope.launch {
            // 1. Günlük sayaçları yalnızca bir kez diskten yükle. Sonraki güncellemeler bellek
            //    üzerinden yürür; böylece hızlı artışlar eski disk değeriyle ezilmez (yarış koşulu yok).
            val prefs = appContext.quotaDataStore.data.first()
            val today = getTodayDateString()
            val lastDate = prefs[KEY_LAST_RESET_DATE] ?: ""
            currentDay = today
            _totalBlockCount.value = prefs[KEY_TOTAL_BLOCK_COUNT] ?: 0L
            if (lastDate != today) {
                resetCountersInMemory()
                persistCounters(appContext)
            } else {
                _todayReelCount.value = prefs[KEY_TODAY_REEL_COUNT] ?: 0
                _todayTimeSpentSeconds.value = prefs[KEY_TODAY_TIME_SPENT_SECONDS] ?: 0L
                _todayBlockCount.value = prefs[KEY_TODAY_BLOCK_COUNT] ?: 0
            }

            // 2. Ayarları (limitler, eşikler) sürekli dinle
            appContext.quotaDataStore.data.collect { p ->
                _dailyCountLimitEnabled.value = p[KEY_DAILY_COUNT_LIMIT_ENABLED] ?: false
                _dailyCountLimit.value = p[KEY_DAILY_COUNT_LIMIT] ?: 10
                _dailyTimeLimitEnabled.value = p[KEY_DAILY_TIME_LIMIT_ENABLED] ?: false
                _dailyTimeLimitMinutes.value = p[KEY_DAILY_TIME_LIMIT_MINUTES] ?: 15
                _warningPercent.value = p[KEY_WARNING_PERCENT] ?: 80
                _singleReelMaxSeconds.value = p[KEY_SINGLE_REEL_MAX_SECONDS] ?: 90
            }
        }
    }

    /**
     * Servis gece yarısını geçerek çalışmaya devam ederse günlük sayaçları sıfırlar.
     * Her sayaç güncellemesinden ve kota kontrolünden önce çağrılır (ucuz bir string karşılaştırması).
     */
    fun ensureToday(context: Context) {
        val today = getTodayDateString()
        if (currentDay.isNotEmpty() && currentDay != today) {
            currentDay = today
            resetCountersInMemory()
            persistCounters(context.applicationContext)
        } else if (currentDay.isEmpty()) {
            currentDay = today
        }
    }

    private fun resetCountersInMemory() {
        _todayReelCount.value = 0
        _todayTimeSpentSeconds.value = 0L
        _todayBlockCount.value = 0
        unsavedTimeSeconds = 0L
        hasWarnedCountToday = false
        hasWarnedTimeToday = false
    }

    private fun persistCounters(appContext: Context) {
        val day = currentDay.ifEmpty { getTodayDateString() }
        val reels = _todayReelCount.value
        val secs = _todayTimeSpentSeconds.value
        val blocks = _todayBlockCount.value
        val total = _totalBlockCount.value
        scope.launch {
            appContext.quotaDataStore.edit { prefs ->
                prefs[KEY_LAST_RESET_DATE] = day
                prefs[KEY_TODAY_REEL_COUNT] = reels
                prefs[KEY_TODAY_TIME_SPENT_SECONDS] = secs
                prefs[KEY_TODAY_BLOCK_COUNT] = blocks
                prefs[KEY_TOTAL_BLOCK_COUNT] = total
            }
            com.nomorereels.app.widget.QuotaAppWidgetProvider.updateAllWidgets(appContext)
        }
    }

    fun incrementReelCount(context: Context): Int {
        ensureToday(context)
        val newCount = _todayReelCount.value + 1
        _todayReelCount.value = newCount
        persistCounters(context.applicationContext)
        return newCount
    }

    fun incrementBlockCount(context: Context): Int {
        ensureToday(context)
        val newCount = _todayBlockCount.value + 1
        _todayBlockCount.value = newCount
        _totalBlockCount.value = _totalBlockCount.value + 1
        persistCounters(context.applicationContext)
        return newCount
    }

    fun addTimeSpent(context: Context, secondsToAdd: Long): Long {
        ensureToday(context)
        if (secondsToAdd <= 0L) return _todayTimeSpentSeconds.value
        val newTotal = _todayTimeSpentSeconds.value + secondsToAdd
        _todayTimeSpentSeconds.value = newTotal
        unsavedTimeSeconds += secondsToAdd
        // Disk yazımını grupla: her saniye yerine en fazla 10 saniyede bir yaz
        if (unsavedTimeSeconds >= TIME_PERSIST_INTERVAL_SEC) {
            unsavedTimeSeconds = 0L
            persistCounters(context.applicationContext)
        }
        return newTotal
    }

    /** Bellekteki yazılmamış süreyi diske yazar (örn. Reels ekranından çıkıldığında). */
    fun flush(context: Context) {
        if (unsavedTimeSeconds > 0L) {
            unsavedTimeSeconds = 0L
            persistCounters(context.applicationContext)
        }
    }

    fun resetTodayUsage(context: Context) {
        currentDay = getTodayDateString()
        resetCountersInMemory()
        persistCounters(context.applicationContext)
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
        hasWarnedCountToday = false
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
        hasWarnedTimeToday = false
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

    /**
     * Yeni bir Reel'e girilirken: bugün izlenen adet limite ulaştıysa yeni Reel'e izin verilmez.
     * Aynı Reel izlenmeye devam ederken: yalnızca limit aşıldıysa engellenir (limiti dolduran son Reel
     * yarıda kesilmez).
     */
    fun isCountLimitExceeded(enteringNewReel: Boolean = true): Boolean {
        val count = _todayReelCount.value
        val limit = _dailyCountLimit.value
        return if (enteringNewReel) count >= limit else count > limit
    }

    fun isTimeLimitExceeded(): Boolean {
        val maxSeconds = _dailyTimeLimitMinutes.value * 60L
        return _todayTimeSpentSeconds.value >= maxSeconds
    }

    /**
     * Kullanıcı kota dolmadan önce uyarı eşiğine geldi mi?
     * Örneğin %80 ayarlandıysa ve limit 10 ise, 8. videoda tek seferlik true döner.
     * Yalnızca aktif mod COUNT_QUOTA iken çağrılmalıdır.
     */
    fun checkAndTriggerCountWarning(): Boolean {
        if (hasWarnedCountToday) return false
        val limit = _dailyCountLimit.value
        val threshold = (limit * _warningPercent.value) / 100
        val count = _todayReelCount.value
        if (count >= threshold && count < limit) {
            hasWarnedCountToday = true
            return true
        }
        return false
    }

    /**
     * Kullanıcı süre dolmadan önce uyarı eşiğine geldi mi?
     * Yalnızca aktif mod TIME_QUOTA iken çağrılmalıdır.
     */
    fun checkAndTriggerTimeWarning(): Boolean {
        if (hasWarnedTimeToday) return false
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
