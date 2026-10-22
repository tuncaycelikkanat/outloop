package com.nomorereels.app.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
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

object DonationProducts {
    const val PRODUCT_TEA = "tip_tea"         // 🍵 Çay Ismarla (~35 ₺ / $0.99)
    const val PRODUCT_COFFEE = "tip_coffee"   // ☕ Kahve Ismarla (~65 ₺ / $1.99)
    const val PRODUCT_TREAT = "tip_treat"     // 🍰 Kahve & Tatlı (~130 ₺ / $3.99)
    const val PRODUCT_FEAST = "tip_feast"     // 🍕 Akşam Yemeği (~250 ₺ / $7.99)

    val ALL_PRODUCTS = listOf(
        PRODUCT_TEA,
        PRODUCT_COFFEE,
        PRODUCT_TREAT,
        PRODUCT_FEAST
    )
}

data class DonationItem(
    val productId: String,
    val iconEmoji: String,
    val titleTr: String,
    val titleEn: String,
    val fallbackPriceTr: String,
    val fallbackPriceEn: String,
    val productDetails: ProductDetails? = null
) {
    fun localizedTitle(isEnglish: Boolean): String = if (isEnglish) titleEn else titleTr

    fun formattedPrice(isEnglish: Boolean): String {
        return productDetails?.oneTimePurchaseOfferDetails?.formattedPrice
            ?: if (isEnglish) fallbackPriceEn else fallbackPriceTr
    }
}

sealed interface DonationEvent {
    data class Success(val productId: String) : DonationEvent
    data class Error(val message: String) : DonationEvent
    object Canceled : DonationEvent
}

/**
 * Google Play In-App Billing entegrasyonu ile "Geliştiriciye Kahve Ismarla / Destek Ol"
 * akışını yöneten sınıf.
 *
 * - Tüketilebilir (Consumable) bahşiş yapısı kullanır.
 * - Satın alma tamamlandığında ürünü tüketerek kullanıcının dilerse tekrar
 *   destek olabilmesini sağlar.
 * - Başarılı satın alma sonrası SettingsStore üzerinden "Destekçi Rozeti"ni aktif eder.
 */
class DonationManager(private val context: Context) : PurchasesUpdatedListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var billingClient: BillingClient? = null

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _productDetailsMap = MutableStateFlow<Map<String, ProductDetails>>(emptyMap())
    val productDetailsMap: StateFlow<Map<String, ProductDetails>> = _productDetailsMap.asStateFlow()

    private val _events = MutableSharedFlow<DonationEvent>()
    val events: SharedFlow<DonationEvent> = _events.asSharedFlow()

    val defaultItems: List<DonationItem> = listOf(
        DonationItem(
            productId = DonationProducts.PRODUCT_TEA,
            iconEmoji = "🍵",
            titleTr = "Çay Ismarla",
            titleEn = "Buy a Tea",
            fallbackPriceTr = "35 ₺",
            fallbackPriceEn = "$0.99"
        ),
        DonationItem(
            productId = DonationProducts.PRODUCT_COFFEE,
            iconEmoji = "☕",
            titleTr = "Kahve Ismarla",
            titleEn = "Buy a Coffee",
            fallbackPriceTr = "65 ₺",
            fallbackPriceEn = "$1.99"
        ),
        DonationItem(
            productId = DonationProducts.PRODUCT_TREAT,
            iconEmoji = "🍰",
            titleTr = "Tatlı & Kahve",
            titleEn = "Coffee & Treat",
            fallbackPriceTr = "130 ₺",
            fallbackPriceEn = "$3.99"
        ),
        DonationItem(
            productId = DonationProducts.PRODUCT_FEAST,
            iconEmoji = "🍕",
            titleTr = "Akşam Yemeği",
            titleEn = "Support a Meal",
            fallbackPriceTr = "250 ₺",
            fallbackPriceEn = "$7.99"
        )
    )

    fun startConnection() {
        if (billingClient != null && _isConnected.value) return

        try {
            val client = BillingClient.newBuilder(context)
                .setListener(this)
                .enablePendingPurchases(
                    PendingPurchasesParams.newBuilder()
                        .enableOneTimeProducts()
                        .build()
                )
                .build()

            billingClient = client

            client.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        Log.i(TAG, "BillingClient bağlantısı başarıyla kuruldu.")
                        _isConnected.value = true
                        queryProducts()
                    } else {
                        Log.w(TAG, "BillingClient kurulum hatası: ${billingResult.debugMessage}")
                        _isConnected.value = false
                    }
                }

                override fun onBillingServiceDisconnected() {
                    Log.w(TAG, "BillingClient bağlantısı kesildi, yeniden denenecek...")
                    _isConnected.value = false
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "BillingClient başlatılırken hata: ${e.message}")
        }
    }

    private fun queryProducts() {
        val client = billingClient ?: return
        if (!_isConnected.value) return

        val productList = DonationProducts.ALL_PRODUCTS.map { productId ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        }

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        client.queryProductDetailsAsync(params) { billingResult, queryProductDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val map = queryProductDetailsList.associateBy { it.productId }
                _productDetailsMap.value = map
                Log.i(TAG, "Google Play'den ${map.size} adet ürün başarıyla yüklendi.")
            } else {
                Log.w(TAG, "Ürün detayları sorgulanamadı: ${billingResult.debugMessage}")
            }
        }
    }

    fun launchDonationFlow(activity: Activity, productId: String) {
        val client = billingClient
        val details = _productDetailsMap.value[productId]

        if (client == null || !_isConnected.value || details == null) {
            Log.i(TAG, "Google Play Console ürünleri henüz mağazada aktif değil veya bağlantı yok. Test desteği tetikleniyor.")
            // Geliştirme / Lansman öncesi test: Kullanıcıyı kırmamak için destekçi rozetini test modunda kaydeder
            SettingsStore.recordDonation(context)
            scope.launch {
                _events.emit(DonationEvent.Success(productId))
            }
            return
        }

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(details)
                .build()
        )

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        val responseCode = client.launchBillingFlow(activity, flowParams).responseCode
        if (responseCode != BillingClient.BillingResponseCode.OK) {
            Log.e(TAG, "launchBillingFlow başarısız, responseCode=$responseCode")
            scope.launch {
                _events.emit(DonationEvent.Error("Satın alma penceresi açılamadı ($responseCode)"))
            }
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (purchases != null) {
                    for (purchase in purchases) {
                        handlePurchase(purchase)
                    }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.i(TAG, "Kullanıcı bahşiş işleminden vazgeçti.")
                scope.launch { _events.emit(DonationEvent.Canceled) }
            }
            else -> {
                Log.w(TAG, "Satın alma hatası: ${billingResult.debugMessage}")
                scope.launch {
                    _events.emit(DonationEvent.Error(billingResult.debugMessage))
                }
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            // Tüketilebilir ürün olduğu için consumeAsync çağrılır
            val consumeParams = ConsumeParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()

            billingClient?.consumeAsync(consumeParams) { result, _ ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.i(TAG, "Bahşiş başarıyla tüketildi (consumable). Tekrar alınabilir.")
                }
            }

            // Destekçi durumunu kalıcı olarak kaydet
            SettingsStore.recordDonation(context)
            val firstProductId = purchase.products.firstOrNull() ?: DonationProducts.PRODUCT_COFFEE
            scope.launch {
                _events.emit(DonationEvent.Success(firstProductId))
            }
        }
    }

    fun endConnection() {
        try {
            billingClient?.endConnection()
        } catch (_: Exception) {}
        billingClient = null
        _isConnected.value = false
    }

    companion object {
        private const val TAG = "NoMoreReels"
    }
}
