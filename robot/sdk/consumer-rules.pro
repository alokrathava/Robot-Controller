# Consumer rules for Robot SDK

# Retain line numbers and attributes for stack traces and debugging
-keepattributes LineNumberTable,SourceFile
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-renamesourcefileattribute SourceFile

# Kotlinx Serialization rules for SDK serialized objects
-keep,allowobfuscation,allowshrinking @kotlinx.serialization.Serializable class *
-keepclassmembers class * {
    @kotlinx.serialization.Serializable static ** Companion;
}
-keepclassmembers class **$serializer {
    *** INSTANCE;
}
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}

# Preserve public Robot SDK API surface for consuming applications
-keep public class com.alokrathava.sdk.RobotClient { public *; }
-keep public class com.alokrathava.sdk.RobotSdk { public *; }
-keep public class com.alokrathava.sdk.RobotSdkConfig { public *; }
-keep public class com.alokrathava.sdk.RobotSdkBuildVersion { public *; }
-keep public class com.alokrathava.sdk.RobotManager { public *; }
-keep public class com.alokrathava.sdk.RobotRepository { public *; }
-keep public class com.alokrathava.sdk.discovery.** { public *; }
-keep public class com.alokrathava.sdk.security.** { public *; }

# Preserve data models, error models, and protocol payloads
-keep class com.alokrathava.sdk.model.** { *; }
-keep class com.alokrathava.sdk.error.** { *; }
-keep class com.alokrathava.sdk.internal.protocol.** { *; }
