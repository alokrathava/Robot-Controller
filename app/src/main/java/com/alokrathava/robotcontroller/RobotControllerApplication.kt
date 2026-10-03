package com.alokrathava.robotcontroller

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class RobotControllerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Force application-wide Light/White Mode regardless of system dark mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }
}
