package com.alokrathava.geonavigation.preview

import com.alokrathava.geospatial.model.*
import com.alokrathava.geonavigation.presentation.GeoNavigationUiState
import com.alokrathava.geonavigation.presentation.NavigationMode

/**
 * Provides preview data and mock states for Compose previews and demo mode.
 */
object GeoNavigationPreviewData {

    val ugvTelemetry = GeoTelemetry(
        pose = GeoPose(
            position = GeoPoint(43.6532, -79.3832, VerticalValue(125.0, VerticalReference.MEAN_SEA_LEVEL))
        ),
        localizationStatus = GeoLocalizationStatus.VALID,
        source = "RTK_GNSS"
    )

    val uavTelemetry = GeoTelemetry(
        pose = GeoPose(
            position = GeoPoint(37.7749, -122.4194, VerticalValue(150.0, VerticalReference.ABOVE_GROUND_LEVEL))
        ),
        localizationStatus = GeoLocalizationStatus.VALID,
        source = "AIRBORNE_INS"
    )

    val defaultState = GeoNavigationUiState(
        activeMode = NavigationMode.WORLD_MAP,
        vehicleCategory = VehicleCategory.UGV,
        telemetry = ugvTelemetry,
        isConnected = true
    )

    val uavState = GeoNavigationUiState(
        activeMode = NavigationMode.WORLD_MAP,
        vehicleCategory = VehicleCategory.UAV,
        telemetry = uavTelemetry,
        isConnected = true,
        draftWaypoint = GeoPoint(37.7800, -122.4200)
    )
}
