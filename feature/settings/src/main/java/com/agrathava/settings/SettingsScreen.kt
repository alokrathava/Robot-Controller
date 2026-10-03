package com.agrathava.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agrathava.home.Sidebar
import com.agrathava.home.SidebarNavItem
import com.agrathava.theme.MonochromeButton
import com.agrathava.theme.MonochromeButtonVariant
import com.agrathava.theme.MonochromeCard
import com.agrathava.theme.MonochromeStatusPill
import com.agrathava.theme.MonochromeTextField
import com.agrathava.theme.MonochromeTheme
import com.agrathava.theme.StatusLevel

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
    selectedNav: SidebarNavItem = SidebarNavItem.SETTINGS,
    onSidebarItemSelected: (SidebarNavItem) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SettingsScreenContent(
        uiState = uiState,
        selectedNav = selectedNav,
        onSidebarItemSelected = onSidebarItemSelected,
        onTabSelected = viewModel::selectTab,
        onIpAddressChanged = viewModel::onRobotIpAddressChanged,
        onPortChanged = viewModel::onPortChanged,
        onAutoConnectToggled = viewModel::onAutoConnectToggled,
        onReconnectionAttemptsChanged = viewModel::onReconnectionAttemptsChanged,
        onConnectionTimeoutChanged = viewModel::onConnectionTimeoutChanged,
        onToggleConnection = viewModel::toggleConnection,
        modifier = modifier
    )
}

