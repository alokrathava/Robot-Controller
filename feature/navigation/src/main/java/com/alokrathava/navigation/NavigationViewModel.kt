package com.alokrathava.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alokrathava.sdk.RobotRepository
import com.alokrathava.sdk.error.RobotResult
import com.alokrathava.sdk.model.BatteryStatus
import com.alokrathava.sdk.model.ConnectionStatus
import com.alokrathava.sdk.model.DockingStatus
import com.alokrathava.sdk.model.NavigationStatus
import com.alokrathava.sdk.model.Pose2D
import com.alokrathava.sdk.model.RobotPosition
import com.alokrathava.sdk.model.RobotTelemetry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private data class FormInputState(
    val selectedTab: NavigationTab = NavigationTab.SavedLocations,
    val selectedDestination: NavigationDestinationUi = NavigationDestinationUi.None,
    val targetPose: Pose2D? = null,
    val xInput: String = "",
    val yInput: String = "",
    val yawInput: String = "",
    val xInputError: String? = null,
    val yInputError: String? = null
)

private data class SavedLocationsState(
    val locations: List<SavedLocationUi> = emptyList(),
    val isAddDialogOpen: Boolean = false,
    val nameInput: String = "",
    val xInput: String = "",
    val yInput: String = ""
)

private data class LocalNavigationState(
    val form: FormInputState,
    val locations: SavedLocationsState,
    val execution: NavigationExecutionStatus
)

private data class RobotStateGroup(
    val battery: BatteryStatus,
    val connection: ConnectionStatus,
    val position: RobotPosition,
    val telemetry: RobotTelemetry,
    val dockingStatus: DockingStatus,
    val occupancyGrid: com.alokrathava.sdk.model.RobotOccupancyGrid?
)

