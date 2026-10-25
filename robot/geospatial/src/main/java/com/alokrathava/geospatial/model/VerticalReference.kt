package com.alokrathava.geospatial.model

import kotlinx.serialization.Serializable

/**
 * Defines the vertical reference datum for height or depth values.
 */
@Serializable
enum class VerticalReference {
    WGS84_ELLIPSOIDAL,
    MEAN_SEA_LEVEL,
    ABOVE_GROUND_LEVEL,
    DEPTH_BELOW_WATER_SURFACE
}

/**
 * Represents a vertical value with an explicitly identified vertical reference.
 */
@Serializable
data class VerticalValue(
    val meters: Double,
    val reference: VerticalReference = VerticalReference.WGS84_ELLIPSOIDAL
) {
    init {
        require(!meters.isNaN() && !meters.isInfinite()) { "Vertical value must not be NaN or infinite" }
    }
}
