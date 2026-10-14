package com.nomorereels.app.action

import android.accessibilityservice.AccessibilityService
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ActionExecutor(
    private val serviceProvider: () -> AccessibilityService?,
    private val cooldownMs: Long = 800L
) {
    private var lastActionTime: Long = 0L

    private val _blockCount = MutableStateFlow(0)
    val blockCount: StateFlow<Int> = _blockCount.asStateFlow()

    fun triggerBackAction(reason: String): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastActionTime < cooldownMs) {
            Log.d(TAG, "Geri dönme işlemi bekleme süresinde (cooldown aktif), atlandı. Neden: $reason")
            return false
        }

        val service = serviceProvider()
        if (service == null) {
            Log.w(TAG, "Erişilebilirlik servisi hazır değil, işlem yapılamadı.")
            return false
        }

        Log.i(TAG, ">>> REELS ENGELLENDİ: GLOBAL_ACTION_BACK tetikleniyor. Neden: $reason <<<")
        val success = service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
        if (success) {
            lastActionTime = now
            _blockCount.value += 1
        }
        return success
    }

    companion object {
        private const val TAG = "NoMoreReels"
    }
}
