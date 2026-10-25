package com.alokrathava.geonavigation.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.alokrathava.geonavigation.components.GeoTelemetryPanel
import com.alokrathava.geospatial.model.VehicleCategory
import com.alokrathava.theme.MonochromeTheme

/**
 * Compose previews for geospatial components and screens.
 */
@Preview(name = "UGV Telemetry Panel", showBackground = true)
@Composable
fun PreviewUgvTelemetryPanel() {
    MonochromeTheme {
        GeoTelemetryPanel(
            telemetry = GeoNavigationPreviewData.ugvTelemetry,
            vehicleCategory = VehicleCategory.UGV
        )
    }
}

@Preview(name = "UAV Telemetry Panel", showBackground = true)
@Composable
fun PreviewUAVTelemetryPanel() {
    MonochromeTheme {
        GeoTelemetryPanel(
            telemetry = GeoNavigationPreviewData.uavTelemetry,
            vehicleCategory = VehicleCategory.UAV
        )
    }
}
