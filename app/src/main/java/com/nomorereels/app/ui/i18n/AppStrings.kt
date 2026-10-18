package com.nomorereels.app.ui.i18n

import com.nomorereels.app.data.AppLanguage

object AppStrings {

    fun get(lang: AppLanguage): Strings = when (lang) {
        AppLanguage.TR -> TurkishStrings
        AppLanguage.EN -> EnglishStrings
    }
}

interface Strings {
    // Navigation Tabs
    val tabProtection: String
    val tabRegions: String
    val tabQuota: String
    val tabSettings: String

    // Header & Status
    val appSubtitle: String
    val serviceActive: String
    val serviceInactive: String
    val serviceActiveDesc: String
    val serviceInactiveDesc: String
    val openSettings: String
    val openInstagram: String

    // Core Protection Modes
    val protectionHeader: String
    val protectionSub: String
    val customConfig: String
    val blockAllTitle: String
    val blockAllDesc: String
    val singleReelTitle: String
    val singleReelDesc: String
    val countQuotaTitle: String
    val countQuotaDesc: String
    val timeQuotaTitle: String
    val timeQuotaDesc: String
    val balancedTitle: String
    val mindfulTitle: String

    // Quota & Stats
    val quotaHeader: String
    val quotaSub: String
    val todayWatched: String
    val timeSpent: String
    val estimatedSavedTime: String
    val totalBlockedReels: String
    val resetUsage: String
    val resetSuccess: String
    val targetLimitReels: String
    val targetTimeMinutes: String
    val customInput: String
    val customCountDialogTitle: String
    val customTimeDialogTitle: String
    val warningThreshold: String
    val warningThresholdDesc: String
    val singleReelTimeout: String
    val singleReelTimeoutDesc: String
    val save: String
    val cancel: String

    // Regional
    val regionsHeader: String
    val regionsSub: String
    val dmTitle: String
    val dmSub: String
    val exploreTitle: String
    val exploreSub: String
    val feedTitle: String
    val feedSub: String
    val profileTitle: String
    val profileSub: String
    val reelsTabTitle: String
    val reelsTabSub: String

    // Bedtime & Willpower
    val bedtimeTitle: String
    val bedtimeSub: String
    val bedtimeActiveNow: String
    val bedtimeWindow: String
    val edit: String
    val willpowerTitle: String
    val willpowerSub: String

    // Settings & Appearance
    val settingsHeader: String
    val languageTitle: String
    val themeTitle: String
    val punchHoleTitle: String
    val punchHoleSub: String
    val testPunchHole: String
    val diagnosticsTitle: String
    val clearLogs: String
    val noLogsYet: String
}

object TurkishStrings : Strings {
    override val tabProtection = "🛡️ Korumalar"
    override val tabRegions = "🗺️ Bölgeler"
    override val tabQuota = "📊 Kota & Süre"
    override val tabSettings = "⚙️ Ayarlar"

    override val appSubtitle = "Instagram Reels & Dikkat Yönetimi"
    override val serviceActive = "KORUMA AKTİF"
    override val serviceInactive = "KORUMA KAPALI"
    override val serviceActiveDesc = "Erişilebilirlik servisi çalışıyor; Reels akışları denetleniyor."
    override val serviceInactiveDesc = "Korumayı başlatmak için Erişilebilirlik Servisi'ni etkinleştirin."
    override val openSettings = "Erişilebilirlik Ayarları"
    override val openInstagram = "Instagram'ı Aç"

    override val protectionHeader = "Reels Koruma Modu"
    override val protectionSub = "Uygulanacak genel Reels politikanızı seçin:"
    override val customConfig = "Özel Yapılandırma"
    override val blockAllTitle = "🛑 Komple Yasak"
    override val blockAllDesc = "Tüm Reels alanları ve alt sekme tamamen engellenir. Sıfır video."
    override val singleReelTitle = "☝️ Sadece Bir Tane"
    override val singleReelDesc = "Açılan ilk Reel serbest; 2. videoya kaydırdığınız an anında kapatır."
    override val countQuotaTitle = "🎯 X Tane İzleme"
    override val countQuotaDesc = "Günlük belirlenen adet kadar izletir; kotanız dolunca kilitler."
    override val timeQuotaTitle = "⏱️ X Dakika İzleme"
    override val timeQuotaDesc = "Günlük belirlenen süre kadar izletir; süreniz dolunca kilitler."
    override val balancedTitle = "⚖️ Dengeli"
    override val mindfulTitle = "🧘 Bilinçli (5 Sn Onay)"

