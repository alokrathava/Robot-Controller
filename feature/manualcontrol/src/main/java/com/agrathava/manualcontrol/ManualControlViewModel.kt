package com.agrathava.manualcontrol

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agrathava.sdk.RobotRepository
import com.agrathava.sdk.model.DirectionCommand
import com.agrathava.sdk.model.RobotSimulationConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

private data class LocalControlsState(
    val tab: ManualControlTab,
    val speed: Float,
    val lastCmd: DirectionCommand?
)

@HiltViewModel
class ManualControlViewModel @Inject constructor(
    private val robotRepository: RobotRepository
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(ManualControlTab.MOVEMENT)
    private val _speedPercent = MutableStateFlow(60f)
    private val _lastCommand = MutableStateFlow<DirectionCommand?>(null)

    private val _controlsState = combine(_selectedTab, _speedPercent, _lastCommand) { tab, speed, lastCmd ->
        LocalControlsState(tab, speed, lastCmd)
    }

    val uiState: StateFlow<ManualControlUiState> = combine(
        _controlsState,
        robotRepository.telemetry,
        robotRepository.position,
        robotRepository.batteryStatus,
        robotRepository.connectionStatus
    ) { controls, telemetry, position, battery, connStatus ->
        ManualControlUiState(
            selectedTab = controls.tab,
            speedPercent = controls.speed,
            isEmergencyStopped = telemetry.isEmergencyStopped,
            lastCommand = controls.lastCmd,
            robotPosition = position,
            batteryStatus = battery,
            connectionStatus = connStatus,
            telemetry = telemetry,
            statusMessage = when {
                telemetry.isEmergencyStopped -> "EMERGENCY BRAKE ENGAGED!"
                controls.lastCmd != null -> "Moving ${controls.lastCmd.name.lowercase()} at ${controls.speed.toInt()}% speed"
                else -> "Ready for manual control"
            }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ManualControlUiState()
    )

    fun selectTab(tab: ManualControlTab) {
        _selectedTab.value = tab
    }

    fun updateSpeed(speed: Float) {
        _speedPercent.value = speed
        val speedMps = (speed / 100f) * 1.5
        robotRepository.updateSimulationConfig(
            RobotSimulationConfig(movementSpeedMps = speedMps)
        )
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
