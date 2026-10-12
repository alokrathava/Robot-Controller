package com.alokrathava.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alokrathava.home.AdaptiveNavigationLayout
import com.alokrathava.home.SidebarNavItem
import com.alokrathava.map.MapFloorPlanCanvas
import com.alokrathava.sdk.model.ConnectionStatus
import com.alokrathava.theme.*

@Composable
fun NavigationScreen(
    modifier: Modifier = Modifier,
    viewModel: NavigationViewModel? = null,
    onSidebarItemSelected: (SidebarNavItem) -> Unit = {}
) {
    if (viewModel != null) {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        NavigationScreenContent(
            uiState = uiState,
            onTabSelected = viewModel::selectTab,
            onDestinationSelected = viewModel::selectDestination,
            onMapTap = viewModel::onMapTapped,
            onXInputChanged = viewModel::updateXInput,
            onYInputChanged = viewModel::updateYInput,
            onYawInputChanged = viewModel::updateYawInput,
            onStartNavigationClick = viewModel::startNavigation,
            onCancelNavigationClick = viewModel::cancelNavigation,
            onOpenAddLocationDialog = viewModel::openAddLocationDialog,
            onSaveNewLocation = viewModel::saveNewLocation,
            onDeleteSavedLocation = viewModel::deleteSavedLocation,
            onSidebarItemSelected = onSidebarItemSelected,
            modifier = modifier
        )
    } else {
        NavigationScreenContent(
            onSidebarItemSelected = onSidebarItemSelected,
            modifier = modifier
        )
    }
}

