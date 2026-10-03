package com.agrathava.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agrathava.sdk.RobotRepository
import com.agrathava.sdk.model.ConnectionStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val robotRepository: RobotRepository
) : ViewModel() {

    private val _userSettingsState = MutableStateFlow(
        SettingsUiState(
            robotIpAddress = "192.168.1.108",
            port = "6080",
            autoConnect = true,
            reconnectionAttempts = 3,
            connectionTimeoutSeconds = 10,
            connectionStatus = ConnectionStatus.CONNECTED,
            connectionAddress = "192.168.1.108:8080",
            latencyMs = 12,
            lastConnected = "Sep 30, 2026 9:41 AM"
        )
    )

    val uiState: StateFlow<SettingsUiState> = combine(
        _userSettingsState,
        robotRepository.connectionStatus,
        robotRepository.connectionConfig
    ) { localState, connStatus, connConfig ->
        val effectiveStatus = if (connStatus == ConnectionStatus.CONNECTED) {
            ConnectionStatus.CONNECTED
        } else {
            localState.connectionStatus
        }
        localState.copy(
            connectionStatus = effectiveStatus,
            connectionAddress = if (connConfig.ipAddress.isNotEmpty()) "${connConfig.ipAddress}:${connConfig.port}" else "${localState.robotIpAddress}:${localState.port}"
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
