package com.alokrathava.geonavigation.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.alokrathava.geonavigation.components.GeoLayerControls
import com.alokrathava.geonavigation.components.GeoMapView
import com.alokrathava.geonavigation.components.GeoMissionPanel
import com.alokrathava.geonavigation.components.GeoTelemetryPanel
import com.alokrathava.geospatial.model.GeoPoint

/**
 * Main screen for geospatial navigation, combining layer controls, interactive map view, telemetry, and mission panel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeoNavigationScreen(
    viewModel: GeoNavigationViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Geospatial Mission Control") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            GeoLayerControls(
                activeMode = uiState.activeMode,
                onModeSelected = { viewModel.setNavigationMode(it) }
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                GeoMapView(
                    telemetry = uiState.telemetry,
                    draftWaypoint = uiState.draftWaypoint,
                    waypoints = uiState.waypoints,
                    onMapTap = { point -> viewModel.setDraftWaypoint(point) },
                    modifier = Modifier.fillMaxSize()
                )

                // Quick tap mock button to drop a sample draft waypoint for testing/demo
                Button(
                    onClick = {
                        viewModel.setDraftWaypoint(GeoPoint(43.6532, -79.3832))
                    },
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.TopEnd)
                ) {
                    Text("Drop Pin")
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GeoTelemetryPanel(
                    telemetry = uiState.telemetry,
                    vehicleCategory = uiState.vehicleCategory
                )

                GeoMissionPanel(
                    draftWaypoint = uiState.draftWaypoint,
                    waypoints = uiState.waypoints,
                    isNavigating = uiState.isNavigating,
                    onAddWaypoint = { viewModel.addWaypointFromDraft() },
                    onClearDrafts = { viewModel.clearDrafts() },
                    onStartNavigation = { viewModel.startGeoNavigation() },
                    onCancelNavigation = { viewModel.cancelGeoNavigation() }
                )
            }
        }
    }
}
