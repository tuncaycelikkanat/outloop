# No More Reels Proje Planı

Oct 14, 2026 · @patron

Tek geliştirici, haftada 10-20 saatle v1'i yaklaşık 8 haftada çıkarmayı hedefleyen, Play Store'a açık kaynak olarak yayınlanacak Android uygulaması planı.

## Kararlar

Aşağıdaki kararlar soru-cevap turunda netleşti; plan bunlara göre kuruldu.

| Konu | Karar |
| --- | --- |
| Amaç | Önce kendi kullanımın, sonra gerçek kullanıcılara yayın |
| Platform | Sadece Android (Kotlin, AccessibilityService) |
| Gelir | Ücretsiz, bağış veya gönüllü destek |
| Dağıtım | Google Play Store |
| Lisans | Tamamen açık kaynak (GitHub) |
| Tempo | Tek kişi, haftada 10-20 saat |
| Test | Kendi fiziksel telefonun, Instagram yüklü |
| Algılama kuralları | Uygulamaya gömülü (`rules.json`) |
| İnternet Politikası | **%100 Çevrimdışı (Zero-Internet):** Hiçbir sürümde `INTERNET` izni alınmaz |
| Engelleme ekranı | Alternatif öneri + Bilinçli Sürtünme ekranı (overlay izni gerekir) |
| Koruma sertliği | Kurulumda kullanıcı seçer: hafif, orta, sert |
| DM'den reel izni | Kurulumda kullanıcı seçer: serbest, süreli (tek reel + kaydırma koruması), onaylı |
| İsim | "No More Reels" çalışma adı, Play'e başvurmadan önce kesinleşir |

**Sürüm kapsamı:**
- **v1:** Algılama + engelleme, DM'den tek reel izni (2. videoya kaydırma korumalı), bilinçli sürtünme (5 sn basılı tutma), atlatması zor koruma.
- **v1.1:** Alternatif öneri içerikleri, günlük kota modeli, görsel cazibe azaltıcı (kota biterken ekranı kademeli karartma/grileştirme) ve Ana Akış (Feed) "Önerilenler / Zombi Kaydırma" koruması.
- **v1.2:** Kas hafızası ısı haritası ("Elim ne zaman Reels'e gidiyor?"), kurtarılan zamanın somut karşılığı, ilerleme/seri/haftalık özet ve Türkçe/İngilizce mağaza konumlandırması.

## Yol haritası

&#91;embedded content: yol haritası · 9 aşama, soldan sağa ve satır satır\]

Haftalar tahminidir ve ölçekli değildir; her kapı geçilmeden sonraki faza başlanmaz. Vurgulu kutu Play onayını, yani v1'in gerçek yayın kapısını gösterir.

## Görev listesi

Toplam v1 tahmini: yaklaşık 120 saat (8 hafta x 15 saat). Saatler kaba tahmindir; Faz 2'deki DM'den tek reel ve kaydırma tespiti en çok dikkat isteyen kısımdır.

### Faz 0: Prototip (Hafta 1, \~12 saat)

- [x] Android Studio projesi, paket adı (geçici), GitHub reposu, lisans dosyası (2 sa)
- [x] Boş AccessibilityService, sadece com.instagram.android olaylarını dinleyen filtre (3 sa)
- [x] Instagram'da gezinirken ekran düğüm ağacını (view ID, sekme seçili durumu) loglama ve Reels ekranının izini bulma (5 sa)
- [x] Reels algılanınca GLOBAL\_ACTION\_BACK tetikleme, ilk çalışan sürüm (2 sa)

### Faz 1: Algılama çekirdeği (Hafta 2-3, \~30 saat)

- [x] Ekran sınıflandırıcı: Ana akış, Reels sekmesi, DM, DM içinden açılmış oynatıcı, Keşfet, Profil (10 sa)
- [x] Algılama kurallarını koddan ayrı veri yapısına (gömülü JSON) taşıma, kural sürümü alanı (6 sa)
- [x] Yanlış pozitif azaltma: bekleme süresi, olay gürültüsünü süzme, tekrar tetiklemeyi önleme (6 sa)
- [x] Birim testler: kayıtlı düğüm ağacı örnekleriyle sınıflandırıcı testi (6 sa)
- [x] Algılama hata ayıklama günlüğü ekranı (yalnızca debug sürümünde) (2 sa)

### Faz 2: DM'den tek reel izni, kaydırma koruması ve engelleme ekranı (Hafta 4-5, \~30 saat)

