package com.nomorereels.app.policy

import com.nomorereels.app.detection.NodeSnapshot
import com.nomorereels.app.detection.ReelContentFingerprint
import com.nomorereels.app.detection.ScreenClassifier
import com.nomorereels.app.detection.ScreenSnapshot
import com.nomorereels.app.detection.ScreenType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ReelsPolicyTest {

    private lateinit var tracker: NavigationTracker
    private lateinit var policy: ReelsPolicy
    private lateinit var classifier: ScreenClassifier

    @Before
    fun setUp() {
        tracker = NavigationTracker()
        policy = ReelsPolicy(singleReelMaxDurationMs = 90_000L)
        classifier = ScreenClassifier()
    }

    @Test
    fun `REELS_TAB should respect reelsTabMode setting`() {
        tracker.update(ScreenType.DIRECT_MESSAGES, nowMs = 1000L)
        tracker.update(ScreenType.REELS_TAB, nowMs = 2000L)

        assertFalse(tracker.isDmOriginatedReel)

        // BLOCK_ALL -> BLOCK_IMMEDIATELY
        val blockDecision = policy.evaluate(ScreenType.REELS_TAB, tracker, DmReelMode.SINGLE_REEL, reelsTabMode = DmReelMode.BLOCK_ALL, nowMs = 2100L)
        assertEquals(PolicyDecision.BLOCK_IMMEDIATELY, blockDecision)

        // FREE -> ALLOW
        val freeDecision = policy.evaluate(ScreenType.REELS_TAB, tracker, DmReelMode.SINGLE_REEL, reelsTabMode = DmReelMode.FREE, nowMs = 2100L)
        assertEquals(PolicyDecision.ALLOW, freeDecision)

        // COUNT_QUOTA not exceeded -> ALLOW, exceeded -> BLOCK_DAILY_COUNT_LIMIT
        val quotaAllowed = policy.evaluate(ScreenType.REELS_TAB, tracker, DmReelMode.SINGLE_REEL, reelsTabMode = DmReelMode.COUNT_QUOTA, isCountLimitExceeded = false, nowMs = 2100L)
        assertEquals(PolicyDecision.ALLOW, quotaAllowed)

        val quotaBlocked = policy.evaluate(ScreenType.REELS_TAB, tracker, DmReelMode.SINGLE_REEL, reelsTabMode = DmReelMode.COUNT_QUOTA, isCountLimitExceeded = true, nowMs = 2100L)
        assertEquals(PolicyDecision.BLOCK_DAILY_COUNT_LIMIT, quotaBlocked)

        // TIME_QUOTA not exceeded -> ALLOW, exceeded -> BLOCK_DAILY_TIME_LIMIT
        val timeAllowed = policy.evaluate(ScreenType.REELS_TAB, tracker, DmReelMode.SINGLE_REEL, reelsTabMode = DmReelMode.TIME_QUOTA, isTimeLimitExceeded = false, nowMs = 2100L)
        assertEquals(PolicyDecision.ALLOW, timeAllowed)

        val timeBlocked = policy.evaluate(ScreenType.REELS_TAB, tracker, DmReelMode.SINGLE_REEL, reelsTabMode = DmReelMode.TIME_QUOTA, isTimeLimitExceeded = true, nowMs = 2100L)
        assertEquals(PolicyDecision.BLOCK_DAILY_TIME_LIMIT, timeBlocked)

        // SINGLE_REEL before swipe -> ALLOW, after swipe -> BLOCK_AFTER_SWIPE
        val singleAllowed = policy.evaluate(ScreenType.REELS_TAB, tracker, DmReelMode.SINGLE_REEL, reelsTabMode = DmReelMode.SINGLE_REEL, nowMs = 2100L)
        assertEquals(PolicyDecision.ALLOW, singleAllowed)

        tracker.onReelPagerScrolled(nowMs = 4000L)
        val singleBlocked = policy.evaluate(ScreenType.REELS_TAB, tracker, DmReelMode.SINGLE_REEL, reelsTabMode = DmReelMode.SINGLE_REEL, nowMs = 4000L)
        assertEquals(PolicyDecision.BLOCK_AFTER_SWIPE, singleBlocked)
    }

    @Test
    fun `REELS_VIEWER opened from EXPLORE should respect exploreMode setting`() {
        tracker.update(ScreenType.EXPLORE, nowMs = 1000L)
        tracker.update(
            newScreen = ScreenType.REELS_VIEWER,
            reelFingerprint = ReelContentFingerprint(author = "creator1"),
            nowMs = 2000L
        )

        assertFalse(tracker.isDmOriginatedReel)
        assertEquals(ScreenType.EXPLORE, tracker.reelOriginScreen)

        // Varsayılan exploreMode = BLOCK_ALL -> hemen engellemeli
        val blockDecision = policy.evaluate(
            detectedScreen = ScreenType.REELS_VIEWER,
            tracker = tracker,
            dmMode = DmReelMode.SINGLE_REEL,
            exploreMode = DmReelMode.BLOCK_ALL,
            nowMs = 2100L
        )
        assertEquals(PolicyDecision.BLOCK_IMMEDIATELY, blockDecision)

        // Kullanıcı Keşfet için SINGLE_REEL seçerse ilk videoya izin vermeli, 2. videoda engellemeli
        val allowFirstDecision = policy.evaluate(
            detectedScreen = ScreenType.REELS_VIEWER,
            tracker = tracker,
            dmMode = DmReelMode.SINGLE_REEL,
            exploreMode = DmReelMode.SINGLE_REEL,
            nowMs = 2100L
        )
        assertEquals(PolicyDecision.ALLOW, allowFirstDecision)

        tracker.onReelPagerScrolled(nowMs = 4000L)
        val blockAfterSwipe = policy.evaluate(
            detectedScreen = ScreenType.REELS_VIEWER,
            tracker = tracker,
            dmMode = DmReelMode.SINGLE_REEL,
            exploreMode = DmReelMode.SINGLE_REEL,
            nowMs = 4000L
        )
        assertEquals(PolicyDecision.BLOCK_AFTER_SWIPE, blockAfterSwipe)
    }

    @Test
    fun `SINGLE_REEL mode should allow first DM reel and block immediately when swiped to second reel`() {
        // 1. Kullanıcı DM ekranında
        tracker.update(ScreenType.DIRECT_MESSAGES, nowMs = 1000L)

        // 2. Arkadaşının gönderdiği Reel'e tıkladı (İlk karede sadece yazar yüklendi)
        val firstReelInitial = ReelContentFingerprint(
            author = "9gofy",
            mediaDesc = "Reel by 9gofy. Double tap to play or pause."
        )
        tracker.update(
            newScreen = ScreenType.REELS_VIEWER,
            reelFingerprint = firstReelInitial,
            nowMs = 2000L
        )

        assertTrue(tracker.isDmOriginatedReel)
        assertEquals(
            PolicyDecision.ALLOW,
            policy.evaluate(ScreenType.REELS_VIEWER, tracker, DmReelMode.SINGLE_REEL, nowMs = 2100L)
        )

        // 3. Aynı Reel'in altyazısı (caption) 200ms sonra yüklendi -> Hâlâ aynı Reel sayılmalı!
        val firstReelWithCaption = ReelContentFingerprint(
            author = "9gofy",
            mediaDesc = "Reel by 9gofy. Double tap to play or pause.",
            caption = "İlk videonun açıklaması"
        )
        tracker.update(
            newScreen = ScreenType.REELS_VIEWER,
            reelFingerprint = firstReelWithCaption,
            nowMs = 2200L
        )
        assertFalse(tracker.hasSwipedToNextReel)
        assertEquals(
            PolicyDecision.ALLOW,
            policy.evaluate(ScreenType.REELS_VIEWER, tracker, DmReelMode.SINGLE_REEL, nowMs = 2200L)
        )

        // 4. Kullanıcı parmağını yukarı kaydırıp 2. Reel'e geçti (Farklı yazar veya farklı altyazı)
        val secondReel = ReelContentFingerprint(
            author = "another_creator",
            mediaDesc = "Reel by another_creator. Double tap to play or pause.",
            caption = "İkinci video"
        )
        tracker.update(
            newScreen = ScreenType.REELS_VIEWER,
            reelFingerprint = secondReel,
            nowMs = 5000L
        )

        assertTrue(tracker.hasSwipedToNextReel)
        assertEquals(
            PolicyDecision.BLOCK_AFTER_SWIPE,
            policy.evaluate(ScreenType.REELS_VIEWER, tracker, DmReelMode.SINGLE_REEL, nowMs = 5000L)
        )

        // 5. Engelleme sonrası DM'e geri dönüldüğünde oturum sıfırlanmalı, yeni bir DM Reel'i açılabilmeli
        tracker.update(ScreenType.DIRECT_MESSAGES, nowMs = 5500L)
        assertFalse(tracker.hasSwipedToNextReel)

        tracker.update(
            newScreen = ScreenType.REELS_VIEWER,
            reelFingerprint = ReelContentFingerprint(author = "friend_video_2"),
            nowMs = 7000L
        )
        assertEquals(
            PolicyDecision.ALLOW,
            policy.evaluate(ScreenType.REELS_VIEWER, tracker, DmReelMode.SINGLE_REEL, nowMs = 7100L)
        )
    }

    @Test
    fun `SINGLE_REEL mode should also detect vertical pager scroll event after settle window`() {
        tracker.update(ScreenType.DIRECT_MESSAGES, nowMs = 1000L)
        tracker.update(ScreenType.REELS_VIEWER, nowMs = 2000L)

        // Açılış animasyonu sırasındaki erken scroll (< 1100ms) yoksayılmalı
        val earlyScroll = tracker.onReelPagerScrolled(nowMs = 2500L)
        assertFalse(earlyScroll)
        assertFalse(tracker.hasSwipedToNextReel)

        // Kullanıcı videoyu biraz izledikten sonra aşağı kaydırdı (> 1100ms)
        val realSwipe = tracker.onReelPagerScrolled(nowMs = 4000L)
        assertTrue(realSwipe)
        assertTrue(tracker.hasSwipedToNextReel)

        val decision = policy.evaluate(ScreenType.REELS_VIEWER, tracker, DmReelMode.SINGLE_REEL, nowMs = 4000L)
        assertEquals(PolicyDecision.BLOCK_AFTER_SWIPE, decision)
    }

    @Test
    fun `ASK_WITH_FRICTION mode should require 5s approval first, then allow 1 reel, then block on swipe`() {
        tracker.update(ScreenType.DIRECT_MESSAGES, nowMs = 1000L)
        tracker.update(
            newScreen = ScreenType.REELS_VIEWER,
            reelFingerprint = ReelContentFingerprint(author = "9gofy", caption = "Birinci video"),
            nowMs = 2000L
        )

        // 1. Henüz onay verilmedi -> ASK_MINDFUL_FRICTION dönmeli
        assertEquals(
            PolicyDecision.ASK_MINDFUL_FRICTION,
            policy.evaluate(ScreenType.REELS_VIEWER, tracker, DmReelMode.ASK_WITH_FRICTION, nowMs = 2100L)
        )

        // 2. Kullanıcı 5 saniye basılı tutarak onayladı
        tracker.approveCurrentDmReel(nowMs = 7100L)
        assertTrue(tracker.isCurrentDmReelApproved)
        assertEquals(
            PolicyDecision.ALLOW,
            policy.evaluate(ScreenType.REELS_VIEWER, tracker, DmReelMode.ASK_WITH_FRICTION, nowMs = 7200L)
        )

        // 3. Kullanıcı 2. videoya kaydırdı -> BLOCK_AFTER_SWIPE dönmeli!
        tracker.update(
            newScreen = ScreenType.REELS_VIEWER,
            reelFingerprint = ReelContentFingerprint(author = "9gofy", caption = "Aynı yazar ama ikinci video"),
            nowMs = 12_000L
        )
        assertEquals(
            PolicyDecision.BLOCK_AFTER_SWIPE,
            policy.evaluate(ScreenType.REELS_VIEWER, tracker, DmReelMode.ASK_WITH_FRICTION, nowMs = 12_000L)
        )
    }

    @Test
    fun `extractReelFingerprint should extract author, mediaDesc and caption from snapshot`() {
        val rootNode = NodeSnapshot(
            className = "android.widget.FrameLayout",
            children = listOf(
                NodeSnapshot(
                    viewId = "com.instagram.android:id/clips_viewer_view_pager",
                    children = listOf(
                        NodeSnapshot(
                            viewId = "com.instagram.android:id/clips_author_username",
                            contentDescription = "9gofy",
                            text = "9gofy"
                        ),
                        NodeSnapshot(
                            viewId = "com.instagram.android:id/clips_video_container",
                            contentDescription = "Reel by 9gofy. Double tap to play or pause."
                        ),
                        NodeSnapshot(
                            viewId = "com.instagram.android:id/clips_caption_component",
                            children = listOf(
                                NodeSnapshot(
                                    contentDescription = "Test reel caption text"
                                )
                            )
                        )
                    )
                )
            )
        )

        val snapshot = ScreenSnapshot(
            packageName = "com.instagram.android",
            currentActivity = "com.instagram.modal.TransparentModalActivity",
            rootNode = rootNode
        )

        val fp = classifier.extractReelFingerprint(snapshot)
        assertEquals("9gofy", fp.author)
        assertEquals("Reel by 9gofy. Double tap to play or pause.", fp.mediaDesc)
        assertEquals("Test reel caption text", fp.caption)
    }

    @Test
    fun `isBedtimeActive should unconditionally block any Reels screen with BLOCK_BEDTIME`() {
        tracker.update(ScreenType.DIRECT_MESSAGES, nowMs = 1000L)
        tracker.update(
            newScreen = ScreenType.REELS_VIEWER,
            reelFingerprint = ReelContentFingerprint(author = "author1"),
            nowMs = 2000L
        )

        // Normalde FREE veya SINGLE_REEL olan bir modda bile, Gece Odak Saati devredeyken BLOCK_BEDTIME dönmeli
        val bedtimeDecision = policy.evaluate(
            detectedScreen = ScreenType.REELS_VIEWER,
            tracker = tracker,
            dmMode = DmReelMode.FREE,
            exploreMode = DmReelMode.FREE,
            feedMode = DmReelMode.FREE,
            profileMode = DmReelMode.FREE,
            reelsTabMode = DmReelMode.FREE,
            isBedtimeActive = true,
            nowMs = 2100L
        )
        assertEquals(PolicyDecision.BLOCK_BEDTIME, bedtimeDecision)
        assertTrue(bedtimeDecision.shouldBlock)

        // Alt bar Reels sekmesinde de Gece Odak Saati engellemelidir
        val tabDecision = policy.evaluate(
            detectedScreen = ScreenType.REELS_TAB,
            tracker = tracker,
            dmMode = DmReelMode.FREE,
            exploreMode = DmReelMode.FREE,
            feedMode = DmReelMode.FREE,
            profileMode = DmReelMode.FREE,
            reelsTabMode = DmReelMode.FREE,
            isBedtimeActive = true,
            nowMs = 2100L
        )
        assertEquals(PolicyDecision.BLOCK_BEDTIME, tabDecision)

        // Ancak ana akış veya mesajlar gibi Reels olmayan ekranlara izin verilmelidir
        val feedDecision = policy.evaluate(
            detectedScreen = ScreenType.MAIN_FEED,
            tracker = tracker,
            dmMode = DmReelMode.BLOCK_ALL,
            isBedtimeActive = true,
            nowMs = 2100L
        )
        assertEquals(PolicyDecision.ALLOW, feedDecision)
    }

    @Test
    fun `ProtectionLevel presets should define correct modes and match properly`() {
        // BALANCED
        assertEquals(DmReelMode.SINGLE_REEL, ProtectionLevel.BALANCED.dmMode)
        assertEquals(DmReelMode.SINGLE_REEL, ProtectionLevel.BALANCED.exploreMode)
        assertEquals(DmReelMode.SINGLE_REEL, ProtectionLevel.BALANCED.feedMode)
        assertEquals(DmReelMode.SINGLE_REEL, ProtectionLevel.BALANCED.profileMode)
        assertEquals(DmReelMode.BLOCK_ALL, ProtectionLevel.BALANCED.reelsTabMode)

        // MINDFUL
        assertEquals(DmReelMode.ASK_WITH_FRICTION, ProtectionLevel.MINDFUL.dmMode)
        assertEquals(DmReelMode.ASK_WITH_FRICTION, ProtectionLevel.MINDFUL.exploreMode)
        assertEquals(DmReelMode.BLOCK_ALL, ProtectionLevel.MINDFUL.feedMode)
        assertEquals(DmReelMode.SINGLE_REEL, ProtectionLevel.MINDFUL.profileMode)
        assertEquals(DmReelMode.BLOCK_ALL, ProtectionLevel.MINDFUL.reelsTabMode)

        // STRICT
        assertEquals(DmReelMode.BLOCK_ALL, ProtectionLevel.STRICT.dmMode)
        assertEquals(DmReelMode.BLOCK_ALL, ProtectionLevel.STRICT.exploreMode)
        assertEquals(DmReelMode.BLOCK_ALL, ProtectionLevel.STRICT.feedMode)
        assertEquals(DmReelMode.BLOCK_ALL, ProtectionLevel.STRICT.profileMode)
        assertEquals(DmReelMode.BLOCK_ALL, ProtectionLevel.STRICT.reelsTabMode)

        // SINGLE_REEL
        assertEquals(DmReelMode.SINGLE_REEL, ProtectionLevel.SINGLE_REEL.dmMode)
        assertEquals(DmReelMode.SINGLE_REEL, ProtectionLevel.SINGLE_REEL.reelsTabMode)

        // COUNT_QUOTA
        assertEquals(DmReelMode.COUNT_QUOTA, ProtectionLevel.COUNT_QUOTA.dmMode)
        assertEquals(DmReelMode.COUNT_QUOTA, ProtectionLevel.COUNT_QUOTA.reelsTabMode)

        // TIME_QUOTA
        assertEquals(DmReelMode.TIME_QUOTA, ProtectionLevel.TIME_QUOTA.dmMode)
        assertEquals(DmReelMode.TIME_QUOTA, ProtectionLevel.TIME_QUOTA.reelsTabMode)

        // Matching
        val matchedBalanced = ProtectionLevel.findMatchingLevel(
            dm = DmReelMode.SINGLE_REEL,
            explore = DmReelMode.SINGLE_REEL,
            feed = DmReelMode.SINGLE_REEL,
            profile = DmReelMode.SINGLE_REEL,
            reelsTab = DmReelMode.BLOCK_ALL
        )
        assertEquals(ProtectionLevel.BALANCED, matchedBalanced)

        val matchedStrict = ProtectionLevel.findMatchingLevel(
            dm = DmReelMode.BLOCK_ALL,
            explore = DmReelMode.BLOCK_ALL,
            feed = DmReelMode.BLOCK_ALL,
            profile = DmReelMode.BLOCK_ALL,
            reelsTab = DmReelMode.BLOCK_ALL
        )
        assertEquals(ProtectionLevel.STRICT, matchedStrict)

        val matchedCountQuota = ProtectionLevel.findMatchingLevel(
            dm = DmReelMode.COUNT_QUOTA,
            explore = DmReelMode.COUNT_QUOTA,
            feed = DmReelMode.COUNT_QUOTA,
            profile = DmReelMode.COUNT_QUOTA,
            reelsTab = DmReelMode.COUNT_QUOTA
        )
        assertEquals(ProtectionLevel.COUNT_QUOTA, matchedCountQuota)

        val matchedTimeQuota = ProtectionLevel.findMatchingLevel(
            dm = DmReelMode.TIME_QUOTA,
            explore = DmReelMode.TIME_QUOTA,
            feed = DmReelMode.TIME_QUOTA,
            profile = DmReelMode.TIME_QUOTA,
            reelsTab = DmReelMode.TIME_QUOTA
        )
        assertEquals(ProtectionLevel.TIME_QUOTA, matchedTimeQuota)

        val customMatch = ProtectionLevel.findMatchingLevel(
            dm = DmReelMode.FREE,
            explore = DmReelMode.FREE,
            feed = DmReelMode.FREE,
            profile = DmReelMode.FREE,
            reelsTab = DmReelMode.FREE
        )
        assertEquals(null, customMatch)
    }

    @Test
    fun `BedtimeCalculator should correctly evaluate overnight and daytime windows`() {
        // Kapalıyken her zaman false dönmeli
        assertFalse(
            com.nomorereels.app.data.BedtimeCalculator.isBedtimeActive(
                enabled = false,
                startHour = 23,
                startMinute = 30,
                endHour = 7,
                endMinute = 0,
                currentHour = 23,
                currentMinute = 45
            )
        )

        // Gece yarısını aşan aralık: 23:30 - 07:00
        // 23:29 -> henüz başlamadı (false)
        assertFalse(
            com.nomorereels.app.data.BedtimeCalculator.isBedtimeActive(
                enabled = true,
                startHour = 23,
                startMinute = 30,
                endHour = 7,
                endMinute = 0,
                currentHour = 23,
                currentMinute = 29
            )
        )
        // 23:30 -> tam başlangıç (true)
        assertTrue(
            com.nomorereels.app.data.BedtimeCalculator.isBedtimeActive(
                enabled = true,
                startHour = 23,
                startMinute = 30,
                endHour = 7,
                endMinute = 0,
                currentHour = 23,
                currentMinute = 30
            )
        )
        // 02:15 -> gece ortası (true)
        assertTrue(
            com.nomorereels.app.data.BedtimeCalculator.isBedtimeActive(
                enabled = true,
                startHour = 23,
                startMinute = 30,
                endHour = 7,
                endMinute = 0,
                currentHour = 2,
                currentMinute = 15
            )
        )
        // 06:59 -> bitişe 1 dakika kala (true)
        assertTrue(
            com.nomorereels.app.data.BedtimeCalculator.isBedtimeActive(
                enabled = true,
                startHour = 23,
                startMinute = 30,
                endHour = 7,
                endMinute = 0,
                currentHour = 6,
                currentMinute = 59
            )
        )
        // 07:00 -> bitiş anı (false)
        assertFalse(
            com.nomorereels.app.data.BedtimeCalculator.isBedtimeActive(
                enabled = true,
                startHour = 23,
                startMinute = 30,
                endHour = 7,
                endMinute = 0,
                currentHour = 7,
                currentMinute = 0
            )
        )
        // 14:00 -> öğlen vakti (false)
        assertFalse(
            com.nomorereels.app.data.BedtimeCalculator.isBedtimeActive(
                enabled = true,
                startHour = 23,
                startMinute = 30,
                endHour = 7,
                endMinute = 0,
                currentHour = 14,
                currentMinute = 0
            )
        )

        // Gündüz aralığı: 14:00 - 18:00 (Çalışma / Odak saati)
        assertTrue(
            com.nomorereels.app.data.BedtimeCalculator.isBedtimeActive(
                enabled = true,
                startHour = 14,
                startMinute = 0,
                endHour = 18,
                endMinute = 0,
                currentHour = 15,
                currentMinute = 30
            )
        )
        assertFalse(
            com.nomorereels.app.data.BedtimeCalculator.isBedtimeActive(
                enabled = true,
                startHour = 14,
                startMinute = 0,
                endHour = 18,
                endMinute = 0,
                currentHour = 18,
                currentMinute = 0
            )
        )
    }

    @Test
    fun `COUNT_QUOTA and TIME_QUOTA modes should allow reels until limit is exceeded and then block`() {
        tracker.update(ScreenType.DIRECT_MESSAGES, nowMs = 1000L)
        tracker.update(ScreenType.REELS_VIEWER, ReelContentFingerprint(author = "author1"), nowMs = 2000L)

        // COUNT_QUOTA: limit aşılmamışken ALLOW
        val countAllowedDecision = policy.evaluate(
            detectedScreen = ScreenType.REELS_VIEWER,
            tracker = tracker,
            dmMode = DmReelMode.COUNT_QUOTA,
            isCountLimitExceeded = false,
            nowMs = 2100L
        )
        assertEquals(PolicyDecision.ALLOW, countAllowedDecision)

        // COUNT_QUOTA: limit aşıldığında BLOCK_DAILY_COUNT_LIMIT
        val countLimitDecision = policy.evaluate(
            detectedScreen = ScreenType.REELS_VIEWER,
            tracker = tracker,
            dmMode = DmReelMode.COUNT_QUOTA,
            isCountLimitExceeded = true,
            nowMs = 2100L
        )
        assertEquals(PolicyDecision.BLOCK_DAILY_COUNT_LIMIT, countLimitDecision)
        assertTrue(countLimitDecision.shouldBlock)

        // TIME_QUOTA: süre dolmamışken ALLOW
        val timeAllowedDecision = policy.evaluate(
            detectedScreen = ScreenType.REELS_VIEWER,
            tracker = tracker,
            dmMode = DmReelMode.TIME_QUOTA,
            isTimeLimitExceeded = false,
            nowMs = 2100L
        )
        assertEquals(PolicyDecision.ALLOW, timeAllowedDecision)

        // TIME_QUOTA: süre dolduğunda BLOCK_DAILY_TIME_LIMIT
        val timeLimitDecision = policy.evaluate(
            detectedScreen = ScreenType.REELS_VIEWER,
            tracker = tracker,
            dmMode = DmReelMode.TIME_QUOTA,
            isTimeLimitExceeded = true,
            nowMs = 2100L
        )
        assertEquals(PolicyDecision.BLOCK_DAILY_TIME_LIMIT, timeLimitDecision)
        assertTrue(timeLimitDecision.shouldBlock)
    }

    @Test
    fun `customSingleReelMaxDurationMs should override default single reel duration`() {
        tracker.update(ScreenType.DIRECT_MESSAGES, nowMs = 1000L)
        tracker.update(ScreenType.REELS_VIEWER, ReelContentFingerprint(author = "author1"), nowMs = 2000L)

        // 30 saniyelik özel süre sınırı ayarlandı
        val customDurationMs = 30_000L

        // 20. saniyede hâlâ serbest olmalı
        val earlyDecision = policy.evaluate(
            detectedScreen = ScreenType.REELS_VIEWER,
            tracker = tracker,
            dmMode = DmReelMode.SINGLE_REEL,
            customSingleReelMaxDurationMs = customDurationMs,
            nowMs = 22_000L
        )
        assertEquals(PolicyDecision.ALLOW, earlyDecision)

        // 31. saniyede BLOCK_AFTER_TIMEOUT dönmeli
        val expiredDecision = policy.evaluate(
            detectedScreen = ScreenType.REELS_VIEWER,
            tracker = tracker,
            dmMode = DmReelMode.SINGLE_REEL,
            customSingleReelMaxDurationMs = customDurationMs,
            nowMs = 33_000L
        )
        assertEquals(PolicyDecision.BLOCK_AFTER_TIMEOUT, expiredDecision)
    }

    @Test
    fun `resolveActiveMode should properly cascade FOLLOW_GLOBAL to generalMode`() {
        // Alt sekme FOLLOW_GLOBAL ise genel mod BLOCK_ALL'a çözülmeli
        val tabResolved = ReelsPolicy.resolveActiveMode(
            detectedScreen = ScreenType.REELS_TAB,
            originScreen = null,
            dmMode = DmReelMode.FOLLOW_GLOBAL,
            exploreMode = DmReelMode.FOLLOW_GLOBAL,
            feedMode = DmReelMode.FOLLOW_GLOBAL,
            profileMode = DmReelMode.FOLLOW_GLOBAL,
            reelsTabMode = DmReelMode.FOLLOW_GLOBAL,
            generalMode = DmReelMode.BLOCK_ALL
        )
        assertEquals(DmReelMode.BLOCK_ALL, tabResolved)

        // DM kökenli video için dmMode COUNT_QUOTA ise genel modu ezmeli
        val dmResolved = ReelsPolicy.resolveActiveMode(
            detectedScreen = ScreenType.REELS_VIEWER,
            originScreen = ScreenType.DIRECT_MESSAGES,
            dmMode = DmReelMode.COUNT_QUOTA,
            exploreMode = DmReelMode.FOLLOW_GLOBAL,
            feedMode = DmReelMode.FOLLOW_GLOBAL,
            profileMode = DmReelMode.FOLLOW_GLOBAL,
            reelsTabMode = DmReelMode.FOLLOW_GLOBAL,
            generalMode = DmReelMode.BLOCK_ALL
        )
        assertEquals(DmReelMode.COUNT_QUOTA, dmResolved)
    }

    @Test
    fun `strictnessRank and isLoosening should correctly detect willpower downgrade`() {
        assertTrue(ReelsPolicy.strictnessRank(DmReelMode.FREE) > ReelsPolicy.strictnessRank(DmReelMode.SINGLE_REEL))
        assertTrue(ReelsPolicy.strictnessRank(DmReelMode.SINGLE_REEL) > ReelsPolicy.strictnessRank(DmReelMode.BLOCK_ALL))

        // BLOCK_ALL'dan SINGLE_REEL'e geçmek bir gevşemedir (loosening)
        val isLoosened = ReelsPolicy.isLoosening(
            oldModes = listOf(DmReelMode.BLOCK_ALL),
            newModes = listOf(DmReelMode.SINGLE_REEL)
        )
        assertTrue(isLoosened)

        // SINGLE_REEL'den BLOCK_ALL'a geçmek sıkılaştırmadır (loosening DEĞİL)
        val isTightened = ReelsPolicy.isLoosening(
            oldModes = listOf(DmReelMode.SINGLE_REEL),
            newModes = listOf(DmReelMode.BLOCK_ALL)
        )
        assertFalse(isTightened)
    }
}
