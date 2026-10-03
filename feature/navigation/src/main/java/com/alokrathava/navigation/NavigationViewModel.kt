package com.alokrathava.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alokrathava.sdk.RobotRepository
import com.alokrathava.sdk.model.BatteryStatus
import com.alokrathava.sdk.model.ConnectionStatus
import com.alokrathava.sdk.model.MapData
import com.alokrathava.sdk.model.NavigationStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

import com.alokrathava.sdk.model.DockingStatus
import com.alokrathava.sdk.model.RobotPosition
import com.alokrathava.sdk.model.RobotTelemetry

private data class FormInputState(
    val selectedTab: NavigationTab = NavigationTab.SavedLocations,
    val selectedDestination: NavigationDestinationUi = NavigationDestinationUi.None,
    val xInput: String = "",
    val yInput: String = "",
    val yawInput: String = "",
    val xInputError: String? = null,
    val yInputError: String? = null
)

private data class SavedLocationsState(
    val locations: List<SavedLocationUi> = sampleLocations,
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

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

private data class RobotNavigationState(
    val battery: BatteryStatus,
    val connection: ConnectionStatus,
    val mapData: MapData,
    val navStatus: NavigationStatus,
    val position: RobotPosition,
    val telemetry: RobotTelemetry,
    val dockingStatus: DockingStatus
)

@HiltViewModel
class NavigationViewModel @Inject constructor(
    private val robotRepository: RobotRepository
) : ViewModel() {

    private val _formState = MutableStateFlow(FormInputState())
    private val _locationsState = MutableStateFlow(SavedLocationsState())
    private val _executionStatus = MutableStateFlow<NavigationExecutionStatus>(NavigationExecutionStatus.Idle)

    private val _localNavigationFlow = combine(
        _formState,
        _locationsState,
        _executionStatus
    ) { form, locations, execution ->
        LocalNavigationState(form, locations, execution)
    }

    private val _robotStateFlow = combine(
        robotRepository.batteryStatus,
        robotRepository.connectionStatus,
        robotRepository.mapData,
        combine(
            robotRepository.navigationStatus,
            robotRepository.position,
            robotRepository.telemetry,
            robotRepository.dockingStatus
        ) { nav, pos, telem, dock -> Quad(nav, pos, telem, dock) }
    ) { battery, connection, mapData, (navStatus, pos, telem, dock) ->
        RobotNavigationState(battery, connection, mapData, navStatus, pos, telem, dock)
    }

    val uiState: StateFlow<NavigationUiState> = combine(
        _localNavigationFlow,
        _robotStateFlow
    ) { local, robot ->
        val mappedExecution = when (robot.navStatus) {
            NavigationStatus.NAVIGATING -> if (local.execution is NavigationExecutionStatus.Navigating) local.execution else NavigationExecutionStatus.Navigating(destination = local.form.selectedDestination)
            NavigationStatus.CANCELLED -> NavigationExecutionStatus.Cancelled
            NavigationStatus.EMERGENCY_STOP -> NavigationExecutionStatus.Failed("Emergency stop active")
            else -> local.execution
        }

        val mapState = if (robot.mapData.isAvailable) {
            MapUiState.Loaded(mapName = robot.mapData.name, isAvailable = true)
        } else {
            MapUiState.NoMapAvailable
        }

        NavigationUiState(
            selectedTab = local.form.selectedTab,
            savedLocations = local.locations.locations,
            selectedDestination = local.form.selectedDestination,
            xInput = local.form.xInput,
            yInput = local.form.yInput,
            yawInput = local.form.yawInput,
            xInputError = local.form.xInputError,
            yInputError = local.form.yInputError,
            executionStatus = mappedExecution,
            mapUiState = mapState,
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
        _formState.value = _formState.value.copy(selectedDestination = destination)
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

    fun startNavigation() {
        val dest = _formState.value.selectedDestination
        when (dest) {
            is NavigationDestinationUi.SavedLocation -> {
                _executionStatus.value = NavigationExecutionStatus.Navigating(destination = dest)
                robotRepository.moveToPosition(dest.x, dest.y)
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

    fun cancelNavigation() {
        robotRepository.cancelNavigation()
        _executionStatus.value = NavigationExecutionStatus.Cancelled
    }

    fun openAddLocationDialog(isOpen: Boolean) {
        _locationsState.value = _locationsState.value.copy(isAddDialogOpen = isOpen)
    }

    fun saveNewLocation(name: String, x: Double, y: Double) {
        if (name.isBlank()) return
        val newLoc = SavedLocationUi(
            id = System.currentTimeMillis().toString(),
            name = name,
            x = x,
            y = y
        )
        val updated = _locationsState.value.locations + newLoc
        _locationsState.value = _locationsState.value.copy(
            locations = updated,
            isAddDialogOpen = false,
            nameInput = "",
            xInput = "",
            yInput = ""
        )
    }
}
