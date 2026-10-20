package com.nomorereels.app.ui.i18n

import androidx.compose.runtime.staticCompositionLocalOf
import com.nomorereels.app.data.AppLanguage
import com.nomorereels.app.policy.DmReelMode
import com.nomorereels.app.ui.theme.NmrThemeMode

val LocalAppStrings = staticCompositionLocalOf<Strings> { TurkishStrings }

object AppStrings {
    fun get(lang: AppLanguage): Strings = when (lang) {
        AppLanguage.TR -> TurkishStrings
        AppLanguage.EN -> EnglishStrings
    }
}

interface Strings {
    // Navigation Tabs (5 Hedefli Mimari)
    val tabToday: String
    val tabProtection: String
    val tabInsights: String
    val tabLog: String
    val tabSettings: String
    val tabRegions: String
    val tabQuota: String

    fun modeTitle(mode: DmReelMode): String
    fun modeSubtitle(mode: DmReelMode): String
    fun themeLabel(theme: NmrThemeMode): String

    // Core Protection Screen
    val appName: String
    val protectionActive: String
    val protectionInactive: String
    val generalModePrefix: String
    val reelsBlockedToday: String
    val noBlocksYetFocusActive: String
    fun secSaved(sec: Long): String
    fun minSaved(min: Long): String

    val serviceInactiveTitle: String
    val serviceInactiveDesc: String
    val openButton: String

    val snoozeBannerTitle: String
    fun snoozeBannerDesc(mins: Int): String
    val endSnoozeButton: String

    val whatHappensTitle: String
    val generalModeHeader: String
    val presetsHeader: String
    val takeSnoozeButton: String

    val blockAllTitle: String
    val blockAllSub: String
    val singleReelTitle: String
    val singleReelSub: String
    val countQuotaTitle: String
    val countQuotaSub: String
    val timeQuotaTitle: String
    val timeQuotaSub: String

    val presetBalanced: String
    val presetMindful: String

    // Regions Screen
    val regionsTitle: String
    val regionsDesc: String
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
    val modeSelectTitle: String
    val modeFollowGlobal: String

    // Quota Screen
    val quotaTitle: String
    val blockAllActiveTitle: String
    val blockAllActiveSub: String
    val blockAllActiveDesc: String
    val singleReelActiveTitle: String
    val singleReelActiveSub: String
    val singleReelActiveDesc: String
    val switchToQuotaMode: String
    val quotaActiveHeader: String
    val activeBadge: String
    val todayWatchedReels: String
    val todayTimeSpent: String
    val midnightResetNote: String
    val savedFocusHeader: String
    val savedFocusDesc: String
    val dailyReelLimitHeader: String
    val dailyTimeLimitHeader: String
    val warningThresholdTitle: String
    val singleReelTimeoutTitle: String
    val customInput: String

    // Settings Screen
    val settingsTitle: String
    val appearanceHeader: String
    val languageLabel: String
    val themeLabel: String
    val protectionHeader: String
    val bedtimeTitle: String
    val habitAnalysisTitle: String
    fun habitAnalysisText(hour: Int): String
    fun applyHabitButton(hour: Int): String
    val willpowerTitle: String
    val willpowerSub: String
    val batteryOptTitle: String
    val batteryOptActiveSub: String
    val batteryOptInactiveSub: String
    val batteryOptExemptBadge: String
    val batteryOptFixButton: String
    val backupHeader: String
    val backupExportTitle: String
    val backupExportSub: String
    val backupImportTitle: String
    val backupImportSub: String
    val backupExportSuccess: String
    val backupExportFail: String
    val backupImportSuccess: String
    val backupImportFail: String
    val developerHeader: String
    val liveLogsTitle: String
    val versionFooter: String

    // Mindful Gateway (Farkındalık Kapısı)
    val mindfulGatewayHeader: String
    val mindfulGatewayTitle: String
    val mindfulGatewaySub: String
    val barrierTypeTitle: String
    val barrierTypeBreathIntent: String
    val barrierTypeMath: String
    val barrierTypeTyping: String
    val nightCompassionTitle: String
    val nightCompassionSub: String
    val previewGatewayButton: String

