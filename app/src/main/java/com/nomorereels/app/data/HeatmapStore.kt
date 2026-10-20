package com.nomorereels.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
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
import java.util.Calendar

private val Context.heatmapDataStore by preferencesDataStore(name = "nmr_heatmap_store")

/**
 * Kullanıcının haftanın hangi gününde ve hangi saatinde Instagram Reels'e girmeye
 * çalıştığını kaydeden 7x24'lük (168 hücre) çevrimdışı ısı haritası deposu.
 *
 * Sıfır internet ve tam gizlilik:
 * Sadece saatlik tamsayı sayaçları tutulur, hiçbir metin, kullanıcı adı veya içerik saklanmaz.
 */
object HeatmapStore {

    private const val TOTAL_CELLS = 7 * 24 // 168 hücre
    private val KEY_MATRIX = stringPreferencesKey("heatmap_matrix_csv")
    private val KEY_FIRST_RECORD_MS = longPreferencesKey("heatmap_first_record_ms")
    private val KEY_TOTAL_ATTEMPTS = longPreferencesKey("heatmap_total_attempts")

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isInitialized = false

    // 168 elemanlı bellek içi sayaç dizisi
    private val _matrix = MutableStateFlow(IntArray(TOTAL_CELLS))
    val matrix: StateFlow<IntArray> = _matrix.asStateFlow()

    private val _firstRecordTimeMs = MutableStateFlow(0L)
    val firstRecordTimeMs: StateFlow<Long> = _firstRecordTimeMs.asStateFlow()

    private val _totalAttempts = MutableStateFlow(0L)
    val totalAttempts: StateFlow<Long> = _totalAttempts.asStateFlow()

    data class BedtimeSuggestion(
        val suggestedStartHour: Int,
        val suggestedEndHour: Int,
        val attemptCountInWindow: Int
    )

    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true

        scope.launch {
            try {
                val prefs = context.heatmapDataStore.data.first()
                val csv = prefs[KEY_MATRIX] ?: ""
                val firstTime = prefs[KEY_FIRST_RECORD_MS] ?: 0L
                val total = prefs[KEY_TOTAL_ATTEMPTS] ?: 0L

                val parsed = IntArray(TOTAL_CELLS)
                if (csv.isNotBlank()) {
                    val tokens = csv.split(",")
                    for (i in 0 until minOf(tokens.size, TOTAL_CELLS)) {
                        parsed[i] = tokens[i].trim().toIntOrNull() ?: 0
                    }
                }

                _matrix.value = parsed
                _firstRecordTimeMs.value = firstTime
                _totalAttempts.value = total
            } catch (_: Exception) {}
        }
    }

    /**
     * Kullanıcı bir Reels ekranına girmeye çalıştığında çağrılır.
     * Kurallardan (engellendi mi, kota mı doldu vb.) bağımsız olarak kullanıcının
     * GERÇEK Reels açma isteğini/alışkanlığını kaydeder.
     */
    fun recordAttempt(context: Context, timestampMs: Long = System.currentTimeMillis()) {
        val cal = Calendar.getInstance().apply { timeInMillis = timestampMs }
        // Calendar.DAY_OF_WEEK: SUNDAY=1, MONDAY=2, ... SATURDAY=7
        // Monday=0 .. Sunday=6 formatına eşleme:
        val dayOfWeek = when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> 0
        }
        val hour = cal.get(Calendar.HOUR_OF_DAY).coerceIn(0, 23)
        val cellIndex = dayOfWeek * 24 + hour

        val updated = _matrix.value.copyOf()
        updated[cellIndex] = updated[cellIndex] + 1
        _matrix.value = updated

        val newTotal = _totalAttempts.value + 1
        _totalAttempts.value = newTotal

        if (_firstRecordTimeMs.value == 0L) {
            _firstRecordTimeMs.value = timestampMs
        }

        // Asenkron DataStore kaydı
        scope.launch {
            try {
                val csv = updated.joinToString(",")
                context.heatmapDataStore.edit { prefs ->
                    prefs[KEY_MATRIX] = csv
                    prefs[KEY_TOTAL_ATTEMPTS] = newTotal
                    if (prefs[KEY_FIRST_RECORD_MS] == null || prefs[KEY_FIRST_RECORD_MS] == 0L) {
                        prefs[KEY_FIRST_RECORD_MS] = timestampMs
                    }
                }
            } catch (_: Exception) {}
        }
    }

    /**
     * Isı haritası verilerini inceleyerek Gece Odak Saati önerisi üretir.
     *
     * Kriterler:
     * 1. En az 7 günlük veri birikmiş olmalı veya en az 20 deneme kaydedilmiş olmalı.
     * 2. Gece (21:00 - 04:00) aralığında belirgin bir tepe (en az 8 deneme) olmalı.
     * 3. Otomatik uygulanmaz; sadece tavsiye nesnesi döner.
     */
    fun analyzeForBedtimeSuggestion(nowMs: Long = System.currentTimeMillis()): BedtimeSuggestion? {
        val total = _totalAttempts.value
        val firstTime = _firstRecordTimeMs.value
        if (total < 15) return null

        val weekDurationMs = 7L * 24 * 60 * 60 * 1000
        val hasAccumulatedEnoughTime = firstTime > 0L && (nowMs - firstTime >= weekDurationMs)
        if (!hasAccumulatedEnoughTime && total < 30) return null

        val mat = _matrix.value

        // Gece saatleri için 24 saatlik toplamları çıkar (Pzt-Paz toplamı)
        val hourlyTotals = IntArray(24)
        for (day in 0..6) {
            for (h in 0..23) {
                hourlyTotals[h] += mat[day * 24 + h]
            }
        }

        // Gece aralıkları: 22:00-01:00, 23:00-02:00, 00:00-03:00, 01:00-04:00
        val candidateWindows = listOf(
            Triple(22, 1, hourlyTotals[22] + hourlyTotals[23] + hourlyTotals[0]),
            Triple(23, 2, hourlyTotals[23] + hourlyTotals[0] + hourlyTotals[1]),
            Triple(0, 3, hourlyTotals[0] + hourlyTotals[1] + hourlyTotals[2]),
            Triple(1, 4, hourlyTotals[1] + hourlyTotals[2] + hourlyTotals[3])
        )

        val bestWindow = candidateWindows.maxByOrNull { it.third } ?: return null
        if (bestWindow.third < 8) return null // Yeterli gece aktivitesi yoksa önerme

        return BedtimeSuggestion(
            suggestedStartHour = bestWindow.first,
            suggestedEndHour = 7, // Sabah 07:00'ye kadar odak
            attemptCountInWindow = bestWindow.third
        )
    }

    /**
     * Isı haritasını sıfırlar.
     */
    fun clear(context: Context) {
        _matrix.value = IntArray(TOTAL_CELLS)
        _firstRecordTimeMs.value = 0L
        _totalAttempts.value = 0L

        scope.launch {
            try {
                context.heatmapDataStore.edit { prefs ->
                    prefs.clear()
                }
            } catch (_: Exception) {}
        }
    }
}
