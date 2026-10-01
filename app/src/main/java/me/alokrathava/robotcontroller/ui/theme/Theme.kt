package me.alokrathava.robotcontroller.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import com.agrathava.theme.MonochromeTheme

@Composable
fun RobotControllerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MonochromeTheme(
        darkTheme = darkTheme,
        content = content
    )
}
