package com.alokrathava.robotcontroller.ui.theme

import androidx.compose.runtime.Composable
import com.alokrathava.theme.MonochromeTheme

@Composable
fun RobotControllerTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MonochromeTheme(
        darkTheme = darkTheme,
        content = content
    )
}
