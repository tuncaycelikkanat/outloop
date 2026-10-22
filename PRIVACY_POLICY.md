# Privacy Policy for NoMoreReels

**Last Updated:** October 2026

## 1. Overview
NoMoreReels ("we", "our", or "the app") is a minimalist digital wellness and focus tool built for Android. Our core design philosophy is **Zero Data Collection**: we believe your attention and digital privacy belong entirely to you.

## 2. Zero Data Collection & Serverless Architecture
NoMoreReels operates with **zero external servers, zero analytics, zero crash reporters, and zero trackers**. 

Network permissions (`android.permission.INTERNET` and `android.permission.ACCESS_NETWORK_STATE`) are included solely by the Google Play Billing Library (`com.android.billingclient`) to communicate directly with Google Play's payment servers for optional in-app developer tips. The core focus tracking, Reels detection, and accessibility services operate entirely offline on your device and are technically incapable of transmitting any user data.

## 3. Use of Android AccessibilityService API
NoMoreReels uses the Android `AccessibilityService` API strictly to help users manage their screen time and overcome compulsive social media scrolling habits.

- **What the service detects:** The service monitors window and scroll events specifically within the Instagram application (`com.instagram.android`) to identify whether the user has navigated into the short-form video ("Reels") player tab or modal viewer.
- **What actions the service takes:** Upon detecting a Reels view that violates user-configured focus goals or time limits, the service triggers an automated "back" navigation action or switches to the main feed tab to exit the video player.
- **What the service NEVER does:**
  - The service **never** reads, intercepts, or logs personal text messages, direct messages (DMs), user posts, comments, contacts, or notifications.
  - The service **never** captures or monitors keystrokes, passwords, or personal credentials.
  - The service **never** transmits, shares, or stores any screen content.

## 4. Other Android Permissions
- `android.permission.SYSTEM_ALERT_WINDOW`: Used exclusively to display a local, on-device mindful pause screen (breathing friction / quota warning badge) over the screen when focus limits are reached.
- `android.permission.VIBRATE`: Used exclusively to provide subtle tactile haptic feedback when a block action occurs or when settings are toggled.
- `android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`: Requested optionally to prevent aggressive manufacturer battery killers from terminating the on-device accessibility background observer.

## 5. In-App Purchases and Donations
NoMoreReels offers optional tip jar items to support open-source development. All transactions are securely processed entirely through Google Play's In-App Billing system (`com.android.billingclient`). We do not collect, store, or have access to any payment card or banking details.

## 6. Children's Privacy
NoMoreReels does not collect any personal information from anyone, including children under the age of 13.

## 7. Open Source & Transparency
The source code of NoMoreReels is open and transparent. Anyone can inspect and verify our strict privacy claims and code integrity.

## 8. Contact
If you have any questions or feedback regarding this Privacy Policy, please contact us via our GitHub repository issues or developer contact information provided on Google Play.
