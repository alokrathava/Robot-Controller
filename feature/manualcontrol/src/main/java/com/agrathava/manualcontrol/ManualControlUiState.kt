package com.agrathava.manualcontrol

import com.agrathava.sdk.model.BatteryStatus
import com.agrathava.sdk.model.ConnectionStatus
import com.agrathava.sdk.model.DirectionCommand
import com.agrathava.sdk.model.RobotPosition
import com.agrathava.sdk.model.RobotTelemetry
import com.agrathava.sdk.model.ThermalState

enum class ManualControlSpeedPreset(val label: String, val speedPercent: Float) {
    LOW("Low (30%)", 30f),
    MEDIUM("Medium (60%)", 60f),
    HIGH("High (100%)", 100f),
    CUSTOM("Custom", 60f)
}

data class ManualControlUiState(
    val selectedTab: ManualControlTab = ManualControlTab.MOVEMENT,
    val speedPercent: Float = 60f,
    val speedPreset: ManualControlSpeedPreset = ManualControlSpeedPreset.MEDIUM,
    val joystickX: Float = 0f,
    val joystickY: Float = 0f,
    val isJoystickActive: Boolean = false,
    val isEmergencyStopped: Boolean = false,
    val isObstacleNear: Boolean = false,
    val obstacleDistanceMeters: Double = 2.5,
    val thermalWarningState: ThermalState = ThermalState.NORMAL,
    val isConnectionLost: Boolean = false,
    val lastCommand: DirectionCommand? = null,
    val robotPosition: RobotPosition = RobotPosition(),
    val batteryStatus: BatteryStatus = BatteryStatus(),
    val connectionStatus: ConnectionStatus = ConnectionStatus.CONNECTED,
    val telemetry: RobotTelemetry = RobotTelemetry(),
    val statusMessage: String = "Ready for manual control"
)
