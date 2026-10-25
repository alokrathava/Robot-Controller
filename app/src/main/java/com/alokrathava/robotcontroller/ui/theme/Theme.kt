package com.alokrathava.robotcontroller.ui.theme

import androidx.compose.runtime.Composable
import com.alokrathava.theme.AppColorTheme
import com.alokrathava.theme.MonochromeTheme

// Application wrapper for MonochromeTheme supporting configurable color themes.
@Composable
fun RobotControllerTheme(
    darkTheme: Boolean = false,
    colorTheme: AppColorTheme = AppColorTheme.CALM_TEAL,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MonochromeTheme(
        darkTheme = darkTheme,
        colorTheme = colorTheme,
        content = content
    )
}
