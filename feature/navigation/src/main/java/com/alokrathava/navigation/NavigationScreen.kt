package com.alokrathava.navigation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alokrathava.home.Sidebar
import com.alokrathava.home.SidebarNavItem
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
            onXInputChanged = viewModel::updateXInput,
            onYInputChanged = viewModel::updateYInput,
            onYawInputChanged = viewModel::updateYawInput,
            onStartNavigationClick = viewModel::startNavigation,
            onCancelNavigationClick = viewModel::cancelNavigation,
            onOpenAddLocationDialog = viewModel::openAddLocationDialog,
            onSaveNewLocation = viewModel::saveNewLocation,
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
    onXInputChanged: (String) -> Unit = {},
    onYInputChanged: (String) -> Unit = {},
    onYawInputChanged: (String) -> Unit = {},
    onStartNavigationClick: () -> Unit = {},
    onCancelNavigationClick: () -> Unit = {},
    onOpenAddLocationDialog: (Boolean) -> Unit = {},
    onSaveNewLocation: (String, Double, Double) -> Unit = { _, _, _ -> },
    onSidebarItemSelected: (SidebarNavItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = MonochromeTheme.colors
    val spacing = MonochromeTheme.spacing

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Sidebar(
            selectedItem = SidebarNavItem.NAVIGATION,
            onItemSelected = onSidebarItemSelected,
            isConnected = true,
            connectionAddress = "192.168.1.108:8080"
        )

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
            NavigationHeader()

            Spacer(modifier = Modifier.height(spacing.space4))

            // Navigation Execution Alert Banner
            NavigationExecutionBanner(
                executionStatus = uiState.executionStatus,
                onCancelClick = onCancelNavigationClick
            )

            Spacer(modifier = Modifier.height(spacing.space4))

            // Main Content: Map (Left) + Right Control Panel
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(spacing.space6)
            ) {
                NavigationMapPanel(
                    modifier = Modifier
                        .weight(1.4f)
                        .fillMaxHeight(),
                    selectedDestination = uiState.selectedDestination
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
                    onOpenAddLocationDialog = { onOpenAddLocationDialog(true) }
                )
            }
        }
    }

    if (uiState.isAddLocationDialogOpen) {
        AddLocationDialog(
            onDismiss = { onOpenAddLocationDialog(false) },
            onSave = onSaveNewLocation
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
            MonochromeBanner(
                title = "NAVIGATING TO DESTINATION",
                description = "ETA: ${executionStatus.etaSeconds}s • Distance: ${executionStatus.distanceMeters}m",
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
    modifier: Modifier = Modifier,
    selectedDestination: NavigationDestinationUi = NavigationDestinationUi.None
) {
    val colors = MonochromeTheme.colors
    val shapes = MonochromeTheme.shapes

    Box(
        modifier = modifier
            .clip(shapes.largeCards)
            .background(colors.surface)
            .border(1.dp, colors.subtleBorder, shapes.largeCards)
    ) {
        val robotXDp = 155.dp
        val robotYDp = 235.dp

        val destXDp = 295.dp
        val destYDp = 120.dp

        val hasDestination = selectedDestination != NavigationDestinationUi.None

        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            val wallColor = colors.defaultBorder
            val strokeW = 1.8f

            drawRect(
                color = wallColor,
                topLeft = Offset(width * 0.08f, height * 0.08f),
                size = Size(width * 0.84f, height * 0.84f),
                style = Stroke(width = 2.5f)
            )

            val hWalls = listOf(0.22f, 0.38f, 0.54f, 0.70f, 0.84f)
            hWalls.forEach { yRatio ->
                drawLine(
                    color = wallColor,
                    start = Offset(width * 0.08f, height * yRatio),
                    end = Offset(width * 0.92f, height * yRatio),
                    strokeWidth = strokeW
                )
            }

            val vWalls = listOf(0.28f, 0.48f, 0.68f, 0.88f)
            vWalls.forEach { xRatio ->
                drawLine(
                    color = wallColor,
                    start = Offset(width * xRatio, height * 0.08f),
                    end = Offset(width * xRatio, height * 0.92f),
                    strokeWidth = strokeW
                )
            }

            if (hasDestination) {
                val path = Path().apply {
                    moveTo(robotXDp.toPx(), robotYDp.toPx())
                    lineTo(robotXDp.toPx(), 180.dp.toPx())
                    lineTo(220.dp.toPx(), 180.dp.toPx())
                    lineTo(220.dp.toPx(), destYDp.toPx())
                    lineTo(destXDp.toPx(), destYDp.toPx())
                }

                drawPath(
                    path = path,
                    color = colors.primaryText,
                    style = Stroke(
                        width = 3.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
                    )
                )
            }
        }

        Box(
            modifier = Modifier
                .offset(x = robotXDp - 24.dp, y = robotYDp - 24.dp)
                .size(48.dp)
                .clip(CircleShape)
                .background(colors.primaryActionBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.SmartToy,
                contentDescription = "Robot Position",
                tint = colors.primaryActionFg,
                modifier = Modifier.size(24.dp)
            )
        }

        if (hasDestination) {
            Box(
                modifier = Modifier
                    .offset(x = destXDp - 20.dp, y = destYDp - 20.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colors.surface)
                    .border(2.dp, colors.primaryActionBg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = "Destination Target",
                    tint = colors.primaryActionBg,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        MapControlsOverlay(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
        )
    }
}

@Composable
fun MapControlsOverlay(
    modifier: Modifier = Modifier
) {
    val colors = MonochromeTheme.colors

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = colors.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.subtleBorder)
        ) {
            Column(modifier = Modifier.padding(2.dp)) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clickable { },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Zoom In", tint = colors.primaryText)
                }

                HorizontalDivider(modifier = Modifier.width(36.dp), color = colors.subtleBorder)

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clickable { },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Zoom Out", tint = colors.primaryText)
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = colors.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.subtleBorder),
            modifier = Modifier.clickable { }
        ) {
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "Center Map", tint = colors.primaryText)
            }
        }
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
    onOpenAddLocationDialog: () -> Unit
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
                        if (uiState.savedLocations.isEmpty()) {
                            EmptySavedLocationsView()
                        } else {
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
                                onOpenAddLocationDialog = onOpenAddLocationDialog
                            )
                        }
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
        Text("Tap directly on the floorplan map on the left to set custom navigation waypoints.", style = typography.caption)
    }
}

