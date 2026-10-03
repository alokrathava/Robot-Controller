package com.alokrathava.robotstatus

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alokrathava.home.Sidebar
import com.alokrathava.home.SidebarNavItem
import com.alokrathava.theme.MonochromeCard
import com.alokrathava.theme.MonochromeTheme
import com.alokrathava.theme.R as ThemeR

@Composable
fun RobotStatusScreen(
    modifier: Modifier = Modifier,
    viewModel: RobotStatusViewModel = hiltViewModel(),
    selectedNav: SidebarNavItem = SidebarNavItem.ROBOT_STATUS,
    onSidebarItemSelected: (SidebarNavItem) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    RobotStatusScreenContent(
        uiState = uiState,
        selectedNav = selectedNav,
        onSidebarItemSelected = onSidebarItemSelected,
        modifier = modifier
    )
}

@Composable
fun RobotStatusScreenContent(
    uiState: RobotStatusUiState,
    modifier: Modifier = Modifier,
    selectedNav: SidebarNavItem = SidebarNavItem.ROBOT_STATUS,
    onSidebarItemSelected: (SidebarNavItem) -> Unit = {}
) {
    val colors = MonochromeTheme.colors
    val typography = MonochromeTheme.typography
    val spacing = MonochromeTheme.spacing

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // Left Sidebar Navigation
        Sidebar(
            selectedItem = selectedNav,
            onItemSelected = onSidebarItemSelected,
            isConnected = uiState.isConnected,
            connectionAddress = uiState.connectionAddress
        )

        // Main Dashboard Area
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
            Text(
                text = "Robot Status",
                style = typography.h2,
                color = colors.primaryText,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(spacing.space1))
            Text(
                text = "Live information from the robot",
                style = typography.bodySmall,
                color = colors.secondaryText
            )

            Spacer(modifier = Modifier.height(spacing.space4))

            // Main Content Area: Robot Illustration (Left) + Metric Cards Grid (Right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(spacing.space6),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Hero Section: Robot Image
                Box(
                    modifier = Modifier
                        .weight(0.40f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = ThemeR.drawable.robot_splash),
                        contentDescription = "Robot Illustration",
                        modifier = Modifier
                            .fillMaxHeight(0.9f)
                            .aspectRatio(0.65f),
                        contentScale = ContentScale.Fit
                    )
                }

                // Right Section: Grid of Cards
                Column(
                    modifier = Modifier
                        .weight(0.60f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(spacing.space4, Alignment.CenterVertically)
                ) {
                    // Top Card: Battery Status Card
                    MonochromeCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = colors.surface,
                        padding = spacing.cardPadding
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            // Large Battery Icon
                            BatteryIcon(
                                percent = uiState.batteryPercent,
                                color = colors.statusActive
                            )

                            // Battery Progress & Info
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "${uiState.batteryPercent}%",
                                    style = typography.h3,
                                    color = colors.primaryText,
                                    fontWeight = FontWeight.Bold
                                )

                                // Progress Bar
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(12.dp)
                                        .clip(CircleShape)
                                        .background(colors.disabledSurface)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(uiState.batteryPercent / 100f)
                                            .fillMaxHeight()
                                            .clip(CircleShape)
                                            .background(colors.statusActive)
                                    )
                                }

                                Text(
                                    text = uiState.estimatedTimeRemaining,
                                    style = typography.caption,
                                    color = colors.secondaryText
                                )
                            }
                        }
                    }

                    // Middle Row: 3 Cards (Position, Yaw Angle, State)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing.space4)
                    ) {
                        // Position Card
                        MonochromeCard(
                            modifier = Modifier.weight(1f),
                            backgroundColor = colors.surface,
                            padding = spacing.cardPadding
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Place,
                                    contentDescription = "Position Icon",
                                    modifier = Modifier.size(20.dp),
                                    tint = colors.primaryText
                                )
                                Text(
                                    text = "Position",
                                    style = typography.label,
                                    color = colors.secondaryText
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "(${String.format(java.util.Locale.US, "%.1f", uiState.positionX)}, ${String.format(java.util.Locale.US, "%.1f", uiState.positionY)})",
                                style = typography.h4,
                                color = colors.primaryText,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = "X, Y (meters)",
                                style = typography.caption,
                                color = colors.mutedText
                            )
                        }

                        // Yaw Angle Card
                        MonochromeCard(
                            modifier = Modifier.weight(1f),
                            backgroundColor = colors.surface,
                            padding = spacing.cardPadding
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GpsFixed,
                                    contentDescription = "Yaw Angle Icon",
                                    modifier = Modifier.size(20.dp),
                                    tint = colors.primaryText
                                )
                                Text(
                                    text = "Yaw Angle",
                                    style = typography.label,
                                    color = colors.secondaryText
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .border(1.5.dp, colors.disabledText, CircleShape)
                                )

                                Text(
                                    text = "${uiState.yawDegrees.toInt()}°",
                                    style = typography.h4,
                                    color = colors.primaryText,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // State Card
                        MonochromeCard(
                            modifier = Modifier.weight(1f),
                            backgroundColor = colors.surface,
                            padding = spacing.cardPadding
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SportsEsports,
                                    contentDescription = "State Icon",
                                    modifier = Modifier.size(20.dp),
                                    tint = colors.primaryText
                                )
                                Text(
                                    text = "State",
                                    style = typography.label,
                                    color = colors.secondaryText
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = uiState.robotState,
                                style = typography.h4,
                                color = colors.primaryText,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Bottom Row: 3 Cards (Connection, Speed, Temperature)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing.space4)
                    ) {
                        // Connection Card
                        MonochromeCard(
                            modifier = Modifier.weight(1f),
                            backgroundColor = colors.surface,
                            padding = spacing.cardPadding
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Wifi,
                                    contentDescription = "Connection Icon",
                                    modifier = Modifier.size(20.dp),
                                    tint = colors.primaryText
                                )
                                Text(
                                    text = "Connection",
                                    style = typography.label,
                                    color = colors.secondaryText
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = if (uiState.isConnected) "Connected" else "Disconnected",
                                style = typography.h4,
                                color = colors.primaryText,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Speed Card
                        MonochromeCard(
                            modifier = Modifier.weight(1f),
                            backgroundColor = colors.surface,
                            padding = spacing.cardPadding
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = "Speed Icon",
                                    modifier = Modifier.size(20.dp),
                                    tint = colors.primaryText
                                )
                                Text(
                                    text = "Speed",
                                    style = typography.label,
                                    color = colors.secondaryText
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "${String.format(java.util.Locale.US, "%.1f", uiState.speedMps)} m/s",
                                style = typography.h4,
                                color = colors.primaryText,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Temperature Card
                        MonochromeCard(
                            modifier = Modifier.weight(1f),
                            backgroundColor = colors.surface,
                            padding = spacing.cardPadding
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Thermostat,
                                    contentDescription = "Temperature Icon",
                                    modifier = Modifier.size(20.dp),
                                    tint = colors.primaryText
                                )
                                Text(
                                    text = "Temperature",
                                    style = typography.label,
                                    color = colors.secondaryText
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "${uiState.temperatureCelsius.toInt()}°C",
                                style = typography.h4,
                                color = colors.primaryText,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BatteryIcon(
    percent: Int,
    modifier: Modifier = Modifier,
    color: Color = MonochromeTheme.colors.primaryText
) {
    Box(
        modifier = modifier
            .width(26.dp)
            .height(48.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            // Terminal nub on top
            Box(
                modifier = Modifier
                    .width(10.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                    .background(color)
            )
            // Main body outline
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .border(2.5.dp, color, RoundedCornerShape(4.dp))
                    .padding(3.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight((percent / 100f).coerceIn(0f, 1f))
                        .clip(RoundedCornerShape(2.dp))
                        .background(color)
                )
            }
        }
    }
}

@Preview(name = "Robot Status Screen Preview", showBackground = true, widthDp = 1000, heightDp = 600)
@Composable
fun RobotStatusScreenPreview() {
    MonochromeTheme(darkTheme = false) {
        RobotStatusScreenContent(
            uiState = RobotStatusUiState(
                batteryPercent = 78,
                estimatedTimeRemaining = "Estimated 2h 15m remaining",
                positionX = 2.4,
                positionY = 5.1,
                yawDegrees = 180.0,
                robotState = "Idle",
                isConnected = true,
                connectionAddress = "192.168.1.108:8080",
                speedMps = 0.0,
                temperatureCelsius = 24.0
            )
        )
    }
}
