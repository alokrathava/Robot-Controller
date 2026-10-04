package com.alokrathava.manualcontrol

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alokrathava.home.Sidebar
import com.alokrathava.home.SidebarNavItem
import com.alokrathava.sdk.model.ConnectionStatus
import com.alokrathava.theme.*
import kotlin.math.roundToInt

enum class ManualControlTab(val title: String) {
    MOVEMENT("Movement"),
    ROTATION("Rotation"),
    TELEMETRY("Telemetry"),
    DIAGNOSTICS("Diagnostics")
}

enum class ManualDirection {
    FORWARD,
    BACKWARD,
    LEFT,
    RIGHT
}

@Composable
fun ManualControlScreen(
    modifier: Modifier = Modifier,
    viewModel: ManualControlViewModel? = null,
    onSidebarItemSelected: (SidebarNavItem) -> Unit = {}
) {
    if (viewModel != null) {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        ManualControlContent(
            uiState = uiState,
            onTabSelected = viewModel::selectTab,
            onSpeedChanged = viewModel::updateSpeed,
            onDirectionClick = viewModel::moveDirection,
            onDirectionHoldStart = viewModel::startDirectionHold,
            onDirectionHoldStop = viewModel::stopDirectionHold,
            onJoystickPositionChanged = viewModel::updateJoystickPosition,
            onEmergencyBrake = viewModel::triggerEmergencyBrake,
            onResetEmergencyBrake = viewModel::resetEmergencyBrake,
            onReconnect = viewModel::reconnect,
            onSidebarItemSelected = onSidebarItemSelected,
            modifier = modifier
        )
    } else {
        ManualControlContent(
            onSidebarItemSelected = onSidebarItemSelected,
            modifier = modifier
        )
    }
}

@Composable
fun ManualControlContent(
    uiState: ManualControlUiState = ManualControlUiState(),
    onTabSelected: (ManualControlTab) -> Unit = {},
    onSpeedChanged: (Float) -> Unit = {},
    onDirectionClick: (ManualDirection) -> Unit = {},
    onDirectionHoldStart: (ManualDirection) -> Unit = {},
    onDirectionHoldStop: () -> Unit = {},
    onJoystickPositionChanged: (Float, Float, Boolean) -> Unit = { _, _, _ -> },
    onEmergencyBrake: () -> Unit = {},
    onResetEmergencyBrake: () -> Unit = {},
    onReconnect: () -> Unit = {},
    onSidebarItemSelected: (SidebarNavItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = MonochromeTheme.colors
    val spacing = MonochromeTheme.spacing
    val isConnected = uiState.connectionStatus == ConnectionStatus.CONNECTED
    val controlsEnabled = isConnected && !uiState.isEmergencyStopped

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // Left Sidebar Navigation
        Sidebar(
            selectedItem = SidebarNavItem.MANUAL_CONTROL,
            onItemSelected = onSidebarItemSelected,
            isConnected = isConnected,
            connectionAddress = uiState.connectionAddress
        )

        // Main Manual Control Area
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(
                    start = spacing.space6,
                    top = spacing.cardPadding,
                    end = spacing.space6,
                    bottom = spacing.cardPadding
                )
        ) {
            // Header Title & Subtitle
            ManualControlHeader()

            Spacer(modifier = Modifier.height(spacing.space4))

            // Tab Switcher (Movement / Rotation / Telemetry / Diagnostics)
            MonochromeSegmentedControl(
                items = ManualControlTab.entries,
                selectedItem = uiState.selectedTab,
                onItemSelected = onTabSelected,
                itemLabel = { it.title },
                modifier = Modifier.fillMaxWidth(0.6f)
            )

            Spacer(modifier = Modifier.height(spacing.space4))

            // Alert Banners
            if (uiState.isEmergencyStopped) {
                MonochromeBanner(
                    title = "EMERGENCY BRAKE ENGAGED",
                    description = "Robot controls are currently disabled for safety.",
                    variant = BannerVariant.Critical,
                    icon = Icons.Default.Warning,
                    action = {
                        MonochromeButton(
                            onClick = onResetEmergencyBrake,
                            text = "Reset Brake",
                            variant = MonochromeButtonVariant.Primary
                        )
                    }
                )
                Spacer(modifier = Modifier.height(spacing.space4))
            } else if (uiState.isConnectionLost) {
                MonochromeBanner(
                    title = "ROBOT DISCONNECTED",
                    description = "Connection lost. Re-establish Wi-Fi/IP connection to control robot.",
                    variant = BannerVariant.Critical,
                    icon = Icons.Default.Warning,
                    action = {
                        MonochromeButton(
                            onClick = onReconnect,
                            text = "Reconnect",
                            variant = MonochromeButtonVariant.Primary
                        )
                    }
                )
                Spacer(modifier = Modifier.height(spacing.space4))
            } else if (uiState.isObstacleNear) {
                MonochromeBanner(
                    title = "PROXIMITY WARNING",
                    description = "Obstacle detected within ${"%.1f".format(uiState.obstacleDistanceMeters)}m. Proceed with caution.",
                    variant = BannerVariant.Warning,
                    icon = Icons.Default.Warning
                )
                Spacer(modifier = Modifier.height(spacing.space4))
            }

            // Main Interactive Panel: Speed/Direction Controls (Left) + Joystick (Right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(spacing.space6)
            ) {
                // Left Control Panel (Speed Slider, 2x2 Grid, Emergency Stop)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(spacing.space4)
                ) {
                    // Speed Control Card
                    SpeedControlCard(
                        speedPercent = uiState.speedPercent,
                        enabled = controlsEnabled,
                        onSpeedChanged = onSpeedChanged
                    )

                    // 2x2 Directional Grid
                    DirectionGrid(
                        enabled = controlsEnabled,
                        onDirectionHoldStart = onDirectionHoldStart,
                        onDirectionHoldStop = onDirectionHoldStop
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Emergency Brake Button
                    EmergencyBrakeButton(
                        isEmergencyStopped = uiState.isEmergencyStopped,
                        onEmergencyBrake = onEmergencyBrake,
                        onResetEmergencyBrake = onResetEmergencyBrake
                    )
                }

                // Right Joystick Controller Wheel
                Box(
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    JoystickControlWheel(
                        enabled = controlsEnabled,
                        onDirectionHoldStart = onDirectionHoldStart,
                        onDirectionHoldStop = onDirectionHoldStop,
                        onJoystickPositionChanged = onJoystickPositionChanged
                    )
                }
            }
        }
    }
}

