package com.alokrathava.geospatial.model

import kotlinx.serialization.Serializable

/**
 * Represents orientation in 3D space using quaternion components (x, y, z, w).
 */
@Serializable
data class Quaternion(
    val x: Double = 0.0,
    val y: Double = 0.0,
    val z: Double = 0.0,
    val w: Double = 1.0
)

/**
 * Represents a geographic pose including position, optional orientation, reference frame, and timestamp.
 */
@Serializable
data class GeoPose(
    val position: GeoPoint,
    val orientation: Quaternion? = null,
    val referenceFrame: String = "wgs84_enu",
    val timestampMs: Long = System.currentTimeMillis(),
    val isValid: Boolean = true,
    val positionUncertaintyMeters: Double? = null
)