    val gatewayPauseTitle: String
    val gatewayBreathPrompt: String
    val gatewayExitImmediate: String
    val gatewayIntentQuestion: String
    val gatewayIntentAutopilot: String
    val gatewayIntentBreak: String
    val gatewayIntentSpecific: String
    val gatewayAutopilotAck: String
    val gatewayPutPhoneDown: String
    val gatewayMindful5Min: String
    val gatewayMindful10Min: String
    val gatewayMicroPlanQuestion: String
    val gatewayMicroPlanWater: String
    val gatewayMicroPlanWork: String
    val gatewayMicroPlanSleep: String
    val gatewayMicroPlanWalk: String
    val gatewayNightTitle: String
    val gatewayNightSub: String
    val gatewayNightSleep: String
    val gatewayNight5Min: String
    val gatewayMathPrompt: String
    val gatewayTypingPrompt: String
    val gatewayTypingSentence: String
    val gatewayWindowExpiredTitle: String
    fun gatewayWindowExpiredDesc(plan: String): String

    val quotaSuffixMinutes: String
    val quotaSuffixSeconds: String
    val quotaConditionNote: String
    val customNumberFieldLabel: String
    val customTimeFieldLabel: String
    val numberRangeText: String

    // Dialogs
    val snoozeDialogTitle: String
    val snoozeDialogDesc: String
    val snooze15Min: String
    val snooze30Min: String
    val snooze60Min: String
    val cancel: String
    val save: String

    val customCountTitle: String
    val customCountDesc: String
    val customTimeTitle: String
    val customTimeDesc: String

    val willpowerDialogTitle: String
    val willpowerDialogDesc: String
    val willpowerDismiss: String
    val willpowerConfirm: String

    val bedtimeDialogTitle: String
    val bedtimeDialogDesc: String
    val bedtimeStartLabel: String
    val bedtimeEndLabel: String

    // Onboarding
    val privacyHeroTitle: String
    val privacyFeature1Title: String
    val privacyFeature1Sub: String
    val privacyFeature2Title: String
    val privacyFeature2Sub: String
    val privacyFeature3Title: String
    val privacyFeature3Sub: String

    val accessibilityStepTitle: String
    val accessibilityStepSub: String
    val step1Text: String
    val step2Text: String
    val step3Text: String
    val waitingPermission: String
    val permissionGranted: String

    val profileStepTitle: String
    val profileStepBalancedSub: String
    val profileStepMindfulSub: String
    val profileStepStrictSub: String
    val profileStepFooter: String

    val nextButton: String
    val backButton: String
    val startButton: String
    val openSettingsButton: String
}

object TurkishStrings : Strings {
    override val tabToday = "Bugün"
    override val tabProtection = "Koruma"
    override val tabInsights = "İçgörü"
    override val tabLog = "Günlük"
    override val tabSettings = "Ayarlar"
    override val tabRegions = "Bölgeler"
    override val tabQuota = "Kota & Süre"

    override fun modeTitle(mode: DmReelMode) = when (mode) {
        DmReelMode.FOLLOW_GLOBAL -> modeFollowGlobal
        DmReelMode.BLOCK_ALL -> blockAllTitle
        DmReelMode.SINGLE_REEL -> singleReelTitle
        DmReelMode.COUNT_QUOTA -> countQuotaTitle
        DmReelMode.TIME_QUOTA -> timeQuotaTitle
        DmReelMode.ASK_WITH_FRICTION -> "5 sn Onaylı"
        DmReelMode.FREE -> "Serbest"
    }

    override fun modeSubtitle(mode: DmReelMode) = when (mode) {
        DmReelMode.FOLLOW_GLOBAL -> "Ana ekrandaki genel koruma modunu takip eder."
        DmReelMode.BLOCK_ALL -> blockAllSub
        DmReelMode.SINGLE_REEL -> singleReelSub
        DmReelMode.COUNT_QUOTA -> countQuotaSub
        DmReelMode.TIME_QUOTA -> timeQuotaSub
        DmReelMode.ASK_WITH_FRICTION -> "5 saniye bekleme kilidi"
        DmReelMode.FREE -> "Korumasız serbest izleme"
    }

    override fun themeLabel(theme: NmrThemeMode) = theme.labelTr

