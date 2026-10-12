package com.alokrathava.map

import com.alokrathava.sdk.model.MapOperationState
import com.alokrathava.sdk.model.NavigationStatus
import com.alokrathava.sdk.model.RobotMap
import com.alokrathava.sdk.model.RobotOccupancyGrid
import com.alokrathava.sdk.model.RobotPosition
import com.alokrathava.sdk.model.RobotTelemetry

/**
 * Represents the UI state for Map Management and SLAM visualization.
 * Holds authoritative map data, live occupancy grids, and mapping operation states.
 */
data class MapUiState(
    val maps: List<RobotMap> = emptyList(),
    val selectedMapId: String = "",
    val activeMap: RobotMap? = null,
    val occupancyGrid: RobotOccupancyGrid? = null,
    val mapOperationState: MapOperationState = MapOperationState.Idle,
    val isMappingActive: Boolean = false,
    val isConnected: Boolean = false,
    val connectionAddress: String = "",
    val isAddMapDialogOpen: Boolean = false,
    val isEditMapDialogOpen: Boolean = false,
    val isDeleteConfirmDialogOpen: Boolean = false,
    val isStopMappingConfirmDialogOpen: Boolean = false,
    val editingMapId: String? = null,
    val newMapNameInput: String = "",
    val editMapNameInput: String = "",
    val errorMessage: String? = null,
    val robotPosition: RobotPosition = RobotPosition(),
    val navigationStatus: NavigationStatus = NavigationStatus.IDLE,
    val telemetry: RobotTelemetry = RobotTelemetry()
) {
    /** Returns the currently selected robot map or active map. */
    val selectedMap: RobotMap?
        get() = maps.find { it.id == selectedMapId } ?: activeMap ?: maps.firstOrNull()
}
