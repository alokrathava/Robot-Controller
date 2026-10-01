package com.agrathava.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.agrathava.sdk.model.ConnectionStatus
import com.agrathava.sdk.model.WifiNetwork
import com.agrathava.theme.MonochromeButton
import com.agrathava.theme.MonochromeButtonSize
import com.agrathava.theme.MonochromeButtonVariant
import com.agrathava.theme.MonochromeCard
import com.agrathava.theme.MonochromeStatusPill
import com.agrathava.theme.MonochromeTextField
import com.agrathava.theme.MonochromeTheme
import com.agrathava.theme.StatusLevel

enum class RobotConnectionStep {
    NETWORK_SELECTION,
    IP_PORT_CONFIG
}

@Composable
fun RobotConnectionScreen(
    connectionStep: RobotConnectionStep,
    availableNetworks: List<WifiNetwork>,
    selectedNetwork: WifiNetwork?,
    ipAddress: String,
    port: String,
    ipError: String?,
    portError: String?,
    connectionStatus: ConnectionStatus,
    onSelectNetwork: (WifiNetwork) -> Unit,
    onRefreshNetworks: () -> Unit,
    onUpdateIpAddress: (String) -> Unit,
    onUpdatePort: (String) -> Unit,
    onNextStep: () -> Unit,
    onPreviousStep: () -> Unit,
    onConnectClick: () -> Unit,
    onCancelClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MonochromeTheme.colors
    val spacing = MonochromeTheme.spacing

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(spacing.space4),
        contentAlignment = Alignment.TopCenter
    ) {
        AnimatedContent(
            targetState = connectionStep,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "ConnectionStepTransition"
        ) { step ->
            when (step) {
                RobotConnectionStep.NETWORK_SELECTION -> {
                    RobotNetworkScreen(
                        availableNetworks = availableNetworks,
                        selectedNetwork = selectedNetwork,
                        onSelectNetwork = onSelectNetwork,
                        onRefreshNetworks = onRefreshNetworks,
                        onNextStep = onNextStep,
                        onCancelClick = onCancelClick
                    )
                }

                RobotConnectionStep.IP_PORT_CONFIG -> {
                    RobotIpPortScreen(
                        selectedNetwork = selectedNetwork,
                        ipAddress = ipAddress,
                        port = port,
                        ipError = ipError,
                        portError = portError,
                        connectionStatus = connectionStatus,
                        onUpdateIpAddress = onUpdateIpAddress,
                        onUpdatePort = onUpdatePort,
                        onPreviousStep = onPreviousStep,
                        onConnectClick = onConnectClick
                    )
                }
            }
        }
    }
}

/**
 * Step 1: Wi-Fi Networks Screen
 */
