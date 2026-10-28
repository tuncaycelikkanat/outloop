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
- **Geliştiriciye Destek:** Harici web bağlantıları (Kreosus / BuyMeACoffee / GitHub Sponsors) üzerinden tamamen kullanıcının standart web tarayıcısında gerçekleşir; uygulama hiçbir kart, ödeme veya finansal veri görmez, işlemez ve saklamaz.

---

### 3. Gizlilik Politikası URL'si (Privacy Policy Link)
GitHub üzerindeki gizlilik politikası doğrudan kullanılabilir:
`https://github.com/tuncaycelikkanat/outloop/blob/main/PRIVACY_POLICY.md` (veya kendi web siteniz).
