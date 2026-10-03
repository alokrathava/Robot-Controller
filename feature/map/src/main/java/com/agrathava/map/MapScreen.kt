package com.agrathava.map

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
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
import com.agrathava.home.Sidebar
import com.agrathava.home.SidebarNavItem
import com.agrathava.theme.*

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
    onDeleteMap: () -> Unit = {}
) {
    val colors = MonochromeTheme.colors
    val typography = MonochromeTheme.typography
    val spacing = MonochromeTheme.spacing
    val shapes = MonochromeTheme.shapes

    val selectedMap = uiState.selectedMap

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
            // Header Bar: Title & Add Map Action Button
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
                        text = "View, switch or manage robot maps",
                        style = typography.bodySmall,
                        color = colors.secondaryText
                    )
                }

                MonochromeButton(
                    onClick = onAddMapClick,
                    text = "Add Map",
                    icon = Icons.Default.Add,
                    variant = MonochromeButtonVariant.Primary,
                    size = MonochromeButtonSize.Large
                )
            }

            Spacer(modifier = Modifier.height(spacing.space5))

            // Split Layout: Left Map List + Right Map Preview Pane
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(spacing.space6)
            ) {
                // Left Panel: Scrollable Map Cards List
                Column(
                    modifier = Modifier
                        .weight(0.45f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(spacing.space3)
                ) {
                    uiState.maps.forEach { mapItem ->
                        val isSelected = (mapItem.id == uiState.selectedMapId)
                        var menuExpanded by remember { mutableStateOf(false) }

                        val cardBorderColor = if (isSelected) {
                            colors.focusBorder
                        } else {
                            colors.defaultBorder
                        }

                        val cardBg = if (isSelected) {
                            colors.elevatedSurface
                        } else {
                            colors.surface
                        }

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
                                // Thumbnail Box
                                MapFloorPlanCanvas(
                                    floorPlanType = mapItem.floorPlanType,
                                    isThumbnail = true,
                                    modifier = Modifier.size(68.dp)
                                )

                                Spacer(modifier = Modifier.width(14.dp))

                                // Map Info
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
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
                                            MonochromeStatusPill(
                                                text = "Active",
                                                level = StatusLevel.Active
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = mapItem.lastUpdated,
                                        style = typography.caption,
                                        color = colors.mutedText
                                    )
                                }

                                // Three-dots overflow options menu
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

                // Right Panel: Map Preview Card + Action Buttons
                Column(
                    modifier = Modifier
                        .weight(0.55f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Large Floor Plan Preview Container
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(shapes.largeCards)
                            .border(1.dp, colors.defaultBorder, shapes.largeCards)
                    ) {
                        if (selectedMap != null) {
                            MapFloorPlanCanvas(
                                floorPlanType = selectedMap.floorPlanType,
                                isThumbnail = false,
                                robotPosition = uiState.robotPosition,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(colors.surface),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No map selected",
                                    style = typography.body,
                                    color = colors.mutedText
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(spacing.space4))

                    // Bottom Row of Action Buttons: "Set as Active", "Edit", "Delete"
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

    // Modal Dialog: Add Map
    if (uiState.isAddMapDialogOpen) {
        MonochromeDialog(
            onDismissRequest = onCloseAddMapDialog,
            title = "Add New Map",
            text = {
                Column {
                    Text(
                        text = "Enter a name for the new floor map:",
                        style = typography.bodySmall,
                        color = colors.secondaryText
                    )
                    Spacer(modifier = Modifier.height(spacing.space3))
                    MonochromeTextField(
                        value = uiState.newMapNameInput,
                        onValueChange = onUpdateNewMapNameInput,
                        placeholder = "e.g., Third Floor"
                    )
                }
            },
            confirmButton = {
                MonochromeButton(
                    onClick = onAddMap,
                    text = "Add Map",
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
                    text = "Are you sure you want to delete this map? This action cannot be undone.",
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

@Preview(name = "Map Management Screen Light Preview", showBackground = true, widthDp = 1024, heightDp = 600)
@Composable
fun MapScreenPreviewLight() {
    MonochromeTheme(darkTheme = false) {
        MapScreenContent(uiState = MapUiState())
    }
}

@Preview(name = "Map Management Screen Dark Preview", showBackground = true, widthDp = 1024, heightDp = 600)
@Composable
fun MapScreenPreviewDark() {
    MonochromeTheme(darkTheme = true) {
        MapScreenContent(uiState = MapUiState())
    }
}
