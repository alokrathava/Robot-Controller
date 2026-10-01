package com.agrathava.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ==========================================
// 1. Core Monochrome Palette Tokens
// ==========================================

// Black Scale
val Black1000 = Color(0xFF000000)
val Black950 = Color(0xFF080808)
val Black900 = Color(0xFF101010)
val Black850 = Color(0xFF161616)
val Black800 = Color(0xFF1C1C1C)
val Black750 = Color(0xFF242424)
val Black700 = Color(0xFF2C2C2C)
val Black600 = Color(0xFF404040)
val Black500 = Color(0xFF525252)

// Gray Scale
val Gray500 = Color(0xFF737373)
val Gray400 = Color(0xFF8A8A8A)
val Gray300 = Color(0xFFA3A3A3)
val Gray250 = Color(0xFFB8B8B8)
val Gray200 = Color(0xFFD4D4D4)
val Gray150 = Color(0xFFE5E5E5)
val Gray100 = Color(0xFFF0F0F0)
val Gray50 = Color(0xFFF7F7F7)

// White Scale
val White90 = Color(0xFFFAFAFA)
val White95 = Color(0xFFFCFCFC)
val White100 = Color(0xFFFFFFFF)

// Additional Subtle Border Token
val SubtleBorderDark = Color(0xFF1F1F1F)

// ==========================================
// 2. Custom Extended Semantic Colors Class
// ==========================================

@Immutable
data class MonochromeColors(
    val background: Color,
    val surface: Color,
    val elevatedSurface: Color,
    val interactiveSurface: Color,
    val hoverSurface: Color,
    val pressedSurface: Color,
    val disabledSurface: Color,
    val strongBorder: Color,
    val defaultBorder: Color,
    val subtleBorder: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val mutedText: Color,
    val disabledText: Color,
    val primaryActionBg: Color,
    val primaryActionFg: Color,
    val primaryActionHoverBg: Color,
    val secondaryActionBorder: Color,
    val secondaryActionText: Color,
    val secondaryActionHoverBg: Color,
    val focusBorder: Color,
    val isDark: Boolean
)

val DarkMonochromeColors = MonochromeColors(
    background = Black950,
    surface = Black900,
    elevatedSurface = Black850,
    interactiveSurface = Black800,
    hoverSurface = Black750,
    pressedSurface = Black700,
    disabledSurface = Black900,
    strongBorder = Black600,
    defaultBorder = Black700,
    subtleBorder = SubtleBorderDark,
    primaryText = White90,
    secondaryText = Gray250,
    mutedText = Gray400,
    disabledText = Black500,
    primaryActionBg = White90,
    primaryActionFg = Black900,
    primaryActionHoverBg = Gray150,
    secondaryActionBorder = Black600,
    secondaryActionText = White90,
    secondaryActionHoverBg = Black800,
    focusBorder = White90,
    isDark = true
)

val LightMonochromeColors = MonochromeColors(
    background = White90,
    surface = White100,
    elevatedSurface = White95,
    interactiveSurface = Gray100,
    hoverSurface = Gray150,
    pressedSurface = Gray150,
    disabledSurface = Gray100,
    strongBorder = Gray300,
    defaultBorder = Gray200,
    subtleBorder = Gray150,
    primaryText = Black900,
    secondaryText = Black600,
    mutedText = Gray500,
    disabledText = Gray300,
    primaryActionBg = Black900,
    primaryActionFg = White100,
    primaryActionHoverBg = Black750,
    secondaryActionBorder = Gray200,
    secondaryActionText = Black900,
    secondaryActionHoverBg = Gray100,
    focusBorder = Black900,
    isDark = false
)

val LocalMonochromeColors = staticCompositionLocalOf { DarkMonochromeColors }

// ==========================================
// 3. Material3 ColorScheme Mapping
// ==========================================

val DarkColorScheme: ColorScheme = darkColorScheme(
    primary = White90,
    onPrimary = Black900,
    primaryContainer = Black800,
    onPrimaryContainer = White90,
    secondary = Gray250,
    onSecondary = Black900,
    secondaryContainer = Black850,
    onSecondaryContainer = White90,
    tertiary = Gray400,
    onTertiary = Black900,
    background = Black950,
    onBackground = White90,
    surface = Black900,
    onSurface = White90,
    surfaceVariant = Black850,
    onSurfaceVariant = Gray250,
    outline = Black700,
    outlineVariant = Black600,
    inverseSurface = White90,
    inverseOnSurface = Black900
)

val LightColorScheme: ColorScheme = lightColorScheme(
    primary = Black900,
    onPrimary = White100,
    primaryContainer = Gray100,
    onPrimaryContainer = Black900,
    secondary = Black600,
    onSecondary = White100,
    secondaryContainer = White95,
    onSecondaryContainer = Black900,
    tertiary = Gray500,
    onTertiary = White100,
    background = White90,
    onBackground = Black900,
    surface = White100,
    onSurface = Black900,
    surfaceVariant = White95,
    onSurfaceVariant = Black600,
    outline = Gray200,
    outlineVariant = Gray300,
    inverseSurface = Black900,
    inverseOnSurface = White100
)
