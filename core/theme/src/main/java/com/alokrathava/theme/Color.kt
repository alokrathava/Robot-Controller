package com.alokrathava.theme

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
    val statusActive: Color,
    val onStatusActive: Color,
    val statusActiveContainer: Color,
    val statusWarning: Color,
    val onStatusWarning: Color,
    val statusWarningContainer: Color,
    val statusCritical: Color,
    val onStatusCritical: Color,
    val statusCriticalContainer: Color,
    val statusInfo: Color,
    val onStatusInfo: Color,
    val statusInfoContainer: Color,
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
    statusActive = Color(0xFF22C55E),
    onStatusActive = Color(0xFFFFFFFF),
    statusActiveContainer = Color(0x2222C55E),
    statusWarning = Color(0xFFF59E0B),
    onStatusWarning = Color(0xFF101010),
    statusWarningContainer = Color(0x22F59E0B),
    statusCritical = Color(0xFFEF4444),
    onStatusCritical = Color(0xFFFFFFFF),
    statusCriticalContainer = Color(0x22EF4444),
    statusInfo = Color(0xFF38BDF8),
    onStatusInfo = Color(0xFF101010),
    statusInfoContainer = Color(0x2238BDF8),
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
    statusActive = Color(0xFF16A34A),
    onStatusActive = Color(0xFFFFFFFF),
    statusActiveContainer = Color(0xFFECFDF5),
    statusWarning = Color(0xFFD97706),
    onStatusWarning = Color(0xFFFFFFFF),
    statusWarningContainer = Color(0xFFFFFBEB),
    statusCritical = Color(0xFFDC2626),
    onStatusCritical = Color(0xFFFFFFFF),
    statusCriticalContainer = Color(0xFFFEF2F2),
    statusInfo = Color(0xFF0284C7),
    onStatusInfo = Color(0xFFFFFFFF),
    statusInfoContainer = Color(0xFFEFF6FF),
    isDark = false
)

// Represents the available color themes for robot control interfaces.
enum class AppColorTheme(val title: String, val description: String) {
    INDUSTRIAL_BLUE("Industrial Blue", "Professional Robotics & Engineering"),
    CALM_TEAL("Calm Teal", "Modern Robotics & Companion Systems"),
    GRAPHITE_BLUE("Graphite Blue", "Premium Industrial & Minimal Control"),
    SAGE_GREEN("Sage Green", "Calm Operations & Service Systems"),
    MUTED_INDIGO("Muted Indigo", "Modern Technology & SDK Demos"),
    DEEP_OCEAN("Deep Ocean", "Refined Autonomous Navigation")
}

// Industrial Blue theme palette optimized for professional robotics and engineering dashboards.
val IndustrialBlueColors = MonochromeColors(
    background = Color(0xFFF5F7FA),
    surface = Color(0xFFFFFFFF),
    elevatedSurface = Color(0xFFE9EFF5),
    interactiveSurface = Color(0xFFE9EFF5),
    hoverSurface = Color(0xFFDCEAFB),
    pressedSurface = Color(0xFFDCEAFB),
    disabledSurface = Color(0xFFE9EFF5),
    strongBorder = Color(0xFFBCCCDC),
    defaultBorder = Color(0xFFDCEAFB),
    subtleBorder = Color(0xFFE9EFF5),
    primaryText = Color(0xFF172536),
    secondaryText = Color(0xFF334E68),
    mutedText = Color(0xFF627D98),
    disabledText = Color(0xFF9FB3C8),
    primaryActionBg = Color(0xFF2457A7),
    primaryActionFg = Color(0xFFFFFFFF),
    primaryActionHoverBg = Color(0xFF1B4382),
    secondaryActionBorder = Color(0xFFBCCCDC),
    secondaryActionText = Color(0xFF172536),
    secondaryActionHoverBg = Color(0xFFDCEAFB),
    focusBorder = Color(0xFF2457A7),
    statusActive = Color(0xFF16A34A),
    onStatusActive = Color(0xFFFFFFFF),
    statusActiveContainer = Color(0xFFECFDF5),
    statusWarning = Color(0xFFD97706),
    onStatusWarning = Color(0xFFFFFFFF),
    statusWarningContainer = Color(0xFFFFFBEB),
    statusCritical = Color(0xFFDC2626),
    onStatusCritical = Color(0xFFFFFFFF),
    statusCriticalContainer = Color(0xFFFEF2F2),
    statusInfo = Color(0xFF0284C7),
    onStatusInfo = Color(0xFFFFFFFF),
    statusInfoContainer = Color(0xFFEFF6FF),
    isDark = false
)