@Composable
private fun ManualControlHeader() {
    val colors = MonochromeTheme.colors
    val typography = MonochromeTheme.typography

    Column {
        Text(
            text = "Manual Control",
            style = typography.h2,
            color = colors.primaryText,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Use the controls to move the robot in real time",
            style = typography.bodySmall,
            color = colors.secondaryText
        )
    }
}

@Composable
private fun JoystickControlWheel(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onDirectionHoldStart: (ManualDirection) -> Unit = {},
    onDirectionHoldStop: () -> Unit = {},
    onJoystickPositionChanged: (Float, Float, Boolean) -> Unit = { _, _, _ -> }
) {
    val colors = MonochromeTheme.colors
    val outerSize = 310.dp
    val innerSize = 150.dp
    val knobSize = 85.dp

    var knobOffset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .size(outerSize)
            .clip(CircleShape)
            .background(if (enabled) colors.surface else colors.interactiveSurface)
            .border(1.dp, colors.defaultBorder, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // Up Arrow Button
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
                .size(36.dp)
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    awaitEachGesture {
                        awaitFirstDown()
                        onDirectionHoldStart(ManualDirection.FORWARD)
                        do {
                            val event = awaitPointerEvent()
                        } while (event.changes.any { it.pressed })
                        onDirectionHoldStop()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = "Forward",
                tint = if (enabled) colors.primaryText else colors.disabledText,
                modifier = Modifier.size(30.dp)
            )
        }

        // Down Arrow Button
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
                .size(36.dp)
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    awaitEachGesture {
                        awaitFirstDown()
                        onDirectionHoldStart(ManualDirection.BACKWARD)
                        do {
                            val event = awaitPointerEvent()
                        } while (event.changes.any { it.pressed })
                        onDirectionHoldStop()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Backward",
                tint = if (enabled) colors.primaryText else colors.disabledText,
                modifier = Modifier.size(30.dp)
            )
        }

        // Left Arrow Button
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 16.dp)
                .size(36.dp)
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    awaitEachGesture {
                        awaitFirstDown()
                        onDirectionHoldStart(ManualDirection.LEFT)
                        do {
                            val event = awaitPointerEvent()
                        } while (event.changes.any { it.pressed })
                        onDirectionHoldStop()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowLeft,
                contentDescription = "Left",
                tint = if (enabled) colors.primaryText else colors.disabledText,
                modifier = Modifier.size(30.dp)
            )
        }

        // Right Arrow Button
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp)
                .size(36.dp)
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    awaitEachGesture {
                        awaitFirstDown()
                        onDirectionHoldStart(ManualDirection.RIGHT)
                        do {
                            val event = awaitPointerEvent()
                        } while (event.changes.any { it.pressed })
                        onDirectionHoldStop()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "Right",
                tint = if (enabled) colors.primaryText else colors.disabledText,
                modifier = Modifier.size(30.dp)
            )
        }

        // Inner Concentric Circle
        Box(
            modifier = Modifier
                .size(innerSize)
                .clip(CircleShape)
                .background(colors.interactiveSurface)
        )

        // Center Joystick Knob
        Box(
            modifier = Modifier
                .offset { IntOffset(knobOffset.x.roundToInt(), knobOffset.y.roundToInt()) }
                .size(knobSize)
                .clip(CircleShape)
                .background(if (enabled) colors.primaryActionBg else colors.disabledText)
                .pointerInput(enabled) {
                    if (enabled) {
                        detectDragGestures(
                            onDragEnd = {
                                knobOffset = Offset.Zero
                                onJoystickPositionChanged(0f, 0f, false)
                            },
                            onDragCancel = {
                                knobOffset = Offset.Zero
                                onJoystickPositionChanged(0f, 0f, false)
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val maxRadius = 40f
                                val newOffset = knobOffset + dragAmount
                                val distance = newOffset.getDistance()
                                knobOffset = if (distance > maxRadius) {
                                    newOffset * (maxRadius / distance)
                                } else {
                                    newOffset
                                }
                                val normX = knobOffset.x / maxRadius
                                val normY = knobOffset.y / maxRadius
                                onJoystickPositionChanged(normX, normY, true)
                            }
                        )
                    }
                }
        )
    }
}

