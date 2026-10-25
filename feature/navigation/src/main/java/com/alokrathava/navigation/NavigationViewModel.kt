package com.alokrathava.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alokrathava.sdk.RobotRepository
import com.alokrathava.sdk.RobotPositionSource
import com.alokrathava.sdk.error.RobotResult
import com.alokrathava.sdk.model.BatteryStatus
import com.alokrathava.sdk.model.ConnectionStatus
import com.alokrathava.sdk.model.DockingStatus
import com.alokrathava.sdk.model.LiveRobotPosition
import com.alokrathava.sdk.model.NavigationStatus
import com.alokrathava.sdk.model.Pose2D
import com.alokrathava.sdk.model.RobotTelemetry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import com.alokrathava.sdk.model.ConnectionState
import com.alokrathava.sdk.model.NavigationState
import com.alokrathava.sdk.model.DockingStateDetails
import com.alokrathava.sdk.model.MapOperationState
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

/** Groups repository samples so navigation UI uses the shared validated pose. */
private data class RobotStateGroup(
    val battery: BatteryStatus,
    val connection: ConnectionStatus,
    val position: LiveRobotPosition,
    val telemetry: RobotTelemetry,
    val dockingStatus: DockingStatus,
    val occupancyGrid: com.alokrathava.sdk.model.RobotOccupancyGrid?
)

/** Combines SDK operation outcomes with local request errors and save activity. */
private data class RuntimeUi(
    val navigation: NavigationState,
    val docking: DockingStateDetails?,
    val error: String?,
    val saving: Boolean
)

