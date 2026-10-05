package com.alokrathava.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.alokrathava.sdk.model.ConnectionStatus
import com.alokrathava.sdk.model.DiscoveredRobot
import com.alokrathava.sdk.model.WifiNetwork
import com.alokrathava.theme.MonochromeButton
import com.alokrathava.theme.MonochromeButtonSize
import com.alokrathava.theme.MonochromeButtonVariant
import com.alokrathava.theme.MonochromeCard
import com.alokrathava.theme.MonochromeStatusPill
import com.alokrathava.theme.MonochromeTextField
import com.alokrathava.theme.MonochromeTheme
import com.alokrathava.theme.StatusLevel

enum class RobotConnectionStep {
    AUTO_DISCOVERY,
    NETWORK_SELECTION,
    IP_PORT_CONFIG,
    CONNECTING,
    CONNECTION_FAILED
}

@Composable
fun RobotConnectionScreen(
    connectionStep: RobotConnectionStep,
    discoveredRobots: List<DiscoveredRobot> = emptyList(),
    selectedDiscoveredRobot: DiscoveredRobot? = null,
    isDiscoveringRobots: Boolean = false,
    availableNetworks: List<WifiNetwork>,
    selectedNetwork: WifiNetwork?,
    ipAddress: String,
    port: String,
    token: String = "",
    useTls: Boolean = false,
    ipError: String?,
    portError: String?,
    tokenError: String? = null,
    connectionErrorMessage: String? = null,
    connectionStatus: ConnectionStatus,
    onStartDiscovery: () -> Unit = {},
    onSelectDiscoveredRobot: (DiscoveredRobot) -> Unit = {},
    onQuickConnectDiscoveredRobot: (DiscoveredRobot) -> Unit = {},
    onSelectNetwork: (WifiNetwork) -> Unit,
    onRefreshNetworks: () -> Unit,
    onUpdateIpAddress: (String) -> Unit,
    onUpdatePort: (String) -> Unit,
    onUpdateToken: (String) -> Unit = {},
    onUpdateUseTls: (Boolean) -> Unit = {},
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 680.dp),
            verticalArrangement = Arrangement.spacedBy(spacing.space4)
        ) {
            // Segmented Navigation Bar
            ConnectionStepTabBar(
                currentStep = connectionStep,
                onStepSelected = { step ->
                    when (step) {
                        RobotConnectionStep.AUTO_DISCOVERY -> {
                            onStartDiscovery()
                        }
                        else -> {}
                    }
                }
            )

            AnimatedContent(
                targetState = connectionStep,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "ConnectionStepTransition"
            ) { step ->
                when (step) {
                    RobotConnectionStep.AUTO_DISCOVERY -> {
                        RobotAutoDiscoveryScreen(
                            discoveredRobots = discoveredRobots,
                            selectedRobot = selectedDiscoveredRobot,
                            isDiscovering = isDiscoveringRobots,
                            onStartDiscovery = onStartDiscovery,
                            onSelectRobot = onSelectDiscoveredRobot,
                            onQuickConnect = onQuickConnectDiscoveredRobot,
                            onManualSetupClick = onNextStep,
                            onNetworkSetupClick = {
                                onNextStep()
                            },
                            onCancelClick = onCancelClick
                        )
                    }

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

                    RobotConnectionStep.IP_PORT_CONFIG,
                    RobotConnectionStep.CONNECTING,
                    RobotConnectionStep.CONNECTION_FAILED -> {
                        RobotIpPortScreen(
                            selectedNetwork = selectedNetwork,
                            selectedDiscoveredRobot = selectedDiscoveredRobot,
                            ipAddress = ipAddress,
                            port = port,
                            token = token,
                            useTls = useTls,
                            ipError = ipError,
                            portError = portError,
                            tokenError = tokenError,
                            connectionErrorMessage = connectionErrorMessage,
                            connectionStatus = connectionStatus,
                            onUpdateIpAddress = onUpdateIpAddress,
                            onUpdatePort = onUpdatePort,
                            onUpdateToken = onUpdateToken,
                            onUpdateUseTls = onUpdateUseTls,
                            onPreviousStep = onPreviousStep,
                            onConnectClick = onConnectClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectionStepTabBar(
    currentStep: RobotConnectionStep,
    onStepSelected: (RobotConnectionStep) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MonochromeTheme.colors
    val typography = MonochromeTheme.typography
    val spacing = MonochromeTheme.spacing

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface, RoundedCornerShape(8.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val steps = listOf(
            RobotConnectionStep.AUTO_DISCOVERY to "1. Auto Discover",
            RobotConnectionStep.IP_PORT_CONFIG to "2. IP & Port",
            RobotConnectionStep.NETWORK_SELECTION to "3. Wi-Fi"
        )

        steps.forEach { (step, label) ->
            val isActive = currentStep == step ||
                (step == RobotConnectionStep.IP_PORT_CONFIG &&
                    (currentStep == RobotConnectionStep.CONNECTING || currentStep == RobotConnectionStep.CONNECTION_FAILED))

            val bg = if (isActive) colors.interactiveSurface else colors.surface
            val textColor = if (isActive) colors.primaryText else colors.secondaryText

            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(bg, RoundedCornerShape(6.dp))
                    .clickable { onStepSelected(step) }
                    .padding(vertical = spacing.space2, horizontal = spacing.space3),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = typography.caption,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                    color = textColor
                )
            }
        }
    }
}

/**
 * Step 1: Auto Discovery Screen
 */
@Composable
fun RobotAutoDiscoveryScreen(
    discoveredRobots: List<DiscoveredRobot>,
    selectedRobot: DiscoveredRobot?,
    isDiscovering: Boolean,
    onStartDiscovery: () -> Unit,
    onSelectRobot: (DiscoveredRobot) -> Unit,
    onQuickConnect: (DiscoveredRobot) -> Unit,
    onManualSetupClick: () -> Unit,
    onNetworkSetupClick: () -> Unit,
    onCancelClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MonochromeTheme.colors
    val typography = MonochromeTheme.typography
    val spacing = MonochromeTheme.spacing

    Column(
        modifier = modifier
            .fillMaxWidth()
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Discovered Local Robots",
                    style = typography.h3,
                    color = colors.primaryText
                )
                Spacer(modifier = Modifier.height(spacing.space1))
                Text(
                    text = "Scanning local subnet for UDP discovery beacons (Port 8088)...",
                    style = typography.bodySmall,
                    color = colors.secondaryText
                )
            }

            MonochromeButton(
                onClick = onStartDiscovery,
                variant = MonochromeButtonVariant.Ghost,
                size = MonochromeButtonSize.Standard,
                icon = Icons.Default.Refresh,
                text = if (isDiscovering) "Scanning..." else "Scan Network",
                enabled = !isDiscovering
            )
        }

        if (isDiscovering) {
            MonochromeCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = colors.interactiveSurface
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.space3)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = colors.primaryText,
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = "Listening for robot discovery beacons...",
                        style = typography.bodySmall,
                        color = colors.primaryText
                    )
                }
            }
        }

        // List of Discovered Robots
        Text(
            text = "DISCOVERED ENDPOINTS (${discoveredRobots.size})",
            style = typography.label,
            color = colors.mutedText,
            fontWeight = FontWeight.SemiBold
        )

        if (discoveredRobots.isEmpty()) {
            MonochromeCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = colors.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(spacing.space3),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(spacing.space2)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = colors.mutedText,
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = "No robots found on local subnet",
                        style = typography.body,
                        color = colors.primaryText,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Ensure your robot is powered on and connected to the same Wi-Fi subnet, or use Manual IP setup.",
                        style = typography.caption,
                        color = colors.secondaryText
                    )
                }
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(spacing.space3),
                modifier = Modifier.fillMaxWidth()
            ) {
                discoveredRobots.forEach { robot ->
                    val isSelected = selectedRobot?.id == robot.id
                    val cardBg = if (isSelected) colors.interactiveSurface else colors.surface
                    val borderColor = if (isSelected) colors.primaryText else colors.defaultBorder

                    MonochromeCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = cardBg,
                        borderColor = borderColor,
                        onClick = { onSelectRobot(robot) }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(spacing.space3),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SmartToy,
                                    contentDescription = null,
                                    tint = if (isSelected) colors.primaryText else colors.secondaryText,
                                    modifier = Modifier.size(28.dp)
                                )

                                Column {
                                    Text(
                                        text = robot.name ?: robot.id,
                                        style = typography.body,
                                        color = colors.primaryText,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${robot.host}:${robot.port} • Protocol v${robot.protocolVersion}",
                                        style = typography.caption,
                                        color = colors.secondaryText
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(spacing.space2)
                            ) {
                                MonochromeStatusPill(
                                    text = "BEACON ONLINE",
                                    level = StatusLevel.Active
                                )

                                MonochromeButton(
                                    onClick = { onQuickConnect(robot) },
                                    variant = MonochromeButtonVariant.Primary,
                                    size = MonochromeButtonSize.Standard,
                                    text = "Connect"
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(spacing.space3))

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
                onClick = onManualSetupClick,
                variant = MonochromeButtonVariant.Secondary,
                size = MonochromeButtonSize.Large,
                text = "Manual IP Setup",
                icon = Icons.AutoMirrored.Filled.ArrowForward,
                modifier = Modifier.weight(1.5f)
            )
        }
    }
}