@Composable
private fun SpeedControlCard(
    speedPercent: Float,
    onSpeedChanged: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val colors = MonochromeTheme.colors
    val typography = MonochromeTheme.typography

    MonochromeCard(
        modifier = modifier.fillMaxWidth(),
        backgroundColor = colors.surface,
        borderColor = colors.defaultBorder,
        padding = MonochromeTheme.spacing.cardPadding
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Speed",
                    style = typography.body,
                    color = colors.primaryText,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${speedPercent.toInt()}%",
                    style = typography.body,
                    color = colors.primaryText,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            MonochromeSlider(
                value = speedPercent,
                enabled = enabled,
                onValueChange = onSpeedChanged,
                valueRange = 0f..100f
            )

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Slow",
                    style = typography.caption,
                    color = colors.mutedText
                )
                Text(
                    text = "Fast",
                    style = typography.caption,
                    color = colors.mutedText
                )
            }
        }
    }
}

@Composable
private fun DirectionGrid(
    onDirectionHoldStart: (ManualDirection) -> Unit,
    onDirectionHoldStop: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DirectionButton(
                title = "Forward",
                icon = Icons.Default.ArrowUpward,
                enabled = enabled,
                onHoldStart = { onDirectionHoldStart(ManualDirection.FORWARD) },
                onHoldStop = onDirectionHoldStop,
                modifier = Modifier.weight(1f)
            )
            DirectionButton(
                title = "Backward",
                icon = Icons.Default.ArrowDownward,
                enabled = enabled,
                onHoldStart = { onDirectionHoldStart(ManualDirection.BACKWARD) },
                onHoldStop = onDirectionHoldStop,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DirectionButton(
                title = "Left",
                icon = Icons.Default.ArrowBack,
                enabled = enabled,
                onHoldStart = { onDirectionHoldStart(ManualDirection.LEFT) },
                onHoldStop = onDirectionHoldStop,
                modifier = Modifier.weight(1f)
            )
            DirectionButton(
                title = "Right",
                icon = Icons.Default.ArrowForward,
                enabled = enabled,
                onHoldStart = { onDirectionHoldStart(ManualDirection.RIGHT) },
                onHoldStop = onDirectionHoldStop,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DirectionButton(
    title: String,
    icon: ImageVector,
    onHoldStart: () -> Unit,
    onHoldStop: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val colors = MonochromeTheme.colors
    val typography = MonochromeTheme.typography

    MonochromeCard(
        modifier = modifier
            .height(76.dp)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    awaitFirstDown()
                    onHoldStart()
                    do {
                        val event = awaitPointerEvent()
                    } while (event.changes.any { it.pressed })
                    onHoldStop()
                }
            },
        backgroundColor = if (enabled) colors.surface else colors.disabledSurface,
        borderColor = colors.defaultBorder,
        padding = 12.dp
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (enabled) colors.primaryText else colors.disabledText,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = typography.bodySmall,
                color = if (enabled) colors.primaryText else colors.disabledText,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun EmergencyBrakeButton(
    isEmergencyStopped: Boolean,
    onEmergencyBrake: () -> Unit,
    onResetEmergencyBrake: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MonochromeTheme.colors
    val typography = MonochromeTheme.typography
    val buttonBg = if (isEmergencyStopped) colors.statusActive else colors.statusCritical
    val buttonText = if (isEmergencyStopped) "RESET EMERGENCY BRAKE" else "EMERGENCY BRAKE"

    Button(
        onClick = {
            if (isEmergencyStopped) {
                onResetEmergencyBrake()
            } else {
                onEmergencyBrake()
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = MonochromeTheme.shapes.buttons,
        colors = ButtonDefaults.buttonColors(
            containerColor = buttonBg,
            contentColor = Color.White
        )
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Emergency Brake Icon",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = buttonText,
                style = typography.body.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
        }
    }
}

@Preview(name = "Manual Control Screen Normal Preview", widthDp = 1000, heightDp = 600)
@Composable
fun ManualControlScreenPreview() {
    MonochromeTheme(darkTheme = false) {
        ManualControlContent(
            uiState = ManualControlUiState(
                isEmergencyStopped = false
            )
        )
    }
}

@Preview(name = "Manual Control Screen Emergency Stop Preview", widthDp = 1000, heightDp = 600)
@Composable
fun ManualControlScreenEmergencyPreview() {
    MonochromeTheme(darkTheme = false) {
        ManualControlContent(
            uiState = ManualControlUiState(
                isEmergencyStopped = true
            )
        )
    }
}