@Composable
fun RobotNetworkScreen(
    availableNetworks: List<WifiNetwork>,
    selectedNetwork: WifiNetwork?,
    onSelectNetwork: (WifiNetwork) -> Unit,
    onRefreshNetworks: () -> Unit,
    onNextStep: () -> Unit,
    onCancelClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MonochromeTheme.colors
    val typography = MonochromeTheme.typography
    val spacing = MonochromeTheme.spacing

    Column(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(spacing.space4),
        horizontalAlignment = Alignment.Start
    ) {
        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Select Robot Network",
                    style = typography.h3,
                    color = colors.primaryText
                )
                Spacer(modifier = Modifier.height(spacing.space1))
                Text(
                    text = "Choose a Wi-Fi network to discover and connect to your robot.",
                    style = typography.bodySmall,
                    color = colors.secondaryText
                )
            }

            MonochromeButton(
                onClick = onRefreshNetworks,
                variant = MonochromeButtonVariant.Ghost,
                size = MonochromeButtonSize.Standard,
                icon = Icons.Default.Refresh,
                text = "Refresh"
            )
        }

        Spacer(modifier = Modifier.height(spacing.space2))

        // Networks List
        Text(
            text = "AVAILABLE NETWORKS (${availableNetworks.size})",
            style = typography.label,
            color = colors.mutedText,
            fontWeight = FontWeight.SemiBold
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(spacing.space3),
            modifier = Modifier.fillMaxWidth()
        ) {
            availableNetworks.forEach { network ->
                val isSelected = selectedNetwork?.ssid == network.ssid
                val borderColor = if (isSelected) colors.primaryText else colors.defaultBorder
                val cardBg = if (isSelected) colors.interactiveSurface else colors.surface

                MonochromeCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = cardBg,
                    borderColor = borderColor,
                    onClick = { onSelectNetwork(network) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(spacing.space3)
                        ) {
                            Icon(
                                imageVector = if (network.isSecured) Icons.Default.WifiLock else Icons.Default.Wifi,
                                contentDescription = null,
                                tint = if (isSelected) colors.primaryText else colors.secondaryText,
                                modifier = Modifier.size(24.dp)
                            )

                            Column {
                                Text(
                                    text = network.ssid,
                                    style = typography.body,
                                    color = colors.primaryText,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${network.frequency} • Signal ${network.signalPercent}%",
                                    style = typography.caption,
                                    color = colors.secondaryText
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(spacing.space2)
                        ) {
                            if (network.isSecured) {
                                MonochromeStatusPill(
                                    text = "WPA2/WPA3",
                                    level = StatusLevel.Default
                                )
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = colors.primaryText,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(spacing.space4))

        // Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.space3)
        ) {
            MonochromeButton(
                onClick = onCancelClick,
                variant = MonochromeButtonVariant.Ghost,
                size = MonochromeButtonSize.Large,
                text = "Back",
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                modifier = Modifier.weight(1f)
            )

            MonochromeButton(
                onClick = onNextStep,
                variant = MonochromeButtonVariant.Primary,
                size = MonochromeButtonSize.Large,
                text = "Configure IP & Port",
                icon = Icons.AutoMirrored.Filled.ArrowForward,
                enabled = selectedNetwork != null,
                modifier = Modifier.weight(1.5f)
            )
        }
    }
}

/**
 * Step 2: IP and Port Configuration Screen
 */
@Composable
fun RobotIpPortScreen(
    selectedNetwork: WifiNetwork?,
    ipAddress: String,
    port: String,
    ipError: String?,
    portError: String?,
    connectionStatus: ConnectionStatus,
    onUpdateIpAddress: (String) -> Unit,
    onUpdatePort: (String) -> Unit,
    onPreviousStep: () -> Unit,
    onConnectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MonochromeTheme.colors
    val typography = MonochromeTheme.typography
    val spacing = MonochromeTheme.spacing

    Column(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(spacing.space4),
        horizontalAlignment = Alignment.Start
    ) {
        // Header
        Column {
            Text(
                text = "Robot IP & Port Setup",
                style = typography.h3,
                color = colors.primaryText
            )
            Spacer(modifier = Modifier.height(spacing.space1))
            Text(
                text = "Specify the IP address and port number for the robot endpoint.",
                style = typography.bodySmall,
                color = colors.secondaryText
            )
        }

        // Selected Network Banner
        if (selectedNetwork != null) {
            MonochromeCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = colors.interactiveSurface
            ) {
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
                            imageVector = Icons.Default.Wifi,
                            contentDescription = null,
                            tint = colors.primaryText,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "Target Wi-Fi Network",
                                style = typography.caption,
                                color = colors.secondaryText
                            )
                            Text(
                                text = selectedNetwork.ssid,
                                style = typography.bodySmall,
                                color = colors.primaryText,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    MonochromeButton(
                        onClick = onPreviousStep,
                        variant = MonochromeButtonVariant.Ghost,
                        size = MonochromeButtonSize.Standard,
                        text = "Change Network"
                    )
                }
            }
        }

        // Form Fields
        Column(
            verticalArrangement = Arrangement.spacedBy(spacing.space3),
            modifier = Modifier.fillMaxWidth()
        ) {
            MonochromeTextField(
                value = ipAddress,
                onValueChange = onUpdateIpAddress,
                label = "IP Address",
                placeholder = "192.168.1.100",
                helperText = ipError ?: "Default IP: 192.168.1.100",
                isError = ipError != null,
                leadingIcon = Icons.Default.Router,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            MonochromeTextField(
                value = port,
                onValueChange = onUpdatePort,
                label = "Port Number",
                placeholder = "8080",
                helperText = portError ?: "Default Port: 8080",
                isError = portError != null,
                leadingIcon = Icons.Default.Dns,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Connection Status Banner
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Connection Status",
                style = typography.label,
                color = colors.secondaryText
            )

            val (statusText, statusLevel) = when (connectionStatus) {
                ConnectionStatus.DISCONNECTED -> "Disconnected" to StatusLevel.Inactive
                ConnectionStatus.CONNECTING -> "Connecting..." to StatusLevel.Default
                ConnectionStatus.CONNECTED -> "Connected" to StatusLevel.Active
                ConnectionStatus.FAILED -> "Connection Failed" to StatusLevel.Critical
            }

            MonochromeStatusPill(
                text = statusText,
                level = statusLevel
            )
        }

        Spacer(modifier = Modifier.height(spacing.space3))

        // Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.space3)
        ) {
            MonochromeButton(
                onClick = onPreviousStep,
                variant = MonochromeButtonVariant.Secondary,
                size = MonochromeButtonSize.Large,
                text = "Back",
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                modifier = Modifier.weight(1f)
            )

            MonochromeButton(
                onClick = onConnectClick,
                variant = MonochromeButtonVariant.Primary,
                size = MonochromeButtonSize.Large,
                text = if (connectionStatus == ConnectionStatus.CONNECTING) "Connecting..." else "Connect Robot",
                icon = Icons.Default.PowerSettingsNew,
                enabled = ipError == null && portError == null && ipAddress.isNotBlank() && port.isNotBlank(),
                modifier = Modifier.weight(1.5f)
            )
        }
    }
}

@Preview(name = "Step 1: Network Selection Preview", showBackground = true)
@Composable
fun RobotNetworkScreenPreview() {
    MonochromeTheme(darkTheme = false) {
        RobotNetworkScreen(
            availableNetworks = listOf(
                WifiNetwork(ssid = "ROBOT_HOTSPOT_5G", signalPercent = 95, isSecured = true),
                WifiNetwork(ssid = "LAB_ROBOTICS_NET", signalPercent = 82, isSecured = true),
                WifiNetwork(ssid = "OFFICE_GUEST_WIFI", signalPercent = 68, isSecured = false)
            ),
            selectedNetwork = WifiNetwork(ssid = "ROBOT_HOTSPOT_5G", signalPercent = 95, isSecured = true),
            onSelectNetwork = {},
            onRefreshNetworks = {},
            onNextStep = {},
            onCancelClick = {}
        )
    }
}

@Preview(name = "Step 2: IP and Port Preview", showBackground = true)
@Composable
fun RobotIpPortScreenPreview() {
    MonochromeTheme(darkTheme = false) {
        RobotIpPortScreen(
            selectedNetwork = WifiNetwork(ssid = "ROBOT_HOTSPOT_5G", signalPercent = 95, isSecured = true),
            ipAddress = "192.168.1.100",
            port = "8080",
            ipError = null,
            portError = null,
            connectionStatus = ConnectionStatus.DISCONNECTED,
            onUpdateIpAddress = {},
            onUpdatePort = {},
            onPreviousStep = {},
            onConnectClick = {}
        )
    }
}