@HiltViewModel
/** Coordinates destination controls with the shared robot position and map state. */
class NavigationViewModel @Inject constructor(
    private val robotRepository: RobotRepository
) : ViewModel() {

    private val _formState = MutableStateFlow(FormInputState())
    private val _locationsState = MutableStateFlow(SavedLocationsState())
    private val _executionStatus = MutableStateFlow<NavigationExecutionStatus>(NavigationExecutionStatus.Idle)
    // Production repositories expose one tracker; existing test fakes have no live pose.
    private val livePosition = (robotRepository as? RobotPositionSource)?.livePosition
        ?: MutableStateFlow(LiveRobotPosition())

    private val navigationState = MutableStateFlow(NavigationState())
    private val dockingDetails = MutableStateFlow<DockingStateDetails?>(null)
    private val operationError = MutableStateFlow<String?>(null)
    private val savingLocation = MutableStateFlow(false)
    private var locationsJob: Job? = null
    private var locationsMapId: String? = null
    private val runtimeUi = combine(navigationState, dockingDetails, operationError, savingLocation) { nav, dock, error, saving ->
        RuntimeUi(nav, dock, error, saving)
    }

    init {
        // Rebind to the authenticated client; collectors are cancelled on disconnect or replacement.
        viewModelScope.launch {
            robotRepository.connectionStatus.collectLatest { status ->
                val client = robotRepository.getActiveRobotClient()
                if (status != ConnectionStatus.CONNECTED || client == null) {
                    locationsJob?.cancel()
                    locationsMapId = null
                    _locationsState.value = _locationsState.value.copy(locations = emptyList())
                    navigationState.value = NavigationState()
                    dockingDetails.value = null
                    _executionStatus.value = NavigationExecutionStatus.Idle
                } else coroutineScope {
                    launch { client.navigation.collect { navigationState.value = it } }
                    launch { client.dockingDetails.collect { dockingDetails.value = it } }
                    combine(client.activeMap, client.mapOperationState) { map, operation -> map to operation }
                        .collect { (map, operation) ->
                            val mapId = map?.id
                            if ((locationsMapId != null && mapId != locationsMapId) || operation !is MapOperationState.Idle) {
                                locationsJob?.cancel()
                                _locationsState.value = _locationsState.value.copy(locations = emptyList())
                                _formState.value = _formState.value.copy(selectedDestination = NavigationDestinationUi.None, targetPose = null)
                                locationsMapId = null
                            }
                            if (!mapId.isNullOrBlank() && operation is MapOperationState.Idle) {
                                if (locationsMapId == null) locationsMapId = mapId
                                loadSavedLocationsFromRobot()
                            }
                        }
                }
            }
        }
    }

    /** Loads only the active map and discards responses from an old connection or map. */
    fun loadSavedLocationsFromRobot() {
        val client = robotRepository.getActiveRobotClient() ?: return
        val mapId = client.activeMap.value?.id?.takeIf { it.isNotBlank() } ?: return
        locationsJob?.cancel()
        locationsJob = viewModelScope.launch {
            when (val result = client.listSavedLocations(mapId)) {
                is RobotResult.Success -> if (client === robotRepository.getActiveRobotClient() && client.activeMap.value?.id == mapId) {
                    locationsMapId = mapId
                    _locationsState.value = _locationsState.value.copy(locations = result.value.map { loc ->
                        SavedLocationUi(loc.id, loc.name, loc.pose.xMeters, loc.pose.yMeters, loc.pose.yawRadians, loc.mapId, loc.frame)
                    })
                    operationError.value = null
                }
                is RobotResult.Failure -> operationError.value = result.error.code + ": " + result.error.message
            }
        }
    }

    /** Rejects unavailable, stale, moving, unlocalized, or cross-map pose samples. */
    private fun currentPoseError(): String? {
        val client = robotRepository.getActiveRobotClient() ?: return "Connect to the robot first."
        if (client.connectionState.value !is ConnectionState.Connected) return "Robot is disconnected."
        val telemetry = client.telemetry.value ?: return "Waiting for robot telemetry."
        val freshness = client.telemetryFreshness.value
        if (freshness.isStale || freshness.receivedAtMillis <= 0L) return "Robot telemetry is stale."
        if (!telemetry.poseValid || telemetry.frame != "map" ||
            !listOf(telemetry.xMeters, telemetry.yMeters, telemetry.yawRadians).all { it.isFinite() }) return "A valid map pose is required."
        if (telemetry.localizationState != "LOCALIZED") return "Wait for localization."
        if (telemetry.isMoving) return "Stop the robot before saving."
        if (client.mapOperationState.value !is MapOperationState.Idle) return "Wait for the map operation to finish."
        val mapId = client.activeMap.value?.id?.takeIf { it.isNotBlank() } ?: return "No active map."
        if (telemetry.activeMap != mapId || client.occupancyGrid.value?.mapId != mapId) return "Waiting for matching active-map telemetry and map."
        return null
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
        livePosition,
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
        _robotStateGroup,
        runtimeUi
    ) { local, robot, runtime ->
        val pending = local.execution as? NavigationExecutionStatus.Navigating
        val result = runtime.navigation.takeIf { pending?.requestId != null && it.requestId == pending.requestId }
        val mappedExecution = when {
            result?.resultStatus == "SUCCEEDED" && (result.errorCode == null || result.errorCode == 0) -> NavigationExecutionStatus.Arrived(
                (pending?.destination as? NavigationDestinationUi.SavedLocation)?.name ?: "destination")
            result?.resultStatus == "CANCELED" || result?.resultStatus == "CANCELLED" -> NavigationExecutionStatus.Cancelled
            result?.resultStatus != null -> NavigationExecutionStatus.Failed(result.errorMsg.ifBlank { result.resultStatus ?: "Navigation failed" })
            pending != null -> pending.copy(distanceMeters = result?.distanceRemainingMeters?.toDouble())
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
            livePosition = robot.position,
            telemetry = robot.telemetry,
            dockingStatus = robot.dockingStatus,
            dockingDetails = runtime.docking,
            navigationState = runtime.navigation,
            operationError = runtime.error,
            isSavingLocation = runtime.saving,
            saveBlockReason = currentPoseError()
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

    /** Sends one Android request and waits for its correlated robot-side terminal result. */
    fun startNavigation() {
        // Prevent a second goal from silently replacing an unfinished Android request.
        if ((_executionStatus.value is NavigationExecutionStatus.CalculatingPath ||
                _executionStatus.value is NavigationExecutionStatus.Navigating) && navigationState.value.resultStatus == null) {
            operationError.value = "Wait for the current operation to finish, or cancel it."
            return
        }
        val dest = _formState.value.selectedDestination
        val client = robotRepository.getActiveRobotClient() ?: return
        operationError.value = null
        _executionStatus.value = NavigationExecutionStatus.CalculatingPath(dest)
        viewModelScope.launch {
            if (dest == NavigationDestinationUi.ChargingDock) {
                when (val result = client.dock()) {
                    is RobotResult.Success -> _executionStatus.value = NavigationExecutionStatus.Idle
                    is RobotResult.Failure -> _executionStatus.value = NavigationExecutionStatus.Failed(result.error.code + ": " + result.error.message)
                }
                return@launch
            }
            val result = when (dest) {
                is NavigationDestinationUi.SavedLocation -> {
                    val location = _locationsState.value.locations.firstOrNull { it.id == dest.id }
                    if (location == null || location.mapId != client.activeMap.value?.id || location.frame != "map") {
                        _executionStatus.value = NavigationExecutionStatus.Failed("Select a location on the active map.")
                        return@launch
                    }
                    client.navigateToLocation(dest.id)
                }
                is NavigationDestinationUi.Coordinates -> {
                    val values = listOf(dest.x, dest.y, dest.yaw).map { it.toDoubleOrNull() }
                    if (values.any { it == null || !it.isFinite() }) {
                        _executionStatus.value = NavigationExecutionStatus.Failed("Enter finite destination coordinates.")
                        return@launch
                    }
                    client.navigateTo(Pose2D(values[0]!!, values[1]!!, values[2]!!))
                }
                else -> {
                    _executionStatus.value = NavigationExecutionStatus.Failed("No destination selected")
                    return@launch
                }
            }
            _executionStatus.value = when (result) {
                is RobotResult.Success -> NavigationExecutionStatus.Navigating(dest, requestId = result.value.value)
                is RobotResult.Failure -> NavigationExecutionStatus.Failed(result.error.code + ": " + result.error.message)
            }
        }
    }

    /** Uses the existing undock action and keeps rejection visible. */
    fun undock() {
        viewModelScope.launch {
            val client = robotRepository.getActiveRobotClient() ?: return@launch
            when (val result = client.undock()) {
                is RobotResult.Success -> operationError.value = null
                is RobotResult.Failure -> operationError.value = result.error.code + ": " + result.error.message
            }
        }
    }

    /** Requests cancellation; the correlated Nav2 result confirms that motion has ended. */
    fun cancelNavigation() {
        viewModelScope.launch {
            val client = robotRepository.getActiveRobotClient() ?: return@launch
            when (val result = client.cancelNavigation()) {
                is RobotResult.Success -> operationError.value = null
                is RobotResult.Failure -> operationError.value = result.error.code + ": " + result.error.message
            }
        }
    }

    /** Opens the current-pose dialog with a clean request error. */
    fun openAddLocationDialog(isOpen: Boolean) {
        operationError.value = null
        _locationsState.value = _locationsState.value.copy(isAddDialogOpen = isOpen)
    }

    /** Saves the current validated localized pose when the user confirms the name. */
    fun saveCurrentLocation(name: String) {
        if (savingLocation.value) return
        val error = if (name.isBlank()) "Enter a location name." else currentPoseError()
        if (error != null) { operationError.value = error; return }
        val client = robotRepository.getActiveRobotClient() ?: return
        val telemetry = client.telemetry.value ?: return
        val mapId = client.activeMap.value?.id ?: return
        savingLocation.value = true
        operationError.value = null
        viewModelScope.launch {
            try {
                when (val result = client.saveLocation(name.trim(), Pose2D(telemetry.xMeters, telemetry.yMeters, telemetry.yawRadians), mapId)) {
                    is RobotResult.Success -> {
                        _locationsState.value = _locationsState.value.copy(isAddDialogOpen = false)
                        loadSavedLocationsFromRobot()
                    }
                    is RobotResult.Failure -> operationError.value = result.error.code + ": " + result.error.message
                }
            } finally { savingLocation.value = false }
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
