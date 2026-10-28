package com.nomorereels.app.detection

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

private val nextDetectionLogId = AtomicLong(1L)

data class DetectionEventLog(
    val id: Long = nextDetectionLogId.getAndIncrement(),
    val timestamp: Long = System.currentTimeMillis(),
    val timeFormatted: String = SimpleDateFormat("HH:mm:ss.SS", Locale.getDefault()).format(Date(timestamp)),
    val screenType: ScreenType,
    val details: String,
    val actionTaken: String? = null,
    val keyNodes: List<String> = emptyList()
)

object DetectionLogStore {
    private const val MAX_LOGS = 50
    private val lock = Any()
    private val logsList = mutableListOf<DetectionEventLog>()

    private val _logs = MutableStateFlow<List<DetectionEventLog>>(emptyList())
    val logs: StateFlow<List<DetectionEventLog>> = _logs.asStateFlow()

    fun log(
        screenType: ScreenType,
        details: String,
        actionTaken: String? = null,
        keyNodes: List<String> = emptyList()
    ) {
        synchronized(lock) {
            val event = DetectionEventLog(
                screenType = screenType,
                details = details,
                actionTaken = actionTaken,
                keyNodes = keyNodes
            )
            logsList.add(0, event)
            if (logsList.size > MAX_LOGS) {
                logsList.removeAt(logsList.size - 1)
            }
            _logs.value = logsList.toList()
        }
    }

    fun clear() {
        synchronized(lock) {
            logsList.clear()
            _logs.value = emptyList()
        }
    }
}
