package com.alokrathava.geospatial.model

import kotlinx.serialization.Serializable

/**
 * Represents a geographic waypoint for mission planning and navigation.
 */
@Serializable
data class GeoWaypoint(
    val waypointId: String,
    val target: GeoPoint,
    val desiredHeadingDeg: Double? = null,
    val arrivalToleranceMeters: Double = 1.0,
    val constraints: Map<String, String> = emptyMap()
)
