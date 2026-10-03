package com.agrathava.manualcontrol

import com.agrathava.sdk.model.BatteryStatus
import com.agrathava.sdk.model.ConnectionStatus
import com.agrathava.sdk.model.DirectionCommand
import com.agrathava.sdk.model.RobotPosition
import com.agrathava.sdk.model.RobotTelemetry

data class ManualControlUiState(
    val selectedTab: ManualControlTab = ManualControlTab.MOVEMENT,
    val speedPercent: Float = 60f,
    val isEmergencyStopped: Boolean = false,
    val lastCommand: DirectionCommand? = null,
    val robotPosition: RobotPosition = RobotPosition(),
    val batteryStatus: BatteryStatus = BatteryStatus(),
    val connectionStatus: ConnectionStatus = ConnectionStatus.CONNECTED,
    val telemetry: RobotTelemetry = RobotTelemetry(),
    val statusMessage: String = "Ready for manual control"
)
