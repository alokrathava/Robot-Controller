package com.agrathava.robotstatus

import com.agrathava.sdk.model.BatteryStatus
import com.agrathava.sdk.model.ConnectionStatus
import com.agrathava.sdk.model.NavigationStatus
import com.agrathava.sdk.model.RobotPosition
import com.agrathava.sdk.model.RobotTelemetry

data class RobotStatusUiState(
    val batteryPercent: Int = 78,
    val isCharging: Boolean = false,
    val estimatedTimeRemaining: String = "Estimated 2h 15m remaining",
    val positionX: Double = 2.4,
    val positionY: Double = 5.1,
    val yawDegrees: Double = 180.0,
    val robotState: String = "Idle",
    val isConnected: Boolean = true,
    val connectionAddress: String = "192.168.1.108:8080",
    val speedMps: Double = 0.0,
    val temperatureCelsius: Double = 24.0,
    val batteryStatus: BatteryStatus = BatteryStatus(levelPercent = 78),
    val robotPosition: RobotPosition = RobotPosition(x = 2.4, y = 5.1, headingDegrees = 180.0),
    val telemetry: RobotTelemetry = RobotTelemetry(),
    val connectionStatus: ConnectionStatus = ConnectionStatus.CONNECTED,
    val navigationStatus: NavigationStatus = NavigationStatus.IDLE
)
