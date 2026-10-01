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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.agrathava.home.Sidebar
import com.agrathava.home.SidebarNavItem
import com.agrathava.theme.Black900
import com.agrathava.theme.MonochromeButton
import com.agrathava.theme.MonochromeButtonSize
import com.agrathava.theme.MonochromeButtonVariant
import com.agrathava.theme.MonochromeTextField
import com.agrathava.theme.MonochromeTheme
import com.agrathava.theme.White100

sealed interface NavigationDestinationUi {
    data object None : NavigationDestinationUi

    data class SavedLocation(
        val id: String,
        val name: String,
        val x: Double,
        val y: Double,
    ) : NavigationDestinationUi

    data class Coordinates(
        val x: String,
        val y: String,
        val yaw: String
    ) : NavigationDestinationUi

    data object ChargingDock : NavigationDestinationUi
}

enum class NavigationTab {
    SavedLocations,
    Coordinates
}

data class SavedLocationUi(
    val id: String,
    val name: String,
    val x: Double,
    val y: Double
) {
    val coordinatesFormatted: String
        get() = "($x, $y)"
}

val sampleLocations = listOf(
    SavedLocationUi(id = "1", name = "Lobby", x = 1.2, y = 3.4),
    SavedLocationUi(id = "2", name = "Dining Area", x = 5.6, y = 2.1),
    SavedLocationUi(id = "3", name = "Activity Room", x = 3.8, y = 6.5),
    SavedLocationUi(id = "4", name = "Reception", x = 7.1, y = 4.2)
)

data class NavigationUiState(
    val selectedTab: NavigationTab = NavigationTab.SavedLocations,
    val savedLocations: List<SavedLocationUi> = sampleLocations,
    val selectedDestination: NavigationDestinationUi = NavigationDestinationUi.None,
    val xInput: String = "",
    val yInput: String = "",
    val yawInput: String = ""
)

@Composable
fun NavigationScreen(
    modifier: Modifier = Modifier,
    uiState: NavigationUiState = NavigationUiState(),
    onTabSelected: (NavigationTab) -> Unit = {},
    onDestinationSelected: (NavigationDestinationUi) -> Unit = {},
    onXInputChanged: (String) -> Unit = {},
    onYInputChanged: (String) -> Unit = {},
    onYawInputChanged: (String) -> Unit = {},
    onStartNavigationClick: () -> Unit = {},
    onSidebarItemSelected: (SidebarNavItem) -> Unit = {}
) {
    val colors = MonochromeTheme.colors

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // Left Sidebar (reused exact design from Home screen)
        Sidebar(
            selectedItem = SidebarNavItem.NAVIGATION,
            onItemSelected = onSidebarItemSelected,
            isConnected = true,
            connectionAddress = "192.168.1.108:8080"
        )

        // Main Navigation Area
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(start = 28.dp, top = 24.dp, end = 28.dp, bottom = 24.dp)
        ) {
            // Header
            NavigationHeader()

            Spacer(modifier = Modifier.height(20.dp))

            // Main Content: Map (Left) + Right Control Panel
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Map Panel (~60% width)
                NavigationMapPanel(
                    modifier = Modifier
                        .weight(1.4f)
                        .fillMaxHeight(),
                    selectedDestination = uiState.selectedDestination
                )

                // Right Panel (~40% width)
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
                    onStartNavigationClick = onStartNavigationClick
                )
            }
        }
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
        // Markers DP coordinates inside map Box
        val robotXDp = 155.dp
        val robotYDp = 235.dp

        val destXDp = 295.dp
        val destYDp = 120.dp

        val hasDestination = selectedDestination != NavigationDestinationUi.None

        // Floorplan Canvas Background & Dashed Route Path
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // 1. Light grayscale floorplan wall lines
            val wallColor = Color(0xFFD3D3D3)
            val strokeW = 1.8f

            // Outer perimeter walls
            drawRect(
                color = wallColor,
                topLeft = Offset(width * 0.08f, height * 0.08f),
                size = Size(width * 0.84f, height * 0.84f),
                style = Stroke(width = 2.5f)
            )

            // Horizontal floorplan wall segments
            val hWalls = listOf(0.22f, 0.38f, 0.54f, 0.70f, 0.84f)
            hWalls.forEach { yRatio ->
                drawLine(
                    color = wallColor,
                    start = Offset(width * 0.08f, height * yRatio),
                    end = Offset(width * 0.92f, height * yRatio),
                    strokeWidth = strokeW
                )
            }

            // Vertical floorplan wall segments
            val vWalls = listOf(0.22f, 0.36f, 0.50f, 0.64f, 0.78f)
            vWalls.forEach { xRatio ->
                drawLine(
                    color = wallColor,
                    start = Offset(width * xRatio, height * 0.08f),
                    end = Offset(width * xRatio, height * 0.92f),
                    strokeWidth = strokeW
                )
            }

            // Room blocks and doorways
            drawRect(
                color = Color(0xFFF0F0F0),
                topLeft = Offset(width * 0.36f, height * 0.22f),
                size = Size(width * 0.28f, height * 0.32f)
            )
            drawRect(
                color = wallColor,
                topLeft = Offset(width * 0.36f, height * 0.22f),
                size = Size(width * 0.28f, height * 0.32f),
                style = Stroke(width = strokeW)
            )

            // 2. Black dashed route line from Robot center to Destination Pin center
            if (hasDestination) {
                val robotPxX = robotXDp.toPx() + 27.dp.toPx() // center of 54.dp marker
                val robotPxY = robotYDp.toPx() + 27.dp.toPx()

                val destPxX = destXDp.toPx() + 20.dp.toPx() // center of pin
                val destPxY = destYDp.toPx() + 32.dp.toPx() // bottom tip/center of pin

                val turnY = robotPxY - 90.dp.toPx()

                val routePath = Path().apply {
                    moveTo(robotPxX, robotPxY)
                    // Go vertically up
                    lineTo(robotPxX, turnY + 16f)
                    // Rounded corner turning right
                    quadraticTo(robotPxX, turnY, robotPxX + 16f, turnY)
                    // Go horizontally right to destination X
                    lineTo(destPxX - 16f, turnY)
                    // Rounded corner turning up to destination pin
                    quadraticTo(destPxX, turnY, destPxX, turnY - 16f)
                    // Go vertically up to destination
                    lineTo(destPxX, destPxY)
                }

                drawPath(
                    path = routePath,
                    color = Color.Black,
                    style = Stroke(
                        width = 3.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
                    )
                )
            }
        }

        // Robot Position Marker
        Box(
            modifier = Modifier
                .offset(x = robotXDp, y = robotYDp)
        ) {
            RobotMarker()
        }

        // Destination Pin Marker
        if (hasDestination) {
            Box(
                modifier = Modifier
                    .offset(x = destXDp, y = destYDp)
            ) {
                DestinationMarker()
            }
        }

        // Map Controls (+, −, Target) in Bottom-Left
        MapControls(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp)
        )
    }
}

