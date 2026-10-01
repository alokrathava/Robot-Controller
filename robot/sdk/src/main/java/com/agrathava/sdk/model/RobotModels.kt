package com.agrathava.sdk.model

data class BatteryStatus(
    val levelPercent: Int = 85,
    val isCharging: Boolean = false
) {
    val displayText: String
        get() = "$levelPercent%"
}

data class RobotPosition(
    val x: Double = 0.0,
    val y: Double = 0.0
) {
    val displayText: String
        get() = "${"%.1f".format(x)}, ${"%.1f".format(y)}"
}

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
    FETCHING_MAP
}

data class MapData(
    val name: String = "Default Map",
    val isAvailable: Boolean = true
) {
    val displayText: String
        get() = if (isAvailable) name else "No map available"
}
