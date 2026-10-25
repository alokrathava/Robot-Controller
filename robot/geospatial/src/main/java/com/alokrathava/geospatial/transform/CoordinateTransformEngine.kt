package com.alokrathava.geospatial.transform

import com.alokrathava.geospatial.model.GeoPoint
import com.alokrathava.geospatial.model.GeoReference
import com.alokrathava.geospatial.model.VerticalReference
import com.alokrathava.geospatial.model.VerticalValue
import kotlin.math.*

/**
 * Engine providing coordinate transformations between WGS84 geographic, ECEF, and local ENU frames.
 */
object CoordinateTransformEngine {

    // WGS84 ellipsoid constants
    private const val SEMI_MAJOR_AXIS_A = 6378137.0 // meters
    private const val FLATTENING_F = 1.0 / 298.257223563
    private const val SEMI_MINOR_AXIS_B = SEMI_MAJOR_AXIS_A * (1.0 - FLATTENING_F)
    private const val ECCENTRICITY_SQUARED = 2.0 * FLATTENING_F - FLATTENING_F * FLATTENING_F

    /**
     * Converts WGS84 geographic coordinates (latitude, longitude, altitude) to ECEF (X, Y, Z) in meters.
     */
    fun geodeticToEcef(latitudeDeg: Double, longitudeDeg: Double, altitudeMeters: Double): Triple<Double, Double, Double> {
        val latRad = Math.toRadians(latitudeDeg)
        val lonRad = Math.toRadians(longitudeDeg)
        val sinLat = sin(latRad)
        val cosLat = cos(latRad)
        val sinLon = sin(lonRad)
        val cosLon = cos(lonRad)

        val chi = sqrt(1.0 - ECCENTRICITY_SQUARED * sinLat * sinLat)
        val n = SEMI_MAJOR_AXIS_A / chi

        val x = (n + altitudeMeters) * cosLat * cosLon
        val y = (n + altitudeMeters) * cosLat * sinLon
        val z = (n * (1.0 - ECCENTRICITY_SQUARED) + altitudeMeters) * sinLat

        return Triple(x, y, z)
    }

    /**
     * Converts ECEF (X, Y, Z) coordinates in meters to WGS84 geographic coordinates (lat, lon, alt).
     */
    fun ecefToGeodetic(x: Double, y: Double, z: Double): GeoPoint {
        val p = sqrt(x * x + y * y)
        if (p < 1e-6) {
            // Near pole case
            val lat = if (z > 0) 90.0 else -90.0
            return GeoPoint(lat, 0.0, VerticalValue(abs(z) - SEMI_MINOR_AXIS_B))
        }

        val lonRad = atan2(y, x)
        val e2 = ECCENTRICITY_SQUARED
        
        val latRadInit = atan2(z, p * (1.0 - e2))
        var latRad = latRadInit
        var alt = 0.0
        
        // Iterative refinement for latitude
        for (step in 0..5) {
            val sinLat = sin(latRad)
            val n = SEMI_MAJOR_AXIS_A / sqrt(1.0 - e2 * sinLat * sinLat)
            alt = p / cos(latRad) - n
            latRad = atan2(z, p * (1.0 - e2 * (n / (n + alt))))
        }

        return GeoPoint(
            latitudeDeg = Math.toDegrees(latRad),
            longitudeDeg = Math.toDegrees(lonRad),
            vertical = VerticalValue(alt, VerticalReference.WGS84_ELLIPSOIDAL)
        )
    }

    /**
     * Converts a local ENU (East, North, Up) offset relative to a reference origin into WGS84 geographic coordinates.
     */
    fun enuToGeodetic(
        eastMeters: Double,
        northMeters: Double,
        upMeters: Double,
        origin: GeoPoint
    ): GeoPoint {
        val originAlt = origin.vertical?.meters ?: 0.0
        val (x0, y0, z0) = geodeticToEcef(origin.latitudeDeg, origin.longitudeDeg, originAlt)

        val latRad = Math.toRadians(origin.latitudeDeg)
        val lonRad = Math.toRadians(origin.longitudeDeg)
        val sinLat = sin(latRad)
        val cosLat = cos(latRad)
        val sinLon = sin(lonRad)
        val cosLon = cos(lonRad)

        // Rotation matrix from ENU to ECEF
        val dx = -sinLon * eastMeters - sinLat * cosLon * northMeters + cosLat * cosLon * upMeters
        val dy =  cosLon * eastMeters - sinLat * sinLon * northMeters + cosLat * sinLon * upMeters
        val dz =  cosLat * northMeters + sinLat * upMeters

        return ecefToGeodetic(x0 + dx, y0 + dy, z0 + dz)
    }

    /**
     * Converts local map X/Y coordinates to a WGS84 GeoPoint using a [GeoReference].
     */
    fun localMapToGeodetic(mapX: Double, mapY: Double, reference: GeoReference): GeoPoint {
        val yawRad = Math.toRadians(reference.originOrientationYawDeg)
        val cosYaw = cos(yawRad)
        val sinYaw = sin(yawRad)

        val east = mapX * cosYaw - mapY * sinYaw
        val north = mapX * sinYaw + mapY * cosYaw

        return enuToGeodetic(east, north, 0.0, reference.origin)
    }
}
