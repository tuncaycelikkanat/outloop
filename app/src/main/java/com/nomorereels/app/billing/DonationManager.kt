package com.nomorereels.app.billing

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.nomorereels.app.data.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object DonationUrls {
    /**
     * Türkiye için yerel kartlar (Troy, Bonus vb.) ve doğrudan TL banka transferi: Kreosus.
     */
    const val KREOSUS_URL = "https://kreosus.com/outloop"

    /**
     * Küresel / Yurt dışı kullanıcılar için Apple Pay & Kredi Kartı: Buy Me a Coffee.
     */
    const val BUYMEACOFFEE_URL = "https://buymeacoffee.com/outloop"

    /**
     * Açık kaynak geliştiriciler için %0 platform kesintili GitHub Sponsors.
     */
    const val GITHUB_SPONSORS_URL = "https://github.com/sponsors/tuncaycelikkanat"

    const val DEFAULT_DONATION_URL = KREOSUS_URL
}

object DonationProducts {
    const val PLATFORM_KREOSUS = "tip_kreosus"
    const val PLATFORM_BUYMEACOFFEE = "tip_buymeacoffee"
    const val PLATFORM_GITHUB = "tip_github"

    val ALL_PRODUCTS = listOf(
        PLATFORM_KREOSUS,
        PLATFORM_BUYMEACOFFEE,
        PLATFORM_GITHUB
    )
}

data class DonationItem(
    val productId: String,
    val iconEmoji: String,
    val titleTr: String,
    val titleEn: String,
    val currencyBadgeTr: String,
    val currencyBadgeEn: String,
    val subtitleTr: String,
    val subtitleEn: String,
    val targetUrl: String = DonationUrls.DEFAULT_DONATION_URL
) {
    fun localizedTitle(isEnglish: Boolean): String = if (isEnglish) titleEn else titleTr
    fun localizedBadge(isEnglish: Boolean): String = if (isEnglish) currencyBadgeEn else currencyBadgeTr
    fun localizedSubtitle(isEnglish: Boolean): String = if (isEnglish) subtitleEn else subtitleTr

    /** Geriye dönük uyumluluk */
    fun formattedPrice(isEnglish: Boolean): String = localizedBadge(isEnglish)
}

sealed interface DonationEvent {
    data class Success(val productId: String) : DonationEvent
    data class Error(val message: String) : DonationEvent
    object Canceled : DonationEvent
}

/**
 * %100 Çevrimdışı (Zero-Internet) mimariye sadık kalarak harici web bağlantıları
 * (Kreosus / BuyMeACoffee / GitHub Sponsors) üzerinden geliştiriciye destek akışını
 * yöneten hafif sınıf.
 */
class DonationManager(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _isConnected = MutableStateFlow(true)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _events = MutableSharedFlow<DonationEvent>()
    val events: SharedFlow<DonationEvent> = _events.asSharedFlow()

    val defaultItems: List<DonationItem> = listOf(
        DonationItem(
            productId = DonationProducts.PLATFORM_KREOSUS,
            iconEmoji = "🇹🇷",
            titleTr = "Kreosus",
            titleEn = "Kreosus",
            currencyBadgeTr = "TL (₺)",
            currencyBadgeEn = "TRY (₺)",
            subtitleTr = "Yerel kart & Havale",
            subtitleEn = "Local TR card & EFT",
            targetUrl = DonationUrls.KREOSUS_URL
        ),
        DonationItem(
            productId = DonationProducts.PLATFORM_BUYMEACOFFEE,
            iconEmoji = "☕",
            titleTr = "Buy Me a Coffee",
            titleEn = "Buy Me a Coffee",
            currencyBadgeTr = "$ / €",
            currencyBadgeEn = "$ / €",
            subtitleTr = "Apple Pay & Kart",
            subtitleEn = "Apple Pay & Cards",
            targetUrl = DonationUrls.BUYMEACOFFEE_URL
        ),
        DonationItem(
            productId = DonationProducts.PLATFORM_GITHUB,
            iconEmoji = "🐙",
            titleTr = "GitHub Sponsors",
            titleEn = "GitHub Sponsors",
            currencyBadgeTr = "%0 Kesinti",
            currencyBadgeEn = "0% Fee",
            subtitleTr = "Açık kaynak destek",
            subtitleEn = "Open source backer",
            targetUrl = DonationUrls.GITHUB_SPONSORS_URL
        )
    )

    fun startConnection() {
        _isConnected.value = true
    }

    fun launchDonationFlow(activity: Activity, productId: String) {
        val item = defaultItems.firstOrNull { it.productId == productId }
            ?: defaultItems.first()

        val url = item.targetUrl

        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            activity.startActivity(intent)

            SettingsStore.recordDonation(context)

            scope.launch {
                _events.emit(DonationEvent.Success(productId))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Tarayıcı açılamadı: ${e.message}")
            val isEnglish = SettingsStore.appLanguage.value == com.nomorereels.app.data.AppLanguage.EN
            val msg = if (isEnglish) "Could not open browser." else "Web tarayıcısı açılamadı."
            scope.launch {
                _events.emit(DonationEvent.Error(msg))
            }
        }
    }

    fun endConnection() {}

    companion object {
        private const val TAG = "NoMoreReels"
    }
}