@HiltViewModel
class NavigationViewModel @Inject constructor(
    private val robotRepository: RobotRepository
) : ViewModel() {

    private val _formState = MutableStateFlow(FormInputState())
    private val _locationsState = MutableStateFlow(SavedLocationsState())
    private val _executionStatus = MutableStateFlow<NavigationExecutionStatus>(NavigationExecutionStatus.Idle)

    init {
        loadSavedLocationsFromRobot()
    }

    /** Loads saved map destinations from the connected robot. */
    fun loadSavedLocationsFromRobot() {
        viewModelScope.launch {
            when (val result = robotRepository.listSavedLocations()) {
                is RobotResult.Success -> {
                    val mapped = result.value.map { loc ->
                        SavedLocationUi(
                            id = loc.id,
                            name = loc.name,
                            x = loc.pose.xMeters,
                            y = loc.pose.yMeters
                        )
                    }
                    _locationsState.value = _locationsState.value.copy(locations = mapped)
                }
                is RobotResult.Failure -> { }
            }
        }
    }

    private val _localNavigationFlow = combine(
        _formState,
        _locationsState,
        _executionStatus
    ) { form, locations, execution ->
        LocalNavigationState(form, locations, execution)
    }

    private val _robotStateGroup = combine(
        robotRepository.batteryStatus,
        robotRepository.connectionStatus,
        robotRepository.position,
        robotRepository.telemetry,
        combine(
            robotRepository.dockingStatus,
            robotRepository.occupancyGrid
        ) { dock, grid -> Pair(dock, grid) }
    ) { battery, connection, pos, telem, (dock, grid) ->
        RobotStateGroup(battery, connection, pos, telem, dock, grid)
    }

    val uiState: StateFlow<NavigationUiState> = combine(
        _localNavigationFlow,
        _robotStateGroup
    ) { local, robot ->
        val mappedExecution = when {
            robot.telemetry.isMoving && robot.telemetry.navigationState.equals("NAVIGATING", ignoreCase = true) -> {
                if (local.execution is NavigationExecutionStatus.Navigating) local.execution
                else NavigationExecutionStatus.Navigating(destination = local.form.selectedDestination)
            }
            robot.telemetry.navigationState.equals("CANCELLED", ignoreCase = true) -> NavigationExecutionStatus.Cancelled
            else -> local.execution
        }

        val mapState = if (robot.occupancyGrid != null && robot.occupancyGrid.isValid) {
            MapUiState.Loaded(mapName = robot.occupancyGrid.mapId.ifEmpty { "Active Map" }, isAvailable = true)
        } else {
            MapUiState.NoMapAvailable
        }

        NavigationUiState(
            selectedTab = local.form.selectedTab,
            savedLocations = local.locations.locations,
            selectedDestination = local.form.selectedDestination,
            targetPose = local.form.targetPose,
            xInput = local.form.xInput,
            yInput = local.form.yInput,
            yawInput = local.form.yawInput,
            xInputError = local.form.xInputError,
            yInputError = local.form.yInputError,
            executionStatus = mappedExecution,
            mapUiState = mapState,
            occupancyGrid = robot.occupancyGrid,
            isAddLocationDialogOpen = local.locations.isAddDialogOpen,
            newLocationNameInput = local.locations.nameInput,
            newLocationXInput = local.locations.xInput,
            newLocationYInput = local.locations.yInput,
            batteryStatus = robot.battery,
            connectionStatus = robot.connection,
            robotPosition = robot.position,
            telemetry = robot.telemetry,
            dockingStatus = robot.dockingStatus
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NavigationUiState()
    )

    fun selectTab(tab: NavigationTab) {
        _formState.value = _formState.value.copy(selectedTab = tab)
    }

    fun selectDestination(destination: NavigationDestinationUi) {
        val target = when (destination) {
            is NavigationDestinationUi.SavedLocation -> Pose2D(xMeters = destination.x, yMeters = destination.y, yawRadians = 0.0)
            is NavigationDestinationUi.Coordinates -> {
                val x = destination.x.toDoubleOrNull() ?: 0.0
                val y = destination.y.toDoubleOrNull() ?: 0.0
                val yaw = destination.yaw.toDoubleOrNull() ?: 0.0
                Pose2D(xMeters = x, yMeters = y, yawRadians = yaw)
            }
            else -> null
        }
        _formState.value = _formState.value.copy(
            selectedDestination = destination,
            targetPose = target
        )
    }

    /** Handles direct map tap gestures to place target destination pin. */
    fun onMapTapped(worldX: Double, worldY: Double) {
        val formattedX = "%.2f".format(worldX)
        val formattedY = "%.2f".format(worldY)
        val dest = NavigationDestinationUi.Coordinates(formattedX, formattedY, "0.0")
        _formState.value = _formState.value.copy(
            selectedDestination = dest,
            targetPose = Pose2D(xMeters = worldX, yMeters = worldY, yawRadians = 0.0),
            xInput = formattedX,
            yInput = formattedY,
            yawInput = "0.0"
        )
    }

    fun updateXInput(x: String) {
        val error = if (x.isNotBlank() && x.toDoubleOrNull() == null) "Invalid number" else null
        _formState.value = _formState.value.copy(xInput = x, xInputError = error)
    }

    fun updateYInput(y: String) {
        val error = if (y.isNotBlank() && y.toDoubleOrNull() == null) "Invalid number" else null
        _formState.value = _formState.value.copy(yInput = y, yInputError = error)
    }

    fun updateYawInput(yaw: String) {
        _formState.value = _formState.value.copy(yawInput = yaw)
    }

    /** Dispatches autonomous navigation task to the robot. */
    fun startNavigation() {
        val dest = _formState.value.selectedDestination
        when (dest) {
            is NavigationDestinationUi.SavedLocation -> {
                _executionStatus.value = NavigationExecutionStatus.Navigating(destination = dest)
                viewModelScope.launch {
                    val result = robotRepository.navigateToLocation(dest.id)
                    if (result is RobotResult.Failure) {
                        robotRepository.moveToPosition(dest.x, dest.y)
                    }
                }
            }

            is NavigationDestinationUi.Coordinates -> {
                val x = dest.x.toDoubleOrNull() ?: 0.0
                val y = dest.y.toDoubleOrNull() ?: 0.0
                _executionStatus.value = NavigationExecutionStatus.Navigating(destination = dest)
                robotRepository.moveToPosition(x, y)
            }

            NavigationDestinationUi.ChargingDock -> {
                _executionStatus.value = NavigationExecutionStatus.Navigating(destination = dest)
                robotRepository.goToCharge()
            }

            NavigationDestinationUi.None -> {
                _executionStatus.value = NavigationExecutionStatus.Failed("No destination selected")
            }
        }
    }

    /** Cancels current navigation task. */
    fun cancelNavigation() {
        robotRepository.cancelNavigation()
        _executionStatus.value = NavigationExecutionStatus.Cancelled
    }

    fun openAddLocationDialog(isOpen: Boolean) {
        _locationsState.value = _locationsState.value.copy(isAddDialogOpen = isOpen)
    }

    /** Saves a new location on the active map on the robot. */
    fun saveNewLocation(name: String, x: Double, y: Double) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val result = robotRepository.saveLocation(name, Pose2D(xMeters = x, yMeters = y, yawRadians = 0.0))
            if (result is RobotResult.Success) {
                loadSavedLocationsFromRobot()
            }
            _locationsState.value = _locationsState.value.copy(
                isAddDialogOpen = false,
                nameInput = "",
                xInput = "",
                yInput = ""
            )
        }
    }

    /** Deletes a saved location from the robot. */
    fun deleteSavedLocation(id: String) {
        viewModelScope.launch {
            when (robotRepository.deleteLocation(id)) {
                is RobotResult.Success -> loadSavedLocationsFromRobot()
                is RobotResult.Failure -> { }
            }
        }
    }
}