    override val appName = "No More Reels"
    override val protectionActive = "Koruma aktif"
    override val protectionInactive = "Koruma kapalı"
    override val generalModePrefix = "Genel mod:"
    override val reelsBlockedToday = "Reel bugün engellendi"
    override val noBlocksYetFocusActive = "Henüz engelleme yok · Odak aktif"
    override fun secSaved(sec: Long) = "$sec sn kurtarıldı"
    override fun minSaved(min: Long) = "$min dk kurtarıldı"

    override val serviceInactiveTitle = "Koruma Servisi Kapalı"
    override val serviceInactiveDesc = "Reels engellemenin çalışması için erişilebilirlik servisini açın."
    override val openButton = "Aç"

    override val snoozeBannerTitle = "MOLA MODU DEVREDE"
    override fun snoozeBannerDesc(mins: Int) = "Kalan: $mins dk · Süre bitince eski koruma otomatik dönecektir."
    override val endSnoozeButton = "Molayı Bitir"

    override val whatHappensTitle = "INSTAGRAM'DA NE OLACAK?"
    override val generalModeHeader = "GENEL MOD"
    override val presetsHeader = "HAZIR PROFİLLER"
    override val takeSnoozeButton = "☕ Süreli Mola Ver (15-60 dk)"

    override val blockAllTitle = "Komple Yasak"
    override val blockAllSub = "Sıfır video"
    override val singleReelTitle = "Sadece Bir Tane"
    override val singleReelSub = "İlk video serbest, kaydırınca kapanır"
    override val countQuotaTitle = "X Tane"
    override val countQuotaSub = "Günlük adet kadar"
    override val timeQuotaTitle = "X Dakika"
    override val timeQuotaSub = "Günlük süre kadar"

    override val presetBalanced = "Dengeli"
    override val presetMindful = "Bilinçli"

    override val regionsTitle = "Bölgeler"
    override val regionsDesc = "Her bölge için Instagram Reels davranışını özelleştirin."
    override val dmTitle = "DM / Mesajlar"
    override val dmSub = "Sohbetten gelen Reels videoları"
    override val exploreTitle = "Keşfet Izgarası"
    override val exploreSub = "Keşfet sayfasındaki videolar"
    override val feedTitle = "Ana Sayfa / Akış"
    override val feedSub = "Akışta gezinirken açılan Reels"
    override val profileTitle = "Profil"
    override val profileSub = "Kullanıcı profillerindeki videolar"
    override val reelsTabTitle = "Alt Bar Reels Sekmesi"
    override val reelsTabSub = "Doğrudan Reels sekmesine basıldığında"
    override val modeSelectTitle = "Mod seç"
    override val modeFollowGlobal = "Genel moda uy"

    override val quotaTitle = "Kota & Süre"
    override val blockAllActiveTitle = "KOMPLE YASAK DEVREDE"
    override val blockAllActiveSub = "Sıfır video politikası aktif"
    override val blockAllActiveDesc = "Tüm Reels girişleri koşulsuz engellenir. Günlük kota sayaçları yerine tam engelleme koruması geçerlidir."
    override val singleReelActiveTitle = "SADECE BİR TANE MODU DEVREDE"
    override val singleReelActiveSub = "Anlık tekil video kuralı aktif"
    override val singleReelActiveDesc = "Açılan ilk video serbest bırakılır; 2. videoya kaydırdığınız an engellenir. Günlük kota yerine anlık kaydırma koruması devrededir."
    override val switchToQuotaMode = "Günlük Kota Moduna Geç (X Tane)"
    override val quotaActiveHeader = "GÜNLÜK KOTA DEVREDE"
    override val activeBadge = "Aktif"
    override val todayWatchedReels = "Bugün izlenen Reel"
    override val todayTimeSpent = "Bugün geçirilen süre"
    override val midnightResetNote = "Her gece 00:00'da sayaçlar otomatik sıfırlanır"
    override val savedFocusHeader = "KURTARILAN ODAK SÜRESİ"
    override val savedFocusDesc = "Engellenen her Reel için ortalama 45 sn kurtarılmış sayılır"
    override val dailyReelLimitHeader = "GÜNLÜK REEL SINIRI"
    override val dailyTimeLimitHeader = "GÜNLÜK SÜRE SINIRI"
    override val warningThresholdTitle = "Uyarı eşiği"
    override val singleReelTimeoutTitle = "Tek Reel süre sınırı"
    override val customInput = "Özel"

