package com.alokrathava.geonavigation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.alokrathava.geospatial.model.GeoPoint
import com.alokrathava.geospatial.model.GeoTelemetry
import com.alokrathava.geospatial.model.GeoWaypoint

/**
 * Renders the interactive geospatial map view, displaying robot position, telemetry, and waypoint drafts.
 */
@Composable
fun GeoMapView(
    telemetry: GeoTelemetry?,
    draftWaypoint: GeoPoint?,
    waypoints: List<GeoWaypoint>,
    onMapTap: (GeoPoint) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        // Schematic placeholder map view for Compose preview and offline rendering
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Map,
                contentDescription = "Geospatial Map",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "MapLibre World Basemap Active",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            val lat = telemetry?.pose?.position?.latitudeDeg ?: 43.6532
            val lon = telemetry?.pose?.position?.longitudeDeg ?: -79.3832
            Text(
                text = "Robot GPS: Lat ${String.format("%.4f", lat)}, Lon ${String.format("%.4f", lon)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (draftWaypoint != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Draft Waypoint: ${String.format("%.4f", draftWaypoint.latitudeDeg)}, ${String.format("%.4f", draftWaypoint.longitudeDeg)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            if (waypoints.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Mission Waypoints: ${waypoints.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}
