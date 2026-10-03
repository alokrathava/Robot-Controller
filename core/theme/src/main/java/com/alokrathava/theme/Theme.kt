package com.alokrathava.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

@Composable
fun MonochromeTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val monochromeColors = if (darkTheme) DarkMonochromeColors else LightMonochromeColors
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(
        LocalMonochromeColors provides monochromeColors,
        LocalMonochromeTypography provides DefaultMonochromeTypography,
        LocalMonochromeSpacing provides DefaultMonochromeSpacing,
        LocalMonochromeShapes provides DefaultMonochromeShapes
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Material3Typography,
            shapes = Material3Shapes,
            content = content
        )
    }
}

object MonochromeTheme {
    val colors: MonochromeColors
        @Composable
        @ReadOnlyComposable
        get() = LocalMonochromeColors.current

    val typography: MonochromeTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalMonochromeTypography.current

    val spacing: MonochromeSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalMonochromeSpacing.current

    val shapes: MonochromeShapes
        @Composable
        @ReadOnlyComposable
        get() = LocalMonochromeShapes.current
}
