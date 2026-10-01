package com.agrathava.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agrathava.theme.MonochromeButton
import com.agrathava.theme.MonochromeButtonSize
import com.agrathava.theme.MonochromeButtonVariant
import com.agrathava.theme.MonochromeCard
import com.agrathava.theme.MonochromeStatusPill
import com.agrathava.theme.MonochromeTheme
import com.agrathava.theme.StatusLevel

enum class ScreenFlow {
    SPLASH,
    CONNECTION,
    DASHBOARD
}

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
    initialFlow: ScreenFlow = ScreenFlow.SPLASH
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
                onMoveForward = viewModel::moveForward,
                onMoveBackward = viewModel::moveBackward,
                onMoveLeft = viewModel::moveLeft,
                onMoveRight = viewModel::moveRight,
                onGoToCharge = viewModel::goToCharge,
                onCancelNavigation = viewModel::cancelNavigation,
                onGetPosition = viewModel::getPosition,
                onMoveToPosition = viewModel::moveToPosition,
                onGetMap = viewModel::getMap,
                onSaveMap = viewModel::saveMap,
                onGetBatteryLevel = viewModel::getBatteryLevel,
                onShowSplash = { currentFlow = ScreenFlow.SPLASH },
                onShowConnection = { currentFlow = ScreenFlow.CONNECTION },
                modifier = modifier
            )
        }
    }
}

@Composable
fun HomeScreenContent(
    uiState: HomeUiState,
    onMoveForward: () -> Unit,
    onMoveBackward: () -> Unit,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    onGoToCharge: () -> Unit,
    onCancelNavigation: () -> Unit,
    onGetPosition: () -> Unit,
    onMoveToPosition: () -> Unit,
    onGetMap: () -> Unit,
    onSaveMap: () -> Unit,
    onGetBatteryLevel: () -> Unit,
    modifier: Modifier = Modifier,
    onShowSplash: () -> Unit = {},
    onShowConnection: () -> Unit = {}
) {
    val colors = MonochromeTheme.colors
    val typography = MonochromeTheme.typography
    val spacing = MonochromeTheme.spacing

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(spacing.space4),
        contentAlignment = Alignment.TopCenter
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(spacing.space5),
            verticalAlignment = Alignment.Top
        ) {
            // Left Pane: Status Overview & Primary Refresh Action
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(spacing.space4)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Robot Navigation",
                        style = typography.h3,
                        color = colors.primaryText
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(spacing.space2),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MonochromeButton(
                            onClick = onShowConnection,
                            variant = MonochromeButtonVariant.Ghost,
                            size = MonochromeButtonSize.Standard,
                            icon = Icons.Default.Wifi,
                            text = "Connection"
                        )
                        MonochromeButton(
                            onClick = onShowSplash,
                            variant = MonochromeButtonVariant.Ghost,
                            size = MonochromeButtonSize.Standard,
                            icon = Icons.Default.SmartToy,
                            text = "Robot View"
                        )
                        MonochromeStatusPill(
                            text = uiState.statusMessage,
                            level = if (uiState.batteryStatus.isCharging) StatusLevel.Active else StatusLevel.Default
                        )
                    }
                }

                // Status Card
                MonochromeCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(spacing.space3)
                    ) {
                        HomeStatusRow(
                            icon = Icons.Default.Wifi,
                            label = "Connected Wi-Fi",
                            value = uiState.selectedNetwork?.ssid ?: "Not Connected",
                            isMono = false
                        )
                        HomeStatusRow(
                            icon = Icons.Default.Router,
                            label = "Robot Endpoint",
                            value = "${uiState.ipAddress}:${uiState.port}",
                            isMono = true
                        )
                        HomeStatusRow(
                            icon = Icons.Default.BatteryFull,
                            label = "Battery Level",
                            value = uiState.batteryStatus.displayText,
                            isMono = true
                        )
                        HomeStatusRow(
                            icon = Icons.Default.LocationOn,
                            label = "Current Position",
                            value = uiState.position.displayText,
                            isMono = true
                        )
                        HomeStatusRow(
                            icon = Icons.Default.Map,
                            label = "Map Data",
                            value = uiState.mapData.displayText,
                            isMono = false
                        )
                    }
                }

                // Primary Battery Refresh Action
                MonochromeButton(
                    onClick = onGetBatteryLevel,
                    variant = MonochromeButtonVariant.Primary,
                    size = MonochromeButtonSize.Large,
                    icon = Icons.Default.BatteryChargingFull,
                    text = "Refresh Battery Status",
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Right Pane: Directional Controls & Navigation Actions
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(spacing.space4),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Directional Movement
                Text(
                    text = "Directional Controls",
                    style = typography.label,
                    color = colors.secondaryText
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(spacing.space2)
                ) {
                    MonochromeButton(
                        onClick = onMoveForward,
                        icon = Icons.Default.KeyboardArrowUp,
                        text = "Forward",
                        variant = MonochromeButtonVariant.Secondary,
                        modifier = Modifier.width(140.dp)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(spacing.space2)
                    ) {
                        MonochromeButton(
                            onClick = onMoveLeft,
                            icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            text = "Left",
                            variant = MonochromeButtonVariant.Secondary,
                            modifier = Modifier.width(140.dp)
                        )

                        MonochromeButton(
                            onClick = onMoveRight,
                            icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            text = "Right",
                            variant = MonochromeButtonVariant.Secondary,
                            modifier = Modifier.width(140.dp)
                        )
                    }

                    MonochromeButton(
                        onClick = onMoveBackward,
                        icon = Icons.Default.KeyboardArrowDown,
                        text = "Backward",
                        variant = MonochromeButtonVariant.Secondary,
                        modifier = Modifier.width(140.dp)
                    )
                }

                // Navigation Actions
                Text(
                    text = "Navigation & Actions",
                    style = typography.label,
                    color = colors.secondaryText
                )

                HomeActionButtonRow(
                    listOf(
                        Triple("Go to Charge", Icons.Default.EvStation, onGoToCharge),
                        Triple("Cancel Navigation", Icons.Default.Cancel, onCancelNavigation)
                    )
                )

                HomeActionButtonRow(
                    listOf(
                        Triple("Get Position", Icons.Default.GpsFixed, onGetPosition),
                        Triple("Move to Position", Icons.Default.MyLocation, onMoveToPosition)
                    )
                )

                HomeActionButtonRow(
                    listOf(
                        Triple("Get Map", Icons.Default.Map, onGetMap),
                        Triple("Save Map", Icons.Default.Save, onSaveMap)
                    )
                )
            }
        }
    }
}

