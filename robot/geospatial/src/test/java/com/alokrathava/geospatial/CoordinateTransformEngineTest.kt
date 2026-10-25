package com.alokrathava.geospatial

import com.alokrathava.geospatial.model.GeoPoint
import com.alokrathava.geospatial.model.GeoReference
import com.alokrathava.geospatial.transform.CoordinateTransformEngine
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for coordinate transformations between geodetic, ECEF, and local ENU frames.
 */
class CoordinateTransformEngineTest {

    @Test
    fun testGeodeticToEcefAndBack() {
        val lat = 43.6532
        val lon = -79.3832
        val alt = 100.0

        val (x, y, z) = CoordinateTransformEngine.geodeticToEcef(lat, lon, alt)
        val recovered = CoordinateTransformEngine.ecefToGeodetic(x, y, z)

        assertEquals(lat, recovered.latitudeDeg, 1e-5)
        assertEquals(lon, recovered.longitudeDeg, 1e-5)
    }

    @Test
    fun testLocalMapToGeodetic() {
        val origin = GeoPoint(43.6532, -79.3832)
        val reference = GeoReference(
            referenceId = "ref-1",
            mapId = "map-1",
            origin = origin,
            localFrameId = "map",
            originOrientationYawDeg = 0.0
        )

        val point = CoordinateTransformEngine.localMapToGeodetic(10.0, 20.0, reference)
        // Verify transformed point is close to origin but offset
        assertEquals(43.6532, point.latitudeDeg, 1e-3)
        assertEquals(-79.3832, point.longitudeDeg, 1e-3)
    }
}
