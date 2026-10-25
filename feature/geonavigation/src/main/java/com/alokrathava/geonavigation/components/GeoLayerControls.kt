package com.alokrathava.geonavigation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.alokrathava.geonavigation.presentation.NavigationMode

/**
 * Controls for switching between World Map, Indoor Map, and 3D Spatial navigation modes.
 */
@Composable
fun GeoLayerControls(
    activeMode: NavigationMode,
    onModeSelected: (NavigationMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        NavigationMode.entries.forEach { mode ->
            val selected = mode == activeMode
            FilterChip(
                selected = selected,
                onClick = { onModeSelected(mode) },
                label = {
                    Text(
                        text = when (mode) {
                            NavigationMode.WORLD_MAP -> "World Map"
                            NavigationMode.INDOOR_MAP -> "Indoor Map"
                            NavigationMode.SPATIAL_3D -> "3D Spatial"
                        }
                    )
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}