@Composable
fun SettingsScreenContent(
    uiState: SettingsUiState,
    modifier: Modifier = Modifier,
    selectedNav: SidebarNavItem = SidebarNavItem.SETTINGS,
    onSidebarItemSelected: (SidebarNavItem) -> Unit = {},
    onTabSelected: (SettingsTab) -> Unit = {},
    onIpAddressChanged: (String) -> Unit = {},
    onPortChanged: (String) -> Unit = {},
    onAutoConnectToggled: (Boolean) -> Unit = {},
    onReconnectionAttemptsChanged: (Int) -> Unit = {},
    onConnectionTimeoutChanged: (Int) -> Unit = {},
    onToggleConnection: () -> Unit = {}
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
                text = "Settings",
                style = typography.h2,
                color = colors.primaryText
            )
            Spacer(modifier = Modifier.height(spacing.space1))
            Text(
                text = "Configure robot, connection and app preferences",
                style = typography.bodySmall,
                color = colors.secondaryText
            )

            Spacer(modifier = Modifier.height(spacing.space5))

            // Main Content Area: Left Form Controls + Right Connection Status Card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(spacing.space6),
                verticalAlignment = Alignment.Top
            ) {
                // Left Column: Form Controls Section
                Column(
                    modifier = Modifier
                        .weight(0.62f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(spacing.space5)
                ) {
                    // Segmented Tab Bar Switcher
                    SettingsTabSwitcher(
                        selectedTab = uiState.selectedTab,
                        onTabSelected = onTabSelected
                    )

                    // Tab Content
                    when (uiState.selectedTab) {
                        SettingsTab.CONNECTION -> {
                            ConnectionTabContent(
                                uiState = uiState,
                                onIpAddressChanged = onIpAddressChanged,
                                onPortChanged = onPortChanged,
                                onAutoConnectToggled = onAutoConnectToggled,
                                onReconnectionAttemptsChanged = onReconnectionAttemptsChanged,
                                onConnectionTimeoutChanged = onConnectionTimeoutChanged
                            )
                        }

                        SettingsTab.NAVIGATION -> {
                            PlaceholderTabContent(title = "Navigation Settings")
                        }

                        SettingsTab.ROBOT -> {
                            PlaceholderTabContent(title = "Robot Preferences")
                        }

                        SettingsTab.ADVANCED -> {
                            PlaceholderTabContent(title = "Advanced Configurations")
                        }
                    }
                }

                // Right Column: Connection Status Card
                Column(
                    modifier = Modifier
                        .weight(0.38f)
                        .fillMaxHeight()
                ) {
                    MonochromeCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = colors.surface,
                        borderColor = colors.hoverSurface,
                        padding = spacing.cardPadding
                    ) {
                        Text(
                            text = "Connection Status",
                            style = typography.h4,
                            color = colors.primaryText
                        )

                        Spacer(modifier = Modifier.height(spacing.space3))

                        // Live Status Pill
                        MonochromeStatusPill(
                            text = if (uiState.isConnected) "Connected" else "Disconnected",
                            level = if (uiState.isConnected) StatusLevel.Active else StatusLevel.Inactive
                        )

                        Spacer(modifier = Modifier.height(spacing.space5))

                        // Connection Details List
                        Column(
                            verticalArrangement = Arrangement.spacedBy(spacing.space3)
                        ) {
                            ConnectionMetadataRow(label = "IP Address", value = uiState.robotIpAddress)
                            ConnectionMetadataRow(label = "Port", value = uiState.port)
                            ConnectionMetadataRow(label = "Latency", value = "${uiState.latencyMs} ms")
                            ConnectionMetadataRow(label = "Last Connected", value = uiState.lastConnected)
                        }

                        Spacer(modifier = Modifier.height(spacing.space6))

                        // Disconnect Action Button
                        MonochromeButton(
                            onClick = onToggleConnection,
                            text = if (uiState.isConnected) "Disconnect" else "Connect",
                            variant = MonochromeButtonVariant.Secondary,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsTabSwitcher(
    selectedTab: SettingsTab,
    onTabSelected: (SettingsTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MonochromeTheme.colors
    val typography = MonochromeTheme.typography
    val spacing = MonochromeTheme.spacing
    val shapes = MonochromeTheme.shapes

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(shapes.inputs),
        color = colors.interactiveSurface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.space1),
            horizontalArrangement = Arrangement.spacedBy(spacing.space1)
        ) {
            SettingsTab.entries.forEach { tab ->
                val isSelected = selectedTab == tab
                val bg = if (isSelected) colors.surface else Color.Transparent

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(shapes.default)
                        .background(bg)
                        .then(
                            if (isSelected) {
                                Modifier.border(
                                    BorderStroke(1.dp, colors.defaultBorder),
                                    shapes.default
                                )
                            } else {
                                Modifier
                            }
                        )
                        .clickable { onTabSelected(tab) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab.title,
                        style = typography.label,
                        color = if (isSelected) colors.primaryText else colors.secondaryText
                    )
                }
            }
        }
    }
}

@Composable
private fun ConnectionTabContent(
    uiState: SettingsUiState,
    onIpAddressChanged: (String) -> Unit,
    onPortChanged: (String) -> Unit,
    onAutoConnectToggled: (Boolean) -> Unit,
    onReconnectionAttemptsChanged: (Int) -> Unit,
    onConnectionTimeoutChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MonochromeTheme.colors
    val typography = MonochromeTheme.typography
    val spacing = MonochromeTheme.spacing
    val shapes = MonochromeTheme.shapes

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.space4)
    ) {
        // Row 1: Robot IP Address & Port
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.space4)
        ) {
            MonochromeTextField(
                value = uiState.robotIpAddress,
                onValueChange = onIpAddressChanged,
                label = "Robot IP Address",
                placeholder = "192.168.1.108",
                modifier = Modifier.weight(1.5f)
            )

            MonochromeTextField(
                value = uiState.port,
                onValueChange = onPortChanged,
                label = "Port",
                placeholder = "6080",
                modifier = Modifier.weight(1.0f)
            )
        }

        // Row 2: Auto Connect Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = spacing.space1),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Auto Connect",
                    style = typography.label,
                    color = colors.primaryText
                )
                Spacer(modifier = Modifier.height(spacing.space1))
                Text(
                    text = "Automatically connect to robot on app launch",
                    style = typography.caption,
                    color = colors.secondaryText
                )
            }

            Switch(
                checked = uiState.autoConnect,
                onCheckedChange = onAutoConnectToggled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = colors.surface,
                    checkedTrackColor = colors.primaryActionBg,
                    uncheckedThumbColor = colors.mutedText,
                    uncheckedTrackColor = colors.interactiveSurface,
                    uncheckedBorderColor = colors.defaultBorder
                )
            )
        }

        // Row 3: Reconnection Attempts Dropdown
        var attemptsExpanded by remember { mutableStateOf(false) }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.space2)
            ) {
                Text(
                    text = "Reconnection Attempts",
                    style = typography.label,
                    color = colors.primaryText
                )
                Icon(
                    imageVector = Icons.Default.CellTower,
                    contentDescription = null,
                    tint = colors.secondaryText,
                    modifier = Modifier.size(18.dp)
                )
            }

            Box {
                Box(
                    modifier = Modifier
                        .width(180.dp)
                        .height(44.dp)
                        .clip(shapes.inputs)
                        .background(colors.surface)
                        .border(1.dp, colors.defaultBorder, shapes.inputs)
                        .clickable { attemptsExpanded = true }
                        .padding(horizontal = spacing.space3),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${uiState.reconnectionAttempts}",
                            style = typography.monoBody,
                            color = colors.primaryText
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Expand attempts menu",
                            tint = colors.secondaryText,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = attemptsExpanded,
                    onDismissRequest = { attemptsExpanded = false }
                ) {
                    uiState.availableReconnectionAttempts.forEach { attempts ->
                        DropdownMenuItem(
                            text = { Text(text = "$attempts", style = typography.monoBody) },
                            onClick = {
                                onReconnectionAttemptsChanged(attempts)
                                attemptsExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // Row 4: Connection Timeout Dropdown
        var timeoutExpanded by remember { mutableStateOf(false) }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Connection Timeout",
                style = typography.label,
                color = colors.primaryText
            )

            Box {
                Box(
                    modifier = Modifier
                        .width(180.dp)
                        .height(44.dp)
                        .clip(shapes.inputs)
                        .background(colors.surface)
                        .border(1.dp, colors.defaultBorder, shapes.inputs)
                        .clickable { timeoutExpanded = true }
                        .padding(horizontal = spacing.space3),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${uiState.connectionTimeoutSeconds} seconds",
                            style = typography.monoBody,
                            color = colors.primaryText
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Expand timeout menu",
                            tint = colors.secondaryText,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = timeoutExpanded,
                    onDismissRequest = { timeoutExpanded = false }
                ) {
                    uiState.availableTimeoutSeconds.forEach { timeout ->
                        DropdownMenuItem(
                            text = { Text(text = "$timeout seconds", style = typography.monoBody) },
                            onClick = {
                                onConnectionTimeoutChanged(timeout)
                                timeoutExpanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectionMetadataRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val colors = MonochromeTheme.colors
    val typography = MonochromeTheme.typography

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = typography.bodySmall,
            color = colors.secondaryText,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = typography.monoBody,
            color = colors.primaryText
        )
    }
}

@Composable
private fun PlaceholderTabContent(
    title: String,
    modifier: Modifier = Modifier
) {
    val colors = MonochromeTheme.colors
    val typography = MonochromeTheme.typography
    val spacing = MonochromeTheme.spacing

    MonochromeCard(
        modifier = modifier.fillMaxWidth(),
        backgroundColor = colors.surface,
        borderColor = colors.hoverSurface,
        padding = spacing.cardPadding
    ) {
        Text(
            text = title,
            style = typography.h4,
            color = colors.primaryText
        )
        Spacer(modifier = Modifier.height(spacing.space2))
        Text(
            text = "Configure parameters and settings for this section.",
            style = typography.bodySmall,
            color = colors.secondaryText
        )
    }
}

@Preview(name = "Settings Screen Preview", showBackground = true, widthDp = 1000, heightDp = 600)
@Composable
fun SettingsScreenPreview() {
    MonochromeTheme(darkTheme = false) {
        SettingsScreenContent(
            uiState = SettingsUiState(
                selectedTab = SettingsTab.CONNECTION,
                robotIpAddress = "192.168.1.108",
                port = "6080",
                autoConnect = true,
                reconnectionAttempts = 3,
                connectionTimeoutSeconds = 10,
                connectionStatus = com.agrathava.sdk.model.ConnectionStatus.CONNECTED,
                connectionAddress = "192.168.1.108:8080",
                latencyMs = 12,
                lastConnected = "Sep 30, 2026 9:41 AM"
            )
        )
    }
}
