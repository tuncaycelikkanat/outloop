package com.nomorereels.app.policy

import com.nomorereels.app.detection.ReelContentFingerprint
import com.nomorereels.app.detection.ScreenType

/**
 * Kullanıcının gezindiği son ekranları, Reels oynatıcısının hangi ekrandan (DM vs Sekme/Keşfet/Akış)
 * açıldığını ve DM'den açılan tekil Reel oturumunda 2. videoya kaydırılıp kaydırılmadığını
 * takip eden durum makinesi.
 */
class NavigationTracker {

    var currentScreen: ScreenType = ScreenType.MAIN_FEED
        private set

    var previousScreen: ScreenType = ScreenType.MAIN_FEED
        private set

    /**
     * Reels dışındaki son bilinen ekran türü.
     * Kullanıcı REELS_VIEWER içindeyken peş peşe gelen olaylarda kökenin (örn. DIRECT_MESSAGES)
     * kaybolmamasını sağlar.
     */
    var lastNonReelsScreen: ScreenType = ScreenType.MAIN_FEED
        private set

    /**
     * Mevcut Reels oturumunun hangi ekrandan başlatıldığı.
     * Sadece REELS_VIEWER veya REELS_TAB aktifken anlamlıdır.
     */
    var reelOriginScreen: ScreenType? = null
        private set

    /**
     * DM'den açılan ilk Reel'in kimlik parmak izi (yazar, medya açıklaması, altyazı).
     */
    var initialReelFingerprint: ReelContentFingerprint = ReelContentFingerprint()
        private set

    var currentReelFingerprint: ReelContentFingerprint = ReelContentFingerprint()
        private set

    /**
     * Kullanıcı DM'den açtığı ilk Reel'den parmağını kaydırarak 2. bir Reel'e geçti mi?
     */
    var hasSwipedToNextReel: Boolean = false
        private set

    /**
     * Oturum içinde yeni bir Reel videosuna geçildiğinde tek seferlik true olan bayrak.
     */
    var isNewReelSwiped: Boolean = false
        private set

    /**
     * Mevcut Reel oturumunun başladığı zaman damgası (ms).
     */
    var reelSessionStartTimeMs: Long = 0L
        private set

    /**
     * "Onaylı (5 Sn Basılı Tut)" DM modunda kullanıcının mevcut tekil Reel için
     * bilinçli sürtünme onayını verip vermediği.
     */
    var isCurrentDmReelApproved: Boolean = false
        private set

    /**
     * Oynatıcının doğrudan DM (Direkt Mesajlar) ekranından açılmış bir REELS_VIEWER olup olmadığı.
     * Alt bardaki REELS_TAB sekmesine basılması hiçbir zaman DM kökenli sayılmaz.
     */
    val isDmOriginatedReel: Boolean
        get() = currentScreen == ScreenType.REELS_VIEWER &&
                reelOriginScreen == ScreenType.DIRECT_MESSAGES

    /**
     * Yeni algılanan ekran türünü, (varsa) aktif Reel parmak izini ve ekran ipucunu değerlendirir.
     * @return Gerçek bir ekran türü değişimi olduysa `true`, aynı ekranda kalındıysa `false` döner.
     */
    fun update(
        newScreen: ScreenType,
        reelFingerprint: ReelContentFingerprint = ReelContentFingerprint(),
        originHint: ScreenType? = null,
        nowMs: Long = System.currentTimeMillis()
    ): Boolean {
        // UNKNOWN geldiyse mevcut ekran türünü koru (bilinmeyen alt olaylar ekranı ve oturumu bozamaz)
        if (newScreen == ScreenType.UNKNOWN) {
            return false
        }

        val isChanged = newScreen != currentScreen

        if (isChanged) {
            previousScreen = currentScreen
            currentScreen = newScreen

            if (!newScreen.isReels) {
                // Reels dışındaki bir ekrana (DM, Ana Akış, Keşfet, Profil) geçildi:
                // Reel oturumunu sıfırla ve son güvenli ekranı güncelle.
                lastNonReelsScreen = newScreen
                clearReelSession()
            } else {
                // Reels ekranına (REELS_VIEWER veya REELS_TAB) yeni girildi:
                if (reelOriginScreen == null) {
                    reelOriginScreen = originHint ?: lastNonReelsScreen
                    reelSessionStartTimeMs = nowMs
                    initialReelFingerprint = reelFingerprint
                    currentReelFingerprint = reelFingerprint
                    hasSwipedToNextReel = false
                    isCurrentDmReelApproved = false
                } else if (newScreen == ScreenType.REELS_TAB) {
                    // Oynatıcıdan alt sekmedeki Reels akışına kayma durumu
                    hasSwipedToNextReel = true
                }
            }
        } else if (newScreen.isReels) {
            // Halihazırda REELS_VIEWER veya REELS_TAB içindeyiz; eğer köken henüz bilinmiyorsa hint ile tamamla
            if (reelOriginScreen == null && originHint != null) {
                reelOriginScreen = originHint
            }
            // parmak izi değişimiyle 2. Reel'e kaydırmayı kontrol et
            updateReelFingerprintInSession(reelFingerprint, nowMs)
        }

        return isChanged
    }

