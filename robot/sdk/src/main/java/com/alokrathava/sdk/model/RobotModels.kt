package com.alokrathava.sdk.model

data class BatteryStatus(
    val levelPercent: Int = 85,
    val isCharging: Boolean = false,
    val plugType: String = "Battery",
    val temperatureCelsius: Double = 25.0,
    val voltageMv: Int = 4000,
    val health: String = "Good",
    val isLowBattery: Boolean = false
) {
    val displayText: String
        get() = if (isCharging) "$levelPercent% (Charging)" else "$levelPercent%"
}

data class RobotPosition(
    val x: Double = 0.0,
    val y: Double = 0.0,
    val headingDegrees: Double = 0.0
) {
    val displayText: String
        get() = "${"%.1f".format(x)}, ${"%.1f".format(y)}"
}

enum class ThermalState {
    NORMAL,
    WARM,
    OVERHEATING,
    CRITICAL
}



data class RobotSimulationConfig(
    val movementSpeedMps: Double = 0.5,
    val simulatedDischargeRatePercentPerMin: Double = 0.1,
    val simulateObstacles: Boolean = true
)

enum class DirectionCommand {
    FORWARD,
    BACKWARD,
    LEFT,
    RIGHT
}

enum class NavigationStatus {
    IDLE,
    NAVIGATING,
    CHARGING,
    CANCELLED,
    SAVING_MAP,
    FETCHING_MAP,
    EMERGENCY_STOP
}

enum class DockingStatus {
    UNDOCKED,
    NAVIGATING_TO_DOCK,
    ALIGNING_WITH_DOCK,
    DOCKED,
    DOCKING_FAILED
}

data class DockStation(
    val id: String = "DOCK_MAIN",
    val name: String = "Primary Charging Dock",
    val position: RobotPosition = RobotPosition(x = 0.0, y = 0.0),
    val isOccupied: Boolean = false
)

data class MapData(
    val name: String = "Default Map",
    val isAvailable: Boolean = true
) {
    val displayText: String
        get() = if (isAvailable) name else "No map available"
}

data class WifiNetwork(
    val ssid: String,
    val signalPercent: Int = 80,
    val isSecured: Boolean = true,
    val isConnected: Boolean = false,
    val frequency: String = "5 GHz"
)

data class ConnectionConfig(
    val ipAddress: String = "192.168.1.100",
    val port: Int = 8080,
    val selectedSsid: String? = null
)

enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    FAILED
}
