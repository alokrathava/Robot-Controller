package com.alokrathava.map

import com.alokrathava.sdk.model.MapData
import com.alokrathava.sdk.model.NavigationStatus
import com.alokrathava.sdk.model.RobotPosition
import com.alokrathava.sdk.model.RobotTelemetry

enum class FloorPlanType {
    MAIN_FLOOR,
    SECOND_FLOOR,
    OUTDOOR,
    TEST_MAP
}

data class RobotMapItem(
    val id: String,
    val name: String,
    val lastUpdated: String,
    val isActive: Boolean = false,
    val floorPlanType: FloorPlanType = FloorPlanType.MAIN_FLOOR
)

data class MapUiState(
    val maps: List<RobotMapItem> = defaultMaps,
    val selectedMapId: String = "main_floor",
    val isConnected: Boolean = true,
    val connectionAddress: String = "192.168.1.108:8080",
    val isAddMapDialogOpen: Boolean = false,
    val isEditMapDialogOpen: Boolean = false,
    val isDeleteConfirmDialogOpen: Boolean = false,
    val editingMapId: String? = null,
    val newMapNameInput: String = "",
    val editMapNameInput: String = "",
    val robotPosition: RobotPosition = RobotPosition(),
    val navigationStatus: NavigationStatus = NavigationStatus.IDLE,
    val mapData: MapData = MapData(),
    val telemetry: RobotTelemetry = RobotTelemetry()
) {
    val selectedMap: RobotMapItem?
        get() = maps.find { it.id == selectedMapId } ?: maps.firstOrNull()
}

val defaultMaps = listOf(
    RobotMapItem(
        id = "main_floor",
        name = "Main Floor",
        lastUpdated = "Last updated 2 days ago",
        isActive = true,
        floorPlanType = FloorPlanType.MAIN_FLOOR
    ),
    RobotMapItem(
        id = "second_floor",
        name = "Second Floor",
        lastUpdated = "Last updated 1 week ago",
        isActive = false,
        floorPlanType = FloorPlanType.SECOND_FLOOR
    ),
    RobotMapItem(
        id = "outdoor",
        name = "Outdoor",
        lastUpdated = "Last updated 3 weeks ago",
        isActive = false,
        floorPlanType = FloorPlanType.OUTDOOR
    ),
    RobotMapItem(
        id = "test_map",
        name = "Test Map",
        lastUpdated = "Last updated 1 month ago",
        isActive = false,
        floorPlanType = FloorPlanType.TEST_MAP
    )
)
