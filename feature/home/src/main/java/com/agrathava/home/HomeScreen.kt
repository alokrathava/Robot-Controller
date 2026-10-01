package com.agrathava.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agrathava.sdk.model.ConnectionStatus
import com.agrathava.theme.MonochromeCard
import com.agrathava.theme.MonochromeTheme

enum class ScreenFlow {
    SPLASH,
    CONNECTION,
    DASHBOARD
}

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
    initialFlow: ScreenFlow = ScreenFlow.DASHBOARD
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var currentFlow by remember { mutableStateOf(initialFlow) }

    when (currentFlow) {
        ScreenFlow.SPLASH -> {
            SplashScreen(
                onConnectClick = {
                    viewModel.setConnectionStep(RobotConnectionStep.NETWORK_SELECTION)
                    currentFlow = ScreenFlow.CONNECTION
                },
                onSelectMode = {
                    currentFlow = ScreenFlow.DASHBOARD
                },
                modifier = modifier
            )
        }

        ScreenFlow.CONNECTION -> {
            RobotConnectionScreen(
                connectionStep = uiState.connectionStep,
                availableNetworks = uiState.availableNetworks,
                selectedNetwork = uiState.selectedNetwork,
                ipAddress = uiState.ipAddress,
                port = uiState.port,
                ipError = uiState.ipError,
                portError = uiState.portError,
                connectionStatus = uiState.connectionStatus,
                onSelectNetwork = viewModel::selectNetwork,
                onRefreshNetworks = viewModel::refreshNetworks,
                onUpdateIpAddress = viewModel::updateIpAddress,
                onUpdatePort = viewModel::updatePort,
                onNextStep = {
                    viewModel.setConnectionStep(RobotConnectionStep.IP_PORT_CONFIG)
                },
                onPreviousStep = {
                    if (uiState.connectionStep == RobotConnectionStep.IP_PORT_CONFIG) {
                        viewModel.setConnectionStep(RobotConnectionStep.NETWORK_SELECTION)
                    } else {
                        currentFlow = ScreenFlow.SPLASH
                    }
                },
                onConnectClick = {
                    viewModel.connectToRobot {
                        currentFlow = ScreenFlow.DASHBOARD
                    }
                },
                onCancelClick = {
                    currentFlow = ScreenFlow.SPLASH
                },
                modifier = modifier
            )
        }

        ScreenFlow.DASHBOARD -> {
            HomeScreenContent(
                uiState = uiState,
                onNavigateClick = viewModel::getMap,
                onManualControlClick = viewModel::moveForward,
                onReturnHomeClick = viewModel::goToCharge,
                onEmergencyReleaseClick = viewModel::cancelNavigation,
                modifier = modifier
            )
        }
    }
}