@Composable
fun RobotMarker(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(54.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer black circular outline
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .border(2.5.dp, Black900, CircleShape)
        )

        // Inner dark circular badge with robot icon
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Black900),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.SmartToy,
                contentDescription = "Robot Location",
                tint = White100,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun DestinationMarker(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(44.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Place,
            contentDescription = "Destination Pin",
            tint = Black900,
            modifier = Modifier.size(42.dp)
        )
    }
}

@Composable
fun MapControls(
    modifier: Modifier = Modifier,
    onZoomInClick: () -> Unit = {},
    onZoomOutClick: () -> Unit = {},
    onRecenterClick: () -> Unit = {}
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MapControlButton(
            onClick = onZoomInClick,
            icon = {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Zoom In",
                    tint = Black900,
                    modifier = Modifier.size(22.dp)
                )
            }
        )

        MapControlButton(
            onClick = onZoomOutClick,
            icon = {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Zoom Out",
                    tint = Black900,
                    modifier = Modifier.size(22.dp)
                )
            }
        )

        MapControlButton(
            onClick = onRecenterClick,
            icon = {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "Recenter",
                    tint = Black900,
                    modifier = Modifier.size(20.dp)
                )
            }
        )
    }
}

@Composable
private fun MapControlButton(
    onClick: () -> Unit,
    icon: @Composable () -> Unit
) {
    val colors = MonochromeTheme.colors

    Surface(
        modifier = Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, colors.defaultBorder, RoundedCornerShape(10.dp))
            .clickable { onClick() },
        color = White100,
        shape = RoundedCornerShape(10.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
    }
}

