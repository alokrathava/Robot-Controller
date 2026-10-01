package com.agrathava.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agrathava.sdk.RobotRepository
import com.agrathava.sdk.model.BatteryStatus
import com.agrathava.sdk.model.ConnectionStatus
import com.agrathava.sdk.model.DirectionCommand
import com.agrathava.sdk.model.MapData
import com.agrathava.sdk.model.NavigationStatus
import com.agrathava.sdk.model.RobotPosition
import com.agrathava.sdk.model.WifiNetwork
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val robotRepository: RobotRepository
) : ViewModel() {

    private val _connectionStep = MutableStateFlow(RobotConnectionStep.NETWORK_SELECTION)
    private val _selectedNetwork = MutableStateFlow<WifiNetwork?>(null)
    private val _ipAddress = MutableStateFlow("192.168.1.100")
    private val _port = MutableStateFlow("8080")
    private val _ipError = MutableStateFlow<String?>(null)
    private val _portError = MutableStateFlow<String?>(null)

    private val _networkSelectionFlow = combine(
        _connectionStep,
        _selectedNetwork
    ) { step, selectedNet ->
        step to selectedNet
    }

    private val _ipPortFormFlow = combine(
        _ipAddress,
        _port,
        _ipError,
        _portError
    ) { ip, port, ipErr, portErr ->
        IpPortForm(ip, port, ipErr, portErr)
    }

    private val _formFlow = combine(
        _networkSelectionFlow,
        _ipPortFormFlow
    ) { (step, selectedNet), form ->
        ConnectionFormState(step, selectedNet, form.ip, form.port, form.ipErr, form.portErr)
    }

    private val _robotStateFlow = combine(
        robotRepository.batteryStatus,
        robotRepository.position,
        robotRepository.navigationStatus,
        robotRepository.mapData,
        robotRepository.connectionStatus
    ) { battery, position, navStatus, mapData, connStatus ->
        RobotBaseState(battery, position, navStatus, mapData, connStatus)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        _robotStateFlow,
        robotRepository.availableNetworks,
        _formFlow
    ) { robotState, availableNets, formState ->
        HomeUiState(
            batteryStatus = robotState.battery,
            position = robotState.position,
            navigationStatus = robotState.navStatus,
            mapData = robotState.mapData,
            statusMessage = getStatusMessage(robotState.navStatus),
            connectionStep = formState.step,
            availableNetworks = availableNets,
            selectedNetwork = formState.selectedNetwork ?: availableNets.firstOrNull(),
            ipAddress = formState.ipAddress,
            port = formState.port,
            ipError = formState.ipError,
            portError = formState.portError,
            connectionStatus = robotState.connStatus
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    fun selectNetwork(network: WifiNetwork) {
        _selectedNetwork.value = network
    }

    fun refreshNetworks() {
        robotRepository.refreshAvailableNetworks()
    }

    fun updateIpAddress(ip: String) {
        _ipAddress.value = ip
        validateIp(ip)
    }

    fun updatePort(portStr: String) {
        _port.value = portStr
        validatePort(portStr)
    }

    fun setConnectionStep(step: RobotConnectionStep) {
        _connectionStep.value = step
    }

    fun connectToRobot(onConnected: () -> Unit) {
        val currentIp = _ipAddress.value
        val currentPort = _port.value
        val currentSsid = _selectedNetwork.value?.ssid ?: "ROBOT_HOTSPOT_5G"

        val isIpValid = validateIp(currentIp)
        val isPortValid = validatePort(currentPort)

        if (isIpValid && isPortValid) {
            val portInt = currentPort.toIntOrNull() ?: 8080
            robotRepository.connectToRobot(currentIp, portInt, currentSsid)
            onConnected()
        }
    }

    fun disconnectRobot() {
        robotRepository.disconnectRobot()
    }

    fun moveForward() {
        robotRepository.move(DirectionCommand.FORWARD)
    }

    fun moveBackward() {
        robotRepository.move(DirectionCommand.BACKWARD)
    }

    fun moveLeft() {
        robotRepository.move(DirectionCommand.LEFT)
    }

    fun moveRight() {
        robotRepository.move(DirectionCommand.RIGHT)
    }

    fun goToCharge() {
        robotRepository.goToCharge()
    }

    fun cancelNavigation() {
        robotRepository.cancelNavigation()
    }

    fun getPosition() {
        robotRepository.refreshPosition()
    }

    fun moveToPosition() {
        robotRepository.moveToPosition(1.0, 2.0)
    }

    fun getMap() {
        robotRepository.fetchMap()
    }

    fun saveMap() {
        robotRepository.saveMap()
    }

    fun getBatteryLevel() {
        robotRepository.refreshBattery()
    }

    private fun validateIp(ip: String): Boolean {
        return if (ip.isBlank()) {
            _ipError.value = "IP address cannot be empty"
            false
        } else if (!isValidIpv4(ip)) {
            _ipError.value = "Enter a valid IPv4 address (e.g. 192.168.1.100)"
            false
        } else {
            _ipError.value = null
            true
        }
    }

    private fun validatePort(portStr: String): Boolean {
        val portInt = portStr.toIntOrNull()
        return if (portStr.isBlank()) {
            _portError.value = "Port number cannot be empty"
            false
        } else if (portInt == null || portInt !in 1..65535) {
            _portError.value = "Port must be a number between 1 and 65535"
            false
        } else {
            _portError.value = null
            true
        }
    }

    private fun isValidIpv4(ip: String): Boolean {
        val parts = ip.split(".")
        if (parts.size != 4) return false
        return parts.all { part ->
            part.toIntOrNull()?.let { it in 0..255 } ?: false
        }
    }

    private fun getStatusMessage(status: NavigationStatus): String {
        return when (status) {
            NavigationStatus.IDLE -> "Robot Idle"
            NavigationStatus.NAVIGATING -> "Robot Navigating"
            NavigationStatus.CHARGING -> "Robot Charging"
            NavigationStatus.CANCELLED -> "Navigation Cancelled"
            NavigationStatus.SAVING_MAP -> "Saving Map Data"
            NavigationStatus.FETCHING_MAP -> "Fetching Map Data"
        }
    }

    private data class IpPortForm(
        val ip: String,
        val port: String,
        val ipErr: String?,
        val portErr: String?
    )

    private data class ConnectionFormState(
        val step: RobotConnectionStep,
        val selectedNetwork: WifiNetwork?,
        val ipAddress: String,
        val port: String,
        val ipError: String?,
        val portError: String?
    )

    private data class RobotBaseState(
        val battery: BatteryStatus,
        val position: RobotPosition,
        val navStatus: NavigationStatus,
        val mapData: MapData,
        val connStatus: ConnectionStatus
    )
}
