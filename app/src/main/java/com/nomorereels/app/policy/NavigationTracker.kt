package com.nomorereels.app.policy

import com.nomorereels.app.detection.ScreenType

/**
 * Kullanıcının gezindiği son ekranları tutan hafif durum makinesi.
 * 
 * Amaç:
 * 1. Olay gürültüsünü süzmek (Kaydırma/animasyon sırasında yüzlerce aynı olayın gelmesini engeller).
 * 2. 'UNKNOWN' ekranları son bilinen geçerli durumla koruyarak kullanıcıya anlamsız 'UNKNOWN' göstermez.
 * 3. Yalnızca gerçek bir ekran DEĞİŞİMİ olduğunda (örn. Feed -> Keşfet) bildirim üretir.
 */
class NavigationTracker {

    var currentScreen: ScreenType = ScreenType.MAIN_FEED
        private set

    var previousScreen: ScreenType = ScreenType.MAIN_FEED
        private set

    /**
     * Yeni algılanan ekran türünü değerlendirir.
     * @return Gerçek bir ekran türü değişimi olduysa `true`, aynı ekranda kalındıysa `false` döner.
     */
    fun update(newScreen: ScreenType): Boolean {
        // UNKNOWN geldiyse mevcut ekran türünü koru (bilinmeyen alt olaylar ekranı bozamaz)
        if (newScreen == ScreenType.UNKNOWN) {
            return false
        }

        if (newScreen != currentScreen) {
            previousScreen = currentScreen
            currentScreen = newScreen
            return true
        }

        return false
    }

    fun reset() {
        currentScreen = ScreenType.MAIN_FEED
        previousScreen = ScreenType.MAIN_FEED
    }
}