@Composable
fun HomeScreenContent(
    uiState: HomeUiState,
    modifier: Modifier = Modifier,
    onNavigateClick: () -> Unit = {},
    onManualControlClick: () -> Unit = {},
    onReturnHomeClick: () -> Unit = {},
    onEmergencyReleaseClick: () -> Unit = {}
) {
    val colors = MonochromeTheme.colors
    val typography = MonochromeTheme.typography

    var selectedNav by remember { mutableStateOf(SidebarNavItem.HOME) }

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // Left Sidebar Navigation
        Sidebar(
            selectedItem = selectedNav,
            onItemSelected = { selectedNav = it },
            isConnected = (uiState.connectionStatus == ConnectionStatus.CONNECTED || uiState.connectionStatus == ConnectionStatus.DISCONNECTED),
            connectionAddress = "${uiState.ipAddress}:${uiState.port}"
        )

        // Main Dashboard Content Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(horizontal = 32.dp, vertical = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                // Top Header Row & Battery Info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    // Header Greeting & Status Title
                    Column {
                        Text(
                            text = "Good Morning",
                            style = typography.body.copy(
                                fontSize = typography.body.fontSize * 1.35f,
                                lineHeight = typography.body.lineHeight * 1.35f
                            ),
                            color = colors.secondaryText
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Robot is Ready",
                            style = typography.h1.copy(
                                fontSize = typography.h1.fontSize * 1.4f,
                                lineHeight = typography.h1.lineHeight * 1.4f
                            ),
                            color = colors.primaryText,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Connected and ready for operation",
                            style = typography.bodySmall.copy(
                                fontSize = typography.bodySmall.fontSize * 1.25f,
                                lineHeight = typography.bodySmall.lineHeight * 1.25f
                            ),
                            color = colors.secondaryText
                        )
                    }

                    // Top-Right Battery Status Indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.BatteryFull,
                            contentDescription = "Battery Status",
                            tint = Color(0xFF22C55E),
                            modifier = Modifier.size(44.dp)
                        )

                        Column {
                            Text(
                                text = uiState.batteryStatus.displayText.ifBlank { "85%" },
                                style = typography.h3.copy(
                                    fontSize = typography.h3.fontSize * 1.35f,
                                    lineHeight = typography.h3.lineHeight * 1.35f
                                ),
                                color = colors.primaryText,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Battery",
                                style = typography.caption.copy(
                                    fontSize = typography.caption.fontSize * 1.2f,
                                    lineHeight = typography.caption.lineHeight * 1.2f
                                ),
                                color = colors.secondaryText
                            )
                            Text(
                                text = "2h 15m remaining",
                                style = typography.caption.copy(
                                    fontSize = typography.caption.fontSize * 1.15f,
                                    lineHeight = typography.caption.lineHeight * 1.15f
                                ),
                                color = colors.mutedText
                            )
                        }
                    }
                }

                // Main Section: Telemetry & Quick Actions Column on left, Robot Image on right
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        // Four Horizontal Telemetry Cards
                        TelemetryRow(
                            uiState = uiState,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // 2 x 2 Quick Actions Grid
                        QuickActionsGrid(
                            onNavigateClick = onNavigateClick,
                            onManualControlClick = onManualControlClick,
                            onReturnHomeClick = onReturnHomeClick,
                            onEmergencyReleaseClick = onEmergencyReleaseClick,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // System Diagnostic & Mission Overview Card
                        SystemStatusOverviewCard(
                            uiState = uiState,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Robot Image Asset (Dedicated right column)
//                    Image(
//                        painter = painterResource(id = R.drawable.robot_splash),
//                        contentDescription = "Robot Visual",
//                        contentScale = ContentScale.Fit,
//                        modifier = Modifier
//                            .width(360.dp)
//                            .height(520.dp)
//                    )
                }
            }
        }
    }
}

/**
 * Horizontal Telemetry Cards Row (4 cards)
 */
