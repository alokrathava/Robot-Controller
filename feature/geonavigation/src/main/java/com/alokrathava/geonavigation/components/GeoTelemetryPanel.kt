package com.alokrathava.geonavigation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.alokrathava.geospatial.model.GeoTelemetry
import com.alokrathava.geospatial.model.VehicleCategory

/**
 * Displays vehicle telemetry, GPS status, and vehicle category information.
 */
@Composable
fun GeoTelemetryPanel(
    telemetry: GeoTelemetry?,
    vehicleCategory: VehicleCategory,
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
                text = "Vehicle Telemetry ($vehicleCategory)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            val lat = telemetry?.pose?.position?.latitudeDeg ?: 0.0
            val lon = telemetry?.pose?.position?.longitudeDeg ?: 0.0
            val alt = telemetry?.pose?.position?.vertical?.meters ?: 0.0
            val status = telemetry?.localizationStatus?.name ?: "UNAVAILABLE"

            TelemetryRow(label = "Latitude", value = String.format("%.6f°", lat))
            TelemetryRow(label = "Longitude", value = String.format("%.6f°", lon))
            TelemetryRow(label = "Altitude", value = String.format("%.2f m", alt))
            TelemetryRow(label = "Localization", value = status)
        }
    }
}

@Composable
private fun TelemetryRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}
