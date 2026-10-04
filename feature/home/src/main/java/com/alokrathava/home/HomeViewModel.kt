package com.alokrathava.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alokrathava.sdk.RobotRepository
import com.alokrathava.sdk.model.BatteryStatus
import com.alokrathava.sdk.model.ConnectionStatus
import com.alokrathava.sdk.model.DirectionCommand
import com.alokrathava.sdk.model.MapData
import com.alokrathava.sdk.model.NavigationStatus
import com.alokrathava.sdk.model.RobotPosition
import com.alokrathava.sdk.model.WifiNetwork
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val robotRepository: RobotRepository,
) : ViewModel() {

    private val _screenFlow = MutableStateFlow(ScreenFlow.SPLASH)
    private val _connectionStep = MutableStateFlow(RobotConnectionStep.NETWORK_SELECTION)
    private val _selectedNetwork = MutableStateFlow<WifiNetwork?>(null)
    private val _isScanningNetworks = MutableStateFlow(false)
    private val _networkScanError = MutableStateFlow<String?>(null)
    private val _ipAddress = MutableStateFlow("192.168.1.100")
    private val _port = MutableStateFlow("8080")
    private val _token = MutableStateFlow("")
    private val _ipError = MutableStateFlow<String?>(null)
    private val _portError = MutableStateFlow<String?>(null)
    private val _tokenError = MutableStateFlow<String?>(null)
    private val _connectionErrorMessage = MutableStateFlow<String?>(null)

    private val _networkSelectionFlow = combine(
        _connectionStep,
        _selectedNetwork,
        _isScanningNetworks,
        _networkScanError
    ) { step, selectedNet, isScanning, scanErr ->
        NetworkSelectionState(step, selectedNet, isScanning, scanErr)
    }

    private val _fieldsFlow = combine(_ipAddress, _port, _token) { ip, port, token ->
        FormFields(ip, port, token)
    }

    private val _errorsFlow = combine(_ipError, _portError, _tokenError, _connectionErrorMessage) { ipErr, portErr, tokenErr, connErr ->
        FormErrors(ipErr, portErr, tokenErr, connErr)
    }

    private val _ipPortFormFlow = combine(_fieldsFlow, _errorsFlow) { fields, errors ->
        IpPortForm(
            ip = fields.ip,
            port = fields.port,
            token = fields.token,
            ipErr = errors.ipErr,
            portErr = errors.portErr,
            tokenErr = errors.tokenErr,
            connErr = errors.connErr
        )
    }

    private val _formFlow = combine(
        _screenFlow,
        _networkSelectionFlow,
        _ipPortFormFlow,
    ) { flow, netState, form ->
        ConnectionFormState(flow, netState, form)
    }

    private val _robotStateFlow = combine(
        robotRepository.batteryStatus,
        robotRepository.position,
        robotRepository.navigationStatus,
        robotRepository.mapData,
        robotRepository.connectionStatus,
    ) { battery, position, navStatus, mapData, connStatus ->
        RobotBaseState(battery, position, navStatus, mapData, connStatus)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        _robotStateFlow,
        robotRepository.availableNetworks,
        robotRepository.telemetry,
        _formFlow,
    ) { robotState, availableNets, telemetry, formState ->
        HomeUiState(
            batteryStatus = robotState.battery,
            position = robotState.position,
            navigationStatus = robotState.navStatus,
            mapData = robotState.mapData,
            statusMessage = getStatusMessage(robotState.navStatus),
            screenFlow = formState.screenFlow,
            connectionStep = formState.netState.step,
            availableNetworks = availableNets,
            selectedNetwork = formState.netState.selectedNetwork ?: availableNets.firstOrNull(),
            isScanningNetworks = formState.netState.isScanning,
            networkScanError = formState.netState.scanError,
            ipAddress = formState.ipForm.ip,
            port = formState.ipForm.port,
            token = formState.ipForm.token,
            ipError = formState.ipForm.ipErr,
            portError = formState.ipForm.portErr,
            tokenError = formState.ipForm.tokenErr,
            connectionErrorMessage = formState.ipForm.connErr,
            connectionStatus = robotState.connStatus,
            isEmergencyStopped = telemetry.isEmergencyStopped,
            isLowBatteryWarning = robotState.battery.isLowBattery || robotState.battery.levelPercent <= 15
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(),
    )

    fun setScreenFlow(flow: ScreenFlow) {
        _screenFlow.value = flow
    }

    fun selectNetwork(network: WifiNetwork) {
        _selectedNetwork.value = network
    }

    fun refreshNetworks() {
        _isScanningNetworks.value = true
        _networkScanError.value = null
        robotRepository.refreshAvailableNetworks()
        _isScanningNetworks.value = false
    }

    fun updateIpAddress(ip: String) {
        _ipAddress.value = ip
        validateIp(ip)
    }

    fun updatePort(portStr: String) {
        _port.value = portStr
        validatePort(portStr)
    }

    fun updateToken(token: String) {
        _token.value = token
    }

    fun setConnectionStep(step: RobotConnectionStep) {
        _connectionStep.value = step
    }

    private var connectionMonitorJob: Job? = null

    fun connectToRobot(onConnected: () -> Unit = {}) {
        val currentIp = _ipAddress.value
        val currentPort = _port.value
        val currentToken = _token.value
        val currentSsid = _selectedNetwork.value?.ssid ?: "ROBOT_HOTSPOT_5G"

        val isIpValid = validateIp(currentIp)
        val isPortValid = validatePort(currentPort)

        if (isIpValid && isPortValid) {
            _connectionStep.value = RobotConnectionStep.CONNECTING
            _connectionErrorMessage.value = null
            val portInt = currentPort.toIntOrNull() ?: 8080

            connectionMonitorJob?.cancel()
            connectionMonitorJob = viewModelScope.launch {
                robotRepository.connectToRobot(currentIp, portInt, currentToken, currentSsid)
                robotRepository.connectionStatus.collect { status ->
                    when (status) {
                        ConnectionStatus.CONNECTED -> {
                            _screenFlow.value = ScreenFlow.DASHBOARD
                            _connectionStep.value = RobotConnectionStep.NETWORK_SELECTION
                            onConnected()
                            this@launch.cancel()
                        }
                        ConnectionStatus.FAILED -> {
                            _connectionStep.value = RobotConnectionStep.CONNECTION_FAILED
                            _connectionErrorMessage.value = "Failed to connect to robot at $currentIp:$portInt"
                            this@launch.cancel()
                        }
                        else -> {
                            // Still connecting or disconnected, remain in CONNECTING step
                        }
                    }
                }
            }
        } else {
            _connectionStep.value = RobotConnectionStep.CONNECTION_FAILED
            _connectionErrorMessage.value = "Please fix configuration errors before connecting"
        }
    }

    @Suppress("unused")
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
        } else if (portInt == null || portInt < 1 || portInt > 65535) {
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
            NavigationStatus.EMERGENCY_STOP -> "Emergency Stop"
        }
    }

    private data class NetworkSelectionState(
        val step: RobotConnectionStep,
        val selectedNetwork: WifiNetwork?,
        val isScanning: Boolean,
        val scanError: String?
    )

    private data class FormFields(
        val ip: String,
        val port: String,
        val token: String
    )

    private data class FormErrors(
        val ipErr: String?,
        val portErr: String?,
        val tokenErr: String?,
        val connErr: String?
    )

    private data class IpPortForm(
        val ip: String,
        val port: String,
        val token: String,
        val ipErr: String?,
        val portErr: String?,
        val tokenErr: String?,
        val connErr: String?
    )

    private data class ConnectionFormState(
        val screenFlow: ScreenFlow,
        val netState: NetworkSelectionState,
        val ipForm: IpPortForm
    )

    private data class RobotBaseState(
        val battery: BatteryStatus,
        val position: RobotPosition,
        val navStatus: NavigationStatus,
        val mapData: MapData,
        val connStatus: ConnectionStatus
    )
}