    override val settingsTitle = "Ayarlar"
    override val appearanceHeader = "GÖRÜNÜM"
    override val languageLabel = "Dil"
    override val themeLabel = "Tema"
    override val protectionHeader = "KORUMA"
    override val bedtimeTitle = "Gece Odak Saati"
    override val habitAnalysisTitle = "ALIŞKANLIK ANALİZİ"
    override fun habitAnalysisText(hour: Int) = "Isı haritası verilerine göre en çok gece ${String.format("%02d:00", hour)} saatlerinde Reels açıyorsun. Gece Odak Saatini bu aralığa güncellemek ister misin?"
    override fun applyHabitButton(hour: Int) = "Öneriyi Uygula (${String.format("%02d:00", hour)} – 07:00)"
    override val willpowerTitle = "İrade Kilidi"
    override val willpowerSub = "Gevşetmeden önce 5 sn bekle"
    override val batteryOptTitle = "Pil Optimizasyonu Koruması"
    override val batteryOptActiveSub = "Muafiyet aktif · Servis arka planda kapatılamaz"
    override val batteryOptInactiveSub = "Korumalı değil · Sistem servisi kapatabilir (Dokun)"
    override val batteryOptExemptBadge = "Muaf"
    override val batteryOptFixButton = "Muaf Tut"
    override val backupHeader = "YEDEKLEME VE VERİ"
    override val backupExportTitle = "Ayarları Dosyaya Yedekle"
    override val backupExportSub = "İnternet gerekmez · JSON formatında cihaza kaydet"
    override val backupImportTitle = "Ayarları Dosyadan Geri Yükle"
    override val backupImportSub = "Daha önce aldığın JSON yedeğini yükle"
    override val backupExportSuccess = "Ayarlar başarıyla dışa aktarıldı (JSON)"
    override val backupExportFail = "Yedekleme başarısız oldu"
    override val backupImportSuccess = "Ayarlar başarıyla geri yüklendi"
    override val backupImportFail = "Ayar dosyası geçersiz veya bozuk"
    override val developerHeader = "GELİŞTİRİCİ"
    override val liveLogsTitle = "Canlı Olay Günlüğü"
    override val versionFooter = "Sürüm 1.0 · Açık kaynak"

    // Mindful Gateway (Farkındalık Kapısı)
    override val mindfulGatewayHeader = "FARKINDALIK KAPISI"
    override val mindfulGatewayTitle = "Farkındalık Kapısı"
    override val mindfulGatewaySub = "Sert engel yerine duraklama, niyet aynası ve bilinçli pencere sunar."
    override val barrierTypeTitle = "Bariyer Türü"
    override val barrierTypeBreathIntent = "Nefes & Niyet (Önerilen)"
    override val barrierTypeMath = "Zihinsel Kıvılcım (Matematik)"
    override val barrierTypeTyping = "Bilinçli Cümle (Katı Mod)"
    override val nightCompassionTitle = "Gece Şefkat Modu"
    override val nightCompassionSub = "23:00'ten sonra test sormaz; yormadan uyku odaklı seçim sunar."
    override val previewGatewayButton = "Farkındalık Kapısını Deneyimle"

    override val gatewayPauseTitle = "Kısa bir duraklama."
    override val gatewayBreathPrompt = "Derin bir nefes al... ve bırak."
    override val gatewayExitImmediate = "Şimdilik Çık"
    override val gatewayIntentQuestion = "Şu an neye ihtiyacın var?"
    override val gatewayIntentAutopilot = "Sadece otomatik açtım / Boşluğuma geldi"
    override val gatewayIntentBreak = "Kısa bir zihin molası arıyorum"
    override val gatewayIntentSpecific = "Belirli bir şeye bakmam gerek"
    override val gatewayAutopilotAck = "Fark etmek en büyük adımdır. Kontrol sende."
    override val gatewayPutPhoneDown = "Telefonu Bırak"
    override val gatewayMindful5Min = "5 Dk Bilinçli Pencere"
    override val gatewayMindful10Min = "10 Dk Bilinçli Pencere"
    override val gatewayMicroPlanQuestion = "Süre bitince ne yapacaksın?"
    override val gatewayMicroPlanWater = "Bir bardak su içeceğim"
    override val gatewayMicroPlanWork = "İşime / dersime döneceğim"
    override val gatewayMicroPlanSleep = "Uyumaya gideceğim"
    override val gatewayMicroPlanWalk = "Kalkıp biraz esneyeceğim"
    override val gatewayNightTitle = "Günün geri kalanı senin."
    override val gatewayNightSub = "Yorgun bir zihni zorlamak yerine dinlenmek iyi gelecektir."
    override val gatewayNightSleep = "Ekranı Kapat / Uyu"
    override val gatewayNight5Min = "5 Dakika Bakıp Kapatacağım"
    override val gatewayMathPrompt = "Zihnini toplamak için soruyu çöz:"
    override val gatewayTypingPrompt = "Niyetini sabitlemek için yaz:"
    override val gatewayTypingSentence = "Şu an dikkatimi ve zamanımı bilinçli harcamayı seçiyorum."
    override val gatewayWindowExpiredTitle = "Bilinçli Penceren Doldu ⏱️"
    override fun gatewayWindowExpiredDesc(plan: String) = if (plan.isNotBlank()) "Planladığın gibi: $plan" else "Bilinçli süren tamamlandı."

