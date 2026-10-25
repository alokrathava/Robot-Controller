package com.alokrathava.geospatial.model

import kotlinx.serialization.Serializable

/**
 * Represents geographic reference metadata connecting local map frames to global coordinates.
 */
@Serializable
data class GeoReference(
    val referenceId: String,
    val mapId: String,
    val origin: GeoPoint,
    val localFrameId: String = "map",
    val originOrientationYawDeg: Double = 0.0,
    val accuracyMeters: Double = 0.5,
    val timestampMs: Long = System.currentTimeMillis(),
    val isValid: Boolean = true
)
