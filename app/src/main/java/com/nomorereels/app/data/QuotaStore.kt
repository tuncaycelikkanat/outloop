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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

private val Context.quotaDataStore: DataStore<Preferences> by preferencesDataStore(name = "nomorereels_quota")

/**
 * Günlük Reels adedi, Reels'de geçirilen süre, engellenen Reel sayısı ve kullanıcıya özel
 * kota/uyarı eşiklerini yöneten ve her gece 00:00'da otomatik olarak sıfırlanan sayaç ve tercih deposu.
 */
object QuotaStore {

    private val KEY_LAST_RESET_DATE = stringPreferencesKey("quota_last_reset_date")
    private val KEY_TODAY_REEL_COUNT = intPreferencesKey("quota_today_reel_count")
    private val KEY_TODAY_TIME_SPENT_SECONDS = longPreferencesKey("quota_today_time_spent_seconds")
    private val KEY_TODAY_BLOCK_COUNT = intPreferencesKey("quota_today_block_count")
    private val KEY_TOTAL_BLOCK_COUNT = longPreferencesKey("quota_total_block_count")
    private val KEY_DAILY_HISTORY_CSV = stringPreferencesKey("quota_daily_history_csv")
    private val KEY_DAILY_SECONDS_HISTORY_CSV = stringPreferencesKey("quota_daily_seconds_history_csv")
    private val KEY_DAILY_BLOCKS_HISTORY_CSV = stringPreferencesKey("quota_daily_blocks_history_csv")
    private val KEY_TODAY_GATEWAY_DISMISS_COUNT = intPreferencesKey("quota_today_gateway_dismiss_count")
    private val KEY_TOTAL_GATEWAY_DISMISS_COUNT = longPreferencesKey("quota_total_gateway_dismiss_count")

    private val KEY_DAILY_COUNT_LIMIT_ENABLED = booleanPreferencesKey("daily_count_limit_enabled")
    private val KEY_DAILY_COUNT_LIMIT = intPreferencesKey("daily_count_limit")

    private val KEY_DAILY_TIME_LIMIT_ENABLED = booleanPreferencesKey("daily_time_limit_enabled")
    private val KEY_DAILY_TIME_LIMIT_MINUTES = intPreferencesKey("daily_time_limit_minutes")

    private val KEY_WARNING_PERCENT = intPreferencesKey("quota_warning_percent")
    private val KEY_SINGLE_REEL_MAX_SECONDS = intPreferencesKey("single_reel_max_seconds")

    private const val TIME_PERSIST_INTERVAL_SEC = 10L
    private const val DUPLICATE_BLOCK_WINDOW_MS = 4000L

