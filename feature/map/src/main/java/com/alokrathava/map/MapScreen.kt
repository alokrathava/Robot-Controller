package com.alokrathava.map

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alokrathava.home.AdaptiveNavigationLayout
import com.alokrathava.home.SidebarNavItem
import com.alokrathava.theme.*

@Composable
fun MapScreen(
    modifier: Modifier = Modifier,
    viewModel: MapViewModel = hiltViewModel(),
    selectedNav: SidebarNavItem = SidebarNavItem.MAPS,
    onSidebarItemSelected: (SidebarNavItem) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    MapScreenContent(
        uiState = uiState,
        selectedNav = selectedNav,
        onSidebarItemSelected = onSidebarItemSelected,
        onSelectMap = viewModel::selectMap,
        onSetActiveMap = viewModel::setMapActive,
        onStartMapping = viewModel::startMapping,
        onStopMapping = { viewModel.stopMapping(false) },
        onAddMapClick = viewModel::openAddMapDialog,
        onCloseAddMapDialog = viewModel::closeAddMapDialog,
        onUpdateNewMapNameInput = viewModel::updateNewMapNameInput,
        onAddMap = viewModel::addMap,
        onEditMapClick = viewModel::openEditMapDialog,
        onCloseEditMapDialog = viewModel::closeEditMapDialog,
        onUpdateEditMapNameInput = viewModel::updateEditMapNameInput,
        onSaveMapName = viewModel::saveMapName,
        onDeleteMapClick = viewModel::openDeleteConfirmDialog,
        onCloseDeleteConfirmDialog = viewModel::closeDeleteConfirmDialog,
        onDeleteMap = viewModel::deleteMap,
        onDismissError = viewModel::dismissError,
        modifier = modifier
    )
}