// Calm Teal theme palette optimized for companion robots and service robotics.
val CalmTealColors = MonochromeColors(
    background = Color(0xFFF4F8F8),
    surface = Color(0xFFFFFFFF),
    elevatedSurface = Color(0xFFE5F0F0),
    interactiveSurface = Color(0xFFE5F0F0),
    hoverSurface = Color(0xFFD8EEEF),
    pressedSurface = Color(0xFFD8EEEF),
    disabledSurface = Color(0xFFE5F0F0),
    strongBorder = Color(0xFF9EC4C7),
    defaultBorder = Color(0xFFD8EEEF),
    subtleBorder = Color(0xFFE5F0F0),
    primaryText = Color(0xFF193638),
    secondaryText = Color(0xFF2E5E61),
    mutedText = Color(0xFF62898C),
    disabledText = Color(0xFF9FB3B5),
    primaryActionBg = Color(0xFF087582),
    primaryActionFg = Color(0xFFFFFFFF),
    primaryActionHoverBg = Color(0xFF065A65),
    secondaryActionBorder = Color(0xFF9EC4C7),
    secondaryActionText = Color(0xFF193638),
    secondaryActionHoverBg = Color(0xFFD8EEEF),
    focusBorder = Color(0xFF087582),
    statusActive = Color(0xFF16A34A),
    onStatusActive = Color(0xFFFFFFFF),
    statusActiveContainer = Color(0xFFECFDF5),
    statusWarning = Color(0xFFD97706),
    onStatusWarning = Color(0xFFFFFFFF),
    statusWarningContainer = Color(0xFFFFFBEB),
    statusCritical = Color(0xFFDC2626),
    onStatusCritical = Color(0xFFFFFFFF),
    statusCriticalContainer = Color(0xFFFEF2F2),
    statusInfo = Color(0xFF0284C7),
    onStatusInfo = Color(0xFFFFFFFF),
    statusInfoContainer = Color(0xFFEFF6FF),
    isDark = false
)

// Graphite Blue theme palette for premium industrial systems and minimal control panels.
val GraphiteBlueColors = MonochromeColors(
    background = Color(0xFFF5F6F7),
    surface = Color(0xFFFFFFFF),
    elevatedSurface = Color(0xFFE6E9EC),
    interactiveSurface = Color(0xFFE6E9EC),
    hoverSurface = Color(0xFFDFE7EF),
    pressedSurface = Color(0xFFDFE7EF),
    disabledSurface = Color(0xFFE6E9EC),
    strongBorder = Color(0xFFCBD5E1),
    defaultBorder = Color(0xFFDFE7EF),
    subtleBorder = Color(0xFFE6E9EC),
    primaryText = Color(0xFF202B36),
    secondaryText = Color(0xFF485A6C),
    mutedText = Color(0xFF7A8B9B),
    disabledText = Color(0xFF94A3B8),
    primaryActionBg = Color(0xFF40566D),
    primaryActionFg = Color(0xFFFFFFFF),
    primaryActionHoverBg = Color(0xFF324456),
    secondaryActionBorder = Color(0xFFCBD5E1),
    secondaryActionText = Color(0xFF202B36),
    secondaryActionHoverBg = Color(0xFFDFE7EF),
    focusBorder = Color(0xFF40566D),
    statusActive = Color(0xFF16A34A),
    onStatusActive = Color(0xFFFFFFFF),
    statusActiveContainer = Color(0xFFECFDF5),
    statusWarning = Color(0xFFD97706),
    onStatusWarning = Color(0xFFFFFFFF),
    statusWarningContainer = Color(0xFFFFFBEB),
    statusCritical = Color(0xFFDC2626),
    onStatusCritical = Color(0xFFFFFFFF),
    statusCriticalContainer = Color(0xFFFEF2F2),
    statusInfo = Color(0xFF0284C7),
    onStatusInfo = Color(0xFFFFFFFF),
    statusInfoContainer = Color(0xFFEFF6FF),
    isDark = false
)

// Sage Green theme palette for service robots and healthcare-oriented environments.
val SageGreenColors = MonochromeColors(
    background = Color(0xFFF6F8F5),
    surface = Color(0xFFFFFFFF),
    elevatedSurface = Color(0xFFE8EEE9),
    interactiveSurface = Color(0xFFE8EEE9),
    hoverSurface = Color(0xFFDEEBE5),
    pressedSurface = Color(0xFFDEEBE5),
    disabledSurface = Color(0xFFE8EEE9),
    strongBorder = Color(0xFFB5C9C0),
    defaultBorder = Color(0xFFDEEBE5),
    subtleBorder = Color(0xFFE8EEE9),
    primaryText = Color(0xFF24352F),
    secondaryText = Color(0xFF3E5A50),
    mutedText = Color(0xFF708C82),
    disabledText = Color(0xFF9CA3AF),
    primaryActionBg = Color(0xFF386B60),
    primaryActionFg = Color(0xFFFFFFFF),
    primaryActionHoverBg = Color(0xFF2B534B),
    secondaryActionBorder = Color(0xFFB5C9C0),
    secondaryActionText = Color(0xFF24352F),
    secondaryActionHoverBg = Color(0xFFDEEBE5),
    focusBorder = Color(0xFF386B60),
    statusActive = Color(0xFF16A34A),
    onStatusActive = Color(0xFFFFFFFF),
    statusActiveContainer = Color(0xFFECFDF5),
    statusWarning = Color(0xFFD97706),
    onStatusWarning = Color(0xFFFFFFFF),
    statusWarningContainer = Color(0xFFFFFBEB),
    statusCritical = Color(0xFFDC2626),
    onStatusCritical = Color(0xFFFFFFFF),
    statusCriticalContainer = Color(0xFFFEF2F2),
    statusInfo = Color(0xFF0284C7),
    onStatusInfo = Color(0xFFFFFFFF),
    statusInfoContainer = Color(0xFFEFF6FF),
    isDark = false
)