@Composable
fun NavigationDestinationPanel(
    modifier: Modifier = Modifier,
    uiState: NavigationUiState,
    onTabSelected: (NavigationTab) -> Unit = {},
    onDestinationSelected: (NavigationDestinationUi) -> Unit = {},
    onXInputChanged: (String) -> Unit = {},
    onYInputChanged: (String) -> Unit = {},
    onYawInputChanged: (String) -> Unit = {},
    onStartNavigationClick: () -> Unit = {}
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f, fill = false)
        ) {
            // Segmented Control Tabs (Saved Locations / Coordinates)
            DestinationTabs(
                selectedTab = uiState.selectedTab,
                onTabSelected = onTabSelected
            )

            // Content Area based on Tab
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
                            }
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
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Bottom section: Charging Dock Shortcut + Primary Start Navigation Button
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Charging Dock System Destination Action
            ChargingDockCard(
                isSelected = uiState.selectedDestination == NavigationDestinationUi.ChargingDock,
                onClick = {
                    onDestinationSelected(NavigationDestinationUi.ChargingDock)
                }
            )

            // Primary Start Navigation Action Button
            StartNavigationButton(
                enabled = uiState.selectedDestination != NavigationDestinationUi.None,
                onClick = onStartNavigationClick
            )
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
            // Saved Locations Tab
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

            // Coordinates Tab
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
        }
    }
}

@Composable
fun SavedLocationsList(
    locations: List<SavedLocationUi>,
    selectedDestination: NavigationDestinationUi,
    onLocationSelected: (SavedLocationUi) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MonochromeTheme.colors
    val shapes = MonochromeTheme.shapes

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(shapes.cards)
            .border(1.dp, colors.defaultBorder, shapes.cards),
        color = White100,
        shape = shapes.cards
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            locations.forEachIndexed { index, location ->
                val isSelected = (selectedDestination is NavigationDestinationUi.SavedLocation) &&
                        (selectedDestination.id == location.id)

                SavedLocationRow(
                    location = location,
                    isSelected = isSelected,
                    onClick = { onLocationSelected(location) }
                )

                if (index < locations.size - 1) {
                    HorizontalDivider(
                        color = colors.subtleBorder,
                        thickness = 1.dp
                    )
                }
            }
        }
    }
}

@Composable
fun SavedLocationRow(
    location: SavedLocationUi,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val colors = MonochromeTheme.colors
    val typography = MonochromeTheme.typography

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(if (isSelected) colors.interactiveSurface else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Location Pin Icon
            Icon(
                imageVector = Icons.Default.Place,
                contentDescription = null,
                tint = colors.primaryText,
                modifier = Modifier.size(24.dp)
            )

            // Location Name and Coordinates
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = location.name,
                    style = typography.body.copy(
                        fontSize = 15.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                    ),
                    color = colors.primaryText
                )
                Text(
                    text = location.coordinatesFormatted,
                    style = typography.monoCaption.copy(
                        fontSize = 12.sp
                    ),
                    color = colors.mutedText
                )
            }
        }

        // More Options Menu Icon
        Icon(
            imageVector = Icons.Default.MoreVert,
            contentDescription = "More options",
            tint = colors.primaryText,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun EmptySavedLocationsView(
    modifier: Modifier = Modifier
) {
    val colors = MonochromeTheme.colors
    val shapes = MonochromeTheme.shapes
    val typography = MonochromeTheme.typography

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(shapes.cards)
            .border(1.dp, colors.defaultBorder, shapes.cards),
        color = White100,
        shape = shapes.cards
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.empty_locations_title),
                style = typography.body.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = colors.primaryText
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.empty_locations_subtitle),
                style = typography.bodySmall.copy(
                    fontSize = 13.sp
                ),
                color = colors.mutedText
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
    onYawInputChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MonochromeTheme.colors
    val shapes = MonochromeTheme.shapes

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(shapes.cards)
            .border(1.dp, colors.defaultBorder, shapes.cards),
        color = White100,
        shape = shapes.cards
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MonochromeTextField(
                value = xInput,
                onValueChange = onXInputChanged,
                label = stringResource(R.string.label_x_coordinate),
                placeholder = stringResource(R.string.placeholder_x),
                modifier = Modifier.fillMaxWidth()
            )

            MonochromeTextField(
                value = yInput,
                onValueChange = onYInputChanged,
                label = stringResource(R.string.label_y_coordinate),
                placeholder = stringResource(R.string.placeholder_y),
                modifier = Modifier.fillMaxWidth()
            )

            MonochromeTextField(
                value = yawInput,
                onValueChange = onYawInputChanged,
                label = stringResource(R.string.label_yaw),
                placeholder = stringResource(R.string.placeholder_yaw),
                trailingIcon = {
                    Text(
                        text = "°",
                        style = MonochromeTheme.typography.bodySmall,
                        color = colors.secondaryText
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun ChargingDockCard(
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MonochromeTheme.colors
    val shapes = MonochromeTheme.shapes
    val typography = MonochromeTheme.typography

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(shapes.cards)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) colors.strongBorder else colors.defaultBorder,
                shape = shapes.cards
            )
            .clickable { onClick() },
        color = if (isSelected) colors.interactiveSurface else White100,
        shape = shapes.cards
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Home/Dock Monochrome Icon
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = null,
                    tint = colors.primaryText,
                    modifier = Modifier.size(24.dp)
                )

                // Location Title and Subtitle
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = stringResource(R.string.loc_charging_dock),
                        style = typography.body.copy(
                            fontSize = 15.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                        ),
                        color = colors.primaryText
                    )
                    Text(
                        text = stringResource(R.string.charging_dock_subtitle),
                        style = typography.monoCaption.copy(
                            fontSize = 12.sp
                        ),
                        color = colors.mutedText
                    )
                }
            }

            // Trailing Chevron Indicator
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
fun StartNavigationButton(
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    MonochromeButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        variant = MonochromeButtonVariant.Primary,
        size = MonochromeButtonSize.Large,
        enabled = enabled,
        icon = Icons.AutoMirrored.Filled.Send,
        text = stringResource(R.string.btn_start_navigation)
    )
}

