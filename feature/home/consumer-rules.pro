# Consumer rules for :feature:home
-keepclassmembers class * {
    @dagger.hilt.android.lifecycle.HiltViewModel <init>(...);
}
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    @javax.inject.Inject <init>(...);
}
