package com.agrathava.manualcontrol

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agrathava.home.Sidebar
import com.agrathava.home.SidebarNavItem
import com.agrathava.theme.Black900
import com.agrathava.theme.MonochromeTheme
import com.agrathava.theme.White100
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

private val RedEmergency = Color(0xFFDC2626)

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
            onEmergencyBrake = viewModel::triggerEmergencyBrake,
            onResetEmergencyBrake = viewModel::resetEmergencyBrake,
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
    onEmergencyBrake: () -> Unit = {},
    onResetEmergencyBrake: () -> Unit = {},
    onSidebarItemSelected: (SidebarNavItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = MonochromeTheme.colors

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // Left Sidebar Navigation
        Sidebar(
            selectedItem = SidebarNavItem.MANUAL_CONTROL,
            onItemSelected = onSidebarItemSelected,
            isConnected = true,
            connectionAddress = "192.168.1.108:8080"
        )

        // Main Manual Control Area
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(start = 28.dp, top = 24.dp, end = 28.dp, bottom = 24.dp)
        ) {
            // Header Title & Subtitle
            ManualControlHeader()

            Spacer(modifier = Modifier.height(16.dp))

            // Tab Switcher (Movement / Rotation)
            ManualControlTabSwitcher(
                selectedTab = uiState.selectedTab,
                onTabSelected = onTabSelected
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Alert Banners
            if (uiState.isEmergencyStopped) {
                EmergencyBrakeActiveBanner(
                    onResetEmergencyBrake = onResetEmergencyBrake
                )
                Spacer(modifier = Modifier.height(16.dp))
            } else if (uiState.isConnectionLost) {
                ConnectionLostBanner()
                Spacer(modifier = Modifier.height(16.dp))
            } else if (uiState.isObstacleNear) {
                ObstacleWarningBanner(
                    distanceMeters = uiState.obstacleDistanceMeters
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Main Interactive Panel: Joystick (Left) + Speed/Direction Controls (Right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                // Left Joystick Controller Wheel
                Box(
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    JoystickControlWheel(
                        enabled = !uiState.isEmergencyStopped,
                        onDirectionClick = onDirectionClick
                    )
                }

                // Right Control Panel (Speed Slider, 2x2 Grid, Emergency Stop)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Speed Control Card
                    SpeedControlCard(
                        speedPercent = uiState.speedPercent,
                        enabled = !uiState.isEmergencyStopped,
                        onSpeedChanged = onSpeedChanged
                    )

                    // 2x2 Directional Grid
                    DirectionGrid(
                        enabled = !uiState.isEmergencyStopped,
                        onDirectionClick = onDirectionClick
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Red Emergency Brake Button
                    RedEmergencyBrakeButton(
                        isEmergencyStopped = uiState.isEmergencyStopped,
                        onEmergencyBrake = onEmergencyBrake,
                        onResetEmergencyBrake = onResetEmergencyBrake
                    )
                }
            }
        }
    }
}

@Composable
private fun EmergencyBrakeActiveBanner(
    onResetEmergencyBrake: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFFEF2F2),
        border = BorderStroke(1.dp, Color(0xFFFCA5A5))
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Emergency Brake",
                    tint = RedEmergency,
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = "EMERGENCY BRAKE ENGAGED",
                        style = MonochromeTheme.typography.body.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = RedEmergency
                    )
                    Text(
                        text = "Robot controls are currently disabled for safety.",
                        style = MonochromeTheme.typography.caption.copy(fontSize = 12.sp),
                        color = Color(0xFF991B1B)
                    )
                }
            }

            Button(
                onClick = onResetEmergencyBrake,
                colors = ButtonDefaults.buttonColors(
                    containerColor = RedEmergency,
                    contentColor = White100
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Reset Brake",
                    style = MonochromeTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
private fun ObstacleWarningBanner(
    distanceMeters: Double,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFFFFBEB),
        border = BorderStroke(1.dp, Color(0xFFFCD34D))
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Obstacle Warning",
                tint = Color(0xFFD97706),
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    text = "PROXIMITY WARNING",
                    style = MonochromeTheme.typography.body.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color(0xFFB45309)
                )
                Text(
                    text = "Obstacle detected within ${"%.1f".format(distanceMeters)}m. Proceed with caution.",
                    style = MonochromeTheme.typography.caption.copy(fontSize = 12.sp),
                    color = Color(0xFF92400E)
                )
            }
        }
    }
}

@Composable
private fun ConnectionLostBanner(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFFEF2F2),
        border = BorderStroke(1.dp, Color(0xFFFCA5A5))
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Connection Lost",
                tint = RedEmergency,
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    text = "ROBOT DISCONNECTED",
                    style = MonochromeTheme.typography.body.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = RedEmergency
                )
                Text(
                    text = "Connection lost. Re-establish Wi-Fi/IP connection to control robot.",
                    style = MonochromeTheme.typography.caption.copy(fontSize = 12.sp),
                    color = Color(0xFF991B1B)
                )
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
            style = typography.h2.copy(
                fontSize = 28.sp,
                lineHeight = 34.sp
            ),
            color = colors.primaryText,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Use the controls to move the robot in real time",
            style = typography.bodySmall.copy(
                fontSize = 14.sp,
                lineHeight = 20.sp
            ),
            color = colors.secondaryText
        )
    }
}

@Composable
private fun ManualControlTabSwitcher(
    selectedTab: ManualControlTab,
    onTabSelected: (ManualControlTab) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFEEEEEE),
        modifier = Modifier.wrapContentSize()
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ManualControlTab.entries.forEach { tab ->
                TabButton(
                    title = tab.title,
                    isSelected = selectedTab == tab,
                    onClick = { onTabSelected(tab) }
                )
            }
        }
    }
}

