package com.alokrathava.geospatial.model

import kotlinx.serialization.Serializable

/**
 * Supported vehicle categories for navigation and telemetry display.
 */
@Serializable
enum class VehicleCategory {
    UGV,
    UAV,
    UUV,
    SPACE_VEHICLE,
    OTHER
}