- [x] Durum makinesi: ekranlar arası geçişi izleyip oynatıcının DM'den mi yoksa sekmeden/keşfetten mi açıldığını çıkarma (12 sa)
- [x] **DM Tek-Reel & Kaydırma Koruması:** DM'den açılan ilk videoya izin verip kullanıcı 2. videoya kaydırdığı an (`TYPE_VIEW_SCROLLED` veya içerik/yazar düğümü değişimi) yakalayarak DM'e geri döndürme (6 sa)
- [x] Üç DM modu: serbest, süreli/tek-reel, onaylı (4 sa)
- [x] Overlay izni akışı, temel engelleme ekranı ve **Bilinçli Sürtünme (Mindful Friction)**: otomatik pilottan çıkarmak için 5 saniye basılı tutma veya niyet onayı (6 sa)
- [x] Oynatıcıdan akışa kayma durumunda müdahale ve test senaryoları (2 sa)

### Faz 3: Koruma ve kurulum akışı (Hafta 6, \~15 saat)

- [x] Üç koruma seviyesi / hazır profiller: Dengeli, Bilinçli Sürtünme (5 sn onay), Katı Detoks (Sıfır Reels) ve İrade Kilidi (8 sa)
- [x] Gece Odak Saati (Bedtime Mode): Gece uyku pencerelerinde (örn. 23:30-07:00) tüm Reels'lerin koşulsuz engellenmesi (`BLOCK_BEDTIME`)
- [x] Kurulum sihirbazı (Onboarding Wizard): 3 adımlı akış (%100 Çevrimdışı/Zero-Internet güvencesi, Erişilebilirlik servisi rehberi, hazır profil seçimi) (5 sa)
- [x] Ayarlar ekranı ve modern Compose dashboard entegrasyonu (2 sa)
- [x] **Tasarım Sistemi & 10 Ekran UI Yenilemesi:** PDF tasarım kılavuzuna ve `NoMoreReelsTheme.kt` mimarisine birebir sadık, 0.dp gölgesiz düz yüzeyler, kart kuralları, Inter & JetBrains Mono tipografi, tnum rakamlar, Açık/AMOLED tema, TR/EN dil desteği, 3 adımlı Onboarding (Ekran 1-3), 4 Ana Sekme (Korumalar, Bölgeler, Kota & Süre, Ayarlar - Ekran 4-7), Canlı Olay Günlüğü (Ekran 8), İrade Kilidi & Özel Kota Diyalogları & Bölgeler Bottom Sheet (Ekran 9), Nokta Kamera Kapsülü (Ekran 10).

### Faz 4: Cihaz ve dayanıklılık testi (Hafta 7, \~15 saat)

- [ ] Pil optimizasyonu ve servisin kapanmasına karşı kullanıcı yönlendirmesi (5 sa)
- [ ] Farklı Instagram sürümlerinde regresyon denemesi, bulguları kurallara işleme (6 sa)
- [ ] Yeniden başlatma, ekran kilidi, çoklu pencere gibi kenar durumlar (4 sa)

### Faz 5: Yayına hazırlık (Hafta 8, \~15 saat)

- [ ] Gizlilik politikası, Play Console veri güvenliği formu (%100 çevrimdışı vurgusu), Erişilebilirlik beyan videosu ve metni (6 sa)
- [ ] Mağaza metinleri, ekran görüntüleri, simge (4 sa)
- [ ] README, katkı rehberi, bağış bağlantısı (3 sa)
- [ ] Kapalı test sürümünün yüklenmesi (2 sa)

### v1.1 (Hafta 11-13): Odak ve Cazibe Azaltma

- [x] **Günlük Kota Modeli & Süre Takibi:** Günlük izlenen Reels sayısı sayacı, Reels'de geçen dakika/saniye takibi, özelleştirilebilir hedef limitler ve erken uyarı eşikleri (%50, %75, %80, %90) (`QuotaStore.kt`)
- [ ] Alternatif öneri içerikleri (not, su iç, kitap, yapılacaklar)
- [ ] **Görsel Cazibe Azaltıcı (Karartma / Monokrom Overlay):** Süreli veya kotalı kullanımda süre bitimine yakın ekranın üzerine dokunmayı engellemeyen (`FLAG_NOT_TOUCHABLE`) yarı saydam gri/siyah katman çekerek dopamin etkisini düşürme
- [ ] **Ana Akış (Feed) Tuzağı ve Önerilenler Koruması:** Ana akışta `"Önerilenler"` / `"Suggested for you"` başlığı algılandığında veya kesintisiz X dakika aşağı kaydırıldığında "Takip ettiklerin bitti, algoritma akışındasın" uyarısı

