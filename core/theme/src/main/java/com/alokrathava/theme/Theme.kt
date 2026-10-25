package com.alokrathava.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

// Resolves the active monochrome colors based on the dark theme flag and selected app color theme.
@Composable
fun MonochromeTheme(
    darkTheme: Boolean = false,
    colorTheme: AppColorTheme = AppColorTheme.CALM_TEAL,
    content: @Composable () -> Unit
) {
    val monochromeColors = if (darkTheme) {
        DarkMonochromeColors
    } else {
        when (colorTheme) {
            AppColorTheme.INDUSTRIAL_BLUE -> IndustrialBlueColors
            AppColorTheme.CALM_TEAL -> CalmTealColors
            AppColorTheme.GRAPHITE_BLUE -> GraphiteBlueColors
            AppColorTheme.SAGE_GREEN -> SageGreenColors
            AppColorTheme.MUTED_INDIGO -> MutedIndigoColors
            AppColorTheme.DEEP_OCEAN -> DeepOceanColors
        }
    }
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
