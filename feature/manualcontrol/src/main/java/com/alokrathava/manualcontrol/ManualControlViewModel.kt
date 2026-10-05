package com.alokrathava.manualcontrol

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alokrathava.sdk.RobotRepository
import com.alokrathava.sdk.model.ConnectionStatus
import com.alokrathava.sdk.model.DirectionCommand
import com.alokrathava.sdk.model.MotionLimits
import com.alokrathava.sdk.model.RobotSimulationConfig
import com.alokrathava.sdk.model.ThermalState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

const val MAX_LINEAR_VELOCITY_MPS = 0.5f
const val MAX_ANGULAR_VELOCITY_RAD_PER_SEC = 1.0f

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

    private var velocityStreamJob: Job? = null
    @Volatile private var targetLinear: Double = 0.0
    @Volatile private var targetAngular: Double = 0.0

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

        val speedFactor = (speed / 100f).coerceIn(0f, 1f)
        val maxLinear = speedFactor * MAX_LINEAR_VELOCITY_MPS
        val maxAngular = speedFactor * MAX_ANGULAR_VELOCITY_RAD_PER_SEC

        robotRepository.updateSimulationConfig(
            RobotSimulationConfig(movementSpeedMps = maxLinear.toDouble())
        )
        viewModelScope.launch {
            robotRepository.updateMotionLimits(
                MotionLimits(
                    maxLinearVelocityMps = maxLinear.toDouble(),
                    maxAngularVelocityRadPerSec = maxAngular.toDouble()
                )
            )
        }

        val js = _joystickState.value
        if (js.isActive) {
            val linear = -js.y * maxLinear
            val angular = -js.x * maxAngular
            startVelocityStreaming(linear.toDouble(), angular.toDouble())
        } else if (_lastCommand.value != null) {
            val cmd = _lastCommand.value
            val (linear, angular) = when (cmd) {
                DirectionCommand.FORWARD -> Pair(maxLinear.toDouble(), 0.0)
                DirectionCommand.BACKWARD -> Pair(-maxLinear.toDouble(), 0.0)
                DirectionCommand.LEFT -> Pair(0.0, maxAngular.toDouble())
                DirectionCommand.RIGHT -> Pair(0.0, -maxAngular.toDouble())
                null -> Pair(0.0, 0.0)
            }
            startVelocityStreaming(linear, angular)
        }
    }

    fun updateJoystickPosition(x: Float, y: Float, isActive: Boolean) {
        _joystickState.value = JoystickState(x, y, isActive)
        if (!isActive) {
            _lastCommand.value = null
            stopVelocityStreaming()
            return
        }

        val speedFactor = (_speedPercent.value / 100f).coerceIn(0f, 1f)
        val maxLinear = speedFactor * MAX_LINEAR_VELOCITY_MPS
        val maxAngular = speedFactor * MAX_ANGULAR_VELOCITY_RAD_PER_SEC

        val linear = -y * maxLinear
        val angular = -x * maxAngular

        val direction = when {
            y < -0.3f -> DirectionCommand.FORWARD
            y > 0.3f -> DirectionCommand.BACKWARD
            x < -0.3f -> DirectionCommand.LEFT
            x > 0.3f -> DirectionCommand.RIGHT
            else -> null
        }
        _lastCommand.value = direction

        startVelocityStreaming(linear.toDouble(), angular.toDouble())
    }

    fun startDirectionHold(direction: ManualDirection) {
        val speedFactor = (_speedPercent.value / 100f).coerceIn(0f, 1f)
        val maxLinear = speedFactor * MAX_LINEAR_VELOCITY_MPS
        val maxAngular = speedFactor * MAX_ANGULAR_VELOCITY_RAD_PER_SEC

        val (command, linear, angular) = when (direction) {
            ManualDirection.FORWARD -> Triple(DirectionCommand.FORWARD, maxLinear.toDouble(), 0.0)
            ManualDirection.BACKWARD -> Triple(DirectionCommand.BACKWARD, -maxLinear.toDouble(), 0.0)
            ManualDirection.LEFT -> Triple(DirectionCommand.LEFT, 0.0, maxAngular.toDouble())
            ManualDirection.RIGHT -> Triple(DirectionCommand.RIGHT, 0.0, -maxAngular.toDouble())
        }
        _lastCommand.value = command
        startVelocityStreaming(linear, angular)
    }

    fun stopDirectionHold() {
        _lastCommand.value = null
        stopVelocityStreaming()
    }

    fun moveDirection(direction: ManualDirection) {
        startDirectionHold(direction)
    }

    private fun startVelocityStreaming(linear: Double, angular: Double) {
        targetLinear = linear
        targetAngular = angular
        robotRepository.setManualVelocity(linear, angular)

        if (velocityStreamJob == null || velocityStreamJob?.isActive == false) {
            velocityStreamJob = viewModelScope.launch {
                while (isActive) {
                    delay(100) // 10 Hz streaming
                    robotRepository.setManualVelocity(targetLinear, targetAngular)
                }
            }
        }
    }

    private fun stopVelocityStreaming() {
        velocityStreamJob?.cancel()
        velocityStreamJob = null
        targetLinear = 0.0
        targetAngular = 0.0
        robotRepository.setManualVelocity(0.0, 0.0)
    }

    fun triggerEmergencyBrake() {
        stopVelocityStreaming()
        robotRepository.triggerEmergencyStop()
    }

    fun resetEmergencyBrake() {
        robotRepository.resetEmergencyStop()
    }
}