    override val quotaSuffixMinutes = "dk"
    override val quotaSuffixSeconds = "sn"
    override val quotaConditionNote = " (Kota açıldığında geçerlidir)"
    override val customNumberFieldLabel = "Günlük Reel sınırı"
    override val customTimeFieldLabel = "Günlük süre sınırı (dk)"
    override val numberRangeText = "1 – 9999"

    override val snoozeDialogTitle = "Süreli Mola Ver"
    override val snoozeDialogDesc = "Seçtiğin süre boyunca koruma gevşetilir. Süre bitince tüm kurallar otomatik olarak eski sıkı haline geri döner."
    override val snooze15Min = "15 Dakika (Hızlı mola)"
    override val snooze30Min = "30 Dakika (Kahve molası)"
    override val snooze60Min = "60 Dakika (Akşam serbestliği)"
    override val cancel = "İptal"
    override val save = "Kaydet"

    override val customCountTitle = "Özel sayı gir"
    override val customCountDesc = "Günlük en fazla kaç Reel izlenebilsin?"
    override val customTimeTitle = "Özel dakika gir"
    override val customTimeDesc = "Günlük en fazla kaç dakika Reels izlenebilsin?"

    override val willpowerDialogTitle = "İrade Kilidi"
    override val willpowerDialogDesc = "Emin misin? Korumayı şimdi gevşetmek istiyor musun?"
    override val willpowerDismiss = "Vazgeç"
    override val willpowerConfirm = "Yine de gevşet"

    override val bedtimeDialogTitle = "Gece Odak Saati"
    override val bedtimeDialogDesc = "Bu saatler arasında tüm Instagram Reels videoları istisnasız engellenir:"
    override val bedtimeStartLabel = "Başlangıç (Uyku):"
    override val bedtimeEndLabel = "Bitiş (Uyanış):"

    override val privacyHeroTitle = "Sıfır internet. Sıfır takip."
    override val privacyFeature1Title = "İnternet izni bile yok"
    override val privacyFeature1Sub = "Uygulama dışarıya tek bayt gönderemez."
    override val privacyFeature2Title = "Verin telefonundan asla çıkmaz"
    override val privacyFeature2Sub = "Sunucu yok, telemetri yok, hesap yok."
    override val privacyFeature3Title = "Açık kaynak ve şeffaf"
    override val privacyFeature3Sub = "Kodun tamamını GitHub'da inceleyebilirsin."

    override val accessibilityStepTitle = "Tek izin: Erişilebilirlik"
    override val accessibilityStepSub = "Yalnızca Instagram'da hangi ekranda olduğunu anlar. Mesajlarını veya içeriğini okumaz."
    override val step1Text = "Ayarlar'ı aç"
    override val step2Text = "No More Reels'i seç"
    override val step3Text = "Servisi aç"
    override val waitingPermission = "İzin bekleniyor"
    override val permissionGranted = "İzin verildi"

    override val profileStepTitle = "Bir profil seç"
    override val profileStepBalancedSub = "Günlük kotayla dengeli izleme"
    override val profileStepMindfulSub = "Gevşetmek için 5 sn basılı tutma"
    override val profileStepStrictSub = "Sıfır video"
    override val profileStepFooter = "Sonra istediğin gibi değiştirebilirsin."