@Composable
private fun HomeStatusRow(
    icon: ImageVector,
    label: String,
    value: String,
    isMono: Boolean
) {
    val colors = MonochromeTheme.colors
    val typography = MonochromeTheme.typography
    val spacing = MonochromeTheme.spacing

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.space2)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.secondaryText,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                style = typography.bodySmall,
                color = colors.secondaryText
            )
        }
        Text(
            text = value,
            style = if (isMono) typography.monoBody else typography.bodySmall,
            color = colors.primaryText
        )
    }
}

@Composable
private fun HomeActionButtonRow(
    buttons: List<Triple<String, ImageVector, () -> Unit>>
) {
    val spacing = MonochromeTheme.spacing

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.space3)
    ) {
        buttons.forEach { (text, icon, action) ->
            MonochromeButton(
                onClick = action,
                icon = icon,
                text = text,
                variant = MonochromeButtonVariant.Secondary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Preview(name = "1920x1080 Widescreen", showBackground = true, device = "spec:width=1920px,height=1080px,dpi=160")
@Preview(name = "1920x1080 High DPI", showBackground = true, device = "spec:width=1920px,height=1080px,dpi=240")
@Preview(name = "Portrait Fallback", showBackground = true, device = "spec:parent=pixel_5")
@Composable
fun HomeScreenPreview() {
    MonochromeTheme {
        HomeScreenContent(
            uiState = HomeUiState(),
            onMoveForward = {},
            onMoveBackward = {},
            onMoveLeft = {},
            onMoveRight = {},
            onGoToCharge = {},
            onCancelNavigation = {},
            onGetPosition = {},
            onMoveToPosition = {},
            onGetMap = {},
            onSaveMap = {},
            onGetBatteryLevel = {}
        )
    }
}