    override val quotaHeader = "Kota & Dijital Detoks İstatistikleri"
    override val quotaSub = "İzleme limitlerini kökleyin, kurtarılan zamanınızı görün:"
    override val todayWatched = "Bugün İzlenen"
    override val timeSpent = "Harcanan Süre"
    override val estimatedSavedTime = "Kurtarılan Zaman"
    override val totalBlockedReels = "Engellenen Toplam"
    override val resetUsage = "Sıfırla 🔄"
    override val resetSuccess = "Bugünkü Reels sayacı sıfırlandı."
    override val targetLimitReels = "Hedef Video Limiti"
    override val targetTimeMinutes = "Hedef Süre Limiti"
    override val customInput = "✏️ Özel"
    override val customCountDialogTitle = "Özel Reel Kotası Belirle"
    override val customTimeDialogTitle = "Özel Dakika Limiti Belirle"
    override val warningThreshold = "🔔 Uyarı Eşiği"
    override val warningThresholdDesc = "Kotanıza yaklaştığınızda kamera kapsülüyle bildirim verilir."
    override val singleReelTimeout = "⏳ Tek Reel İçi Süre Sınırı"
    override val singleReelTimeoutDesc = "Kaydırmadan tek bir videoda kalma sınırı."
    override val save = "Kaydet"
    override val cancel = "İptal"

    override val regionsHeader = "Bölgelere Göre Reels Ayarları"
    override val regionsSub = "Her bölge için uygulanacak davranışı ayrı ayrı özelleştirin:"
    override val dmTitle = "💬 DM / Mesajlar"
    override val dmSub = "Sohbetten gelen Reels videoları"
    override val exploreTitle = "🔍 Keşfet / Mercek"
    override val exploreSub = "Keşfet ızgarasından açılan videolar"
    override val feedTitle = "🏠 Ana Sayfa / Akış"
    override val feedSub = "Ana akışta gezinirken açılan Reels oynatıcısı"
    override val profileTitle = "👤 Profil"
    override val profileSub = "Kullanıcı profilinden açılan Reels videoları"
    override val reelsTabTitle = "🎬 Alt Bar Reels Sekmesi"
    override val reelsTabSub = "Doğrudan alt menüdeki Reels sekmesine dokunulduğunda"

    override val bedtimeTitle = "🌙 Gece Odak Saati"
    override val bedtimeSub = "Uyku saatlerinizde tüm Reels videolarını koşulsuz kilitler."
    override val bedtimeActiveNow = "ŞU AN AKTİF"
    override val bedtimeWindow = "Saat Aralığı:"
    override val edit = "Düzenle ✏️"
    override val willpowerTitle = "🔒 İrade Kilidi (Bilinçli Gevşetme)"
    override val willpowerSub = "Korumayı zayıflatırken veya serbest bırakırken 5 sn bekleme onayı ister."

    override val settingsHeader = "Görünüm & Özel Donanım Ayarları"
    override val languageTitle = "🌐 Uygulama Dili"
    override val themeTitle = "🌓 Tema & Renk Modu"
    override val punchHoleTitle = "👁️ Nokta Kamera Kapsülü (Punch-Hole Cutout)"
    override val punchHoleSub = "Samsung Galaxy S22 gibi nokta kameralı ekranlar için kamera deliğinden genişleyen dinamik uyarı kapsülü."
    override val testPunchHole = "Nokta Kamera Animasyonunu Test Et 👁️"
    override val diagnosticsTitle = "📋 Canlı Teşhis ve Olay Günlüğü"
    override val clearLogs = "Temizle"
    override val noLogsYet = "Henüz olay kaydı yok."
}

object EnglishStrings : Strings {
    override val tabProtection = "🛡️ Protection"
    override val tabRegions = "🗺️ Regions"
    override val tabQuota = "📊 Quota & Time"
    override val tabSettings = "⚙️ Settings"

