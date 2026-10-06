// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}

tasks.register("testAndAssemble") {
    group = "verification"
    description = "Runs unit tests for SDK, manual control, home features, and assembles the debug APK in a single optimized pass."
    dependsOn(
        ":robot:sdk:testDebugUnitTest",
        ":feature:manualcontrol:testDebugUnitTest",
        ":feature:home:testDebugUnitTest",
        ":app:assembleDebug"
    )
}