### v1.2 (Hafta 14-16): Farkındalık ve İçgörü

- [ ] **Kas Hafızası Isı Haritası ("Elim Ne Zaman Reels'e Gidiyor?"):** Engellemelerin gün/saat dağılımını çıkarıp kullanıcının en zayıf olduğu saat dilimlerini (örn. gece 23:30-01:00) raporlama
- [ ] **Kurtarılan Zamanın Somut Karşılığı:** Engellenen Reels sayısını tahmini kurtarılan süreye ve somut karşılıklara (örn. okunabilecek kitap sayfası) dönüştüren haftalık özet
- [ ] İlerleme/seri (streak) takibi ve Türkçe/İngilizce mağaza konumlandırması

## Mimari

Tek modüllü, bağımlılığı az, **%100 çevrimdışı** bir yapı; ağır çerçeve yok. Servis ince tutulur, mantık saf Kotlin sınıflarında durur ki cihaz olmadan test edilebilsin.

```text
app/src/main/java/.../nomorereels/
  service/
    ReelsAccessibilityService.kt   // olayları alır, sadece Instagram paketi
  detection/
    ScreenSnapshot.kt              // düğüm ağacından çıkarılan sade veri
    ScreenClassifier.kt            // snapshot -> ScreenType
    RuleSet.kt                     // gömülü JSON kuralları, sürüm alanı
  policy/
    NavigationTracker.kt           // DM'den mi sekmeden mi açıldı + DM içi kaydırma (2. reel) takibi
    ReelsPolicy.kt                 // izin / engelle / sor kararı + Feed önerilenler kontrolü
    ProtectionLevel.kt             // hafif, orta, sert
  action/
    ActionExecutor.kt              // Ana sayfaya yönlendirme, GLOBAL_ACTION_BACK, devre kesici
    OverlayController.kt           // engelleme / 5 sn sürtünme / karartma (dimmer) ekranı
  data/
    SettingsStore.kt               // DataStore tercihleri
    QuotaStore.kt                  // v1.1 kota sayacı
    HeatmapStore.kt                // v1.2 kas hafızası zaman damgaları ve istatistikler
  ui/
    onboarding/ settings/ blocked/ // Compose ekranları
```

**Bileşen sözleşmeleri**

- **ScreenSnapshot:** Servis düğüm ağacını hemen basit bir veri sınıfına çevirir ve ham düğümü bırakır; kişisel mesaj içerikleri asla kaydedilmez.
- **ScreenClassifier:** Saf fonksiyon, Android bağımlılığı yok; kural seti değişince tek yer değişir.
- **NavigationTracker:** Son ekran türlerini ve DM'den açılan oynatıcıdaki kaydırma/video değişimi durumunu tutan durum makinesi.
- **ReelsPolicy:** Girdi: ekran türü, geliş yolu, kaydırma sayısı, DM modu, koruma seviyesi, kota. Çıktı: `Allow`, `Block`, `Ask`, `Dim`.
- **ActionExecutor / OverlayController:** Tek sorumluluk, kararı uygular; tekrar tetiklemeyi önlemek için bekleme süresini ve devre kesiciyi burada tutar.

**Teknik tercihler:** Kotlin, Jetpack Compose (ayarlar ve overlay içeriği), DataStore, minSdk 26, bağımlılık enjeksiyon çerçevesi yok; elle kurulum yeterli. `AndroidManifest.xml` içinde `INTERNET` izni yer almaz.

## Araçlar ve Teknoloji Yığını

- **Geliştirme:**
  - Android Studio (güncel kararlı sürüm)
  - Kotlin + Gradle (Kotlin DSL - `build.gradle.kts`)
  - Jetpack Compose (Ayarlar, kurulum sihirbazı ve overlay içeriği)
  - DataStore Preferences (Koruma seviyesi, DM modu, v1.1 kota sayacı)
  - Elle Kurulum / Manuel DI (Hilt/Koin yok; hafif, hızlı ve sıfır ek yük)
