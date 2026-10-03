package com.alokrathava.sdk.model

enum class BatteryState {
    UNKNOWN,
    DISCHARGING,
    LOW,
    CRITICAL,
    CHARGING,
    FULL,
    FAULT
}

data class RobotBatteryState(
    val state: BatteryState,
    val percentage: Float,
    val isCharging: Boolean,
    val voltageVolts: Float,
    val currentAmps: Float? = null,
    val temperatureCelsius: Float? = null,
    val dataAgeSeconds: Float? = null,
    val detail: String? = null
)
