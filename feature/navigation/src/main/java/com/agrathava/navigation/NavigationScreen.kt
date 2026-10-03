package com.agrathava.navigation

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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agrathava.home.Sidebar
import com.agrathava.home.SidebarNavItem
import com.agrathava.theme.Black900
import com.agrathava.theme.MonochromeButton
import com.agrathava.theme.MonochromeButtonSize
import com.agrathava.theme.MonochromeButtonVariant
import com.agrathava.theme.MonochromeTextField
import com.agrathava.theme.MonochromeTheme
import com.agrathava.theme.White100

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
                .padding(start = 28.dp, top = 24.dp, end = 28.dp, bottom = 24.dp)
        ) {
            NavigationHeader()

            Spacer(modifier = Modifier.height(16.dp))

            // Navigation Execution Alert Banner
            NavigationExecutionBanner(
                executionStatus = uiState.executionStatus,
                onCancelClick = onCancelNavigationClick
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Main Content: Map (Left) + Right Control Panel
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
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
            Surface(
                modifier = modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFEFF6FF),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF93C5FD))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "NAVIGATING TO DESTINATION",
                            style = MonochromeTheme.typography.body.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF1E40AF)
                        )
                        Text(
                            text = "ETA: ${executionStatus.etaSeconds}s • Distance: ${executionStatus.distanceMeters}m",
                            style = MonochromeTheme.typography.caption,
                            color = Color(0xFF1D4ED8)
                        )
                    }
                    Button(
                        onClick = onCancelClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                    ) {
                        Text("Cancel Navigation", color = White100)
                    }
                }
            }
        }

        is NavigationExecutionStatus.ObstacleDetected -> {
            Surface(
                modifier = modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFFFFBEB),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCD34D))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFD97706))
                    Text(executionStatus.message, style = MonochromeTheme.typography.body.copy(fontWeight = FontWeight.Bold), color = Color(0xFFB45309))
                }
            }
        }

        is NavigationExecutionStatus.Arrived -> {
            Surface(
                modifier = modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFECFDF5),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF6EE7B7))
            ) {
                Text(
                    text = "Arrived at ${executionStatus.destinationName}",
                    modifier = Modifier.padding(16.dp),
                    style = MonochromeTheme.typography.body.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF047857)
                )
            }
        }

        is NavigationExecutionStatus.Failed -> {
            Surface(
                modifier = modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFFEF2F2),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5))
            ) {
                Text(
                    text = "Navigation Error: ${executionStatus.errorMessage}",
                    modifier = Modifier.padding(16.dp),
                    style = MonochromeTheme.typography.body.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFFDC2626)
                )
            }
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
            style = typography.h2.copy(
                fontSize = 28.sp,
                lineHeight = 34.sp
            ),
            color = colors.primaryText,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.nav_subtitle),
            style = typography.bodySmall.copy(
                fontSize = 14.sp,
                lineHeight = 20.sp
            ),
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

            val wallColor = Color(0xFFD3D3D3)
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
                    color = Color.Black,
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
                .background(Black900),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.SmartToy,
                contentDescription = "Robot Position",
                tint = White100,
                modifier = Modifier.size(24.dp)
            )
        }

        if (hasDestination) {
            Box(
                modifier = Modifier
                    .offset(x = destXDp - 20.dp, y = destYDp - 20.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(White100)
                    .border(2.dp, Black900, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = "Destination Target",
                    tint = Black900,
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
                DestinationTabs(
                    selectedTab = uiState.selectedTab,
                    onTabSelected = onTabSelected
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
fun DestinationTabs(
    selectedTab: NavigationTab,
    onTabSelected: (NavigationTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MonochromeTheme.colors

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.interactiveSurface)
            .padding(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            val isSavedSelected = selectedTab == NavigationTab.SavedLocations
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSavedSelected) Black900 else Color.Transparent)
                    .clickable { onTabSelected(NavigationTab.SavedLocations) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.tab_saved_locations),
                    style = MonochromeTheme.typography.label.copy(
                        fontSize = 13.sp,
                        fontWeight = if (isSavedSelected) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (isSavedSelected) White100 else colors.primaryText
                )
            }

            val isCoordsSelected = selectedTab == NavigationTab.Coordinates
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isCoordsSelected) Black900 else Color.Transparent)
                    .clickable { onTabSelected(NavigationTab.Coordinates) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.tab_coordinates),
                    style = MonochromeTheme.typography.label.copy(
                        fontSize = 13.sp,
                        fontWeight = if (isCoordsSelected) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (isCoordsSelected) White100 else colors.primaryText
                )
            }

            val isMapPickerSelected = selectedTab == NavigationTab.MapPicker
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isMapPickerSelected) Black900 else Color.Transparent)
                    .clickable { onTabSelected(NavigationTab.MapPicker) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Map Picker",
                    style = MonochromeTheme.typography.label.copy(
                        fontSize = 13.sp,
                        fontWeight = if (isMapPickerSelected) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (isMapPickerSelected) White100 else colors.primaryText
                )
            }
        }
    }
}

