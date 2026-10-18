# No More Reels — GSD Core Sistem Şartnamesi (SPEC)

## 1. Proje Amacı ve Kimliği
"No More Reels", yalnızca Instagram Reels dikey video akışını engelleyip DM, Keşfet ızgarası ve normal ana akış gönderilerini kullanılabilir bırakan, %100 çevrimdışı bir Android dijital sağlık (digital wellbeing) uygulamasıdır.

## 2. Sarsılmaz Temel Kurallar (Non-Negotiable Invariants)
1. **%100 Çevrimdışı (Zero-Internet):**
   - `AndroidManifest.xml` içinde asla `android.permission.INTERNET` izni yer alamaz.
   - Hiçbir ağ kütüphanesi (OkHttp, Retrofit vb.) veya telemetri servisi eklenemez.
2. **Tasarım Sistemi (Flat Material 3):**
   - `tonalElevation = 0.dp`, `shadowElevation = 0.dp`. Gölge, gradyan, parıltı (glow) veya glassmorphism yasaktır.
   - Kartlar (16.dp) yalnızca en üstteki `NmrHeroCard` ve seçili liste satırında (`NmrSelectableRow`) kullanılır. İç içe kart (nested card) yasaktır.
   - Sayısal sayaç ve değerlerde hizalı rakam (`fontFeatureSettings = "tnum"`) aktiftir.
   - Emojiler, maskotlar veya 3B grafikler arayüzde kullanılamaz. Dil seçimi yalnızca `TR` | `EN` şeklindedir.
3. **Erişilebilirlik Performansı:**
   - `onAccessibilityEvent` içinde asla I/O veya blocking işlem yapılmaz.
   - Düğüm ağacı taramalarında `recycle()` ve bellek sızıntısı önlemlerine uyulur.

## 3. Mimari Bileşenler
- **Algılama Motoru:** `ScreenClassifier.kt` ve `rules.json` (Versiyonlu kural seti).
- **Politika Değerlendirici:** `ReelsPolicy.kt` ve `NavigationTracker.kt` (Durum makinesi).
- **Aksiyon İcracısı:** `ActionExecutor.kt` (Geri tuşu veya sekme tıklama).
- **Depolama:** `SettingsStore.kt` & `QuotaStore.kt` (StateFlow & SharedPreferences tabanlı reaktif yapı).
- **Arayüz:** Jetpack Compose, `NoMoreReelsTheme.kt` ve `MainActivity.kt`.
