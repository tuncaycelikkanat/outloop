package com.nomorereels.app.service

import android.accessibilityservice.AccessibilityServiceInfo
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.view.accessibility.AccessibilityManager

/**
 * Erişilebilirlik servisinin canlılık durumunu, sistemde açık olup olmadığını ve
 * üretici pil optimizasyonlarının (Doze mode / Battery saver) servisi öldürme riskini
 * denetleyen sağlık bekçisi sınıfı.
 */
object ServiceHealthMonitor {

    /**
     * Servisin hem bellek içi StateFlow olarak hem de Android sistem ayarlarında
     * etkinleştirilmiş olup olmadığını denetler.
     */
    fun isServiceHealthy(context: Context): Boolean {
        if (!ReelsAccessibilityService.isServiceActive.value) {
            return false
        }
        return isAccessibilityPermissionGranted(context)
    }

    /**
     * Sistem ayarlarında servisin açık olup olmadığını doğrular.
     */
    fun isAccessibilityPermissionGranted(context: Context): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager ?: return false
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        val myPackage = context.packageName
        return enabledServices.any {
            val serviceInfo = it.resolveInfo?.serviceInfo
            serviceInfo != null && serviceInfo.packageName == myPackage
        }
    }

    /**
     * Uygulamanın pil optimizasyonlarından muaf tutulup tutulmadığını sorgular.
     * Samsung, Xiaomi, Huawei gibi cihazlarda muafiyet verilmezse servis arka planda öldürülür.
     */
    fun isBatteryOptimizationIgnored(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return true
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    /**
     * Kullanıcıyı pil optimizasyonu istisnası verme ekranına yönlendirir.
     */
    @SuppressLint("BatteryLife")
    fun requestIgnoreBatteryOptimization(context: Context) {
        // 1. Doğrudan paket bazlı istisna diyaloğu
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return
        } catch (_: Exception) {}

        // 2. Uygulama Detayları Sayfası (Pil ayarının doğrudan seçilebildiği yerel Android ekranı)
        try {
            val appDetailsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(appDetailsIntent)
            android.widget.Toast.makeText(
                context,
                "Pil ayarlarından 'Kısıtlanmamış' (Optimize Edilmemiş) seçin.",
                android.widget.Toast.LENGTH_LONG
            ).show()
            return
        } catch (_: Exception) {}

        // 3. Genel Pil Optimizasyonu Listesi
        try {
            val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallbackIntent)
            android.widget.Toast.makeText(
                context,
                "Outloop'u bulup 'Optimize Etme' seçeneğini işaretleyin.",
                android.widget.Toast.LENGTH_LONG
            ).show()
        } catch (_: Exception) {
            android.widget.Toast.makeText(context, "Pil ayarları açılamadı.", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}