@Composable
fun MapPickerHelpView() {
    val colors = MonochromeTheme.colors
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = colors.interactiveSurface,
        modifier = Modifier.fillMaxWidth().padding(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Tap Anywhere On Map", style = MonochromeTheme.typography.body.copy(fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(4.dp))
            Text("Tap directly on the floorplan map on the left to set custom navigation waypoints.", style = MonochromeTheme.typography.caption)
        }
    }
}

@Composable
fun SavedLocationsList(
    locations: List<SavedLocationUi>,
    selectedDestination: NavigationDestinationUi,
    onLocationSelected: (SavedLocationUi) -> Unit,
    onOpenAddLocationDialog: () -> Unit
) {
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
                style = MonochromeTheme.typography.label.copy(fontSize = 11.sp),
                color = MonochromeTheme.colors.secondaryText
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

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) colors.interactiveSurface else colors.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) Black900 else colors.subtleBorder
        )
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
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
                        .background(if (isSelected) Black900 else colors.interactiveSurface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null,
                        tint = if (isSelected) White100 else colors.primaryText,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column {
                    Text(
                        text = location.name,
                        style = MonochromeTheme.typography.body.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        ),
                        color = colors.primaryText
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Coordinates: ${location.coordinatesFormatted}",
                        style = MonochromeTheme.typography.caption.copy(fontSize = 12.sp),
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

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.interactiveSurface),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Place,
                contentDescription = null,
                tint = colors.mutedText,
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = "No saved locations yet",
                style = MonochromeTheme.typography.bodySmall,
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

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) colors.interactiveSurface else colors.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) Black900 else colors.subtleBorder
        )
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
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
                        .background(if (isSelected) Black900 else colors.interactiveSurface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = null,
                        tint = if (isSelected) White100 else colors.primaryText,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column {
                    Text(
                        text = "Primary Charging Dock",
                        style = MonochromeTheme.typography.body.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        ),
                        color = colors.primaryText
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Available • Standard Dock",
                        style = MonochromeTheme.typography.caption.copy(fontSize = 12.sp),
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

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MonochromeTheme.colors.surface,
            modifier = Modifier.padding(16.dp).fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Add Saved Location",
                    style = MonochromeTheme.typography.h3,
                    color = MonochromeTheme.colors.primaryText
                )

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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    MonochromeButton(
                        text = "Cancel",
                        variant = MonochromeButtonVariant.Ghost,
                        onClick = onDismiss
                    )
                    Spacer(modifier = Modifier.width(8.dp))
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
                }
            }
        }
    }
}

@Preview(name = "Navigation Screen Preview", showBackground = true, widthDp = 1280, heightDp = 800)
@Composable
fun NavigationScreenPreview() {
    MonochromeTheme(darkTheme = false) {
        NavigationScreenContent()
    }
}
