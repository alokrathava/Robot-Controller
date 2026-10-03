package com.alokrathava.navigation

import com.alokrathava.sdk.model.BatteryStatus
import com.alokrathava.sdk.model.ConnectionStatus
import com.alokrathava.sdk.model.DockingStatus
import com.alokrathava.sdk.model.RobotPosition
import com.alokrathava.sdk.model.RobotTelemetry

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
        get() = "($x, $y)"
}

sealed interface NavigationExecutionStatus {
    data object Idle : NavigationExecutionStatus
    data class CalculatingPath(val destination: NavigationDestinationUi) : NavigationExecutionStatus
    data class Navigating(
        val destination: NavigationDestinationUi,
        val etaSeconds: Int = 45,
        val distanceMeters: Double = 12.4
    ) : NavigationExecutionStatus
    data class ObstacleDetected(val message: String = "Obstacle blocking path, rerouting...") : NavigationExecutionStatus
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
    val yawInput: String = "",
    val xInputError: String? = null,
    val yInputError: String? = null,
    val executionStatus: NavigationExecutionStatus = NavigationExecutionStatus.Idle,
    val mapUiState: MapUiState = MapUiState.Loaded(mapName = "Default Map"),
    val isAddLocationDialogOpen: Boolean = false,
    val newLocationNameInput: String = "",
    val newLocationXInput: String = "",
    val newLocationYInput: String = "",
    val batteryStatus: BatteryStatus = BatteryStatus(),
    val connectionStatus: ConnectionStatus = ConnectionStatus.CONNECTED,
    val robotPosition: RobotPosition = RobotPosition(),
    val telemetry: RobotTelemetry = RobotTelemetry(),
    val dockingStatus: DockingStatus = DockingStatus.UNDOCKED
)
