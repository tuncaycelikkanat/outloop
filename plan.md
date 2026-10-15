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
| Algılama kuralları | Önce uygulamaya gömülü; sonra opsiyonel uzaktan güncelleme |
| Engelleme ekranı | Alternatif öneri ekranı (overlay izni gerekir) |
| Koruma sertliği | Kurulumda kullanıcı seçer: hafif, orta, sert |
| DM'den reel izni | Kurulumda kullanıcı seçer: serbest, süreli, onaylı |
| İsim | "No More Reels" çalışma adı, Play'e başvurmadan önce kesinleşir |

**Sürüm kapsamı:** v1 = algılama + engelleme, DM'den reel izni, atlatması zor koruma. v1.1 = alternatif öneri içerikleri ve kota modeli. v1.2 = ilerleme/seri/haftalık özet, Türkçe konumlandırma ve opsiyonel uzaktan kural güncelleme.

## Yol haritası

&#91;embedded content: yol haritası · 9 aşama, soldan sağa ve satır satır\]

Haftalar tahminidir ve ölçekli değildir; her kapı geçilmeden sonraki faza başlanmaz. Vurgulu kutu Play onayını, yani v1'in gerçek yayın kapısını gösterir.

## Görev listesi

Toplam v1 tahmini: yaklaşık 120 saat (8 hafta x 15 saat). Saatler kaba tahmindir; Faz 1'deki algılama işi en çok sapma riski taşıyan kısımdır.

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

### Faz 2: DM'den reel izni ve engelleme ekranı (Hafta 4-5, \~30 saat)

- [ ] Durum makinesi: ekranlar arası geçişi izleyip oynatıcının DM'den mi sekmeden mi açıldığını çıkarma (12 sa)
- [ ] Üç mod: serbest, süreli (tek reel), onaylı (12 sa içindeki mantığa eklenir, ek 6 sa)
- [ ] Overlay izni akışı ve temel engelleme ekranı (Reels engelli mesajı, bekleme, çıkış butonu) (8 sa)
- [ ] Oynatıcıdan akışa kayma durumunda müdahale ve test senaryoları (4 sa)

### Faz 3: Koruma ve kurulum akışı (Hafta 6, \~15 saat)

- [ ] Üç koruma seviyesi: bekleme süresi + yazılı onay, gece yasağı ve günlük kapatma limiti, güvenilen kişi kodu (8 sa)
- [ ] Kurulum sihirbazı: izin adımları, koruma seviyesi seçimi, DM modu seçimi (5 sa)
- [ ] Ayarlar ekranı, Türkçe ve İngilizce string kaynakları (2 sa)

### Faz 4: Cihaz ve dayanıklılık testi (Hafta 7, \~15 saat)

- [ ] Pil optimizasyonu ve servisin kapanmasına karşı kullanıcı yönlendirmesi (5 sa)
- [ ] Farklı Instagram sürümlerinde regresyon denemesi, bulguları kurallara işleme (6 sa)
- [ ] Yeniden başlatma, ekran kilidi, çoklu pencere gibi kenar durumlar (4 sa)

### Faz 5: Yayına hazırlık (Hafta 8, \~15 saat)

- [ ] Gizlilik politikası, Play Console veri güvenliği formu, Erişilebilirlik beyan videosu ve metni (6 sa)
- [ ] Mağaza metinleri, ekran görüntüleri, simge (4 sa)
- [ ] README, katkı rehberi, bağış bağlantısı (3 sa)
- [ ] Kapalı test sürümünün yüklenmesi (2 sa)

### v1.1 (Hafta 11-13) ve v1.2 (Hafta 14-16)

- [ ] v1.1: alternatif öneri içerikleri (not, su iç, kitap, yapılacaklar) ve günlük kota modeli
- [ ] v1.2: ilerleme, seri ve haftalık özet; Türkçe konumlandırma ve mağaza metni; opsiyonel uzaktan kural güncelleme (INTERNET izni yalnızca bu sürümle eklenir)

## Mimari

Tek modüllü, bağımlılığı az bir yapı; ağır çerçeve yok. Servis ince tutulur, mantık saf Kotlin sınıflarında durur ki cihaz olmadan test edilebilsin.

```text
app/src/main/java/.../nomorereels/
  service/
    ReelsAccessibilityService.kt   // olayları alır, sadece Instagram paketi
  detection/
    ScreenSnapshot.kt              // düğüm ağacından çıkarılan sade veri
    ScreenClassifier.kt            // snapshot -> ScreenType
    RuleSet.kt                     // gömülü JSON kuralları, sürüm alanı
  policy/
    NavigationTracker.kt           // DM'den mi sekmeden mi açıldı
    ReelsPolicy.kt                 // izin / engelle / sor kararı
    ProtectionLevel.kt             // hafif, orta, sert
  action/
    ActionExecutor.kt              // GLOBAL_ACTION_BACK, ana sayfaya dönüş
    OverlayController.kt           // engelleme / alternatif ekranı
  data/
    SettingsStore.kt               // DataStore tercihleri
    QuotaStore.kt                  // v1.1
  ui/
    onboarding/ settings/ blocked/ // Compose ekranları
```

**Bileşen sözleşmeleri**