    override val nextButton = "Devam"
    override val backButton = "Geri"
    override val startButton = "Başla"
    override val openSettingsButton = "Erişilebilirlik ayarlarını aç"
}

object EnglishStrings : Strings {
    override val tabToday = "Today"
    override val tabProtection = "Protection"
    override val tabInsights = "Insights"
    override val tabLog = "Log"
    override val tabSettings = "Settings"
    override val tabRegions = "Regions"
    override val tabQuota = "Quota & Time"

    override fun modeTitle(mode: DmReelMode) = when (mode) {
        DmReelMode.FOLLOW_GLOBAL -> modeFollowGlobal
        DmReelMode.BLOCK_ALL -> blockAllTitle
        DmReelMode.SINGLE_REEL -> singleReelTitle
        DmReelMode.COUNT_QUOTA -> countQuotaTitle
        DmReelMode.TIME_QUOTA -> timeQuotaTitle
        DmReelMode.ASK_WITH_FRICTION -> "5s Hold"
        DmReelMode.FREE -> "Free"
    }

    override fun modeSubtitle(mode: DmReelMode) = when (mode) {
        DmReelMode.FOLLOW_GLOBAL -> "Follows general protection mode on home tab."
        DmReelMode.BLOCK_ALL -> blockAllSub
        DmReelMode.SINGLE_REEL -> singleReelSub
        DmReelMode.COUNT_QUOTA -> countQuotaSub
        DmReelMode.TIME_QUOTA -> timeQuotaSub
        DmReelMode.ASK_WITH_FRICTION -> "5-second hold to unlock"
        DmReelMode.FREE -> "Unrestricted Reels playback"
    }

    override fun themeLabel(theme: NmrThemeMode) = theme.labelEn

    override val appName = "No More Reels"
    override val protectionActive = "Protection active"
    override val protectionInactive = "Protection off"
    override val generalModePrefix = "General mode:"
    override val reelsBlockedToday = "Reels blocked today"
    override val noBlocksYetFocusActive = "No blocks yet · Focus active"
    override fun secSaved(sec: Long) = "$sec sec saved"
    override fun minSaved(min: Long) = "$min min saved"

    override val serviceInactiveTitle = "Protection Service Inactive"
    override val serviceInactiveDesc = "Enable accessibility service to start blocking Reels."
    override val openButton = "Enable"

    override val snoozeBannerTitle = "SNOOZE MODE ACTIVE"
    override fun snoozeBannerDesc(mins: Int) = "Remaining: $mins min · Previous rules restore automatically."
    override val endSnoozeButton = "End Break"

    override val whatHappensTitle = "WHAT HAPPENS IN INSTAGRAM?"
    override val generalModeHeader = "GENERAL MODE"
    override val presetsHeader = "PRESETS"
    override val takeSnoozeButton = "☕ Take a Break (15-60 min)"

    override val blockAllTitle = "Block All"
    override val blockAllSub = "Zero videos"
    override val singleReelTitle = "Single Reel"
    override val singleReelSub = "First video allowed, closes on scroll"
    override val countQuotaTitle = "X Count"
    override val countQuotaSub = "Up to daily count"
    override val timeQuotaTitle = "X Minutes"
    override val timeQuotaSub = "Up to daily duration"

    override val presetBalanced = "Balanced"
    override val presetMindful = "Mindful"

    override val regionsTitle = "Regions"
    override val regionsDesc = "Customize Instagram Reels behavior for each individual area."
    override val dmTitle = "DM / Direct Messages"
    override val dmSub = "Reels received in conversations"
    override val exploreTitle = "Explore Grid"
    override val exploreSub = "Videos opened from the Explore page"
    override val feedTitle = "Home / Main Feed"
    override val feedSub = "Reels opened while scrolling your feed"
    override val profileTitle = "Profile"
    override val profileSub = "Videos on user profiles"
    override val reelsTabTitle = "Bottom Reels Tab"
    override val reelsTabSub = "Tapping the bottom navigation Reels tab"
    override val modeSelectTitle = "Select Mode"
    override val modeFollowGlobal = "Follow general mode"

