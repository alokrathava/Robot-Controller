package com.alokrathava.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alokrathava.sdk.RobotRepository
import com.alokrathava.sdk.model.ConnectionStatus
import com.alokrathava.sdk.model.MotionLimits
import com.alokrathava.sdk.model.RobotSimulationConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val robotRepository: RobotRepository
) : ViewModel() {

    private val _userSettingsState = MutableStateFlow(
        SettingsUiState(
            robotIpAddress = "192.168.1.100",
            port = "8080",
            autoConnect = true,
            reconnectionAttempts = 3,
            connectionTimeoutSeconds = 10,
            connectionStatus = ConnectionStatus.CONNECTED,
            connectionAddress = "192.168.1.100:8080",
            latencyMs = 12,
            lastConnected = "Sep 30, 2026 9:41 AM"
        )
    )

    val uiState: StateFlow<SettingsUiState> = combine(
        _userSettingsState,
        combine(
            robotRepository.connectionStatus,
            robotRepository.connectionConfig,
            robotRepository.simulationConfig,
            robotRepository.telemetry
        ) { connStatus, connConfig, simConfig, telem -> Quad(connStatus, connConfig, simConfig, telem) }
    ) { localState, (connStatus, connConfig, simConfig, telem) ->
        val effectiveStatus = if (connStatus == ConnectionStatus.CONNECTED) {
            ConnectionStatus.CONNECTED
        } else {
            localState.connectionStatus
        }
        localState.copy(
            connectionStatus = effectiveStatus,
            connectionAddress = if (connConfig.ipAddress.isNotEmpty()) "${connConfig.ipAddress}:${connConfig.port}" else "${localState.robotIpAddress}:${localState.port}",
            simulationConfig = simConfig,
            telemetry = telem
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = _userSettingsState.value
    )

    fun selectTab(tab: SettingsTab) {
        _userSettingsState.update { it.copy(selectedTab = tab) }
    }

    fun onRobotIpAddressChanged(ip: String) {
        _userSettingsState.update { it.copy(robotIpAddress = ip) }
    }

    fun onPortChanged(port: String) {
        _userSettingsState.update { it.copy(port = port) }
    }

    fun onAutoConnectToggled(autoConnect: Boolean) {
        _userSettingsState.update { it.copy(autoConnect = autoConnect) }
    }

    fun onReconnectionAttemptsChanged(attempts: Int) {
        _userSettingsState.update { it.copy(reconnectionAttempts = attempts) }
    }

    fun onConnectionTimeoutChanged(timeoutSeconds: Int) {
        _userSettingsState.update { it.copy(connectionTimeoutSeconds = timeoutSeconds) }
    }

    fun updateMovementSpeed(speedMps: Double) {
        val currentConfig = robotRepository.simulationConfig.value
        robotRepository.updateSimulationConfig(currentConfig.copy(movementSpeedMps = speedMps))
        viewModelScope.launch {
            robotRepository.updateMotionLimits(
                MotionLimits(maxLinearVelocityMps = speedMps, maxAngularVelocityRadPerSec = 1.5)
            )
        }
    }

    fun updateDischargeRate(rate: Double) {
        val currentConfig = robotRepository.simulationConfig.value
        robotRepository.updateSimulationConfig(currentConfig.copy(simulatedDischargeRatePercentPerMin = rate))
    }

    fun toggleSimulateObstacles(enable: Boolean) {
        val currentConfig = robotRepository.simulationConfig.value
        robotRepository.updateSimulationConfig(currentConfig.copy(simulateObstacles = enable))
    }

    fun resetEmergencyStop() {
        robotRepository.resetEmergencyStop()
    }

    fun toggleConnection() {
        val currentState = _userSettingsState.value
        if (currentState.isConnected) {
            robotRepository.disconnectRobot()
            _userSettingsState.update {
                it.copy(
                    connectionStatus = ConnectionStatus.DISCONNECTED
                )
            }
        } else {
            val portInt = currentState.port.toIntOrNull() ?: 8080
            robotRepository.connectToRobot(currentState.robotIpAddress, portInt, "")
            _userSettingsState.update {
                it.copy(
                    connectionStatus = ConnectionStatus.CONNECTED
                )
            }
        }
    }
}