    override val appSubtitle = "Instagram Reels Guard & Wellbeing"
    override val serviceActive = "PROTECTION ACTIVE"
    override val serviceInactive = "PROTECTION OFF"
    override val serviceActiveDesc = "Accessibility service is running; inspecting Reels in real time."
    override val serviceInactiveDesc = "Enable Accessibility Service to activate Reels protection."
    override val openSettings = "Accessibility Settings"
    override val openInstagram = "Open Instagram"

    override val protectionHeader = "Reels Protection Mode"
    override val protectionSub = "Select your primary Reels protection policy:"
    override val customConfig = "Custom Config"
    override val blockAllTitle = "🛑 Strict Detox"
    override val blockAllDesc = "All Reels viewer areas and bottom tab locked. Zero videos."
    override val singleReelTitle = "☝️ Single Reel Only"
    override val singleReelDesc = "First Reel allowed; swiping to 2nd video immediately exits."
    override val countQuotaTitle = "🎯 X Count Quota"
    override val countQuotaDesc = "Watch up to daily count limit; blocks when quota runs out."
    override val timeQuotaTitle = "⏱️ X Minute Quota"
    override val timeQuotaDesc = "Watch up to daily time limit; blocks when time expires."
    override val balancedTitle = "⚖️ Balanced"
    override val mindfulTitle = "🧘 Mindful (5s Hold)"

    override val quotaHeader = "Quota & Focus Analytics"
    override val quotaSub = "Tune your limits to the max and monitor saved focus time:"
    override val todayWatched = "Today Watched"
    override val timeSpent = "Time Spent"
    override val estimatedSavedTime = "Saved Focus"
    override val totalBlockedReels = "Total Blocked"
    override val resetUsage = "Reset 🔄"
    override val resetSuccess = "Today's Reels counter reset."
    override val targetLimitReels = "Target Reel Limit"
    override val targetTimeMinutes = "Target Duration Limit"
    override val customInput = "✏️ Custom"
    override val customCountDialogTitle = "Set Custom Reel Limit"
    override val customTimeDialogTitle = "Set Custom Minute Limit"
    override val warningThreshold = "🔔 Alert Threshold"
    override val warningThresholdDesc = "Notifies via camera cutout pill as you approach quota."
    override val singleReelTimeout = "⏳ Single Reel Timeout"
    override val singleReelTimeoutDesc = "Max time allowed on a single Reel without swiping."
    override val save = "Save"
    override val cancel = "Cancel"

    override val regionsHeader = "Regional Reels Rules"
    override val regionsSub = "Customize enforcement behavior individually for each area:"
    override val dmTitle = "💬 DM / Direct Messages"
    override val dmSub = "Reels sent by friends in chat"
    override val exploreTitle = "🔍 Explore Grid"
    override val exploreSub = "Videos opened from Explore grid"
    override val feedTitle = "🏠 Main Feed"
    override val feedSub = "Reels viewer opened from home feed"
    override val profileTitle = "👤 Profile"
    override val profileSub = "Reels opened from user profiles"
    override val reelsTabTitle = "🎬 Bottom Reels Tab"
    override val reelsTabSub = "Direct bottom navigation tab"

    override val bedtimeTitle = "🌙 Bedtime Focus Window"
    override val bedtimeSub = "Locks all Reels unconditionally during your sleep hours."
    override val bedtimeActiveNow = "ACTIVE NOW"
    override val bedtimeWindow = "Schedule Window:"
    override val edit = "Edit ✏️"
    override val willpowerTitle = "🔒 Willpower Lock (Mindful Delay)"
    override val willpowerSub = "Requires 5s confirmation when loosening or disabling rules."

    override val settingsHeader = "Appearance & Hardware Customization"
    override val languageTitle = "🌐 App Language"
    override val themeTitle = "🌓 Theme & Dark Mode"
    override val punchHoleTitle = "👁️ Punch-Hole Dynamic Pill"
    override val punchHoleSub = "Sleek animated cutout capsule tailored for Galaxy S22 & punch-hole screens."
    override val testPunchHole = "Test Punch-Hole Animation 👁️"
    override val diagnosticsTitle = "📋 Live Event Diagnostics"
    override val clearLogs = "Clear"
    override val noLogsYet = "No logs recorded yet."
}
