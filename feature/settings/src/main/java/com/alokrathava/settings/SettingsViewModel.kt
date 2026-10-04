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

private data class quintuple<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)

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
            connectionStatus = ConnectionStatus.DISCONNECTED,
            connectionAddress = "192.168.1.100:8080",
            latencyMs = null,
            lastConnected = "Disconnected"
        )
    )

    private val _repoStateFlow = combine(
        robotRepository.connectionStatus,
        robotRepository.connectionConfig,
        robotRepository.connectionMetrics,
        robotRepository.simulationConfig,
        robotRepository.telemetry
    ) { connStatus, connConfig, metrics, simConfig, telem ->
        quintuple(connStatus, connConfig, metrics, simConfig, telem)
    }

    val uiState: StateFlow<SettingsUiState> = combine(
        _userSettingsState,
        _repoStateFlow
    ) { localState, (connStatus, connConfig, metrics, simConfig, telem) ->
        val ip = connConfig.ipAddress.ifEmpty { localState.robotIpAddress }
        val portStr = if (connConfig.port > 0) connConfig.port.toString() else localState.port
        val address = "$ip:$portStr"

        val durationSec = metrics.connectedDurationMs / 1000
        val lastConnText = if (connStatus == ConnectionStatus.CONNECTED) {
            if (durationSec < 60) "${durationSec}s connected" else "${durationSec / 60}m connected"
        } else {
            "Disconnected"
        }

        localState.copy(
            connectionStatus = connStatus,
            robotIpAddress = ip,
            port = portStr,
            connectionAddress = address,
            latencyMs = metrics.latencyMs,
            reconnectCount = metrics.reconnectCount,
            lastMessageAgeMs = metrics.lastMessageAgeMs,
            lastConnected = lastConnText,
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

    fun onTokenChanged(token: String) {
        _userSettingsState.update { it.copy(token = token) }
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
        val currentState = uiState.value
        if (currentState.isConnected) {
            robotRepository.disconnectRobot()
        } else {
            val portInt = currentState.port.toIntOrNull() ?: 8080
            robotRepository.connectToRobot(currentState.robotIpAddress, portInt, currentState.token, "")
        }
    }
}
