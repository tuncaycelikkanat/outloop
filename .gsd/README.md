# No More Reels — Otonom AI Geliştirme Paketi (GSD + Ralph Loop + CodeRabbit)

Bu sistem, projeyi bağlam çürümesi (context rot) yaşamadan, atomik görevlerle ve otomatik güvenlik/kod incelemeleriyle geliştirmek üzere kurulmuştur.

---

## 🏗️ 1. GSD Core (Şartname Odaklı Hafıza)
- **Konum:** `.gsd/`
- **Görev:** Ajanların uzun sohbet geçmişine ihtiyaç duymadan, sıfır bağlamla çalışmasını sağlar.
- **Dosyalar:**
  - [`.gsd/SPEC.md`](file:///home/tuncay/Projects/NoMoreReels/.gsd/SPEC.md): Projenin sarsılmaz kuralları (%100 Çevrimdışı, Flat Design kuralları, Erişilebilirlik prensipleri).
  - [`.gsd/tasks.json`](file:///home/tuncay/Projects/NoMoreReels/.gsd/tasks.json): Sırada bekleyen atomik görevlerin kuyruğu.
  - [`.gsd/config.json`](file:///home/tuncay/Projects/NoMoreReels/.gsd/config.json): Doğrulama komutları ve ortam ayarları.

---

## 🔄 2. Ralph Loop (Otonom Döngü)
- **Konum:** [`scripts/ralph_loop.sh`](file:///home/tuncay/Projects/NoMoreReels/scripts/ralph_loop.sh)
- **Nasıl Çalışır?**
  1. Kuyruktan (`tasks.json`) bekleyen ilk görevi çeker.
  2. Geliştirmeyi doğrulamak için `./gradlew testDebugUnitTest` testlerini koşturur.
  3. Testler başarılıysa görevi `completed` yapar, `progress.log` kütüğüne işler ve otomatik Git Commit atar.
  4. Kuyruk bitene kadar döngüyü sıfır bağlamla tekrarlar.
- **Çalıştırma:**
  ```bash
  ./scripts/ralph_loop.sh
  ```

---

## 🐇 3. CodeRabbit (AI PR & Kod İnceleme)
- **Konum:** [`.coderabbit.yaml`](file:///home/tuncay/Projects/NoMoreReels/.coderabbit.yaml)
- **Yapılandırılan Kurallar:**
  - **Zero-Internet Alarmı:** `AndroidManifest.xml` içine kazara `INTERNET` izni eklenirse PR'ı bloke eder.
  - **Flat UI Güvencesi:** Jetpack Compose kodlarında gölge (`shadowElevation > 0.dp`) veya iç içe kart tespit ederse uyarır.
  - **Erişilebilirlik Performansı:** `onAccessibilityEvent` içinde thread bloklamalarını ve düğüm ağacı sızıntılarını denetler.
- **GitHub'da Etkinleştirme:**
  1. Kodları GitHub reposuna push'layın (`git push`).
  2. [coderabbit.ai](https://coderabbit.ai) adresine gidip reponuza CodeRabbit GitHub App'ini ücretsiz ekleyin.
  3. Açtığınız her Pull Request artık Türkçe ve bu kurallara göre otomatik incelenir.
