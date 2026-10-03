# Consumer rules for :feature:robotstatus
-keepclassmembers class * {
    @dagger.hilt.android.lifecycle.HiltViewModel <init>(...);
}
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    @javax.inject.Inject <init>(...);
}
