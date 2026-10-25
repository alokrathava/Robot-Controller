package com.alokrathava.geonavigation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.alokrathava.geospatial.model.GeoPoint
import com.alokrathava.geospatial.model.GeoWaypoint

/**
 * Panel for managing mission waypoints, previewing routes, and dispatching navigation commands.
 */
@Composable
fun GeoMissionPanel(
    draftWaypoint: GeoPoint?,
    waypoints: List<GeoWaypoint>,
    isNavigating: Boolean,
    onAddWaypoint: () -> Unit,
    onClearDrafts: () -> Unit,
    onStartNavigation: () -> Unit,
    onCancelNavigation: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Geospatial Mission Control",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Draft Waypoints: ${waypoints.size}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onAddWaypoint,
                    enabled = draftWaypoint != null,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Add Waypoint")
                }
                OutlinedButton(
                    onClick = onClearDrafts,
                    enabled = waypoints.isNotEmpty() || draftWaypoint != null,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Clear")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (!isNavigating) {
                Button(
                    onClick = onStartNavigation,
                    enabled = waypoints.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Start Navigation")
                }
            } else {
                Button(
                    onClick = onCancelNavigation,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel Navigation")
                }
            }
        }
    }
}
