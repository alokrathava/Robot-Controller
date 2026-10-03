# R8 / ProGuard rules for Controller Application (:app)

# Retain line numbers and source attributes for crash reporting & stack traces
-keepattributes LineNumberTable,SourceFile
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-renamesourcefileattribute SourceFile

# Hilt & Dependency Injection
-keep class * extends android.app.Application
-keepclassmembers class * {
    @dagger.hilt.android.lifecycle.HiltViewModel <init>(...);
}
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    @javax.inject.Inject <init>(...);
}

# Room Database
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Jetpack Compose
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}

# Kotlin Coroutines
-dontwarn kotlinx.coroutines.**

# Preserve Application classes
-keep class com.alokrathava.robotcontroller.** { *; }
