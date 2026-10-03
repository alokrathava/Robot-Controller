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
    initialFlow: ScreenFlow? = null,
    selectedNav: SidebarNavItem = SidebarNavItem.HOME,
    onSidebarItemSelected: (SidebarNavItem) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(initialFlow) {
        if (initialFlow != null && uiState.screenFlow != initialFlow) {
            viewModel.setScreenFlow(initialFlow)
        }
    }

    when (uiState.screenFlow) {
        ScreenFlow.SPLASH -> {
            SplashScreen(
                onConnectClick = {
                    viewModel.setConnectionStep(RobotConnectionStep.NETWORK_SELECTION)
                    viewModel.setScreenFlow(ScreenFlow.CONNECTION)
                },
                onSelectMode = {
                    viewModel.setScreenFlow(ScreenFlow.DASHBOARD)
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
                        viewModel.setScreenFlow(ScreenFlow.SPLASH)
                    }
                },
                onConnectClick = {
                    viewModel.connectToRobot {
                        viewModel.setScreenFlow(ScreenFlow.DASHBOARD)
                    }
                },
                onCancelClick = {
                    viewModel.setScreenFlow(ScreenFlow.SPLASH)
                },
                modifier = modifier
            )
        }

        ScreenFlow.DASHBOARD -> {
            HomeScreenContent(
                uiState = uiState,
                selectedNav = selectedNav,
                onSidebarItemSelected = onSidebarItemSelected,
                onNavigateClick = {
                    viewModel.getMap()
                    onSidebarItemSelected(SidebarNavItem.NAVIGATION)
                },
                onManualControlClick = {
                    onSidebarItemSelected(SidebarNavItem.MANUAL_CONTROL)
                },
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
    selectedNav: SidebarNavItem = SidebarNavItem.HOME,
    onSidebarItemSelected: (SidebarNavItem) -> Unit = {},
    onNavigateClick: () -> Unit = {},
    onManualControlClick: () -> Unit = {},
    onReturnHomeClick: () -> Unit = {},
    onEmergencyReleaseClick: () -> Unit = {}
) {
    val colors = MonochromeTheme.colors
    val typography = MonochromeTheme.typography

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // Left Sidebar Navigation
        Sidebar(
            selectedItem = selectedNav,
            onItemSelected = onSidebarItemSelected,
            isConnected = (uiState.connectionStatus == ConnectionStatus.CONNECTED || uiState.connectionStatus == ConnectionStatus.DISCONNECTED),
            connectionAddress = "${uiState.ipAddress}:${uiState.port}"
        )

        // Main Dashboard Content Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(
                    start = MonochromeTheme.spacing.space6,
                    top = MonochromeTheme.spacing.cardPadding,
                    end = MonochromeTheme.spacing.space6,
                    bottom = MonochromeTheme.spacing.cardPadding
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(MonochromeTheme.spacing.cardPadding)
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
                            style = typography.body,
                            color = colors.secondaryText
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Robot is Ready",
                            style = typography.h1,
                            color = colors.primaryText,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Connected and ready for operation",
                            style = typography.bodySmall,
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
                            tint = colors.statusActive,
                            modifier = Modifier.size(40.dp)
                        )

                        Column {
                            Text(
                                text = uiState.batteryStatus.displayText.ifBlank { "85%" },
                                style = typography.h3,
                                color = colors.primaryText,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Battery",
                                style = typography.label,
                                color = colors.secondaryText
                            )
                            Text(
                                text = uiState.batteryTimeRemainingFormatted,
                                style = typography.caption,
                                color = colors.mutedText
                            )
                        }
                    }
                }

                // Main Section: Telemetry & Quick Actions Column on left
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MonochromeTheme.spacing.space6),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(MonochromeTheme.spacing.space6)
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
        padding = MonochromeTheme.spacing.cardPadding
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
                modifier = Modifier.size(32.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = primaryValue,
                    style = typography.bodyLarge,
                    color = colors.primaryText,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = secondaryLabel,
                    style = typography.caption,
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
        padding = MonochromeTheme.spacing.cardPadding,
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
                    modifier = Modifier.size(32.dp)
                )

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = title,
                        style = typography.bodyLarge,
                        color = colors.primaryText,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        style = typography.caption,
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
                modifier = Modifier.size(24.dp)
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
        padding = MonochromeTheme.spacing.cardPadding
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
                    style = typography.bodyLarge,
                    color = colors.primaryText,
                    fontWeight = FontWeight.Bold
                )

                com.agrathava.theme.MonochromeStatusPill(
                    text = "SYSTEM OPERATIONAL",
                    level = com.agrathava.theme.StatusLevel.Active
                )
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
                        style = typography.caption,
                        color = colors.mutedText
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = uiState.mapData.displayText,
                        style = typography.bodySmall,
                        color = colors.primaryText,
                        fontWeight = FontWeight.SemiBold,
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
                        style = typography.caption,
                        color = colors.mutedText
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Primary Charging Dock",
                        style = typography.bodySmall,
                        color = colors.primaryText,
                        fontWeight = FontWeight.SemiBold,
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
                        style = typography.caption,
                        color = colors.mutedText
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${uiState.ipAddress}:${uiState.port}",
                        style = typography.bodySmall,
                        color = colors.primaryText,
                        fontWeight = FontWeight.SemiBold,
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