    override val quotaTitle = "Quota & Time"
    override val blockAllActiveTitle = "BLOCK ALL ACTIVE"
    override val blockAllActiveSub = "Zero-video policy in effect"
    override val blockAllActiveDesc = "All Reels entries are unconditionally blocked. Total blocking applies instead of daily quota counters."
    override val singleReelActiveTitle = "SINGLE REEL MODE ACTIVE"
    override val singleReelActiveSub = "Single-video session rule active"
    override val singleReelActiveDesc = "First opened video is permitted; scrolling to the 2nd video triggers an immediate block."
    override val switchToQuotaMode = "Switch to Daily Quota Mode (X Count)"
    override val quotaActiveHeader = "DAILY QUOTA ACTIVE"
    override val activeBadge = "Active"
    override val todayWatchedReels = "Reels watched today"
    override val todayTimeSpent = "Time spent today"
    override val midnightResetNote = "Counters reset automatically every night at 00:00"
    override val savedFocusHeader = "RECLAIMED FOCUS TIME"
    override val savedFocusDesc = "Estimated 45 seconds saved for every blocked Reel"
    override val dailyReelLimitHeader = "DAILY REEL LIMIT"
    override val dailyTimeLimitHeader = "DAILY TIME LIMIT"
    override val warningThresholdTitle = "Alert threshold"
    override val singleReelTimeoutTitle = "Single Reel time limit"
    override val customInput = "Custom"

    override val settingsTitle = "Settings"
    override val appearanceHeader = "APPEARANCE"
    override val languageLabel = "Language"
    override val themeLabel = "Theme"
    override val protectionHeader = "PROTECTION"
    override val bedtimeTitle = "Bedtime Focus Hours"
    override val habitAnalysisTitle = "HABIT INSIGHT"
    override fun habitAnalysisText(hour: Int) = "Heatmap analytics reveal peak Reels opening attempts around ${String.format("%02d:00", hour)}. Would you like to schedule Bedtime Focus to this window?"
    override fun applyHabitButton(hour: Int) = "Apply Suggestion (${String.format("%02d:00", hour)} – 07:00)"
    override val willpowerTitle = "Willpower Lock"
    override val willpowerSub = "Wait 5s before loosening rules"
    override val batteryOptTitle = "Battery Optimization Protection"
    override val batteryOptActiveSub = "Exemption active · System cannot kill background service"
    override val batteryOptInactiveSub = "Not protected · System may kill background service (Tap)"
    override val batteryOptExemptBadge = "Exempt"
    override val batteryOptFixButton = "Exempt"
    override val backupHeader = "BACKUP & DATA"
    override val backupExportTitle = "Backup Settings to File"
    override val backupExportSub = "No internet required · Save as JSON to device"
    override val backupImportTitle = "Restore Settings from File"
    override val backupImportSub = "Restore from previously saved JSON file"
    override val backupExportSuccess = "Settings exported successfully (JSON)"
    override val backupExportFail = "Backup failed"
    override val backupImportSuccess = "Settings restored successfully"
    override val backupImportFail = "Settings file is invalid or corrupt"
    override val developerHeader = "DEVELOPER"
    override val liveLogsTitle = "Live Event Log"
    override val versionFooter = "Version 1.0 · Open Source"

    // Mindful Gateway (Farkındalık Kapısı)
    override val mindfulGatewayHeader = "MINDFUL GATEWAY"
    override val mindfulGatewayTitle = "Mindful Gateway"
    override val mindfulGatewaySub = "Offers pause, intent mirror, and conscious windows instead of hard blocks."
    override val barrierTypeTitle = "Barrier Type"
    override val barrierTypeBreathIntent = "Breath & Intent (Recommended)"
    override val barrierTypeMath = "Cognitive Spark (Math)"
    override val barrierTypeTyping = "Mindful Sentence (Strict Mode)"
    override val nightCompassionTitle = "Night Compassion Mode"
    override val nightCompassionSub = "No tests after 23:00; offers restful sleep-focused choices."
    override val previewGatewayButton = "Experience Mindful Gateway"

