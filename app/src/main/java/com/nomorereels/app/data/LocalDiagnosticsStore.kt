package com.nomorereels.app.data

import android.content.Context
import android.content.Intent
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Sıfır internet politikasına uygun, tamamen cihaz içi (yerel) çalışan çökme yakalayıcı
 * ve Instagram güncellemesi sonrası tespit sağlığı (Detection Health) izleyicisi.
 * Hiçbir kişisel veri, ekran metni veya kullanıcı adı kaydetmez.
 */
object LocalDiagnosticsStore {

    private const val CRASH_LOG_FILE = "outloop_diagnostics.log"
    private const val MAX_LOG_CHARS = 16_000
    private const val WARNING_IG_FOREGROUND_SECONDS = 30 * 60L // 30 dakika

    private val _instagramForegroundSecondsToday = MutableStateFlow(0L)
    val instagramForegroundSecondsToday: StateFlow<Long> = _instagramForegroundSecondsToday.asStateFlow()

    private val _reelsDetectionsToday = MutableStateFlow(0)
    val reelsDetectionsToday: StateFlow<Int> = _reelsDetectionsToday.asStateFlow()

    private val _isDetectionHealthWarningActive = MutableStateFlow(false)
    val isDetectionHealthWarningActive: StateFlow<Boolean> = _isDetectionHealthWarningActive.asStateFlow()

    @Volatile
    private var lastIgEventWallMs: Long = 0L

    fun installCrashHandler(context: Context) {
        val appContext = context.applicationContext
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                appendCrashLog(appContext, thread.name, throwable)
            } catch (_: Exception) {}
            previousHandler?.uncaughtException(thread, throwable)
        }
    }

    fun recordInstagramEvent(nowMs: Long = System.currentTimeMillis(), isReelsScreen: Boolean) {
        val prev = lastIgEventWallMs
        if (prev > 0L) {
            val deltaSec = ((nowMs - prev) / 1000L).coerceIn(0L, 15L)
            if (deltaSec > 0L) {
                _instagramForegroundSecondsToday.value += deltaSec
            }
        }
        lastIgEventWallMs = nowMs

        if (isReelsScreen) {
            _reelsDetectionsToday.value += 1
        }

        _isDetectionHealthWarningActive.value =
            _instagramForegroundSecondsToday.value >= WARNING_IG_FOREGROUND_SECONDS &&
                _reelsDetectionsToday.value == 0
    }

    fun onLeaveInstagram() {
        lastIgEventWallMs = 0L
    }

    private fun appendCrashLog(context: Context, threadName: String, throwable: Throwable) {
        val file = File(context.filesDir, CRASH_LOG_FILE)
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val entry = buildString {
            appendLine("=== Crash @ $timestamp (Thread: $threadName, SDK: ${Build.VERSION.SDK_INT}) ===")
            appendLine(sw.toString().take(4000))
            appendLine()
        }
        val existing = if (file.exists()) file.readText(Charsets.UTF_8) else ""
        val combined = (existing + entry).takeLast(MAX_LOG_CHARS)
        file.writeText(combined, Charsets.UTF_8)
    }

    fun buildDiagnosticReport(context: Context): String {
        val file = File(context.filesDir, CRASH_LOG_FILE)
        val crashContent = if (file.exists()) file.readText(Charsets.UTF_8).ifBlank { "Kayıtlı çökme yok (No crashes recorded)." }
        else "Kayıtlı çökme yok (No crashes recorded)."

        val recentEvents = com.nomorereels.app.detection.DetectionLogStore.logs.value.take(15)
        return buildString {
            appendLine("Outloop Local Diagnostic Report")
            appendLine("SDK: ${Build.VERSION.SDK_INT} (${Build.MANUFACTURER} ${Build.MODEL})")
            appendLine("Service Active: ${com.nomorereels.app.service.ReelsAccessibilityService.isServiceActive.value}")
            appendLine("Master Protection: ${SettingsStore.protectionMasterEnabled.value}")
            appendLine("General Mode: ${SettingsStore.generalMode.value.name}")
            appendLine("IG Foreground Today (s): ${_instagramForegroundSecondsToday.value}")
            appendLine("Reels Detections Today: ${_reelsDetectionsToday.value}")
            appendLine("Today Blocks: ${QuotaStore.todayBlockCount.value}")
            appendLine()
            appendLine("--- Recent Structural Transitions (${recentEvents.size}) ---")
            for (ev in recentEvents) {
                appendLine("${ev.timeFormatted} | ${ev.screenType.name} | ${ev.details} | ${ev.actionTaken ?: "-"}")
            }
            appendLine()
            appendLine("--- Crash Log ---")
            appendLine(crashContent)
        }
    }

    fun shareDiagnosticReport(context: Context) {
        val report = buildDiagnosticReport(context)
        val isEnglish = SettingsStore.appLanguage.value == AppLanguage.EN
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Outloop Diagnostic Report")
            putExtra(Intent.EXTRA_TEXT, report)
        }
        val chooser = Intent.createChooser(
            sendIntent,
            if (isEnglish) "Share Diagnostic Log" else "Tanılama Günlüğünü Paylaş"
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(chooser)
        } catch (_: Exception) {}
    }
}