    // P1-2: Tüm DataStore yazımlarını tek iş parçacıklı kuyrukta sıralı çalıştır
    @OptIn(ExperimentalCoroutinesApi::class)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1))

    @Volatile
    private var isInitialized = false

    @Volatile
    private var isDataLoaded = false

    private val loadLock = Any()

    @Volatile
    private var currentDay: String = ""

    private var unsavedTimeSeconds = 0L

    @Volatile
    private var lastBlockIncrementAtMs = 0L

    @Volatile
    private var cachedTodayString: String = ""

    @Volatile
    private var cachedTodayAtMs: Long = 0L

    private val _todayReelCount = MutableStateFlow(0)
    val todayReelCount: StateFlow<Int> = _todayReelCount.asStateFlow()

    private val _todayTimeSpentSeconds = MutableStateFlow(0L)
    val todayTimeSpentSeconds: StateFlow<Long> = _todayTimeSpentSeconds.asStateFlow()

    private val _todayBlockCount = MutableStateFlow(0)
    val todayBlockCount: StateFlow<Int> = _todayBlockCount.asStateFlow()

    private val _totalBlockCount = MutableStateFlow(0L)
    val totalBlockCount: StateFlow<Long> = _totalBlockCount.asStateFlow()

    private val _todayGatewayDismissCount = MutableStateFlow(0)
    val todayGatewayDismissCount: StateFlow<Int> = _todayGatewayDismissCount.asStateFlow()

    private val _totalGatewayDismissCount = MutableStateFlow(0L)
    val totalGatewayDismissCount: StateFlow<Long> = _totalGatewayDismissCount.asStateFlow()

    // P1-5: Saniye cinsinden ham günlük geçmiş (son 30 gün)
    private val _dailySecondsHistory = MutableStateFlow<Map<String, Long>>(emptyMap())
    val dailySecondsHistory: StateFlow<Map<String, Long>> = _dailySecondsHistory.asStateFlow()

    // Grafik ve 10 günlük ortalama için dakika cinsinden geçmiş
    private val _dailyHistory = MutableStateFlow<Map<String, Long>>(emptyMap())
    val dailyHistory: StateFlow<Map<String, Long>> = _dailyHistory.asStateFlow()

    // 4.2-4: Haftalık/aylık kurtarılan odak hesabı için günlük engel geçmişi (son 30 gün)
    private val _dailyBlocksHistory = MutableStateFlow<Map<String, Int>>(emptyMap())
    val dailyBlocksHistory: StateFlow<Map<String, Int>> = _dailyBlocksHistory.asStateFlow()

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

    private fun secondsMapToMinutesMap(secondsMap: Map<String, Long>): Map<String, Long> {
        return secondsMap.mapValues { (_, secs) ->
            when {
                secs <= 0L -> 0L
                secs < 60L -> 1L
                else -> (secs + 30L) / 60L
            }
        }
    }

    private fun parseLongCsv(csv: String): Map<String, Long> {
        if (csv.isBlank()) return emptyMap()
        return csv.split(",").mapNotNull { entry ->
            val parts = entry.split(":")
            if (parts.size == 2) {
                val d = parts[0]
                val v = parts[1].toLongOrNull() ?: 0L
                d to v
            } else null
        }.toMap()
    }

    private fun parseIntCsv(csv: String): Map<String, Int> {
        if (csv.isBlank()) return emptyMap()
        return csv.split(",").mapNotNull { entry ->
            val parts = entry.split(":")
            if (parts.size == 2) {
                val d = parts[0]
                val v = parts[1].toIntOrNull() ?: 0
                d to v
            } else null
        }.toMap()
    }

    private fun applyInitialPrefsLocked(appContext: Context, prefs: Preferences) {
        synchronized(loadLock) {
            if (isDataLoaded) return
            val today = getTodayDateString()
            val lastDate = prefs[KEY_LAST_RESET_DATE] ?: ""
            currentDay = today
            _totalBlockCount.value = prefs[KEY_TOTAL_BLOCK_COUNT] ?: 0L
            _totalGatewayDismissCount.value = prefs[KEY_TOTAL_GATEWAY_DISMISS_COUNT] ?: 0L

            val secondsCsv = prefs[KEY_DAILY_SECONDS_HISTORY_CSV] ?: ""
            val legacyMinsCsv = prefs[KEY_DAILY_HISTORY_CSV] ?: ""
            val loadedSecondsMap = when {
                secondsCsv.isNotBlank() -> parseLongCsv(secondsCsv)
                legacyMinsCsv.isNotBlank() -> parseLongCsv(legacyMinsCsv).mapValues { it.value * 60L }
                else -> emptyMap()
            }
            _dailySecondsHistory.value = loadedSecondsMap
            _dailyHistory.value = secondsMapToMinutesMap(loadedSecondsMap)

            val blocksCsv = prefs[KEY_DAILY_BLOCKS_HISTORY_CSV] ?: ""
            _dailyBlocksHistory.value = parseIntCsv(blocksCsv)

            _dailyCountLimitEnabled.value = prefs[KEY_DAILY_COUNT_LIMIT_ENABLED] ?: false
            _dailyCountLimit.value = prefs[KEY_DAILY_COUNT_LIMIT] ?: 10
            _dailyTimeLimitEnabled.value = prefs[KEY_DAILY_TIME_LIMIT_ENABLED] ?: false
            _dailyTimeLimitMinutes.value = prefs[KEY_DAILY_TIME_LIMIT_MINUTES] ?: 15
            _warningPercent.value = prefs[KEY_WARNING_PERCENT] ?: 80
            _singleReelMaxSeconds.value = prefs[KEY_SINGLE_REEL_MAX_SECONDS] ?: 90

            isDataLoaded = true

            if (lastDate != today) {
                // Önceki günün son değerlerini geçmişte koru
                if (lastDate.isNotBlank()) {
                    val prevSecs = prefs[KEY_TODAY_TIME_SPENT_SECONDS] ?: 0L
                    val prevBlocks = prefs[KEY_TODAY_BLOCK_COUNT] ?: 0
                    if (prevSecs > 0L) {
                        val updatedSecs = (_dailySecondsHistory.value + (lastDate to prevSecs)).toList().takeLast(30).toMap()
                        _dailySecondsHistory.value = updatedSecs
                        _dailyHistory.value = secondsMapToMinutesMap(updatedSecs)
                    }
                    if (prevBlocks > 0) {
                        _dailyBlocksHistory.value = (_dailyBlocksHistory.value + (lastDate to prevBlocks)).toList().takeLast(30).toMap()
                    }
                }
                resetCountersInMemory()
                persistCounters(appContext, updateWidget = true)
            } else {
                _todayReelCount.value = prefs[KEY_TODAY_REEL_COUNT] ?: 0
                _todayTimeSpentSeconds.value = prefs[KEY_TODAY_TIME_SPENT_SECONDS] ?: 0L
                _todayBlockCount.value = prefs[KEY_TODAY_BLOCK_COUNT] ?: 0
                _todayGatewayDismissCount.value = prefs[KEY_TODAY_GATEWAY_DISMISS_COUNT] ?: 0
            }
        }
    }

    suspend fun awaitInit(context: Context) {
        val appContext = context.applicationContext
        if (!isDataLoaded) {
            val prefs = appContext.quotaDataStore.data.first()
            applyInitialPrefsLocked(appContext, prefs)
        }
        if (!isInitialized) {
            init(appContext)
        }
    }

    private fun ensureLoadedBlocking(context: Context) {
        if (isDataLoaded) return
        val appContext = context.applicationContext
        try {
            val prefs = kotlinx.coroutines.runBlocking(Dispatchers.IO) {
                appContext.quotaDataStore.data.first()
            }
            applyInitialPrefsLocked(appContext, prefs)
        } catch (_: Exception) {}
    }

    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true
        val appContext = context.applicationContext

        scope.launch {
            val prefs = appContext.quotaDataStore.data.first()
            applyInitialPrefsLocked(appContext, prefs)

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
     * P1-6: Servis gece yarısını geçerek çalışmaya devam ederse, sıfırlamadan önce önceki günün
     * birikmiş saniye ve engel verilerini geçmiş haritasına kaydeder.
     */
    fun ensureToday(context: Context) {
        ensureLoadedBlocking(context)
        val today = getTodayDateString()
        if (currentDay.isNotEmpty() && currentDay != today) {
            val previousDay = currentDay
            val prevSecs = _todayTimeSpentSeconds.value
            val prevBlocks = _todayBlockCount.value
            if (prevSecs > 0L) {
                val updatedSecs = (_dailySecondsHistory.value + (previousDay to prevSecs)).toList().takeLast(30).toMap()
                _dailySecondsHistory.value = updatedSecs
                _dailyHistory.value = secondsMapToMinutesMap(updatedSecs)
            }
            if (prevBlocks > 0) {
                _dailyBlocksHistory.value = (_dailyBlocksHistory.value + (previousDay to prevBlocks)).toList().takeLast(30).toMap()
            }
            currentDay = today
            resetCountersInMemory()
            persistCounters(context.applicationContext, updateWidget = true)
        } else if (currentDay.isEmpty()) {
            currentDay = today
        }
    }

    private fun resetCountersInMemory() {
        _todayReelCount.value = 0
        _todayTimeSpentSeconds.value = 0L
        _todayBlockCount.value = 0
        _todayGatewayDismissCount.value = 0
        unsavedTimeSeconds = 0L
        hasWarnedCountToday = false
        hasWarnedTimeToday = false
    }

    private fun persistCounters(appContext: Context, updateWidget: Boolean = true) {
        val day = currentDay.ifEmpty { getTodayDateString() }
        val reels = _todayReelCount.value
        val secs = _todayTimeSpentSeconds.value
        val blocks = _todayBlockCount.value
        val total = _totalBlockCount.value
        val gatewayDismissToday = _todayGatewayDismissCount.value
        val gatewayDismissTotal = _totalGatewayDismissCount.value

        val updatedSecondsMap = (_dailySecondsHistory.value + (day to secs)).toList().takeLast(30).toMap()
        _dailySecondsHistory.value = updatedSecondsMap
        val updatedMinutesMap = secondsMapToMinutesMap(updatedSecondsMap)
        _dailyHistory.value = updatedMinutesMap

        val updatedBlocksMap = (_dailyBlocksHistory.value + (day to blocks)).toList().takeLast(30).toMap()
        _dailyBlocksHistory.value = updatedBlocksMap

        val secondsCsv = updatedSecondsMap.entries.joinToString(",") { "${it.key}:${it.value}" }
        val historyCsv = updatedMinutesMap.entries.joinToString(",") { "${it.key}:${it.value}" }
        val blocksCsv = updatedBlocksMap.entries.joinToString(",") { "${it.key}:${it.value}" }

        scope.launch {
            appContext.quotaDataStore.edit { prefs ->
                prefs[KEY_LAST_RESET_DATE] = day
                prefs[KEY_TODAY_REEL_COUNT] = reels
                prefs[KEY_TODAY_TIME_SPENT_SECONDS] = secs
                prefs[KEY_TODAY_BLOCK_COUNT] = blocks
                prefs[KEY_TOTAL_BLOCK_COUNT] = total
                prefs[KEY_TODAY_GATEWAY_DISMISS_COUNT] = gatewayDismissToday
                prefs[KEY_TOTAL_GATEWAY_DISMISS_COUNT] = gatewayDismissTotal
                prefs[KEY_DAILY_SECONDS_HISTORY_CSV] = secondsCsv
                prefs[KEY_DAILY_HISTORY_CSV] = historyCsv
                prefs[KEY_DAILY_BLOCKS_HISTORY_CSV] = blocksCsv
            }
            if (updateWidget) {
                com.nomorereels.app.widget.QuotaAppWidgetProvider.updateAllWidgets(appContext)
            }
        }
    }

    fun incrementReelCount(context: Context): Int {
        ensureToday(context)
        val newCount = _todayReelCount.value + 1
        _todayReelCount.value = newCount
        persistCounters(context.applicationContext, updateWidget = true)
        return newCount
    }

    /**
     * 4.2-2: Aynı oturumda peş peşe (4 sn içinde) tetiklenen çift engellemeleri tek döngü kırılması sayar.
     */
    fun incrementBlockCount(context: Context, nowMs: Long = System.currentTimeMillis()): Int {
        ensureToday(context)
        if (nowMs - lastBlockIncrementAtMs < DUPLICATE_BLOCK_WINDOW_MS) {
            return _todayBlockCount.value
        }
        lastBlockIncrementAtMs = nowMs
        val newCount = _todayBlockCount.value + 1
        _todayBlockCount.value = newCount
        _totalBlockCount.value = _totalBlockCount.value + 1
        persistCounters(context.applicationContext, updateWidget = true)
        return newCount
    }

    /** 4.3-7: Farkındalık Kapısı'nda "Vazgeç ve Geri Dön" seçildiğinde sayacı artırır. */
    fun incrementGatewayDismissCount(context: Context): Int {
        ensureToday(context)
        val newCount = _todayGatewayDismissCount.value + 1
        _todayGatewayDismissCount.value = newCount
        _totalGatewayDismissCount.value = _totalGatewayDismissCount.value + 1L
        persistCounters(context.applicationContext, updateWidget = false)
        return newCount
    }

    fun addTimeSpent(context: Context, secondsToAdd: Long): Long {
        ensureToday(context)
        if (secondsToAdd <= 0L) return _todayTimeSpentSeconds.value
        val prevTotal = _todayTimeSpentSeconds.value
        val newTotal = prevTotal + secondsToAdd
        _todayTimeSpentSeconds.value = newTotal
        unsavedTimeSeconds += secondsToAdd
        // Disk yazımını grupla: her saniye yerine en fazla 10 saniyede bir yaz
        if (unsavedTimeSeconds >= TIME_PERSIST_INTERVAL_SEC) {
            val minuteChanged = (prevTotal / 60L) != (newTotal / 60L)
            unsavedTimeSeconds = 0L
            persistCounters(context.applicationContext, updateWidget = minuteChanged)
        }
        return newTotal
    }

    /** Bellekteki yazılmamış süreyi diske yazar (örn. Reels ekranından çıkıldığında). */
    fun flush(context: Context) {
        if (unsavedTimeSeconds > 0L) {
            unsavedTimeSeconds = 0L
            persistCounters(context.applicationContext, updateWidget = true)
        }
    }

    fun resetTodayUsage(context: Context) {
        currentDay = getTodayDateString()
        resetCountersInMemory()
        persistCounters(context.applicationContext, updateWidget = true)
    }

    fun resetAllStatistics(context: Context) {
        val appContext = context.applicationContext
        currentDay = getTodayDateString()
        resetCountersInMemory()
        _todayBlockCount.value = 0
        _totalBlockCount.value = 0L
        _todayGatewayDismissCount.value = 0
        _totalGatewayDismissCount.value = 0L
        _dailySecondsHistory.value = emptyMap()
        _dailyHistory.value = emptyMap()
        _dailyBlocksHistory.value = emptyMap()
        scope.launch {
            try {
                appContext.quotaDataStore.edit { prefs ->
                    prefs[KEY_TODAY_REEL_COUNT] = 0
                    prefs[KEY_TODAY_TIME_SPENT_SECONDS] = 0L
                    prefs[KEY_TODAY_BLOCK_COUNT] = 0
                    prefs[KEY_TOTAL_BLOCK_COUNT] = 0L
                    prefs[KEY_TODAY_GATEWAY_DISMISS_COUNT] = 0
                    prefs[KEY_TOTAL_GATEWAY_DISMISS_COUNT] = 0L
                    prefs[KEY_DAILY_SECONDS_HISTORY_CSV] = ""
                    prefs[KEY_DAILY_HISTORY_CSV] = ""
                    prefs[KEY_DAILY_BLOCKS_HISTORY_CSV] = ""
                    prefs[KEY_LAST_RESET_DATE] = currentDay
                }
                com.nomorereels.app.widget.QuotaAppWidgetProvider.updateAllWidgets(appContext)
            } catch (_: Exception) {}
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
        hasWarnedCountToday = false
        val appContext = context.applicationContext
        scope.launch {
            appContext.quotaDataStore.edit { it[KEY_DAILY_COUNT_LIMIT] = limit }
            com.nomorereels.app.widget.QuotaAppWidgetProvider.updateAllWidgets(appContext)
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
            com.nomorereels.app.widget.QuotaAppWidgetProvider.updateAllWidgets(appContext)
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

    // --- Kişiselleştirilmiş Kurtarılan Odak Hesabı (4.2-1 & 4.2-4) ---

    /**
     * Kullanıcının kendi ortalama Reel izleme süresini (varsa) ve kırılan döngü katsayısını
     * baz alarak engel başına kurtarılan saniyeyi (35..75 sn aralığında, varsayılan 45 sn) hesaplar.
     */
    fun estimateSavedSecondsPerBlock(
        watchedReels: Int = _todayReelCount.value,
        spentSeconds: Long = _todayTimeSpentSeconds.value
    ): Long {
        if (watchedReels >= 2 && spentSeconds >= 20L) {
            val avgReelSec = (spentSeconds / watchedReels).coerceIn(20L, 50L)
            return ((avgReelSec * 3L) / 2L).coerceIn(35L, 75L)
        }
        return 45L
    }

    fun estimateSavedSecondsForBlocks(
        blockCount: Int,
        watchedReels: Int = _todayReelCount.value,
        spentSeconds: Long = _todayTimeSpentSeconds.value
    ): Long {
        if (blockCount <= 0) return 0L
        return blockCount.toLong() * estimateSavedSecondsPerBlock(watchedReels, spentSeconds)
    }

    // --- Durum Değerlendirme Fonksiyonları ---

    fun isCountLimitExceeded(enteringNewReel: Boolean = true): Boolean {
        val count = _todayReelCount.value
        val limit = _dailyCountLimit.value
        return if (enteringNewReel) count >= limit else count > limit
    }

    fun isTimeLimitExceeded(): Boolean {
        val maxSeconds = _dailyTimeLimitMinutes.value * 60L
        return _todayTimeSpentSeconds.value >= maxSeconds
    }

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

    fun getTenDayAverageMinutes(fallbackWeeklyMinutes: List<Long>): Long {
        val chronologicalValues = _dailyHistory.value.entries
            .sortedBy { it.key }
            .map { it.value }
            .dropWhile { it <= 0L }
            .takeLast(10)
        return if (chronologicalValues.size >= 2) {
            chronologicalValues.average().toLong().coerceAtLeast(1L)
        } else {
            val activeFallback = fallbackWeeklyMinutes.dropWhile { it <= 0L }
            if (activeFallback.isNotEmpty()) {
                activeFallback.average().toLong().coerceAtLeast(1L)
            } else {
                15L
            }
        }
    }

    fun getTodayDateString(nowMs: Long = System.currentTimeMillis()): String {
        val cached = cachedTodayString
        if (cached.isNotEmpty() && nowMs - cachedTodayAtMs in 0L..1000L) {
            return cached
        }
        val computed = LocalDate.now().toString()
        cachedTodayString = computed
        cachedTodayAtMs = nowMs
        return computed
    }
}
