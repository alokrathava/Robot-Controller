package com.alokrathava.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alokrathava.sdk.RobotRepository
import com.alokrathava.sdk.error.RobotResult
import com.alokrathava.sdk.model.ConnectionStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

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

    private fun observeRepository() {
        viewModelScope.launch {
            combine(
                robotRepository.connectionStatus,
                robotRepository.connectionConfig,
                robotRepository.position,
                combine(
                    robotRepository.navigationStatus,
                    robotRepository.mapData,
                    robotRepository.telemetry
                ) { nav, map, telem -> Triple(nav, map, telem) }
            ) { connStatus, connConfig, pos, navMapTelem ->
                val (navStatus, mapData, telem) = navMapTelem
                val address = if (connConfig.ipAddress.isNotEmpty()) "${connConfig.ipAddress}:${connConfig.port}" else "192.168.1.108:8080"
                _uiState.update { currentState ->
                    currentState.copy(
                        isConnected = (connStatus == ConnectionStatus.CONNECTED),
                        connectionAddress = address,
                        robotPosition = pos,
                        navigationStatus = navStatus,
                        mapData = mapData,
                        telemetry = telem
                    )
                }
            }.collect {}
        }
    }

    fun refreshMapsFromRobot() {
        viewModelScope.launch {
            robotRepository.fetchMap()
            when (val result = robotRepository.listMaps()) {
                is RobotResult.Success -> {
                    if (result.value.isNotEmpty()) {
                        val mappedList = result.value.map { sdkMap ->
                            RobotMapItem(
                                id = sdkMap.id,
                                name = sdkMap.name,
                                lastUpdated = "Synchronized from robot",
                                isActive = sdkMap.isActive,
                                floorPlanType = FloorPlanType.MAIN_FLOOR
                            )
                        }
                        _uiState.update { currentState ->
                            currentState.copy(
                                maps = mappedList,
                                selectedMapId = mappedList.firstOrNull { it.isActive }?.id ?: mappedList.first().id
                            )
                        }
                    }
                }
                is RobotResult.Failure -> { }
            }
        }
    }

    fun selectMap(mapId: String) {
        _uiState.update { currentState ->
            currentState.copy(selectedMapId = mapId)
        }
    }

    fun setMapActive(mapId: String) {
        _uiState.update { currentState ->
            val updatedMaps = currentState.maps.map { mapItem ->
                mapItem.copy(isActive = (mapItem.id == mapId))
            }
            currentState.copy(
                maps = updatedMaps,
                selectedMapId = mapId
            )
        }
        viewModelScope.launch {
            robotRepository.switchMap(mapId)
            robotRepository.saveMap()
        }
    }

    fun openAddMapDialog() {
        _uiState.update {
            it.copy(
                isAddMapDialogOpen = true,
                newMapNameInput = ""
            )
        }
    }

    fun closeAddMapDialog() {
        _uiState.update {
            it.copy(
                isAddMapDialogOpen = false,
                newMapNameInput = ""
            )
        }
    }

    fun updateNewMapNameInput(name: String) {
        _uiState.update {
            it.copy(newMapNameInput = name)
        }
    }

    fun addMap() {
        val name = _uiState.value.newMapNameInput.trim()
        if (name.isEmpty()) return

        val newId = "map_${UUID.randomUUID().toString().take(8)}"
        val newMap = RobotMapItem(
            id = newId,
            name = name,
            lastUpdated = "Just now",
            isActive = false,
            floorPlanType = FloorPlanType.MAIN_FLOOR
        )

        _uiState.update { currentState ->
            val updatedList = currentState.maps + newMap
            currentState.copy(
                maps = updatedList,
                selectedMapId = newId,
                isAddMapDialogOpen = false,
                newMapNameInput = ""
            )
        }

        viewModelScope.launch {
            robotRepository.saveCurrentMap(name)
        }
    }

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

    fun closeEditMapDialog() {
        _uiState.update {
            it.copy(
                isEditMapDialogOpen = false,
                editingMapId = null,
                editMapNameInput = ""
            )
        }
    }

    fun updateEditMapNameInput(name: String) {
        _uiState.update {
            it.copy(editMapNameInput = name)
        }
    }

    fun saveMapName() {
        val editingId = _uiState.value.editingMapId ?: return
        val newName = _uiState.value.editMapNameInput.trim()
        if (newName.isEmpty()) return

        _uiState.update { currentState ->
            val updatedMaps = currentState.maps.map { map ->
                if (map.id == editingId) map.copy(name = newName, lastUpdated = "Updated just now") else map
            }
            currentState.copy(
                maps = updatedMaps,
                isEditMapDialogOpen = false,
                editingMapId = null,
                editMapNameInput = ""
            )
        }
    }

    fun openDeleteConfirmDialog(mapId: String) {
        _uiState.update {
            it.copy(
                isDeleteConfirmDialogOpen = true,
                editingMapId = mapId
            )
        }
    }

    fun closeDeleteConfirmDialog() {
        _uiState.update {
            it.copy(
                isDeleteConfirmDialogOpen = false,
                editingMapId = null
            )
        }
    }

    fun deleteMap() {
        val mapIdToDelete = _uiState.value.editingMapId ?: return
        _uiState.update { currentState ->
            val updatedMaps = currentState.maps.filterNot { it.id == mapIdToDelete }
            val nextSelectedId = if (currentState.selectedMapId == mapIdToDelete) {
                updatedMaps.firstOrNull()?.id ?: ""
            } else {
                currentState.selectedMapId
            }
            currentState.copy(
                maps = updatedMaps,
                selectedMapId = nextSelectedId,
                isDeleteConfirmDialogOpen = false,
                editingMapId = null
            )
        }

        viewModelScope.launch {
            robotRepository.deleteMap(mapIdToDelete)
        }
    }
}
