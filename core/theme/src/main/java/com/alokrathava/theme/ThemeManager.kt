package com.alokrathava.theme

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Manages the application-wide active color theme and emits updates to reactive observers.
object ThemeManager {
    private val _currentTheme = MutableStateFlow(AppColorTheme.CALM_TEAL)
    val currentTheme: StateFlow<AppColorTheme> = _currentTheme.asStateFlow()

    // Updates the active application color theme across all observing screens.
    fun setTheme(theme: AppColorTheme) {
        _currentTheme.value = theme
    }
}