@Composable
fun SavedLocationsList(
    locations: List<SavedLocationUi>,
    selectedDestination: NavigationDestinationUi,
    onLocationSelected: (SavedLocationUi) -> Unit,
    onOpenAddLocationDialog: () -> Unit
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

        locations.forEach { location ->
            val isSelected = selectedDestination is NavigationDestinationUi.SavedLocation && selectedDestination.id == location.id
            SavedLocationCard(
                location = location,
                isSelected = isSelected,
                onClick = { onLocationSelected(location) }
            )
        }
    }
}

@Composable
fun SavedLocationCard(
    location: SavedLocationUi,
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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column {
                    Text(
                        text = location.name,
                        style = typography.body,
                        fontWeight = FontWeight.Bold,
                        color = colors.primaryText
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Coordinates: ${location.coordinatesFormatted}",
                        style = typography.caption,
                        color = colors.secondaryText
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = colors.mutedText,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun EmptySavedLocationsView() {
    val colors = MonochromeTheme.colors
    val typography = MonochromeTheme.typography

    MonochromeCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        backgroundColor = colors.interactiveSurface,
        borderColor = colors.defaultBorder
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Place,
                contentDescription = null,
                tint = colors.mutedText,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "No saved locations yet",
                style = typography.bodySmall,
                color = colors.secondaryText
            )
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
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                MonochromeTextField(
                    value = xInput,
                    onValueChange = onXInputChanged,
                    label = "X Position (m)",
                    placeholder = "0.0"
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                MonochromeTextField(
                    value = yInput,
                    onValueChange = onYInputChanged,
                    label = "Y Position (m)",
                    placeholder = "0.0"
                )
            }
        }

        MonochromeTextField(
            value = yawInput,
            onValueChange = onYawInputChanged,
            label = "Yaw Heading (°)",
            placeholder = "0.0°"
        )
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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column {
                    Text(
                        text = "Primary Charging Dock",
                        style = typography.body,
                        fontWeight = FontWeight.Bold,
                        color = colors.primaryText
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Available • Standard Dock",
                        style = typography.caption,
                        color = colors.secondaryText
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = colors.mutedText,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun AddLocationDialog(
    onDismiss: () -> Unit,
    onSave: (String, Double, Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var xStr by remember { mutableStateOf("") }
    var yStr by remember { mutableStateOf("") }

    MonochromeDialog(
        onDismissRequest = onDismiss,
        title = "Add Saved Location",
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MonochromeTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Location Name",
                    placeholder = "e.g. Conference Room"
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        MonochromeTextField(
                            value = xStr,
                            onValueChange = { xStr = it },
                            label = "X Coordinate",
                            placeholder = "2.5"
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        MonochromeTextField(
                            value = yStr,
                            onValueChange = { yStr = it },
                            label = "Y Coordinate",
                            placeholder = "4.0"
                        )
                    }
                }
            }
        },
        confirmButton = {
            MonochromeButton(
                text = "Save",
                variant = MonochromeButtonVariant.Primary,
                enabled = name.isNotBlank(),
                onClick = {
                    val x = xStr.toDoubleOrNull() ?: 0.0
                    val y = yStr.toDoubleOrNull() ?: 0.0
                    onSave(name, x, y)
                }
            )
        },
        dismissButton = {
            MonochromeButton(
                text = "Cancel",
                variant = MonochromeButtonVariant.Ghost,
                onClick = onDismiss
            )
        }
    )
}

@Preview(name = "Navigation Screen Preview", showBackground = true, widthDp = 1280, heightDp = 800)
@Composable
fun NavigationScreenPreview() {
    MonochromeTheme(darkTheme = false) {
        NavigationScreenContent()
    }
}
