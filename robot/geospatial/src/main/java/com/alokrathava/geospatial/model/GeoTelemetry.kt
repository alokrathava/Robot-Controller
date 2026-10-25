package com.alokrathava.geospatial.model

import kotlinx.serialization.Serializable

/**
 * Localization status of the geospatial telemetry source.
 */
@Serializable
enum class GeoLocalizationStatus {
    UNAVAILABLE,
    ACQUIRING,
    VALID,
    DEGRADED,
    STALE
}

/**
 * Represents geographic telemetry data for the vehicle.
 */
@Serializable
data class GeoTelemetry(
    val pose: GeoPose,
    val localizationStatus: GeoLocalizationStatus = GeoLocalizationStatus.VALID,
    val source: String = "gnss_ins",
    val horizontalUncertaintyMeters: Double = 1.0,
    val verticalUncertaintyMeters: Double = 1.5,
    val freshnessTimestampMs: Long = System.currentTimeMillis()
)