@Composable
fun MapScreenContent(
    uiState: MapUiState,
    modifier: Modifier = Modifier,
    selectedNav: SidebarNavItem = SidebarNavItem.MAPS,
    onSidebarItemSelected: (SidebarNavItem) -> Unit = {},
    onSelectMap: (String) -> Unit = {},
    onSetActiveMap: (String) -> Unit = {},
    onStartMapping: () -> Unit = {},
    onStopMapping: () -> Unit = {},
    onAddMapClick: () -> Unit = {},
    onCloseAddMapDialog: () -> Unit = {},
    onUpdateNewMapNameInput: (String) -> Unit = {},
    onAddMap: () -> Unit = {},
    onEditMapClick: (String) -> Unit = {},
    onCloseEditMapDialog: () -> Unit = {},
    onUpdateEditMapNameInput: (String) -> Unit = {},
    onSaveMapName: () -> Unit = {},
    onDeleteMapClick: (String) -> Unit = {},
    onCloseDeleteConfirmDialog: () -> Unit = {},
    onDeleteMap: () -> Unit = {},
    onDismissError: () -> Unit = {}
) {
    val colors = MonochromeTheme.colors
    val typography = MonochromeTheme.typography
    val spacing = MonochromeTheme.spacing
    val shapes = MonochromeTheme.shapes
    val deviceMode = LocalDeviceMode.current

    val selectedMap = uiState.selectedMap

    AdaptiveNavigationLayout(
        selectedItem = selectedNav,
        onItemSelected = onSidebarItemSelected,
        isConnected = uiState.isConnected,
        connectionAddress = uiState.connectionAddress,
        modifier = modifier
    ) {
        if (deviceMode == DeviceMode.MOBILE) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Header + Action Controls
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Map Management",
                                style = typography.h3,
                                color = colors.primaryText,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = selectedMap?.name ?: "No map selected",
                                style = typography.caption,
                                color = colors.secondaryText
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (uiState.isMappingActive) {
                                MonochromeButton(
                                    onClick = onStopMapping,
                                    text = "Stop Mapping",
                                    icon = Icons.Default.Stop,
                                    variant = MonochromeButtonVariant.Secondary
                                )
                                MonochromeButton(
                                    onClick = onAddMapClick,
                                    text = "Save",
                                    icon = Icons.Default.Save,
                                    variant = MonochromeButtonVariant.Primary
                                )
                            } else {
                                MonochromeButton(
                                    onClick = onStartMapping,
                                    text = "Start Mapping",
                                    icon = Icons.Default.PlayArrow,
                                    variant = MonochromeButtonVariant.Primary
                                )
                            }
                        }
                    }

                    // Disconnected Warning Banner
                    if (!uiState.isConnected) {
                        MonochromeBanner(
                            title = "ROBOT DISCONNECTED",
                            description = "Connect to robot in Home or Robot Status to start mapping or perform map operations",
                            variant = BannerVariant.Warning,
                            icon = Icons.Default.Warning,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Error Message Banner
                    if (uiState.errorMessage != null) {
                        MonochromeBanner(
                            title = "MAP OPERATION FAILED",
                            description = uiState.errorMessage,
                            variant = BannerVariant.Critical,
                            icon = Icons.Default.Warning,
                            modifier = Modifier.fillMaxWidth(),
                            action = {
                                MonochromeButton(
                                    onClick = onDismissError,
                                    text = "Dismiss",
                                    variant = MonochromeButtonVariant.Ghost
                                )
                            }
                        )
                    }

                    // Mapping Status Banner
                    if (uiState.isMappingActive) {
                        Surface(
                            shape = shapes.buttons,
                            color = colors.interactiveSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, colors.focusBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "SLAM Mapping Active — Drive robot to map environment",
                                    style = typography.caption,
                                    color = colors.primaryText,
                                    fontWeight = FontWeight.Bold
                                )
                                MonochromeStatusPill(text = "MAPPING", level = StatusLevel.Active)
                            }
                        }
                    }

                    // Horizontal Map Selector Chips
                    if (uiState.maps.isNotEmpty()) {
                        androidx.compose.foundation.lazy.LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(uiState.maps.size) { index ->
                                val mapItem = uiState.maps[index]
                                val isSelected = (mapItem.id == uiState.selectedMapId)
                                val bg = if (isSelected) colors.interactiveSurface else colors.surface
                                val border = if (isSelected) colors.focusBorder else colors.defaultBorder

                                Surface(
                                    shape = shapes.buttons,
                                    color = bg,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, border),
                                    modifier = Modifier.clickable { onSelectMap(mapItem.id) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = mapItem.name,
                                            style = typography.bodySmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = colors.primaryText
                                        )
                                        if (mapItem.isActive) {
                                            MonochromeStatusPill(text = "ACTIVE", level = StatusLevel.Active)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Middle Occupancy Grid Map Canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 8.dp)
                        .clip(shapes.largeCards)
                        .border(1.dp, colors.defaultBorder, shapes.largeCards)
                ) {
                    MapFloorPlanCanvas(
                        occupancyGrid = uiState.occupancyGrid,
                        isThumbnail = false,
                        robotPosition = uiState.robotPosition,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Bottom Action Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isSelectedActive = selectedMap?.isActive == true

                    MonochromeButton(
                        onClick = { selectedMap?.let { onSetActiveMap(it.id) } },
                        text = if (isSelectedActive) "Active" else "Set Active",
                        enabled = selectedMap != null && !isSelectedActive,
                        variant = if (isSelectedActive) MonochromeButtonVariant.Ghost else MonochromeButtonVariant.Primary,
                        modifier = Modifier.weight(1.2f)
                    )

                    MonochromeButton(
                        onClick = { selectedMap?.let { onEditMapClick(it.id) } },
                        text = "Edit",
                        enabled = selectedMap != null,
                        variant = MonochromeButtonVariant.Secondary,
                        modifier = Modifier.weight(0.9f)
                    )

                    MonochromeButton(
                        onClick = { selectedMap?.let { onDeleteMapClick(it.id) } },
                        text = "Delete",
                        enabled = selectedMap != null,
                        variant = MonochromeButtonVariant.Secondary,
                        modifier = Modifier.weight(0.9f)
                    )
                }
            }
        } else {
            // Tablet / Desktop Layout
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = spacing.space6, top = spacing.cardPadding, end = spacing.space6, bottom = spacing.cardPadding)
            ) {
                // Header Bar: Title & SLAM Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Map Management",
                            style = typography.h2,
                            color = colors.primaryText,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(spacing.space1))
                        Text(
                            text = "View live SLAM occupancy grids, switch or save robot maps",
                            style = typography.bodySmall,
                            color = colors.secondaryText
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.space3)) {
                        if (uiState.isMappingActive) {
                            MonochromeButton(
                                onClick = onStopMapping,
                                text = "Stop Mapping",
                                icon = Icons.Default.Stop,
                                variant = MonochromeButtonVariant.Secondary,
                                size = MonochromeButtonSize.Large
                            )
                            MonochromeButton(
                                onClick = onAddMapClick,
                                text = "Save Map",
                                icon = Icons.Default.Save,
                                variant = MonochromeButtonVariant.Primary,
                                size = MonochromeButtonSize.Large
                            )
                        } else {
                            MonochromeButton(
                                onClick = onStartMapping,
                                text = "Start SLAM Mapping",
                                icon = Icons.Default.PlayArrow,
                                variant = MonochromeButtonVariant.Primary,
                                size = MonochromeButtonSize.Large
                            )
                            MonochromeButton(
                                onClick = onAddMapClick,
                                text = "Add Map",
                                icon = Icons.Default.Add,
                                variant = MonochromeButtonVariant.Secondary,
                                size = MonochromeButtonSize.Large
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(spacing.space4))

                // Disconnected Warning Banner
                if (!uiState.isConnected) {
                    MonochromeBanner(
                        title = "ROBOT DISCONNECTED",
                        description = "Connect to robot in Home or Robot Status to start SLAM mapping or manage maps.",
                        variant = BannerVariant.Warning,
                        icon = Icons.Default.Warning,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(spacing.space3))
                }

                // Error Message Banner
                if (uiState.errorMessage != null) {
                    MonochromeBanner(
                        title = "MAP OPERATION FAILED",
                        description = uiState.errorMessage,
                        variant = BannerVariant.Critical,
                        icon = Icons.Default.Warning,
                        modifier = Modifier.fillMaxWidth(),
                        action = {
                            MonochromeButton(
                                onClick = onDismissError,
                                text = "Dismiss",
                                variant = MonochromeButtonVariant.Ghost
                            )
                        }
                    )
                    Spacer(modifier = Modifier.height(spacing.space3))
                }

                // Split Layout: Left Map List + Right Live Map View Pane
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(spacing.space6)
                ) {
                    // Left Panel: Saved Maps List
                    Column(
                        modifier = Modifier
                            .weight(0.42f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(spacing.space3)
                    ) {
                        if (uiState.maps.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(spacing.space6),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No saved maps on robot. Start SLAM to generate a map.",
                                    style = typography.bodySmall,
                                    color = colors.mutedText
                                )
                            }
                        }

                        uiState.maps.forEach { mapItem ->
                            val isSelected = (mapItem.id == uiState.selectedMapId)
                            var menuExpanded by remember { mutableStateOf(false) }

                            val cardBorderColor = if (isSelected) colors.focusBorder else colors.defaultBorder
                            val cardBg = if (isSelected) colors.elevatedSurface else colors.surface

                            MonochromeCard(
                                modifier = Modifier.fillMaxWidth(),
                                backgroundColor = cardBg,
                                borderColor = cardBorderColor,
                                padding = 14.dp,
                                onClick = { onSelectMap(mapItem.id) }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    MapFloorPlanCanvas(
                                        occupancyGrid = uiState.occupancyGrid,
                                        isThumbnail = true,
                                        modifier = Modifier.size(68.dp)
                                    )

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = mapItem.name,
                                                style = typography.h4,
                                                color = colors.primaryText,
                                                fontWeight = FontWeight.Bold
                                            )

                                            if (mapItem.isActive) {
                                                MonochromeStatusPill(text = "Active", level = StatusLevel.Active)
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            text = "ID: ${mapItem.id}",
                                            style = typography.caption,
                                            color = colors.mutedText
                                        )
                                    }

                                    Box {
                                        IconButton(
                                            onClick = { menuExpanded = true },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.MoreVert,
                                                contentDescription = "Map Options",
                                                tint = colors.secondaryText
                                            )
                                        }

                                        DropdownMenu(
                                            expanded = menuExpanded,
                                            onDismissRequest = { menuExpanded = false },
                                            modifier = Modifier.background(colors.surface)
                                        ) {
                                            if (!mapItem.isActive) {
                                                DropdownMenuItem(
                                                    text = { Text("Set as Active", style = typography.bodySmall) },
                                                    onClick = {
                                                        onSetActiveMap(mapItem.id)
                                                        menuExpanded = false
                                                    }
                                                )
                                            }
                                            DropdownMenuItem(
                                                text = { Text("Edit Name", style = typography.bodySmall) },
                                                onClick = {
                                                    onEditMapClick(mapItem.id)
                                                    menuExpanded = false
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Delete Map", style = typography.bodySmall) },
                                                onClick = {
                                                    onDeleteMapClick(mapItem.id)
                                                    menuExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Right Panel: Live Occupancy Grid View Card + Action Buttons
                    Column(
                        modifier = Modifier
                            .weight(0.58f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clip(shapes.largeCards)
                                .border(1.dp, colors.defaultBorder, shapes.largeCards)
                        ) {
                            MapFloorPlanCanvas(
                                occupancyGrid = uiState.occupancyGrid,
                                isThumbnail = false,
                                robotPosition = uiState.robotPosition,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.height(spacing.space4))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(spacing.space3)
                        ) {
                            val isSelectedActive = selectedMap?.isActive == true

                            MonochromeButton(
                                onClick = { selectedMap?.let { onSetActiveMap(it.id) } },
                                text = if (isSelectedActive) "Active Map" else "Set as Active",
                                enabled = selectedMap != null && !isSelectedActive,
                                variant = if (isSelectedActive) MonochromeButtonVariant.Ghost else MonochromeButtonVariant.Primary,
                                size = MonochromeButtonSize.Large,
                                modifier = Modifier.weight(1.2f)
                            )

                            MonochromeButton(
                                onClick = { selectedMap?.let { onEditMapClick(it.id) } },
                                text = "Edit",
                                enabled = selectedMap != null,
                                variant = MonochromeButtonVariant.Secondary,
                                size = MonochromeButtonSize.Large,
                                modifier = Modifier.weight(0.9f)
                            )

                            MonochromeButton(
                                onClick = { selectedMap?.let { onDeleteMapClick(it.id) } },
                                text = "Delete",
                                enabled = selectedMap != null,
                                variant = MonochromeButtonVariant.Secondary,
                                size = MonochromeButtonSize.Large,
                                modifier = Modifier.weight(0.9f)
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal Dialog: Save/Add Map
    if (uiState.isAddMapDialogOpen) {
        MonochromeDialog(
            onDismissRequest = onCloseAddMapDialog,
            title = "Save Map",
            text = {
                Column {
                    Text(
                        text = "Enter a name to save the current robot occupancy grid map:",
                        style = typography.bodySmall,
                        color = colors.secondaryText
                    )
                    Spacer(modifier = Modifier.height(spacing.space3))
                    MonochromeTextField(
                        value = uiState.newMapNameInput,
                        onValueChange = onUpdateNewMapNameInput,
                        placeholder = "e.g., Office Floor 1"
                    )
                }
            },
            confirmButton = {
                MonochromeButton(
                    onClick = onAddMap,
                    text = "Save Map",
                    variant = MonochromeButtonVariant.Primary,
                    enabled = uiState.newMapNameInput.isNotBlank()
                )
            },
            dismissButton = {
                MonochromeButton(
                    onClick = onCloseAddMapDialog,
                    text = "Cancel",
                    variant = MonochromeButtonVariant.Ghost
                )
            }
        )
    }

    // Modal Dialog: Edit Map Name
    if (uiState.isEditMapDialogOpen) {
        MonochromeDialog(
            onDismissRequest = onCloseEditMapDialog,
            title = "Edit Map Name",
            text = {
                Column {
                    Text(
                        text = "Update the map display name:",
                        style = typography.bodySmall,
                        color = colors.secondaryText
                    )
                    Spacer(modifier = Modifier.height(spacing.space3))
                    MonochromeTextField(
                        value = uiState.editMapNameInput,
                        onValueChange = onUpdateEditMapNameInput,
                        placeholder = "Map name"
                    )
                }
            },
            confirmButton = {
                MonochromeButton(
                    onClick = onSaveMapName,
                    text = "Save",
                    variant = MonochromeButtonVariant.Primary,
                    enabled = uiState.editMapNameInput.isNotBlank()
                )
            },
            dismissButton = {
                MonochromeButton(
                    onClick = onCloseEditMapDialog,
                    text = "Cancel",
                    variant = MonochromeButtonVariant.Ghost
                )
            }
        )
    }

    // Modal Dialog: Confirm Delete
    if (uiState.isDeleteConfirmDialogOpen) {
        MonochromeDialog(
            onDismissRequest = onCloseDeleteConfirmDialog,
            title = "Delete Map?",
            text = {
                Text(
                    text = "Are you sure you want to delete this map from the robot? This action cannot be undone.",
                    style = typography.bodySmall,
                    color = colors.secondaryText
                )
            },
            confirmButton = {
                MonochromeButton(
                    onClick = onDeleteMap,
                    text = "Delete",
                    variant = MonochromeButtonVariant.Primary
                )
            },
            dismissButton = {
                MonochromeButton(
                    onClick = onCloseDeleteConfirmDialog,
                    text = "Cancel",
                    variant = MonochromeButtonVariant.Ghost
                )
            }
        )
    }
}