    private fun updateReelFingerprintInSession(
        newFingerprint: ReelContentFingerprint,
        nowMs: Long = System.currentTimeMillis()
    ) {
        if (newFingerprint.isEmpty) return

        currentReelFingerprint = newFingerprint

        if (initialReelFingerprint.isEmpty) {
            initialReelFingerprint = newFingerprint
            return
        }

        // Oturumun ilk 800ms'sinde gelen parmak izi değişimleri
        // (örneğin önceki Reel'in önbellekten kalan düğümü yerine yeni açılan Reel'in gelmesi)
        // kaydırma sayılmamalı, başlangıç parmak izi yeni Reel'e güncellenmelidir!
        if (nowMs - reelSessionStartTimeMs < INITIAL_FINGERPRINT_SETTLE_MS) {
            if (initialReelFingerprint.isDifferentReelFrom(newFingerprint)) {
                initialReelFingerprint = newFingerprint
            } else {
                initialReelFingerprint = initialReelFingerprint.mergeWith(newFingerprint)
            }
            return
        }

        if (initialReelFingerprint.isDifferentReelFrom(newFingerprint)) {
            hasSwipedToNextReel = true
            isNewReelSwiped = true
            initialReelFingerprint = newFingerprint
        } else {
            // Aynı Reel'in geç yüklenen alanlarını (örn. altyazı) tamamla
            initialReelFingerprint = initialReelFingerprint.mergeWith(newFingerprint)
        }
    }

    /**
     * REELS_VIEWER içindeki dikey ViewPager / RecyclerView kaydırma olayını (`TYPE_VIEW_SCROLLED`)
     * işler. Açılış animasyonu sırasındaki ilk sahte kaydırma olaylarını süzmek için
     * `SCROLL_SETTLE_WINDOW_MS` sonrasında gelen dikey sayfa kaydırmalarını 2. Reel geçişi sayar.
     */
    fun onReelPagerScrolled(
        nowMs: Long = System.currentTimeMillis(),
        settleWindowMs: Long = SCROLL_SETTLE_WINDOW_MS
    ): Boolean {
        if (currentScreen != ScreenType.REELS_VIEWER && currentScreen != ScreenType.REELS_TAB) return false
        if (reelSessionStartTimeMs <= 0L) return false

        val elapsed = nowMs - reelSessionStartTimeMs
        if (elapsed >= settleWindowMs) {
            hasSwipedToNextReel = true
            isNewReelSwiped = true
            return true
        }
        return false
    }

    fun checkAndClearNewReelSwiped(): Boolean {
        if (isNewReelSwiped) {
            isNewReelSwiped = false
            return true
        }
        return false
    }

    /**
     * "Onaylı (Bilinçli Sürtünme)" modunda kullanıcı 5 saniye basılı tutmayı tamamladığında
     * mevcut Reel'e tek seferlik izleme onayı verir ve süreyi başlatır.
     */
    fun approveCurrentDmReel(nowMs: Long = System.currentTimeMillis()) {
        if (currentScreen == ScreenType.REELS_VIEWER) {
            isCurrentDmReelApproved = true
            reelSessionStartTimeMs = nowMs
            hasSwipedToNextReel = false
            // Onay anındaki video parmak izini temel al
            if (!currentReelFingerprint.isEmpty) {
                initialReelFingerprint = currentReelFingerprint
            }
        }
    }

    /**
     * Tek Reel izleme süresinin (varsayılan 90 saniye) dolup dolmadığını kontrol eder.
     */
    fun isSingleReelTimeoutExpired(
        maxDurationMs: Long = DEFAULT_SINGLE_REEL_TIMEOUT_MS,
        nowMs: Long = System.currentTimeMillis()
    ): Boolean {
        if (!currentScreen.isReels || reelSessionStartTimeMs <= 0L) return false
        return (nowMs - reelSessionStartTimeMs) > maxDurationMs
    }

    fun markExitInitiated() {
        hasSwipedToNextReel = false
        isNewReelSwiped = false
    }

    private fun clearReelSession() {
        reelOriginScreen = null
        initialReelFingerprint = ReelContentFingerprint()
        currentReelFingerprint = ReelContentFingerprint()
        hasSwipedToNextReel = false
        isNewReelSwiped = false
        reelSessionStartTimeMs = 0L
        isCurrentDmReelApproved = false
    }

    fun reset() {
        currentScreen = ScreenType.MAIN_FEED
        previousScreen = ScreenType.MAIN_FEED
        lastNonReelsScreen = ScreenType.MAIN_FEED
        clearReelSession()
    }

    companion object {
        const val INITIAL_FINGERPRINT_SETTLE_MS = 800L
        const val SCROLL_SETTLE_WINDOW_MS = 1100L
        const val DEFAULT_SINGLE_REEL_TIMEOUT_MS = 90_000L
    }
}
