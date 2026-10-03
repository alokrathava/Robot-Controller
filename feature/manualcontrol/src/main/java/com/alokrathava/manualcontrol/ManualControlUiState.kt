package com.alokrathava.manualcontrol

import com.alokrathava.sdk.model.BatteryStatus
import com.alokrathava.sdk.model.ConnectionStatus
import com.alokrathava.sdk.model.DirectionCommand
import com.alokrathava.sdk.model.RobotPosition
import com.alokrathava.sdk.model.RobotTelemetry
import com.alokrathava.sdk.model.ThermalState

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
    val connectionAddress: String = "192.168.1.100:8080",
    val telemetry: RobotTelemetry = RobotTelemetry(),
    val statusMessage: String = "Ready for manual control"
)
