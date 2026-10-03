package com.agrathava.manualcontrol

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agrathava.sdk.RobotRepository
import com.agrathava.sdk.model.ConnectionStatus
import com.agrathava.sdk.model.DirectionCommand
import com.agrathava.sdk.model.RobotSimulationConfig
import com.agrathava.sdk.model.ThermalState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

private data class JoystickState(
    val x: Float = 0f,
    val y: Float = 0f,
    val isActive: Boolean = false
)

private data class LocalControlsState(
    val tab: ManualControlTab,
    val speed: Float,
    val preset: ManualControlSpeedPreset,
    val joystick: JoystickState,
    val lastCmd: DirectionCommand?
)

@HiltViewModel
class ManualControlViewModel @Inject constructor(
    private val robotRepository: RobotRepository
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(ManualControlTab.MOVEMENT)
    private val _speedPercent = MutableStateFlow(60f)
    private val _speedPreset = MutableStateFlow(ManualControlSpeedPreset.MEDIUM)
    private val _joystickState = MutableStateFlow(JoystickState())
    private val _lastCommand = MutableStateFlow<DirectionCommand?>(null)

    private val _controlsState = combine(
        _selectedTab,
        _speedPercent,
        _speedPreset,
        _joystickState,
        _lastCommand
    ) { tab, speed, preset, joystick, lastCmd ->
        LocalControlsState(tab, speed, preset, joystick, lastCmd)
    }

    private val _connectionInfoState = combine(
        robotRepository.connectionStatus,
        robotRepository.connectionConfig
    ) { connStatus, connConfig ->
        Pair(connStatus, connConfig)
    }

    val uiState: StateFlow<ManualControlUiState> = combine(
        _controlsState,
        robotRepository.telemetry,
        robotRepository.position,
        robotRepository.batteryStatus,
        _connectionInfoState
    ) { controls, telemetry, position, battery, connInfo ->
        val (connStatus, connConfig) = connInfo
        val isObstacle = telemetry.obstacleDistanceMeters < 0.8
        val isConnLost = connStatus == ConnectionStatus.DISCONNECTED || connStatus == ConnectionStatus.FAILED
        val address = "${connConfig.ipAddress}:${connConfig.port}"

        ManualControlUiState(
            selectedTab = controls.tab,
            speedPercent = controls.speed,
            speedPreset = controls.preset,
            joystickX = controls.joystick.x,
            joystickY = controls.joystick.y,
            isJoystickActive = controls.joystick.isActive,
            isEmergencyStopped = telemetry.isEmergencyStopped,
            isObstacleNear = isObstacle,
            obstacleDistanceMeters = telemetry.obstacleDistanceMeters,
            thermalWarningState = telemetry.thermalState,
            isConnectionLost = isConnLost,
            lastCommand = controls.lastCmd,
            robotPosition = position,
            batteryStatus = battery,
            connectionStatus = connStatus,
            connectionAddress = address,
            telemetry = telemetry,
            statusMessage = when {
                telemetry.isEmergencyStopped -> "EMERGENCY BRAKE ENGAGED!"
                connStatus == ConnectionStatus.CONNECTING -> "Connecting to robot..."
                isConnLost -> "Connection lost! Controls disabled."
                isObstacle -> "WARNING: Obstacle detected within ${"%.1f".format(telemetry.obstacleDistanceMeters)}m"
                telemetry.thermalState == ThermalState.CRITICAL -> "CRITICAL THERMAL WARNING!"
                controls.lastCmd != null -> "Moving ${controls.lastCmd.name.lowercase()} at ${controls.speed.toInt()}% speed"
                else -> "Ready for manual control"
            }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ManualControlUiState()
    )

    fun reconnect() {
        val config = robotRepository.connectionConfig.value
        val ip = config.ipAddress.ifBlank { "192.168.1.100" }
        val port = if (config.port > 0) config.port else 8080
        val ssid = config.selectedSsid ?: "ROBOT_HOTSPOT_5G"
        robotRepository.connectToRobot(ip, port, ssid)
    }

    fun selectTab(tab: ManualControlTab) {
        _selectedTab.value = tab
    }

    fun selectSpeedPreset(preset: ManualControlSpeedPreset) {
        _speedPreset.value = preset
        if (preset != ManualControlSpeedPreset.CUSTOM) {
            updateSpeed(preset.speedPercent)
        }
    }

    fun updateSpeed(speed: Float) {
        _speedPercent.value = speed
        val matchingPreset = ManualControlSpeedPreset.entries.find { it.speedPercent == speed }
            ?: ManualControlSpeedPreset.CUSTOM
        _speedPreset.value = matchingPreset

        val speedMps = (speed / 100f) * 1.5
        robotRepository.updateSimulationConfig(
            RobotSimulationConfig(movementSpeedMps = speedMps)
        )
    }

    fun updateJoystickPosition(x: Float, y: Float, isActive: Boolean) {
        _joystickState.value = JoystickState(x, y, isActive)
        if (!isActive) {
            _lastCommand.value = null
            return
        }

        val direction = when {
            y < -0.3f -> DirectionCommand.FORWARD
            y > 0.3f -> DirectionCommand.BACKWARD
            x < -0.3f -> DirectionCommand.LEFT
            x > 0.3f -> DirectionCommand.RIGHT
            else -> null
        }

        if (direction != null) {
            _lastCommand.value = direction
            robotRepository.move(direction)
        }
    }

    fun moveDirection(direction: ManualDirection) {
        val command = when (direction) {
            ManualDirection.FORWARD -> DirectionCommand.FORWARD
            ManualDirection.BACKWARD -> DirectionCommand.BACKWARD
            ManualDirection.LEFT -> DirectionCommand.LEFT
            ManualDirection.RIGHT -> DirectionCommand.RIGHT
        }
        _lastCommand.value = command
        robotRepository.move(command)
    }

    fun triggerEmergencyBrake() {
        robotRepository.triggerEmergencyStop()
    }

    fun resetEmergencyBrake() {
        robotRepository.resetEmergencyStop()
    }
}