@Composable
private fun TabButton(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) Black900 else Color.Transparent
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                style = MonochromeTheme.typography.bodySmall.copy(
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (isSelected) White100 else Color(0xFF555555)
            )
        }
    }
}

@Composable
private fun JoystickControlWheel(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onDirectionClick: (ManualDirection) -> Unit = {}
) {
    val outerSize = 310.dp
    val innerSize = 150.dp
    val knobSize = 85.dp

    var knobOffset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .size(outerSize)
            .clip(CircleShape)
            .background(if (enabled) Color(0xFFFAFAFA) else Color(0xFFF3F4F6))
            .border(1.dp, Color(0xFFE2E2E2), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // Up Arrow
        IconButton(
            enabled = enabled,
            onClick = { onDirectionClick(ManualDirection.FORWARD) },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
                .size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = "Forward",
                tint = if (enabled) Black900 else Color.Gray,
                modifier = Modifier.size(30.dp)
            )
        }

        // Down Arrow
        IconButton(
            enabled = enabled,
            onClick = { onDirectionClick(ManualDirection.BACKWARD) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
                .size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Backward",
                tint = if (enabled) Black900 else Color.Gray,
                modifier = Modifier.size(30.dp)
            )
        }

        // Left Arrow
        IconButton(
            enabled = enabled,
            onClick = { onDirectionClick(ManualDirection.LEFT) },
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 16.dp)
                .size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowLeft,
                contentDescription = "Left",
                tint = if (enabled) Black900 else Color.Gray,
                modifier = Modifier.size(30.dp)
            )
        }

        // Right Arrow
        IconButton(
            enabled = enabled,
            onClick = { onDirectionClick(ManualDirection.RIGHT) },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp)
                .size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "Right",
                tint = if (enabled) Black900 else Color.Gray,
                modifier = Modifier.size(30.dp)
            )
        }

        // Inner Concentric Circle
        Box(
            modifier = Modifier
                .size(innerSize)
                .clip(CircleShape)
                .background(Color(0xFFEBEBEB))
        )

        // Center Joystick Knob
        Box(
            modifier = Modifier
                .offset { IntOffset(knobOffset.x.roundToInt(), knobOffset.y.roundToInt()) }
                .size(knobSize)
                .clip(CircleShape)
                .background(if (enabled) Black900 else Color.Gray)
                .pointerInput(enabled) {
                    if (enabled) {
                        detectDragGestures(
                            onDragEnd = { knobOffset = Offset.Zero },
                            onDragCancel = { knobOffset = Offset.Zero },
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
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = White100,
        border = BorderStroke(1.dp, Color(0xFFE5E5E5))
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Speed",
                    style = MonochromeTheme.typography.body.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Black900
                )
                Text(
                    text = "${speedPercent.toInt()}%",
                    style = MonochromeTheme.typography.body.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Black900
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Slider(
                value = speedPercent,
                enabled = enabled,
                onValueChange = onSpeedChanged,
                valueRange = 0f..100f,
                colors = SliderDefaults.colors(
                    thumbColor = Black900,
                    activeTrackColor = Black900,
                    inactiveTrackColor = Color(0xFFE5E5E5)
                )
            )

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Slow",
                    style = MonochromeTheme.typography.caption.copy(fontSize = 12.sp),
                    color = Color(0xFF888888)
                )
                Text(
                    text = "Fast",
                    style = MonochromeTheme.typography.caption.copy(fontSize = 12.sp),
                    color = Color(0xFF888888)
                )
            }
        }
    }
}

@Composable
private fun DirectionGrid(
    onDirectionClick: (ManualDirection) -> Unit,
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
                onClick = { onDirectionClick(ManualDirection.FORWARD) },
                modifier = Modifier.weight(1f)
            )
            DirectionButton(
                title = "Backward",
                icon = Icons.Default.ArrowDownward,
                enabled = enabled,
                onClick = { onDirectionClick(ManualDirection.BACKWARD) },
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
                onClick = { onDirectionClick(ManualDirection.LEFT) },
                modifier = Modifier.weight(1f)
            )
            DirectionButton(
                title = "Right",
                icon = Icons.Default.ArrowForward,
                enabled = enabled,
                onClick = { onDirectionClick(ManualDirection.RIGHT) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DirectionButton(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(84.dp),
        shape = RoundedCornerShape(16.dp),
        color = if (enabled) White100 else Color(0xFFF3F4F6),
        border = BorderStroke(1.dp, Color(0xFFE5E5E5))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (enabled) Black900 else Color.Gray,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                style = MonochromeTheme.typography.bodySmall.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = if (enabled) Black900 else Color.Gray
            )
        }
    }
}

@Composable
private fun RedEmergencyBrakeButton(
    isEmergencyStopped: Boolean,
    onEmergencyBrake: () -> Unit,
    onResetEmergencyBrake: () -> Unit,
    modifier: Modifier = Modifier
) {
    val buttonBg = if (isEmergencyStopped) Color(0xFF15803D) else RedEmergency
    val buttonText = if (isEmergencyStopped) "RESET EMERGENCY BRAKE" else "EMERGENCY BRAKE"

    Surface(
        onClick = {
            if (isEmergencyStopped) {
                onResetEmergencyBrake()
            } else {
                onEmergencyBrake()
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(14.dp),
        color = buttonBg
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize()
        ) {
            if (!isEmergencyStopped) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(White100, shape = RoundedCornerShape(2.dp))
                )
                Spacer(modifier = Modifier.width(10.dp))
            } else {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Reset Brake",
                    tint = White100,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = buttonText,
                style = MonochromeTheme.typography.body.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                ),
                color = White100
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
