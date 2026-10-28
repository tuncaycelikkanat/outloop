# Privacy Policy for NoMoreReels

**Last Updated:** October 2026

## 1. Overview
NoMoreReels ("we", "our", or "the app") is a minimalist digital wellness and focus tool built for Android. Our core design philosophy is **Zero Data Collection**: we believe your attention and digital privacy belong entirely to you.

## 2. Zero Data Collection & Serverless Architecture
NoMoreReels operates with **zero external servers, zero analytics, zero crash reporters, and zero trackers**. 

The application does **NOT** request or declare network permissions (`android.permission.INTERNET` or `android.permission.ACCESS_NETWORK_STATE`). It operates 100% offline on your device and is technically incapable of transmitting any user data.

## 3. Use of Android AccessibilityService API
NoMoreReels uses the Android `AccessibilityService` API strictly to help users manage their screen time and overcome compulsive social media scrolling habits.

- **What the service detects:** The service monitors window and scroll events specifically within the Instagram application (`com.instagram.android`) to identify whether the user has navigated into the short-form video ("Reels") player tab or modal viewer.
- **What actions the service takes:** Upon detecting a Reels view that violates user-configured focus goals or time limits, the service triggers an automated "back" navigation action or switches to the main feed tab to exit the video player.
- **What the service NEVER does:**
  - The service **never** reads, intercepts, or logs personal text messages, direct messages (DMs), user posts, comments, contacts, or notifications.
  - The service **never** captures or monitors keystrokes, passwords, or personal credentials.
  - The service **never** transmits, shares, or stores any screen content.

## 4. Other Android Permissions
- `android.permission.SYSTEM_ALERT_WINDOW`: Used optionally / via accessibility overlay to display a local, on-device mindful pause screen (breathing friction / quota warning badge) over the screen when focus limits are reached.
- `android.permission.VIBRATE`: Used exclusively to provide subtle tactile haptic feedback when a block action occurs or when settings are toggled.
- `android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`: Requested optionally to prevent aggressive manufacturer battery killers from terminating the on-device accessibility background observer.

## 5. Voluntary Donations and Support
NoMoreReels offers optional external links (such as Kreosus, Buy Me a Coffee, and GitHub Sponsors) for users who wish to support open-source development. All donation pages are opened outside the application in your standard web browser. The app itself never collects, handles, or has access to any financial or payment information.

## 6. Children's Privacy
NoMoreReels does not collect any personal information from anyone, including children under the age of 13.

## 7. Open Source & Transparency
The source code of NoMoreReels is open and transparent. Anyone can inspect and verify our strict privacy claims and code integrity.

## 8. Contact
If you have any questions or feedback regarding this Privacy Policy, please contact us via our GitHub repository issues or developer contact information provided on Google Play.