// ====================================================================
// Compose Previews
// ====================================================================

@Preview(name = "State 1 — Saved Locations / Nothing Selected", widthDp = 1080, heightDp = 680, showBackground = true)
@Composable
fun NavigationScreen_Default_Preview() {
    MonochromeTheme(darkTheme = false) {
        NavigationScreen(
            uiState = NavigationUiState(
                selectedTab = NavigationTab.SavedLocations,
                selectedDestination = NavigationDestinationUi.None
            )
        )
    }
}

@Preview(name = "State 2 — Saved Location Selected", widthDp = 1080, heightDp = 680, showBackground = true)
@Composable
fun NavigationScreen_LocationSelected_Preview() {
    val selectedLoc = sampleLocations[2] // Activity Room
    MonochromeTheme(darkTheme = false) {
        NavigationScreen(
            uiState = NavigationUiState(
                selectedTab = NavigationTab.SavedLocations,
                selectedDestination = NavigationDestinationUi.SavedLocation(
                    id = selectedLoc.id,
                    name = selectedLoc.name,
                    x = selectedLoc.x,
                    y = selectedLoc.y
                )
            )
        )
    }
}

@Preview(name = "State 4 — Coordinates Tab / Empty", widthDp = 1080, heightDp = 680, showBackground = true)
@Composable
fun NavigationScreen_CoordinatesEmpty_Preview() {
    MonochromeTheme(darkTheme = false) {
        NavigationScreen(
            uiState = NavigationUiState(
                selectedTab = NavigationTab.Coordinates,
                selectedDestination = NavigationDestinationUi.None
            )
        )
    }
}

@Preview(name = "State 5 — Coordinates Entered", widthDp = 1080, heightDp = 680, showBackground = true)
@Composable
fun NavigationScreen_CoordinatesEntered_Preview() {
    MonochromeTheme(darkTheme = false) {
        NavigationScreen(
            uiState = NavigationUiState(
                selectedTab = NavigationTab.Coordinates,
                xInput = "2.4",
                yInput = "5.1",
                yawInput = "180",
                selectedDestination = NavigationDestinationUi.Coordinates("2.4", "5.1", "180")
            )
        )
    }
}

@Preview(name = "State 6 — Charging Dock Selected", widthDp = 1080, heightDp = 680, showBackground = true)
@Composable
fun NavigationScreen_ChargingDockSelected_Preview() {
    MonochromeTheme(darkTheme = false) {
        NavigationScreen(
            uiState = NavigationUiState(
                selectedTab = NavigationTab.SavedLocations,
                selectedDestination = NavigationDestinationUi.ChargingDock
            )
        )
    }
}

@Preview(name = "State 7 — Saved Locations Empty", widthDp = 1080, heightDp = 680, showBackground = true)
@Composable
fun NavigationScreen_EmptySavedLocations_Preview() {
    MonochromeTheme(darkTheme = false) {
        NavigationScreen(
            uiState = NavigationUiState(
                selectedTab = NavigationTab.SavedLocations,
                savedLocations = emptyList(),
                selectedDestination = NavigationDestinationUi.None
            )
        )
    }
}
