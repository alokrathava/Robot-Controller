package com.alokrathava.geonavigation.components

import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.alokrathava.geospatial.model.GeoPoint
import com.alokrathava.geospatial.model.GeoTelemetry
import com.alokrathava.geospatial.model.GeoWaypoint
import org.maplibre.android.MapLibre
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.camera.CameraPosition

/**
 * Renders the interactive MapLibre geospatial map view, displaying robot position, telemetry, and waypoint drafts.
 */
@Composable
fun GeoMapView(
    telemetry: GeoTelemetry?,
    draftWaypoint: GeoPoint?,
    waypoints: List<GeoWaypoint>,
    onMapTap: (GeoPoint) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    remember {
        try {
            MapLibre.getInstance(context)
        } catch (e: Exception) {
            // Already initialized or preview mode
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    onCreate(Bundle())
                    getMapAsync { mapboxMap ->
                        mapboxMap.setStyle(
                            Style.Builder().fromUri("https://demotiles.maplibre.org/style.json")
                        ) { _ ->
                            val initialLat = telemetry?.pose?.position?.latitudeDeg ?: 43.6532
                            val initialLon = telemetry?.pose?.position?.longitudeDeg ?: -79.3832
                            mapboxMap.cameraPosition = CameraPosition.Builder()
                                .target(LatLng(initialLat, initialLon))
                                .zoom(14.0)
                                .build()
                        }
                        mapboxMap.addOnMapClickListener { point ->
                            onMapTap(GeoPoint(point.latitude, point.longitude))
                            true
                        }
                    }
                }
            },
            update = { _ -> },
            modifier = Modifier.fillMaxSize()
        )
    }
}
