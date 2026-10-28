# ==============================================================================
# ProGuard / R8 Rules for NoMoreReels (Production Release)
# ==============================================================================

# 1. Android Accessibility Service & System Components
-keep public class com.nomorereels.app.service.ReelsAccessibilityService {
    public <init>();
    *;
}
-keep public class com.nomorereels.app.widget.QuotaAppWidgetProvider {
    public <init>();
    *;
}

# 2. DataStore & Serialization Models
-keepclassmembers class * {
    @androidx.annotation.Keep <fields>;
    @androidx.annotation.Keep <methods>;
}
-keep public class com.nomorereels.app.OutloopApplication {
    public <init>();
    *;
}
-keep class com.nomorereels.app.detection.RuleSet { *; }
-keep class com.nomorereels.app.detection.ScreenRule { *; }
-keep class com.nomorereels.app.detection.ActionTargetsRule { *; }

# 4. Strip Debug & Verbose Logging from Release Builds
# Release derlemesinde Log.d ve Log.v çağrılarını bayt kodundan tamamen siler
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}

# 5. Jetpack Compose Runtime Rules
-keepclassmembers class * extends androidx.compose.runtime.State { *; }
