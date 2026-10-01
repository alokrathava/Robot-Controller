package com.agrathava.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Default font families for UI and Monospace/Code metadata
val DefaultSansFontFamily = FontFamily.Default
val TechnicalMonoFontFamily = FontFamily.Monospace

@Immutable
data class MonochromeTypography(
    val display: TextStyle = TextStyle(
        fontFamily = DefaultSansFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 48.sp,
        lineHeight = 56.sp
    ),
    val h1: TextStyle = TextStyle(
        fontFamily = DefaultSansFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 36.sp,
        lineHeight = 44.sp
    ),
    val h2: TextStyle = TextStyle(
        fontFamily = DefaultSansFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 30.sp,
        lineHeight = 38.sp
    ),
    val h3: TextStyle = TextStyle(
        fontFamily = DefaultSansFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp
    ),
    val h4: TextStyle = TextStyle(
        fontFamily = DefaultSansFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp
    ),
    val bodyLarge: TextStyle = TextStyle(
        fontFamily = DefaultSansFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 28.sp
    ),
    val body: TextStyle = TextStyle(
        fontFamily = DefaultSansFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    val bodySmall: TextStyle = TextStyle(
        fontFamily = DefaultSansFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    val label: TextStyle = TextStyle(
        fontFamily = DefaultSansFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    val caption: TextStyle = TextStyle(
        fontFamily = DefaultSansFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    // Monospace variants for code, technical metadata, timestamps, IDs, API data
    val monoBody: TextStyle = TextStyle(
        fontFamily = TechnicalMonoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    val monoCaption: TextStyle = TextStyle(
        fontFamily = TechnicalMonoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )
)

val DefaultMonochromeTypography = MonochromeTypography()

val LocalMonochromeTypography = staticCompositionLocalOf { DefaultMonochromeTypography }

// Material3 Typography Mapping
val Material3Typography = Typography(
    displayLarge = DefaultMonochromeTypography.display,
    headlineLarge = DefaultMonochromeTypography.h1,
    headlineMedium = DefaultMonochromeTypography.h2,
    headlineSmall = DefaultMonochromeTypography.h3,
    titleLarge = DefaultMonochromeTypography.h4,
    bodyLarge = DefaultMonochromeTypography.bodyLarge,
    bodyMedium = DefaultMonochromeTypography.body,
    bodySmall = DefaultMonochromeTypography.bodySmall,
    labelLarge = DefaultMonochromeTypography.label,
    labelMedium = DefaultMonochromeTypography.label,
    labelSmall = DefaultMonochromeTypography.caption
)
