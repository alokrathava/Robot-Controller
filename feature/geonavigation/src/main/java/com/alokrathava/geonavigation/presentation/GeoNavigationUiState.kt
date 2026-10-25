package com.alokrathava.geonavigation.presentation

import com.alokrathava.geospatial.model.GeoPoint
import com.alokrathava.geospatial.model.GeoTelemetry
import com.alokrathava.geospatial.model.GeoWaypoint
import com.alokrathava.geospatial.model.VehicleCategory

/**
 * UI State for the geospatial navigation screen, supporting multiple navigation modes and vehicle telemetry.
 */
data class GeoNavigationUiState(
    val activeMode: NavigationMode = NavigationMode.WORLD_MAP,
    val vehicleCategory: VehicleCategory = VehicleCategory.UGV,
    val telemetry: GeoTelemetry? = null,
    val draftWaypoint: GeoPoint? = null,
    val waypoints: List<GeoWaypoint> = emptyList(),
    val isConnected: Boolean = false,
    val isNavigating: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Navigation display modes available in the controller.
 */
enum class NavigationMode {
    WORLD_MAP,
    INDOOR_MAP,
    SPATIAL_3D
}