- **ScreenSnapshot:** Servis düğüm ağacını hemen basit bir veri sınıfına çevirir ve ham düğümü bırakır; içerik metni okunmaz, yalnızca view ID, sınıf adı ve seçili durum alınır.
- **ScreenClassifier:** Saf fonksiyon, Android bağımlılığı yok; kural seti değişince tek yer değişir.
- **NavigationTracker:** Son birkaç ekran türünü tutan küçük durum makinesi (aşağıdaki diyagram).
- **ReelsPolicy:** Girdi: ekran türü, geliş yolu, DM modu, koruma seviyesi, kota. Çıktı: `Allow`, `Block`, `Ask`.
- **ActionExecutor / OverlayController:** Tek sorumluluk, kararı uygular; tekrar tetiklemeyi önlemek için bekleme süresini burada tutar.

**Teknik tercihler:** Kotlin, Jetpack Compose (ayarlar ve overlay içeriği), DataStore, minSdk 26 civarı (kesinleştirilecek), bağımlılık enjeksiyon çerçevesi yok; elle kurulum yeterli.

## Araçlar ve Teknoloji Yığını

- **Geliştirme:**
  - Android Studio (güncel kararlı sürüm)
  - Kotlin + Gradle (Kotlin DSL - `build.gradle.kts`)
  - Jetpack Compose (Ayarlar, kurulum sihirbazı ve overlay içeriği)
  - DataStore Preferences (Koruma seviyesi, DM modu, v1.1 kota sayacı)
  - Elle Kurulum / Manuel DI (Hilt/Koin yok; hafif, hızlı ve sıfır ek yük)
- **Algılama ve Hata Ayıklama:**
  - Layout Inspector & uiautomatorviewer (Instagram düğüm ağacını ve view ID'lerini incelemek için)
  - ADB (`adb shell uiautomator dump`, `adb logcat`): ekran dökümlerini almak ve servis günlüklerini izlemek için
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

NavigationTracker son ekran türlerini tutar; oynatıcı açılınca geliş yolunu ReelsPolicy'ye verir. Sekmeden veya akıştan gelen oynatıcı her zaman engellenir; yalnızca DM kökenli oynatıcı kurulumda seçilen moda göre davranır. Oynatıcının DM'den mi sekmeden mi açıldığını güvenle ayırt etme işi Faz 2'nin asıl riskidir.

## Riskler

| Risk | Etki | Önlem |
| --- | --- | --- |
| Instagram arayüzü değişir, algılama bozulur | Yüksek | Kurallar koddan ayrı JSON; kayıtlı düğüm ağacı testleri; v1.2'de opsiyonel uzaktan güncelleme |
| Play Store Erişilebilirlik Servisi incelemesi reddi | Yüksek | Beyanı baştan net yaz, sadece gerekli olayları dinle, içerik okumadığını belgele; ret olursa APK/GitHub yedeği |
| DM'den açılan reel ile sekmeden açılanı ayırmak zor | Orta | Faz 2'ye en fazla zaman ayrıldı; ayırt edilemezse varsayılan davranış "onaylı" mod |
| Üretici pil optimizasyonu servisi kapatır | Orta | Kurulumda yönlendirme, servis durumunu uygulama açılışında kontrol |
| Yanlış pozitif: DM veya profil yanlışlıkla engellenir | Orta | Ekran sınıflandırıcıda muhafazakâr kural, hata ayıklama günlüğü, hızlı "bu yanlış" geri bildirim yolu |
| Kapsam şişmesi (yedi özellik) | Orta | Aşamalı çıkış; v1'e yeni özellik eklenmez |
| Koruma açık kaynak olduğu için kod değiştirilerek kaldırılabilir | Düşük | Amaç kendine karşı anlık irade koruması; kırılamaz kilit değil, bu açıkça belgelenir |

## Play Store hazırlığı

Tahminen en çok zaman alacak dış süreç bu; v1 kodu bitmeden başlamak mantıklı. Aşağıdaki kurallar hafızamdan; başvurudan önce güncel Play politikalarından doğrula.

1. **Geliştirici hesabı:** Tek seferlik kayıt ücreti vardır (yaklaşık 25 dolar). Kişisel hesaplarda yayından önce kapalı test şartı olabilir (yaklaşık 12 test kullanıcısı, 14 gün); bu süre takvime eklenmeli, Faz 5 sonrası 2 hafta ayrıldı.
2. **Erişilebilirlik beyanı:** Servisin neden gerekli olduğu, hangi olayları dinlediği ve içerik okumadığı; kısa bir ekran kaydı videosuyla gösterilir.
3. **Veri güvenliği formu ve gizlilik politikası:** v1'de INTERNET izni yok, veri toplanmıyor; bu yüzden form sade. v1.2'de uzaktan güncelleme eklenirse form ve politika güncellenir.
4. **Overlay izni:** "Diğer uygulamaların üzerinde göster" izninin gerekçesi (engelleme ekranı) beyanda yer alır.
5. **Paket adı:** Yayından sonra değiştirilemez; başvurudan önce isim kesinleşmeli.
6. **Yedek plan:** Ret gelirse GitHub Releases üzerinden APK dağıtımı ve gerekçeyi düzeltip yeniden başvuru.
