# Google Play Veri Güvenliği (Data Safety) Formu Cevapları

Google Play Console'da "Uygulama İçeriği -> Veri Güvenliği" (Data Safety) bölümünü doldururken aşağıdaki adımları birebir seçebilirsiniz:

---

### 1. Veri toplama ve güvenliği (Data collection and security)
- **Uygulamanız kullanıcı verisi topluyor veya paylaşıyor mu?**
  👉 **HAYIR (NO)** — Uygulama hiçbir veri toplamaz veya üçüncü taraflarla paylaşmaz.
- **Toplanan tüm kullanıcı verileri aktarım sırasında şifreleniyor mu?**
  👉 Veri toplanmadığı için bu soru devre dışı kalır (veya internet izni olmadığı için veri aktarımı yoktur).
- **Kullanıcılara verilerinin silinmesini talep etme yöntemi sunuyor musunuz?**
  👉 **HAYIR (NO)** — Uygulamada hesap veya sunucu bulunmadığından ve veri toplanmadığından veri silme talebi uygulanamaz.

---

### 2. İzinler ve Özel İfadeler
- **android.permission.INTERNET:** Bulunmaz.
- **android.permission.BIND_ACCESSIBILITY_SERVICE:** Yalnızca yerel arayüz tetiklemesi ve pencere tespiti için kullanılır, hiçbir veri kaydedilmez.
- **Google Play Billing (Fatura):** Bağış/Kahve ısmarlama işlemleri tamamen Google Play'in kendi güvenli ödeme penceresi tarafından yönetilir, Outloop hiçbir kart veya ödeme bilgisi görmez ve saklamaz.

---

### 3. Gizlilik Politikası URL'si (Privacy Policy Link)
GitHub üzerindeki gizlilik politikası doğrudan kullanılabilir:
`https://github.com/tuncaycelikkanat/NoMoreReels/blob/main/PRIVACY_POLICY.md` (veya kendi web siteniz).
