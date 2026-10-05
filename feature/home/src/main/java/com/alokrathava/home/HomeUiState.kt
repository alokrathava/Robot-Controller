package com.alokrathava.home

import com.alokrathava.sdk.model.BatteryStatus
import com.alokrathava.sdk.model.ConnectionStatus
import com.alokrathava.sdk.model.DiscoveredRobot
import com.alokrathava.sdk.model.MapData
import com.alokrathava.sdk.model.NavigationStatus
import com.alokrathava.sdk.model.RobotPosition
import com.alokrathava.sdk.model.WifiNetwork

data class HomeUiState(
    val batteryStatus: BatteryStatus = BatteryStatus(),
    val position: RobotPosition = RobotPosition(),
    val navigationStatus: NavigationStatus = NavigationStatus.IDLE,
    val mapData: MapData = MapData(),
    val statusMessage: String = "Ready",
    val screenFlow: ScreenFlow = ScreenFlow.DASHBOARD,
    val connectionStep: RobotConnectionStep = RobotConnectionStep.AUTO_DISCOVERY,
    val discoveredRobots: List<DiscoveredRobot> = emptyList(),
    val selectedDiscoveredRobot: DiscoveredRobot? = null,
    val isDiscoveringRobots: Boolean = false,
    val availableNetworks: List<WifiNetwork> = emptyList(),
    val selectedNetwork: WifiNetwork? = null,
    val isScanningNetworks: Boolean = false,
    val networkScanError: String? = null,
    val ipAddress: String = "192.168.1.100",
    val port: String = "8080",
    val token: String = "",
    val useTls: Boolean = false,
    val ipError: String? = null,
    val portError: String? = null,
    val tokenError: String? = null,
    val connectionErrorMessage: String? = null,
    val connectionStatus: ConnectionStatus = ConnectionStatus.DISCONNECTED,
    val isEmergencyStopped: Boolean = false,
    val isLowBatteryWarning: Boolean = false
) {
    val batteryTimeRemainingFormatted: String
        get() = if (batteryStatus.isCharging) {
            "Charging..."
        } else {
            val totalMins = (batteryStatus.levelPercent * 1.8).toInt()
            val hours = totalMins / 60
            val mins = totalMins % 60
            "${hours}h ${mins}m remaining"
        }
}