@Composable
private fun TelemetryRow(
    uiState: HomeUiState,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TelemetryCard(
            icon = Icons.Default.SmartToy,
            primaryValue = uiState.statusMessage.ifBlank { "Ready" },
            secondaryLabel = "Robot State",
            modifier = Modifier.weight(1f)
        )

        TelemetryCard(
            icon = Icons.Default.Place,
            primaryValue = if (uiState.position.displayText.isBlank() || uiState.position.displayText == "(0.0, 0.0)") "(2.4, 5.1)" else uiState.position.displayText,
            secondaryLabel = "Current Position",
            modifier = Modifier.weight(1f)
        )

        TelemetryCard(
            icon = Icons.Default.Navigation,
            primaryValue = "180°",
            secondaryLabel = "Yaw Angle",
            modifier = Modifier.weight(1f)
        )

        TelemetryCard(
            icon = Icons.Default.Thermostat,
            primaryValue = "24°C",
            secondaryLabel = "Temperature",
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Single Telemetry Card
 */
@Composable
private fun TelemetryCard(
    icon: ImageVector,
    primaryValue: String,
    secondaryLabel: String,
    modifier: Modifier = Modifier
) {
    val colors = MonochromeTheme.colors
    val typography = MonochromeTheme.typography

    MonochromeCard(
        modifier = modifier,
        backgroundColor = colors.surface,
        borderColor = colors.defaultBorder,
        padding = 18.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = secondaryLabel,
                tint = colors.primaryText,
                modifier = Modifier.size(34.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = primaryValue,
                    style = typography.body.copy(
                        fontSize = typography.body.fontSize * 1.3f,
                        lineHeight = typography.body.lineHeight * 1.3f
                    ),
                    color = colors.primaryText,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = secondaryLabel,
                    style = typography.caption.copy(
                        fontSize = typography.caption.fontSize * 1.15f,
                        lineHeight = typography.caption.lineHeight * 1.15f
                    ),
                    color = colors.mutedText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * 2 × 2 Quick Actions Grid
 */
@Composable
private fun QuickActionsGrid(
    onNavigateClick: () -> Unit,
    onManualControlClick: () -> Unit,
    onReturnHomeClick: () -> Unit,
    onEmergencyReleaseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            QuickActionCard(
                icon = Icons.AutoMirrored.Filled.Send,
                title = "Navigate",
                subtitle = "Go to a location on map",
                onClick = onNavigateClick,
                modifier = Modifier.weight(1f)
            )

            QuickActionCard(
                icon = Icons.Default.SportsEsports,
                title = "Manual Control",
                subtitle = "Move robot manually",
                onClick = onManualControlClick,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            QuickActionCard(
                icon = Icons.Default.Home,
                title = "Return to Home",
                subtitle = "Go to charging dock",
                onClick = onReturnHomeClick,
                modifier = Modifier.weight(1f)
            )

            QuickActionCard(
                icon = Icons.Default.Warning,
                title = "Emergency Release",
                subtitle = "Release emergency stop",
                onClick = onEmergencyReleaseClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Single Quick Action Card
 */
@Composable
private fun QuickActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MonochromeTheme.colors
    val typography = MonochromeTheme.typography

    MonochromeCard(
        modifier = modifier,
        backgroundColor = colors.surface,
        borderColor = colors.defaultBorder,
        padding = 22.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = colors.primaryText,
                    modifier = Modifier.size(38.dp)
                )

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = title,
                        style = typography.body.copy(
                            fontSize = typography.body.fontSize * 1.3f,
                            lineHeight = typography.body.lineHeight * 1.3f
                        ),
                        color = colors.primaryText,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        style = typography.caption.copy(
                            fontSize = typography.caption.fontSize * 1.15f,
                            lineHeight = typography.caption.lineHeight * 1.15f
                        ),
                        color = colors.secondaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Action",
                tint = colors.primaryText,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

/**
 * System Diagnostic Overview Component
 */
@Composable
private fun SystemStatusOverviewCard(
    uiState: HomeUiState,
    modifier: Modifier = Modifier
) {
    val colors = MonochromeTheme.colors
    val typography = MonochromeTheme.typography

    MonochromeCard(
        modifier = modifier,
        backgroundColor = colors.surface,
        borderColor = colors.defaultBorder,
        padding = 22.dp
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "System Diagnostics & Mission Status",
                    style = typography.body.copy(
                        fontSize = typography.body.fontSize * 1.25f,
                        lineHeight = typography.body.lineHeight * 1.25f
                    ),
                    color = colors.primaryText,
                    fontWeight = FontWeight.Bold
                )

                Box(
                    modifier = Modifier
                        .background(
                            color = Color(0xFF22C55E).copy(alpha = 0.12f),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "SYSTEM OPERATIONAL",
                        style = typography.caption.copy(
                            fontSize = typography.caption.fontSize * 1.05f,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFF16A34A)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Active Map Detail
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Active Map",
                        style = typography.caption.copy(
                            fontSize = typography.caption.fontSize * 1.1f
                        ),
                        color = colors.mutedText
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = uiState.mapData.displayText,
                        style = typography.bodySmall.copy(
                            fontSize = typography.bodySmall.fontSize * 1.15f,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = colors.primaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Dock Station Status
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Dock Station",
                        style = typography.caption.copy(
                            fontSize = typography.caption.fontSize * 1.1f
                        ),
                        color = colors.mutedText
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Primary Charging Dock",
                        style = typography.bodySmall.copy(
                            fontSize = typography.bodySmall.fontSize * 1.15f,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = colors.primaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Connection Network
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Network Endpoint",
                        style = typography.caption.copy(
                            fontSize = typography.caption.fontSize * 1.1f
                        ),
                        color = colors.mutedText
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${uiState.ipAddress}:${uiState.port}",
                        style = typography.bodySmall.copy(
                            fontSize = typography.bodySmall.fontSize * 1.15f,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = colors.primaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Preview(name = "1920x1080 Widescreen", showBackground = true, device = "spec:width=1920px,height=1080px,dpi=160")
@Preview(name = "1920x1080 High DPI", showBackground = true, device = "spec:width=1920px,height=1080px,dpi=240")
@Composable
fun HomeScreenPreview() {
    MonochromeTheme(darkTheme = false) {
        HomeScreenContent(
            uiState = HomeUiState()
        )
    }
}
