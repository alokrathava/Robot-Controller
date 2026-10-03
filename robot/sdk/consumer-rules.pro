# Consumer rules for Robot SDK
-keepclassmembers class * {
    *** Companion;
}
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}
-keep class com.alokrathava.sdk.model.** { *; }
-keep class com.alokrathava.sdk.error.** { *; }
-keep class com.alokrathava.sdk.RobotClient { *; }
-keep class com.alokrathava.sdk.RobotSdk { *; }
-keep class com.alokrathava.sdk.RobotSdkConfig { *; }
