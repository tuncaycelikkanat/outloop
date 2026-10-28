# Google Play Erişilebilirlik Hizmeti (AccessibilityService) Bildirimi ve Video Rehberi

Google Play Console, `AccessibilityService` izni kullanan tüm uygulamalardan şu iki şeyi ister:
1. Konsoldaki Erişilebilirlik Beyan Formu'nda net gerekçe
2. Hizmetin nasıl çalıştığını gösteren kısa (30-60 saniyelik) YouTube videosu (Liste dışı / Unlisted)

---

## 1. Google Play Console Formuna Yazılacak Gerekçe Metni

### Türkçe (Play Console Türkçe panel için):
> Outloop, bir dijital refah ve odaklanma uygulamasıdır. Erişilebilirlik Hizmetini (AccessibilityService API) yalnızca şu amaçla kullanır:
> 1. Kullanıcı Instagram uygulamasını kullanırken, kısa video (Reels) akışının açılıp açılmadığını ve bir sonraki videoya kaydırılıp kaydırılmadığını pencere içeriğindeki arayüz düğümlerinden cihaz belleğinde anlık olarak tespit etmek.
> 2. Kullanıcı bir Reels akışına girdiğinde veya belirlediği sınırları aştığında, odaklanma ayarlarına göre Farkındalık Kapısı göstermek veya ekrandan güvenli şekilde geri çıkış (GLOBAL_ACTION_BACK) eylemini gerçekleştirmek.
> 
> Uygulama tamamen sunucusuz ve çevrimdışıdır. Bu hizmet aracılığıyla hiçbir kişisel veri, özel mesaj, şifre, tuş vuruşu veya ekran içeriği kalıcı olarak kaydedilmez, saklanmaz veya harici hiçbir yere iletilmez.

### English (Play Console English panel):
> Outloop is a digital wellbeing and focus application. It uses the AccessibilityService API strictly for the following purposes:
> 1. To detect when short-video reels and video swipe transitions occur within the Instagram application by transiently inspecting active UI window nodes in device memory.
> 2. To display a Mindful Gateway or perform the navigation exit action (GLOBAL_ACTION_BACK) to help users avoid addictive doomscrolling based on their personal digital wellbeing settings.
> 
> Outloop operates 100% offline. It never permanently logs, stores, or transmits any personal data, private messages, keystrokes, passwords, or screen contents.

---

## 2. YouTube Tanıtım Videosu Senaryosu (30-45 Saniye)

Google denetçileri (reviewers) videoda şunları görmek ister:
1. **0:00 - 0:10:** Outloop açılır, Onboarding veya Ayarlar'da "Erişilebilirlik İzni" düğmesine basılır. Ekranda beliren "Açık İzin / Prominent Disclosure" kartı (ekrandaki şeffaf bilgilendirme) gösterilir.
2. **0:10 - 0:20:** Android Sistem Ayarları'na gidilir, "İndirilen Uygulamalar / Erişilebilirlik" altından Outloop açılır ve sistem uyarısı onaylanır.
3. **0:20 - 0:35:** Instagram açılır. Reels sekmesine veya bir Reels videosuna tıklanır. Outloop'un videoyu algılayıp geri çıktığı veya Farkındalık Kapısı'nı gösterdiği an gösterilir.
4. **0:35 - 0:40:** Normal akışın, mesajların (DM) ve profilin çalıştığı gösterilerek video bitirilir.

*Not: Video YouTube'a yüklenirken "Liste Dışı (Unlisted)" olarak kaydedilip linki Play Console'a yapıştırılır.*
