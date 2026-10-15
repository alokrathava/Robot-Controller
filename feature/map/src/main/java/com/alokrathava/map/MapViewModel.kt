package com.alokrathava.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alokrathava.sdk.RobotRepository
import com.alokrathava.sdk.error.RobotResult
import com.alokrathava.sdk.model.ConnectionStatus
import com.alokrathava.sdk.model.MapOperationState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel managing Map Management UI, SLAM mapping lifecycle,
 * live occupancy grid streams, and map operations.
 */
@HiltViewModel
class MapViewModel @Inject constructor(
    private val robotRepository: RobotRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    init {
        observeRepository()
        refreshMapsFromRobot()
    }

    /** Observes robot repository flows for connection, telemetry, occupancy grid, position, and map operation states. */
    private fun observeRepository() {
        viewModelScope.launch {
            var previousStatus: ConnectionStatus? = null
            combine(
                robotRepository.connectionStatus,
                robotRepository.connectionConfig,
                robotRepository.position,
                robotRepository.occupancyGrid,
                robotRepository.telemetry
            ) { connStatus, connConfig, pos, grid, telem ->
                val address = if (connConfig.ipAddress.isNotEmpty()) "${connConfig.ipAddress}:${connConfig.port}" else "192.168.1.108:8080"
                val isConn = (connStatus == ConnectionStatus.CONNECTED)
                if (isConn && previousStatus != ConnectionStatus.CONNECTED) {
                    refreshMapsFromRobot()
                }
                previousStatus = connStatus
                _uiState.update { currentState ->
                    currentState.copy(
                        isConnected = isConn,
                        connectionAddress = address,
                        robotPosition = pos,
                        occupancyGrid = grid,
                        telemetry = telem
                    )
                }
            }.collect {}
        }

        viewModelScope.launch {
            robotRepository.mapOperationState.collect { opState ->
                _uiState.update { currentState ->
                    val mappingActive = when (opState) {
                        is MapOperationState.InitializingMapping,
                        is MapOperationState.WaitingForMap,
                        is MapOperationState.Mapping,
                        is MapOperationState.MapUpdating,
                        is MapOperationState.Saving -> true
                        else -> false
                    }
                    currentState.copy(
                        mapOperationState = opState,
                        isMappingActive = mappingActive
                    )
                }
            }
        }
    }

    /** Synchronizes saved map list from the connected robot. */
    fun refreshMapsFromRobot() {
        viewModelScope.launch {
            when (val result = robotRepository.listMaps()) {
                is RobotResult.Success -> {
                    val mapList = result.value
                    val activeMap = mapList.firstOrNull { it.isActive }
                    _uiState.update { currentState ->
                        currentState.copy(
                            maps = mapList,
                            activeMap = activeMap,
                            selectedMapId = currentState.selectedMapId.ifEmpty {
                                activeMap?.id ?: mapList.firstOrNull()?.id ?: ""
                            }
                        )
                    }
                }
                is RobotResult.Failure -> {
                    _uiState.update { it.copy(errorMessage = result.error.message) }
                }
            }
        }
    }

    /** Selects a map from the UI list. */
    fun selectMap(mapId: String) {
        _uiState.update { currentState ->
            currentState.copy(selectedMapId = mapId)
        }
    }

    /** Switches active map on the robot gateway without triggering unsaved map save. */
    fun setMapActive(mapId: String) {
        if (_uiState.value.isMappingActive || _uiState.value.mapOperationState is MapOperationState.Mapping) {
            _uiState.update { it.copy(errorMessage = "Cannot switch map while SLAM mapping is active.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(mapOperationState = MapOperationState.Switching(mapId)) }
            when (val result = robotRepository.switchMap(mapId)) {
                is RobotResult.Success -> {
                    refreshMapsFromRobot()
                    _uiState.update {
                        it.copy(
                            selectedMapId = mapId,
                            mapOperationState = MapOperationState.Idle
                        )
                    }
                }
                is RobotResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            mapOperationState = MapOperationState.Failed(mapId, result.error.message),
                            errorMessage = result.error.message
                        )
                    }
                }
            }
        }
    }

    /** Initiates real-time SLAM mapping on the robot with optional session recovery parameters. */
    fun startMapping(
        resumeUnsaved: Boolean = false,
        discardUnsaved: Boolean = false
    ) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isStartMappingLoading = true,
                    errorMessage = null
                )
            }
            when (val result = robotRepository.startMapping(resumeUnsaved, discardUnsaved)) {
                is RobotResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isStartMappingLoading = false,
                            isRecoverableSessionDialogOpen = false,
                            isStartFreshConfirmDialogOpen = false
                        )
                    }
                }
                is RobotResult.Failure -> {
                    val errorCode = result.error.code
                    if (errorCode == "RECOVERABLE_SESSION_EXISTS") {
                        _uiState.update {
                            it.copy(
                                isStartMappingLoading = false,
                                isRecoverableSessionDialogOpen = true,
                                errorMessage = null
                            )
                        }
                    } else {
                        val friendlyMessage = formatErrorMessage(errorCode, result.error.message)
                        _uiState.update {
                            it.copy(
                                isStartMappingLoading = false,
                                mapOperationState = MapOperationState.Failed(null, friendlyMessage),
                                errorMessage = friendlyMessage
                            )
                        }
                    }
                }
            }
        }
    }

    /** Resumes the previous unfinished mapping session. */
    fun resumeMappingSession() {
        startMapping(resumeUnsaved = true, discardUnsaved = false)
    }

    /** Opens confirmation dialog before starting a fresh session and discarding unsaved data. */
    fun openStartFreshConfirmDialog() {
        _uiState.update {
            it.copy(isStartFreshConfirmDialogOpen = true)
        }
    }

    /** Confirms starting a fresh mapping session, discarding previous unsaved session data. */
    fun confirmStartFreshMapping() {
        startMapping(resumeUnsaved = false, discardUnsaved = true)
    }

    /** Cancels start fresh confirmation dialog. */
    fun cancelStartFreshConfirmDialog() {
        _uiState.update {
            it.copy(isStartFreshConfirmDialogOpen = false)
        }
    }

    /** Cancels recoverable session dialog without sending commands or discarding data. */
    fun cancelRecoverableSessionDialog() {
        _uiState.update {
            it.copy(
                isRecoverableSessionDialogOpen = false,
                isStartFreshConfirmDialogOpen = false,
                mapOperationState = MapOperationState.Idle
            )
        }
    }

    /** Formats gateway error codes into friendly user messages. */
    private fun formatErrorMessage(code: String, defaultMsg: String): String {
        return when (code) {
            "RECOVERABLE_SESSION_EXISTS" -> "An unfinished mapping session exists on the robot."
            "MAPPING_ACTIVE" -> "Mapping is already running."
            "MAP_TRANSITION" -> "Another map operation is in progress."
            "SERVICE_UNAVAILABLE" -> "Robot mapping service is unavailable."
            "COMMAND_TIMEOUT" -> "Robot did not respond to the mapping request."
            "CONTROL_SESSION_IN_USE" -> "Another device currently controls the robot."
            "NOT_CONNECTED" -> "Not connected to robot."
            "INVALID_ARGUMENT" -> "Invalid mapping parameter requested."
            else -> defaultMsg.ifBlank { "Map operation failed" }
        }
    }

    /** Stops SLAM mapping on the robot, optionally discarding unsaved updates. */
    fun stopMapping(discardUnsaved: Boolean = false) {
        viewModelScope.launch {
            when (val result = robotRepository.stopMapping(discardUnsaved)) {
                is RobotResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isStopMappingConfirmDialogOpen = false
                        )
                    }
                    refreshMapsFromRobot()
                }
                is RobotResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isStopMappingConfirmDialogOpen = false,
                            errorMessage = result.error.message
                        )
                    }
                }
            }
        }
    }

    /** Opens dialog to add/save new map. */
    fun openAddMapDialog() {
        _uiState.update {
            it.copy(
                isAddMapDialogOpen = true,
                newMapNameInput = ""
            )
        }
    }

    /** Closes add/save new map dialog. */
    fun closeAddMapDialog() {
        _uiState.update {
            it.copy(
                isAddMapDialogOpen = false,
                newMapNameInput = ""
            )
        }
    }

    /** Updates text input for new map name. */
    fun updateNewMapNameInput(name: String) {
        _uiState.update {
            it.copy(newMapNameInput = name)
        }
    }

    /** Saves current mapped session into a permanent named map. */
    fun addMap() {
        val name = _uiState.value.newMapNameInput.trim()
        if (name.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(mapOperationState = MapOperationState.Saving(name)) }
            when (val result = robotRepository.saveCurrentMap(name)) {
                is RobotResult.Success -> {
                    refreshMapsFromRobot()
                    _uiState.update {
                        it.copy(
                            isAddMapDialogOpen = false,
                            newMapNameInput = "",
                            selectedMapId = result.value.id
                        )
                    }
                }
                is RobotResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            mapOperationState = MapOperationState.Failed(null, result.error.message),
                            errorMessage = result.error.message
                        )
                    }
                }
            }
        }
    }

    /** Opens rename map dialog. */
    fun openEditMapDialog(mapId: String) {
        val map = _uiState.value.maps.find { it.id == mapId } ?: return
        _uiState.update {
            it.copy(
                isEditMapDialogOpen = true,
                editingMapId = mapId,
                editMapNameInput = map.name
            )
        }
    }

    /** Closes rename map dialog. */
    fun closeEditMapDialog() {
        _uiState.update {
            it.copy(
                isEditMapDialogOpen = false,
                editingMapId = null,
                editMapNameInput = ""
            )
        }
    }

    /** Updates text input for map rename. */
    fun updateEditMapNameInput(name: String) {
        _uiState.update {
            it.copy(editMapNameInput = name)
        }
    }

    /** Commits map rename command to the robot. */
    fun saveMapName() {
        val editingId = _uiState.value.editingMapId ?: return
        val newName = _uiState.value.editMapNameInput.trim()
        if (newName.isEmpty()) return

        viewModelScope.launch {
            when (val result = robotRepository.renameMap(editingId, newName)) {
                is RobotResult.Success -> {
                    refreshMapsFromRobot()
                    _uiState.update {
                        it.copy(
                            isEditMapDialogOpen = false,
                            editingMapId = null,
                            editMapNameInput = ""
                        )
                    }
                }
                is RobotResult.Failure -> {
                    _uiState.update { it.copy(errorMessage = result.error.message) }
                }
            }
        }
    }

    /** Opens delete map confirmation dialog. */
    fun openDeleteConfirmDialog(mapId: String) {
        val map = _uiState.value.maps.find { it.id == mapId }
        if (map == null || map.isActive || _uiState.value.isMappingActive || !_uiState.value.isConnected) {
            _uiState.update { it.copy(errorMessage = "Cannot delete active map or while mapping is active.") }
            return
        }
        _uiState.update {
            it.copy(
                isDeleteConfirmDialogOpen = true,
                editingMapId = mapId
            )
        }
    }

    /** Closes delete map confirmation dialog. */
    fun closeDeleteConfirmDialog() {
        _uiState.update {
            it.copy(
                isDeleteConfirmDialogOpen = false,
                editingMapId = null
            )
        }
    }

    /** Deletes specified map from the robot. */
    fun deleteMap() {
        val mapIdToDelete = _uiState.value.editingMapId ?: return
        val map = _uiState.value.maps.find { it.id == mapIdToDelete }
        if (map == null || map.isActive || _uiState.value.isMappingActive || !_uiState.value.isConnected) {
            _uiState.update {
                it.copy(
                    isDeleteConfirmDialogOpen = false,
                    editingMapId = null,
                    errorMessage = "Cannot delete active map or while mapping is active."
                )
            }
            return
        }
        viewModelScope.launch {
            when (val result = robotRepository.deleteMap(mapIdToDelete)) {
                is RobotResult.Success -> {
                    refreshMapsFromRobot()
                    _uiState.update {
                        it.copy(
                            isDeleteConfirmDialogOpen = false,
                            editingMapId = null
                        )
                    }
                }
                is RobotResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isDeleteConfirmDialogOpen = false,
                            errorMessage = result.error.message
                        )
                    }
                }
            }
        }
    }

    /** Clears active error message banner. */
    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