- **Algılama ve Hata Ayıklama:**
  - Layout Inspector & uiautomatorviewer (Instagram düğüm ağacını ve view ID'lerini incelemek için)
  - ADB (`adb shell uiautomator dump`, `adb logcat`) ve `tools/dump_screen.py`
  - Kayıtlı düğüm ağacı örnekleri (Repo içinde JSON/XML olarak saklanır, sınıflandırıcı testleri bunlarla çalışır)
- **Test:**
  - JUnit (Saf Kotlin sınıfları için birim testler)
  - İhtiyaç halinde AndroidX Test (Kırılgan UI testleri yerine kayıtlı dökümler ve fiziksel cihazda elle doğrulama)
- **Kod Yönetimi ve Dağıtım:**
  - Git + GitHub (Açık kaynak repo, MIT veya Apache 2.0 lisansı)
  - GitHub Actions (Derleme ve birim testler için hafif CI)
  - Google Play Console (Yayın, kapalı test, beyan formları) + GitHub Releases (APK yedeği)
- **Tasarım ve Belgeler:**
  - Doğrudan Compose Önizlemeleri (`@Preview`) veya basit Figma taslakları
  - Markdown belgeleri (README, CONTRIBUTING, PRIVACY)
  - GitHub Pages (Play Console'un zorunlu tuttuğu Gizlilik Politikası URL'i için)

## DM'den gelen reel için durum makinesi

&#91;embedded content: DM'den reel kararı · 1 karar, 3 mod\]

NavigationTracker son ekran türlerini tutar; oynatıcı açılınca geliş yolunu ReelsPolicy'ye verir. Sekmeden veya akıştan gelen oynatıcı her zaman engellenir; yalnızca DM kökenli oynatıcı kurulumda seçilen moda göre davranır. "Süreli / Tek-Reel" modunda kullanıcı DM'den açtığı videoyu izleyebilir ancak parmağını kaydırıp bir sonraki Reel'e geçtiği an (`TYPE_VIEW_SCROLLED` veya içerik düğümü değişimi) engellenerek sohbete geri döndürülür.

## Riskler

| Risk | Etki | Önlem |
| --- | --- | --- |
| Instagram arayüzü değişir, algılama bozulur | Yüksek | Kurallar koddan ayrı JSON (`rules.json`); kayıtlı düğüm ağacı testleri |
| Play Store Erişilebilirlik Servisi incelemesi reddi | Yüksek | Uygulamada **`INTERNET` izni olmaması** en büyük güven kanıtıdır; beyanda "Odaklanma / Dijital Farkındalık yardımcısı" çerçevesi ve sıfır veri aktarımı vurgulanır |
| DM'den açılan reel ile sekmeden açılanı ayırmak zor | Orta | Faz 2'ye en fazla zaman ayrıldı; ayırt edilemezse varsayılan davranış "onaylı (5 sn basılı tut)" mod |
| Üretici pil optimizasyonu servisi kapatır | Orta | Kurulumda yönlendirme, servis durumunu uygulama açılışında kontrol |
| Yanlış pozitif: DM veya profil yanlışlıkla engellenir | Orta | Ekran sınıflandırıcıda muhafazakâr kural ve hata ayıklama günlüğü |
| Kapsam şişmesi | Orta | Aşamalı çıkış; v1.1 ve v1.2 özellikleri Faz 2-3 çekirdeği bittikten sonra eklenir |
| Koruma açık kaynak olduğu için kod değiştirilerek kaldırılabilir | Düşük | Amaç kendine karşı anlık irade koruması; kırılamaz kilit değil, bu açıkça belgelenir |

## Play Store hazırlığı

Tahminen en çok zaman alacak dış süreç bu; v1 kodu bitmeden başlamak mantıklı. Aşağıdaki kurallar başvurudan önce güncel Play politikalarından doğrulanmalıdır.

1. **Geliştirici hesabı:** Tek seferlik kayıt ücreti vardır (yaklaşık 25 dolar). Kişisel hesaplarda yayından önce kapalı test şartı olabilir (yaklaşık 12 test kullanıcısı, 14 gün); bu süre takvime eklenmeli, Faz 5 sonrası 2 hafta ayrıldı.
2. **Erişilebilirlik beyanı:** Servisin neden gerekli olduğu, hangi olayları dinlediği ve kişisel içerik okumadığı; kısa bir ekran kaydı videosuyla gösterilir.
3. **Veri güvenliği formu ve gizlilik politikası:** Uygulamanın hiçbir sürümünde **`INTERNET` izni yoktur**; cihaz dışına veri çıkışı teknik olarak imkânsızdır. Bu durum hem Play Console incelemesinde hem de kullanıcı güveninde en güçlü kozdur.
4. **Overlay izni:** "Diğer uygulamaların üzerinde göster" izninin gerekçesi (farkındalık / engelleme ekranı) beyanda yer alır.
5. **Paket adı:** Yayından sonra değiştirilemez; başvurudan önce isim kesinleşmeli.
6. **Yedek plan:** Ret gelirse GitHub Releases üzerinden APK dağıtımı ve gerekçeyi düzeltip yeniden başvuru.
