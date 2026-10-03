package com.agrathava.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agrathava.sdk.RobotRepository
import com.agrathava.sdk.model.ConnectionStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    }

    private fun observeRepository() {
        viewModelScope.launch {
            robotRepository.connectionStatus.collect { status ->
                _uiState.update { currentState ->
                    currentState.copy(
                        isConnected = (status == ConnectionStatus.CONNECTED)
                    )
                }
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
        robotRepository.saveMap()
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
    }
}