@Composable
fun NavigationScreenContent(
    uiState: NavigationUiState = NavigationUiState(),
    onTabSelected: (NavigationTab) -> Unit = {},
    onDestinationSelected: (NavigationDestinationUi) -> Unit = {},
    onMapTap: (Double, Double) -> Unit = { _, _ -> },
    onXInputChanged: (String) -> Unit = {},
    onYInputChanged: (String) -> Unit = {},
    onYawInputChanged: (String) -> Unit = {},
    onStartNavigationClick: () -> Unit = {},
    onCancelNavigationClick: () -> Unit = {},
    onOpenAddLocationDialog: (Boolean) -> Unit = {},
    onSaveNewLocation: (String, Double, Double) -> Unit = { _, _, _ -> },
    onDeleteSavedLocation: (String) -> Unit = {},
    onSidebarItemSelected: (SidebarNavItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = MonochromeTheme.colors
    val spacing = MonochromeTheme.spacing
    val deviceMode = LocalDeviceMode.current

    var mobileViewMode by remember { mutableStateOf("Map View") }

    AdaptiveNavigationLayout(
        selectedItem = SidebarNavItem.NAVIGATION,
        onItemSelected = onSidebarItemSelected,
        isConnected = uiState.connectionStatus == ConnectionStatus.CONNECTED,
        connectionAddress = "192.168.1.108:8080",
        modifier = modifier
    ) {
        if (deviceMode == DeviceMode.MOBILE) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NavigationHeader()

                    NavigationExecutionBanner(
                        executionStatus = uiState.executionStatus,
                        onCancelClick = onCancelNavigationClick
                    )

                    MonochromeSegmentedControl(
                        items = listOf("Map View", "Destinations"),
                        selectedItem = mobileViewMode,
                        onItemSelected = { mobileViewMode = it },
                        itemLabel = { it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    if (mobileViewMode == "Map View") {
                        Box(modifier = Modifier.fillMaxSize()) {
                            NavigationMapPanel(
                                uiState = uiState,
                                onMapTap = onMapTap,
                                modifier = Modifier.fillMaxSize()
                            )

                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                shape = MonochromeTheme.shapes.cards,
                                color = colors.surface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, colors.subtleBorder)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = when (val dest = uiState.selectedDestination) {
                                                is NavigationDestinationUi.SavedLocation -> dest.name
                                                is NavigationDestinationUi.ChargingDock -> "Primary Charging Dock"
                                                is NavigationDestinationUi.Coordinates -> "Coordinates (${dest.x}, ${dest.y})"
                                                NavigationDestinationUi.None -> "Tap Map To Target"
                                            },
                                            style = MonochromeTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.primaryText
                                        )
                                        Text(
                                            text = if (uiState.selectedDestination == NavigationDestinationUi.None) "Tap anywhere on map or choose destination" else "Target set",
                                            style = MonochromeTheme.typography.caption,
                                            color = colors.secondaryText
                                        )
                                    }

                                    MonochromeButton(
                                        text = "Start Nav",
                                        variant = MonochromeButtonVariant.Primary,
                                        size = MonochromeButtonSize.Standard,
                                        icon = Icons.AutoMirrored.Filled.Send,
                                        enabled = uiState.selectedDestination != NavigationDestinationUi.None,
                                        onClick = onStartNavigationClick
                                    )
                                }
                            }
                        }
                    } else {
                        NavigationDestinationPanel(
                            modifier = Modifier.fillMaxSize(),
                            uiState = uiState,
                            onTabSelected = onTabSelected,
                            onDestinationSelected = onDestinationSelected,
                            onXInputChanged = onXInputChanged,
                            onYInputChanged = onYInputChanged,
                            onYawInputChanged = onYawInputChanged,
                            onStartNavigationClick = onStartNavigationClick,
                            onOpenAddLocationDialog = { onOpenAddLocationDialog(true) },
                            onDeleteSavedLocation = onDeleteSavedLocation
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = spacing.space6,
                        top = spacing.cardPadding,
                        end = spacing.space6,
                        bottom = spacing.cardPadding
                    )
            ) {
                NavigationHeader()

                Spacer(modifier = Modifier.height(spacing.space4))

                NavigationExecutionBanner(
                    executionStatus = uiState.executionStatus,
                    onCancelClick = onCancelNavigationClick
                )

                Spacer(modifier = Modifier.height(spacing.space4))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(spacing.space6)
                ) {
                    NavigationMapPanel(
                        uiState = uiState,
                        onMapTap = onMapTap,
                        modifier = Modifier
                            .weight(1.4f)
                            .fillMaxHeight()
                    )

                    NavigationDestinationPanel(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        uiState = uiState,
                        onTabSelected = onTabSelected,
                        onDestinationSelected = onDestinationSelected,
                        onXInputChanged = onXInputChanged,
                        onYInputChanged = onYInputChanged,
                        onYawInputChanged = onYawInputChanged,
                        onStartNavigationClick = onStartNavigationClick,
                        onOpenAddLocationDialog = { onOpenAddLocationDialog(true) },
                        onDeleteSavedLocation = onDeleteSavedLocation
                    )
                }
            }
        }
    }

    if (uiState.isAddLocationDialogOpen) {
        var nameInput by remember { mutableStateOf("") }
        var xInput by remember { mutableStateOf(uiState.xInput) }
        var yInput by remember { mutableStateOf(uiState.yInput) }

        MonochromeDialog(
            onDismissRequest = { onOpenAddLocationDialog(false) },
            title = "Add Saved Waypoint",
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    MonochromeTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        placeholder = "Waypoint Name (e.g. Charging Dock)"
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MonochromeTextField(
                            value = xInput,
                            onValueChange = { xInput = it },
                            placeholder = "X meters",
                            modifier = Modifier.weight(1f)
                        )
                        MonochromeTextField(
                            value = yInput,
                            onValueChange = { yInput = it },
                            placeholder = "Y meters",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                MonochromeButton(
                    text = "Save Waypoint",
                    variant = MonochromeButtonVariant.Primary,
                    enabled = nameInput.isNotBlank() && xInput.toDoubleOrNull() != null && yInput.toDoubleOrNull() != null,
                    onClick = {
                        val x = xInput.toDoubleOrNull() ?: 0.0
                        val y = yInput.toDoubleOrNull() ?: 0.0
                        onSaveNewLocation(nameInput, x, y)
                    }
                )
            },
            dismissButton = {
                MonochromeButton(
                    text = "Cancel",
                    variant = MonochromeButtonVariant.Ghost,
                    onClick = { onOpenAddLocationDialog(false) }
                )
            }
        )
    }
}

@Composable
fun NavigationExecutionBanner(
    executionStatus: NavigationExecutionStatus,
    onCancelClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (executionStatus) {
        is NavigationExecutionStatus.Navigating -> {
            val etaStr = executionStatus.etaSeconds?.let { "ETA: ${it}s • " } ?: ""
            val distStr = executionStatus.distanceMeters?.let { "Distance: ${"%.1f".format(it)}m" } ?: "Autonomous Navigation In Progress"
            MonochromeBanner(
                title = "NAVIGATING TO DESTINATION",
                description = "$etaStr$distStr",
                variant = BannerVariant.Info,
                modifier = modifier,
                action = {
                    MonochromeButton(
                        onClick = onCancelClick,
                        text = "Cancel Navigation",
                        variant = MonochromeButtonVariant.Primary
                    )
                }
            )
        }

        is NavigationExecutionStatus.ObstacleDetected -> {
            MonochromeBanner(
                title = "OBSTACLE DETECTED",
                description = executionStatus.message,
                variant = BannerVariant.Warning,
                icon = Icons.Default.Warning,
                modifier = modifier
            )
        }

        is NavigationExecutionStatus.Arrived -> {
            MonochromeBanner(
                title = "ARRIVED",
                description = "Arrived at ${executionStatus.destinationName}",
                variant = BannerVariant.Active,
                modifier = modifier
            )
        }

        is NavigationExecutionStatus.Failed -> {
            MonochromeBanner(
                title = "NAVIGATION ERROR",
                description = executionStatus.errorMessage,
                variant = BannerVariant.Critical,
                icon = Icons.Default.Warning,
                modifier = modifier
            )
        }

        else -> {}
    }
}