// Muted Indigo theme palette for robotics software and autonomous systems.
val MutedIndigoColors = MonochromeColors(
    background = Color(0xFFF7F7FB),
    surface = Color(0xFFFFFFFF),
    elevatedSurface = Color(0xFFECECF5),
    interactiveSurface = Color(0xFFECECF5),
    hoverSurface = Color(0xFFE5E3F6),
    pressedSurface = Color(0xFFE5E3F6),
    disabledSurface = Color(0xFFECECF5),
    strongBorder = Color(0xFFC7C4E8),
    defaultBorder = Color(0xFFE5E3F6),
    subtleBorder = Color(0xFFECECF5),
    primaryText = Color(0xFF26243D),
    secondaryText = Color(0xFF47436B),
    mutedText = Color(0xFF7C789E),
    disabledText = Color(0xFFA5B4FC),
    primaryActionBg = Color(0xFF514B91),
    primaryActionFg = Color(0xFFFFFFFF),
    primaryActionHoverBg = Color(0xFF403B73),
    secondaryActionBorder = Color(0xFFC7C4E8),
    secondaryActionText = Color(0xFF26243D),
    secondaryActionHoverBg = Color(0xFFE5E3F6),
    focusBorder = Color(0xFF514B91),
    statusActive = Color(0xFF16A34A),
    onStatusActive = Color(0xFFFFFFFF),
    statusActiveContainer = Color(0xFFECFDF5),
    statusWarning = Color(0xFFD97706),
    onStatusWarning = Color(0xFFFFFFFF),
    statusWarningContainer = Color(0xFFFFFBEB),
    statusCritical = Color(0xFFDC2626),
    onStatusCritical = Color(0xFFFFFFFF),
    statusCriticalContainer = Color(0xFFFEF2F2),
    statusInfo = Color(0xFF0284C7),
    onStatusInfo = Color(0xFFFFFFFF),
    statusInfoContainer = Color(0xFFEFF6FF),
    isDark = false
)

// Deep Ocean theme palette for autonomous navigation systems and technical operator interfaces.
val DeepOceanColors = MonochromeColors(
    background = Color(0xFFF3F7F9),
    surface = Color(0xFFFFFFFF),
    elevatedSurface = Color(0xFFE3EDF2),
    interactiveSurface = Color(0xFFE3EDF2),
    hoverSurface = Color(0xFFD7EAF2),
    pressedSurface = Color(0xFFD7EAF2),
    disabledSurface = Color(0xFFE3EDF2),
    strongBorder = Color(0xFFA8C5D4),
    defaultBorder = Color(0xFFD7EAF2),
    subtleBorder = Color(0xFFE3EDF2),
    primaryText = Color(0xFF183443),
    secondaryText = Color(0xFF2E5467),
    mutedText = Color(0xFF6B8B9B),
    disabledText = Color(0xFF94A3B8),
    primaryActionBg = Color(0xFF175A78),
    primaryActionFg = Color(0xFFFFFFFF),
    primaryActionHoverBg = Color(0xFF11435C),
    secondaryActionBorder = Color(0xFFA8C5D4),
    secondaryActionText = Color(0xFF183443),
    secondaryActionHoverBg = Color(0xFFD7EAF2),
    focusBorder = Color(0xFF175A78),
    statusActive = Color(0xFF16A34A),
    onStatusActive = Color(0xFFFFFFFF),
    statusActiveContainer = Color(0xFFECFDF5),
    statusWarning = Color(0xFFD97706),
    onStatusWarning = Color(0xFFFFFFFF),
    statusWarningContainer = Color(0xFFFFFBEB),
    statusCritical = Color(0xFFDC2626),
    onStatusCritical = Color(0xFFFFFFFF),
    statusCriticalContainer = Color(0xFFFEF2F2),
    statusInfo = Color(0xFF0284C7),
    onStatusInfo = Color(0xFFFFFFFF),
    statusInfoContainer = Color(0xFFEFF6FF),
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
