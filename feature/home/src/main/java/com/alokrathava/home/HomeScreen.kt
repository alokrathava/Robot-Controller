package com.alokrathava.home

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
import com.alokrathava.sdk.model.ConnectionStatus
import com.alokrathava.theme.DeviceMode
import com.alokrathava.theme.LocalDeviceMode
import com.alokrathava.theme.MonochromeCard
import com.alokrathava.theme.MonochromeTheme

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
                    viewModel.setConnectionStep(RobotConnectionStep.AUTO_DISCOVERY)
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
                discoveredRobots = uiState.discoveredRobots,
                selectedDiscoveredRobot = uiState.selectedDiscoveredRobot,
                isDiscoveringRobots = uiState.isDiscoveringRobots,
                availableNetworks = uiState.availableNetworks,
                selectedNetwork = uiState.selectedNetwork,
                ipAddress = uiState.ipAddress,
                port = uiState.port,
                token = uiState.token,
                useTls = uiState.useTls,
                ipError = uiState.ipError,
                portError = uiState.portError,
                tokenError = uiState.tokenError,
                connectionErrorMessage = uiState.connectionErrorMessage,
                connectionStatus = uiState.connectionStatus,
                onStartDiscovery = viewModel::startRobotDiscovery,
                onSelectDiscoveredRobot = viewModel::selectDiscoveredRobot,
                onQuickConnectDiscoveredRobot = { robot ->
                    viewModel.connectToDiscoveredRobot(robot)
                },
                onSelectNetwork = viewModel::selectNetwork,
                onRefreshNetworks = viewModel::refreshNetworks,
                onUpdateIpAddress = viewModel::updateIpAddress,
                onUpdatePort = viewModel::updatePort,
                onUpdateToken = viewModel::updateToken,
                onUpdateUseTls = viewModel::updateUseTls,
                onNextStep = {
                    when (uiState.connectionStep) {
                        RobotConnectionStep.AUTO_DISCOVERY -> viewModel.setConnectionStep(RobotConnectionStep.IP_PORT_CONFIG)
                        RobotConnectionStep.NETWORK_SELECTION -> viewModel.setConnectionStep(RobotConnectionStep.IP_PORT_CONFIG)
                        else -> {}
                    }
                },
                onPreviousStep = {
                    when (uiState.connectionStep) {
                        RobotConnectionStep.IP_PORT_CONFIG -> viewModel.setConnectionStep(RobotConnectionStep.AUTO_DISCOVERY)
                        RobotConnectionStep.NETWORK_SELECTION -> viewModel.setConnectionStep(RobotConnectionStep.AUTO_DISCOVERY)
                        else -> viewModel.setScreenFlow(ScreenFlow.SPLASH)
                    }
                },
                onConnectClick = {
                    viewModel.connectToRobot()
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
    val deviceMode = LocalDeviceMode.current

    val paddingValues = if (deviceMode == DeviceMode.MOBILE) {
        MonochromeTheme.spacing.space4
    } else {
        MonochromeTheme.spacing.cardPadding
    }

    AdaptiveNavigationLayout(
        selectedItem = selectedNav,
        onItemSelected = onSidebarItemSelected,
        isConnected = (uiState.connectionStatus == ConnectionStatus.CONNECTED || uiState.connectionStatus == ConnectionStatus.DISCONNECTED),
        connectionAddress = "${uiState.ipAddress}:${uiState.port}",
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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
                        style = if (deviceMode == DeviceMode.MOBILE) typography.h2 else typography.h1,
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.BatteryFull,
                        contentDescription = "Battery Status",
                        tint = colors.statusActive,
                        modifier = Modifier.size(if (deviceMode == DeviceMode.MOBILE) 28.dp else 40.dp)
                    )

                    Column {
                        Text(
                            text = uiState.batteryStatus.displayText.ifBlank { "85%" },
                            style = if (deviceMode == DeviceMode.MOBILE) typography.h4 else typography.h3,
                            color = colors.primaryText,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Battery",
                            style = typography.label,
                            color = colors.secondaryText
                        )
                    }
                }
            }

            // Main Section: Telemetry & Quick Actions
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(MonochromeTheme.spacing.space6)
            ) {
                // Four Telemetry Cards
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

/**
 * Telemetry Cards Row
 */
@Composable
private fun TelemetryRow(
    uiState: HomeUiState,
    modifier: Modifier = Modifier
) {
    val deviceMode = LocalDeviceMode.current

    val card1 = @Composable { m: Modifier ->
        TelemetryCard(
            icon = Icons.Default.SmartToy,
            primaryValue = uiState.statusMessage.ifBlank { "Ready" },
            secondaryLabel = "Robot State",
            modifier = m
        )
    }
    val card2 = @Composable { m: Modifier ->
        TelemetryCard(
            icon = Icons.Default.Place,
            primaryValue = if (uiState.position.displayText.isBlank() || uiState.position.displayText == "(0.0, 0.0)") "(2.4, 5.1)" else uiState.position.displayText,
            secondaryLabel = "Current Position",
            modifier = m
        )
    }
    val card3 = @Composable { m: Modifier ->
        TelemetryCard(
            icon = Icons.Default.Navigation,
            primaryValue = "180°",
            secondaryLabel = "Yaw Angle",
            modifier = m
        )
    }
    val card4 = @Composable { m: Modifier ->
        TelemetryCard(
            icon = Icons.Default.Thermostat,
            primaryValue = "24°C",
            secondaryLabel = "Temperature",
            modifier = m
        )
    }

    if (deviceMode == DeviceMode.MOBILE) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                card1(Modifier.weight(1f))
                card2(Modifier.weight(1f))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                card3(Modifier.weight(1f))
                card4(Modifier.weight(1f))
            }
        }
    } else {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            card1(Modifier.weight(1f))
            card2(Modifier.weight(1f))
            card3(Modifier.weight(1f))
            card4(Modifier.weight(1f))
        }
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
    val deviceMode = LocalDeviceMode.current

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
                    text = "System Diagnostics",
                    style = typography.bodyLarge,
                    color = colors.primaryText,
                    fontWeight = FontWeight.Bold
                )

                com.alokrathava.theme.MonochromeStatusPill(
                    text = "OPERATIONAL",
                    level = com.alokrathava.theme.StatusLevel.Active
                )
            }

            if (deviceMode == DeviceMode.MOBILE) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column {
                        Text(text = "Active Map", style = typography.caption, color = colors.mutedText)
                        Text(text = uiState.mapData.displayText, style = typography.bodySmall, color = colors.primaryText, fontWeight = FontWeight.SemiBold)
                    }
                    Column {
                        Text(text = "Dock Station", style = typography.caption, color = colors.mutedText)
                        Text(text = "Primary Charging Dock", style = typography.bodySmall, color = colors.primaryText, fontWeight = FontWeight.SemiBold)
                    }
                    Column {
                        Text(text = "Network Endpoint", style = typography.caption, color = colors.mutedText)
                        Text(text = "${uiState.ipAddress}:${uiState.port}", style = typography.bodySmall, color = colors.primaryText, fontWeight = FontWeight.SemiBold)
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Active Map", style = typography.caption, color = colors.mutedText)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = uiState.mapData.displayText, style = typography.bodySmall, color = colors.primaryText, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Dock Station", style = typography.caption, color = colors.mutedText)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = "Primary Charging Dock", style = typography.bodySmall, color = colors.primaryText, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Network Endpoint", style = typography.caption, color = colors.mutedText)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = "${uiState.ipAddress}:${uiState.port}", style = typography.bodySmall, color = colors.primaryText, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Preview(name = "Pixel 7 Portrait", showBackground = true, device = "id:pixel_7")
@Preview(name = "Pixel 8 Portrait", showBackground = true, device = "id:pixel_8")
@Preview(name = "1920x1080 Widescreen", showBackground = true, device = "spec:width=1920px,height=1080px,dpi=160")
@Composable
fun HomeScreenPreview() {
    MonochromeTheme(darkTheme = false) {
        HomeScreenContent(
            uiState = HomeUiState()
        )
    }
}