@Composable
fun NavigationHeader(
    modifier: Modifier = Modifier
) {
    val colors = MonochromeTheme.colors
    val typography = MonochromeTheme.typography

    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.nav_title),
            style = typography.h2,
            color = colors.primaryText,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.nav_subtitle),
            style = typography.bodySmall,
            color = colors.secondaryText
        )
    }
}

@Composable
fun NavigationMapPanel(
    uiState: NavigationUiState,
    onMapTap: (Double, Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MonochromeTheme.colors
    val shapes = MonochromeTheme.shapes

    Box(
        modifier = modifier
            .clip(shapes.largeCards)
            .background(colors.surface)
            .border(1.dp, colors.subtleBorder, shapes.largeCards)
    ) {
        MapFloorPlanCanvas(
            occupancyGrid = uiState.occupancyGrid,
            robotPosition = uiState.robotPosition,
            targetPose = uiState.targetPose,
            onMapTap = onMapTap,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun NavigationDestinationPanel(
    modifier: Modifier = Modifier,
    uiState: NavigationUiState,
    onTabSelected: (NavigationTab) -> Unit,
    onDestinationSelected: (NavigationDestinationUi) -> Unit,
    onXInputChanged: (String) -> Unit,
    onYInputChanged: (String) -> Unit,
    onYawInputChanged: (String) -> Unit,
    onStartNavigationClick: () -> Unit,
    onOpenAddLocationDialog: () -> Unit,
    onDeleteSavedLocation: (String) -> Unit
) {
    val colors = MonochromeTheme.colors
    val shapes = MonochromeTheme.shapes

    Surface(
        modifier = modifier,
        shape = shapes.largeCards,
        color = colors.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.subtleBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                val savedLocationsTabTitle = stringResource(R.string.tab_saved_locations)
                val coordinatesTabTitle = stringResource(R.string.tab_coordinates)

                MonochromeSegmentedControl(
                    items = NavigationTab.entries,
                    selectedItem = uiState.selectedTab,
                    onItemSelected = onTabSelected,
                    itemLabel = { tab ->
                        when (tab) {
                            NavigationTab.SavedLocations -> savedLocationsTabTitle
                            NavigationTab.Coordinates -> coordinatesTabTitle
                            NavigationTab.MapPicker -> "Map Picker"
                        }
                    }
                )

                when (uiState.selectedTab) {
                    NavigationTab.SavedLocations -> {
                        SavedLocationsList(
                            locations = uiState.savedLocations,
                            selectedDestination = uiState.selectedDestination,
                            onLocationSelected = { location ->
                                val dest = NavigationDestinationUi.SavedLocation(
                                    id = location.id,
                                    name = location.name,
                                    x = location.x,
                                    y = location.y
                                )
                                onDestinationSelected(dest)
                            },
                            onOpenAddLocationDialog = onOpenAddLocationDialog,
                            onDeleteLocation = onDeleteSavedLocation
                        )
                    }

                    NavigationTab.Coordinates -> {
                        CoordinatesInputPanel(
                            xInput = uiState.xInput,
                            yInput = uiState.yInput,
                            yawInput = uiState.yawInput,
                            onXInputChanged = onXInputChanged,
                            onYInputChanged = onYInputChanged,
                            onYawInputChanged = onYawInputChanged
                        )
                    }

                    NavigationTab.MapPicker -> {
                        MapPickerHelpView()
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ChargingDockCard(
                    isSelected = uiState.selectedDestination == NavigationDestinationUi.ChargingDock,
                    onClick = {
                        onDestinationSelected(NavigationDestinationUi.ChargingDock)
                    }
                )

                MonochromeButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.btn_start_navigation),
                    variant = MonochromeButtonVariant.Primary,
                    size = MonochromeButtonSize.Large,
                    icon = Icons.AutoMirrored.Filled.Send,
                    enabled = uiState.selectedDestination != NavigationDestinationUi.None,
                    onClick = onStartNavigationClick
                )
            }
        }
    }
}

@Composable
fun MapPickerHelpView() {
    val colors = MonochromeTheme.colors
    val typography = MonochromeTheme.typography

    MonochromeCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = colors.interactiveSurface,
        borderColor = colors.defaultBorder
    ) {
        Text("Tap Anywhere On Map", style = typography.body, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text("Tap directly on the live occupancy map to set custom target navigation waypoints.", style = typography.caption)
    }
}

@Composable
fun SavedLocationsList(
    locations: List<SavedLocationUi>,
    selectedDestination: NavigationDestinationUi,
    onLocationSelected: (SavedLocationUi) -> Unit,
    onOpenAddLocationDialog: () -> Unit,
    onDeleteLocation: (String) -> Unit
) {
    val colors = MonochromeTheme.colors
    val typography = MonochromeTheme.typography

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SAVED WAYPOINTS (${locations.size})",
                style = typography.label,
                color = colors.secondaryText
            )
            MonochromeButton(
                text = "Add Waypoint",
                variant = MonochromeButtonVariant.Ghost,
                size = MonochromeButtonSize.Standard,
                icon = Icons.Default.Add,
                onClick = onOpenAddLocationDialog
            )
        }

        if (locations.isEmpty()) {
            Text(
                text = "No saved waypoints on active map.",
                style = typography.caption,
                color = colors.mutedText,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        locations.forEach { location ->
            val isSelected = selectedDestination is NavigationDestinationUi.SavedLocation && selectedDestination.id == location.id
            SavedLocationCard(
                location = location,
                isSelected = isSelected,
                onClick = { onLocationSelected(location) },
                onDelete = { onDeleteLocation(location.id) }
            )
        }
    }
}

@Composable
fun SavedLocationCard(
    location: SavedLocationUi,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit = {}
) {
    val colors = MonochromeTheme.colors
    val typography = MonochromeTheme.typography

    MonochromeCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = if (isSelected) colors.interactiveSurface else colors.surface,
        borderColor = if (isSelected) colors.primaryActionBg else colors.subtleBorder,
        padding = 14.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) colors.primaryActionBg else colors.interactiveSurface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null,
                        tint = if (isSelected) colors.primaryActionFg else colors.primaryText,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Text(
                        text = location.name,
                        style = typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.primaryText
                    )
                    Text(
                        text = location.coordinatesFormatted,
                        style = typography.caption,
                        color = colors.secondaryText
                    )
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Waypoint",
                    tint = colors.mutedText,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun CoordinatesInputPanel(
    xInput: String,
    yInput: String,
    yawInput: String,
    onXInputChanged: (String) -> Unit,
    onYInputChanged: (String) -> Unit,
    onYawInputChanged: (String) -> Unit
) {
    val colors = MonochromeTheme.colors
    val typography = MonochromeTheme.typography

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("TARGET POSE COORDINATES", style = typography.label, color = colors.secondaryText)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text("X (meters)", style = typography.caption, color = colors.secondaryText)
                Spacer(modifier = Modifier.height(4.dp))
                MonochromeTextField(
                    value = xInput,
                    onValueChange = onXInputChanged,
                    placeholder = "0.0"
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text("Y (meters)", style = typography.caption, color = colors.secondaryText)
                Spacer(modifier = Modifier.height(4.dp))
                MonochromeTextField(
                    value = yInput,
                    onValueChange = onYInputChanged,
                    placeholder = "0.0"
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text("Yaw (rad)", style = typography.caption, color = colors.secondaryText)
                Spacer(modifier = Modifier.height(4.dp))
                MonochromeTextField(
                    value = yawInput,
                    onValueChange = onYawInputChanged,
                    placeholder = "0.0"
                )
            }
        }
    }
}

@Composable
fun ChargingDockCard(
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = MonochromeTheme.colors
    val typography = MonochromeTheme.typography

    MonochromeCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = if (isSelected) colors.interactiveSurface else colors.surface,
        borderColor = if (isSelected) colors.primaryActionBg else colors.subtleBorder,
        padding = 14.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) colors.primaryActionBg else colors.interactiveSurface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = null,
                        tint = if (isSelected) colors.primaryActionFg else colors.primaryText,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Text(
                        text = stringResource(R.string.loc_charging_dock),
                        style = typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.primaryText
                    )
                    Text(
                        text = stringResource(R.string.charging_dock_subtitle),
                        style = typography.caption,
                        color = colors.secondaryText
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = colors.secondaryText
            )
        }
    }
}
