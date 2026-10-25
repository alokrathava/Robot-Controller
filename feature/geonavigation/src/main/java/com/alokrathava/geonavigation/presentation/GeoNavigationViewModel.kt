package com.alokrathava.geonavigation.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alokrathava.geospatial.model.GeoPoint
import com.alokrathava.geospatial.model.GeoWaypoint
import com.alokrathava.sdk.RobotClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel managing geospatial navigation state, map modes, waypoint drafting, and SDK command dispatch.
 */
@HiltViewModel
class GeoNavigationViewModel @Inject constructor(
    private val robotClient: RobotClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(GeoNavigationUiState())
    val uiState: StateFlow<GeoNavigationUiState> = _uiState.asStateFlow()

    init {
        observeRobotState()
    }

    /** Observes connection state, telemetry, and vehicle category from RobotClient. */
    private fun observeRobotState() {
        viewModelScope.launch {
            robotClient.connectionState.collect { state ->
                _uiState.update { it.copy(isConnected = state is com.alokrathava.sdk.model.ConnectionState.Connected) }
            }
        }
        viewModelScope.launch {
            robotClient.geoTelemetry.collect { telemetry ->
                _uiState.update { it.copy(telemetry = telemetry) }
            }
        }
        viewModelScope.launch {
            robotClient.vehicleCategory.collect { category ->
                if (category != null) {
                    _uiState.update { it.copy(vehicleCategory = category) }
                }
            }
        }
    }

    /** Switches between World Map, Indoor Map, and 3D Spatial navigation modes. */
    fun setNavigationMode(mode: NavigationMode) {
        _uiState.update { it.copy(activeMode = mode) }
    }

    /** Sets a draft waypoint on the map for user review before navigation. */
    fun setDraftWaypoint(point: GeoPoint) {
        _uiState.update { it.copy(draftWaypoint = point) }
    }

    /** Adds the current draft waypoint to the active mission route. */
    fun addWaypointFromDraft() {
        val draft = _uiState.value.draftWaypoint ?: return
        val newWaypoint = GeoWaypoint(
            waypointId = UUID.randomUUID().toString(),
            target = draft
        )
        _uiState.update {
            it.copy(
                waypoints = it.waypoints + newWaypoint,
                draftWaypoint = null
            )
        }
    }

    /** Clears all drafted waypoints. */
    fun clearDrafts() {
        _uiState.update { it.copy(waypoints = emptyList(), draftWaypoint = null) }
    }

    /** Submits the drafted waypoints to the robot via SDK. */
    fun startGeoNavigation() {
        val waypoints = _uiState.value.waypoints
        if (waypoints.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isNavigating = true, errorMessage = null) }
            val result = if (waypoints.size == 1) {
                robotClient.navigateToGeo(waypoints.first())
            } else {
                robotClient.navigateThroughGeo(waypoints)
            }
            when (result) {
                is com.alokrathava.sdk.error.RobotResult.Success -> {
                    _uiState.update { it.copy(isNavigating = true) }
                }
                is com.alokrathava.sdk.error.RobotResult.Failure -> {
                    _uiState.update { it.copy(isNavigating = false, errorMessage = result.error.message) }
                }
            }
        }
    }

    /** Cancels active geospatial navigation. */
    fun cancelGeoNavigation() {
        viewModelScope.launch {
            robotClient.cancelGeoNavigation()
            _uiState.update { it.copy(isNavigating = false) }
        }
    }
}