    override val gatewayPauseTitle = "A short pause."
    override val gatewayBreathPrompt = "Take a deep breath... and release."
    override val gatewayExitImmediate = "Exit for now"
    override val gatewayIntentQuestion = "What do you need right now?"
    override val gatewayIntentAutopilot = "Opened on autopilot / out of habit"
    override val gatewayIntentBreak = "Looking for a short mental break"
    override val gatewayIntentSpecific = "Need to check something specific"
    override val gatewayAutopilotAck = "Noticing is the biggest step. You are in control."
    override val gatewayPutPhoneDown = "Put Phone Down"
    override val gatewayMindful5Min = "5 Min Mindful Window"
    override val gatewayMindful10Min = "10 Min Mindful Window"
    override val gatewayMicroPlanQuestion = "What will you do when time is up?"
    override val gatewayMicroPlanWater = "Drink a glass of water"
    override val gatewayMicroPlanWork = "Return to work / study"
    override val gatewayMicroPlanSleep = "Go to sleep"
    override val gatewayMicroPlanWalk = "Get up and stretch"
    override val gatewayNightTitle = "The rest of the day is yours."
    override val gatewayNightSub = "Resting is better than pushing a tired mind."
    override val gatewayNightSleep = "Turn off screen / Sleep"
    override val gatewayNight5Min = "Watch 5 mins then close"
    override val gatewayMathPrompt = "Solve to refocus your mind:"
    override val gatewayTypingPrompt = "Type to anchor your intention:"
    override val gatewayTypingSentence = "I choose to spend my attention and time consciously right now."
    override val gatewayWindowExpiredTitle = "Mindful Window Expired ⏱️"
    override fun gatewayWindowExpiredDesc(plan: String) = if (plan.isNotBlank()) "As planned: $plan" else "Your mindful time is complete."

    override val quotaSuffixMinutes = "min"
    override val quotaSuffixSeconds = "sec"
    override val quotaConditionNote = " (Applies when quota is active)"
    override val customNumberFieldLabel = "Daily Reel limit"
    override val customTimeFieldLabel = "Daily time limit (min)"
    override val numberRangeText = "1 – 9999"

    override val snoozeDialogTitle = "Take a Break"
    override val snoozeDialogDesc = "Protection will be eased for the selected duration. Once time expires, rules automatically re-lock to their strict state."
    override val snooze15Min = "15 Minutes (Quick break)"
    override val snooze30Min = "30 Minutes (Coffee break)"
    override val snooze60Min = "60 Minutes (Evening free)"
    override val cancel = "Cancel"
    override val save = "Save"

    override val customCountTitle = "Enter custom number"
    override val customCountDesc = "What is the daily maximum Reels limit?"
    override val customTimeTitle = "Enter custom minutes"
    override val customTimeDesc = "What is the daily maximum watch time in minutes?"

    override val willpowerDialogTitle = "Willpower Lock"
    override val willpowerDialogDesc = "Are you sure? Do you want to loosen your protection right now?"
    override val willpowerDismiss = "Keep Focus"
    override val willpowerConfirm = "Loosen Anyway"

    override val bedtimeDialogTitle = "Bedtime Focus Hours"
    override val bedtimeDialogDesc = "All Instagram Reels are unconditionally blocked during these hours:"
    override val bedtimeStartLabel = "Bedtime (Sleep):"
    override val bedtimeEndLabel = "Wakeup (Morning):"

    override val privacyHeroTitle = "Zero internet. Zero tracking."
    override val privacyFeature1Title = "No internet permission at all"
    override val privacyFeature1Sub = "The app cannot send a single byte externally."
    override val privacyFeature2Title = "Your data never leaves your phone"
    override val privacyFeature2Sub = "No servers, no telemetry, no accounts."
    override val privacyFeature3Title = "Open source & transparent"
    override val privacyFeature3Sub = "Inspect the full codebase on GitHub."

    override val accessibilityStepTitle = "Only one permission: Accessibility"
    override val accessibilityStepSub = "Detects which screen is active in Instagram only. Never reads messages or private content."
    override val step1Text = "Open Settings"
    override val step2Text = "Select No More Reels"
    override val step3Text = "Turn on Service"
    override val waitingPermission = "Waiting for permission"
    override val permissionGranted = "Permission granted"

    override val profileStepTitle = "Choose a profile"
    override val profileStepBalancedSub = "Balanced viewing with daily limits"
    override val profileStepMindfulSub = "5s hold to loosen protection"
    override val profileStepStrictSub = "Zero videos"
    override val profileStepFooter = "You can change this anytime later."

    override val nextButton = "Next"
    override val backButton = "Back"
    override val startButton = "Start"
    override val openSettingsButton = "Open accessibility settings"
}
