package com.alokrathava.geospatial.model

import kotlinx.serialization.Serializable

/**
 * Represents a geographic coordinate point with latitude, longitude, and optional vertical value.
 */
@Serializable
data class GeoPoint(
    val latitudeDeg: Double,
    val longitudeDeg: Double,
    val vertical: VerticalValue? = null
) {
    init {
        require(!latitudeDeg.isNaN() && !latitudeDeg.isInfinite()) { "Latitude must not be NaN or infinite" }
        require(!longitudeDeg.isNaN() && !longitudeDeg.isInfinite()) { "Longitude must not be NaN or infinite" }
        require(latitudeDeg in -90.0..90.0) { "Latitude must be between -90 and 90 degrees" }
        require(longitudeDeg in -180.0..180.0) { "Longitude must be between -180 and 180 degrees" }
    }
}