/**
 * Step 2: Wi-Fi Networks Screen
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
 * Step 3: IP, Port & Token Configuration Screen
 */
@Composable
fun RobotIpPortScreen(
    selectedNetwork: WifiNetwork?,
    selectedDiscoveredRobot: DiscoveredRobot? = null,
    ipAddress: String,
    port: String,
    token: String = "",
    useTls: Boolean = false,
    ipError: String?,
    portError: String?,
    tokenError: String? = null,
    connectionErrorMessage: String? = null,
    connectionStatus: ConnectionStatus,
    onUpdateIpAddress: (String) -> Unit,
    onUpdatePort: (String) -> Unit,
    onUpdateToken: (String) -> Unit = {},
    onUpdateUseTls: (Boolean) -> Unit = {},
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
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(spacing.space4),
        horizontalAlignment = Alignment.Start
    ) {
        // Header
        Column {
            Text(
                text = "Robot Connection Endpoint",
                style = typography.h3,
                color = colors.primaryText
            )
            Spacer(modifier = Modifier.height(spacing.space1))
            Text(
                text = "Specify the IP address, port number, TLS security, and gateway token.",
                style = typography.bodySmall,
                color = colors.secondaryText
            )
        }

        // Connection Error Alert Banner if failed
        if (connectionErrorMessage != null) {
            MonochromeCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = colors.surface,
                borderColor = colors.primaryText
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.space3)
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = "Error",
                        tint = colors.primaryText,
                        modifier = Modifier.size(24.dp)
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Connection Error",
                            style = typography.bodySmall,
                            color = colors.primaryText,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = connectionErrorMessage,
                            style = typography.caption,
                            color = colors.secondaryText
                        )
                    }

                    MonochromeStatusPill(
                        text = "FAILED",
                        level = StatusLevel.Critical
                    )
                }
            }
        }

        // Target Info Banner
        if (selectedDiscoveredRobot != null) {
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
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = null,
                            tint = colors.primaryText,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "Discovered Robot Endpoint",
                                style = typography.caption,
                                color = colors.secondaryText
                            )
                            Text(
                                text = "${selectedDiscoveredRobot.name ?: selectedDiscoveredRobot.id} (${selectedDiscoveredRobot.host})",
                                style = typography.bodySmall,
                                color = colors.primaryText,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    MonochromeStatusPill(
                        text = "AUTO-SELECTED",
                        level = StatusLevel.Active
                    )
                }
            }
        } else if (selectedNetwork != null) {
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
                label = "IP Address / Hostname",
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

            MonochromeTextField(
                value = token,
                onValueChange = onUpdateToken,
                label = "Gateway Auth Token",
                placeholder = "Enter token (or leave blank if unauthenticated)",
                helperText = tokenError ?: "Matches ROBOT_GATEWAY_TOKEN on server",
                isError = tokenError != null,
                leadingIcon = Icons.Default.VpnKey,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )

            // TLS Security Card
            MonochromeCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = colors.surface
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
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = colors.primaryText,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "Use Secure WebSocket (WSS / TLS)",
                                style = typography.bodySmall,
                                color = colors.primaryText,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Encrypts telemetry and command stream",
                                style = typography.caption,
                                color = colors.secondaryText
                            )
                        }
                    }

                    Switch(
                        checked = useTls,
                        onCheckedChange = onUpdateUseTls,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colors.surface,
                            checkedTrackColor = colors.primaryText,
                            uncheckedThumbColor = colors.secondaryText,
                            uncheckedTrackColor = colors.surface
                        )
                    )
                }
            }
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

@Preview(name = "Auto Discovery Preview", showBackground = true)
@Composable
fun RobotAutoDiscoveryScreenPreview() {
    MonochromeTheme(darkTheme = false) {
        RobotAutoDiscoveryScreen(
            discoveredRobots = listOf(
                DiscoveredRobot(id = "robot_a1", name = "AMR Mobile Robot 01", host = "192.168.1.100", port = 8080, protocolVersion = 1),
                DiscoveredRobot(id = "robot_a2", name = "Warehouse Rover 02", host = "192.168.1.105", port = 8080, protocolVersion = 1)
            ),
            selectedRobot = null,
            isDiscovering = false,
            onStartDiscovery = {},
            onSelectRobot = {},
            onQuickConnect = {},
            onManualSetupClick = {},
            onNetworkSetupClick = {},
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
