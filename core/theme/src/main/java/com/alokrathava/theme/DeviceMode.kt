package com.alokrathava.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class DeviceMode {
    TABLET,
    MOBILE
}

val LocalDeviceMode = staticCompositionLocalOf { DeviceMode.TABLET }

fun calculateDeviceMode(width: Dp): DeviceMode {
    return if (width < 600.dp) DeviceMode.MOBILE else DeviceMode.TABLET
}

val MonochromeTheme.deviceMode: DeviceMode
    @Composable
    @ReadOnlyComposable
    get() = LocalDeviceMode.current
