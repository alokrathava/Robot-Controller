package com.alokrathava.navigation

import com.alokrathava.sdk.model.BatteryStatus
import com.alokrathava.sdk.model.LiveRobotPosition
import com.alokrathava.sdk.model.ConnectionStatus
import com.alokrathava.sdk.model.DockingStatus
import com.alokrathava.sdk.model.NavigationState
import com.alokrathava.sdk.model.Pose2D
import com.alokrathava.sdk.model.RobotMap
import com.alokrathava.sdk.model.RobotOccupancyGrid
import com.alokrathava.sdk.model.RobotTelemetry
import com.alokrathava.sdk.model.SafetyState

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
    Coordinates,
    MapPicker
}

data class SavedLocationUi(
    val id: String,
    val name: String,
    val x: Double,
    val y: Double
) {
    val coordinatesFormatted: String
        get() = "(${ "%.2f".format(x) }, ${ "%.2f".format(y) })"
}

sealed interface NavigationExecutionStatus {
    data object Idle : NavigationExecutionStatus
    data class CalculatingPath(val destination: NavigationDestinationUi) : NavigationExecutionStatus
    data class Navigating(
        val destination: NavigationDestinationUi,
        val etaSeconds: Int? = null,
        val distanceMeters: Double? = null
    ) : NavigationExecutionStatus
    data class ObstacleDetected(val message: String = "Obstacle blocking path") : NavigationExecutionStatus
    data class Arrived(val destinationName: String) : NavigationExecutionStatus
    data object Cancelled : NavigationExecutionStatus
    data class Failed(val errorMessage: String) : NavigationExecutionStatus
}

sealed interface MapUiState {
    data object Loading : MapUiState
    data class Loaded(val mapName: String, val isAvailable: Boolean = true) : MapUiState
    data object NoMapAvailable : MapUiState
    data class Error(val message: String) : MapUiState
}

/** Holds navigation controls and the nullable live robot pose for the map panel. */
data class NavigationUiState(
    val selectedTab: NavigationTab = NavigationTab.SavedLocations,
    val savedLocations: List<SavedLocationUi> = emptyList(),
    val selectedDestination: NavigationDestinationUi = NavigationDestinationUi.None,
    val targetPose: Pose2D? = null,
    val xInput: String = "",
    val yInput: String = "",
    val yawInput: String = "",
    val xInputError: String? = null,
    val yInputError: String? = null,
    val executionStatus: NavigationExecutionStatus = NavigationExecutionStatus.Idle,
    val mapUiState: MapUiState = MapUiState.NoMapAvailable,
    val activeMap: RobotMap? = null,
    val occupancyGrid: RobotOccupancyGrid? = null,
    val navigationState: NavigationState = NavigationState(),
    val safetyState: SafetyState? = null,
    val isAddLocationDialogOpen: Boolean = false,
    val newLocationNameInput: String = "",
    val newLocationXInput: String = "",
    val newLocationYInput: String = "",
    val batteryStatus: BatteryStatus = BatteryStatus(),
    val connectionStatus: ConnectionStatus = ConnectionStatus.CONNECTED,
    val livePosition: LiveRobotPosition = LiveRobotPosition(),
    val telemetry: RobotTelemetry = RobotTelemetry(),
    val dockingStatus: DockingStatus = DockingStatus.UNDOCKED
)
