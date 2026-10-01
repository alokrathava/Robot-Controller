package com.agrathava.sdk

import com.agrathava.database.dao.RobotLogDao
import com.agrathava.database.entity.RobotLogEntity
import com.agrathava.sdk.battery.DeviceBatteryDataSource
import com.agrathava.sdk.model.BatteryStatus
import com.agrathava.sdk.model.ConnectionConfig
import com.agrathava.sdk.model.ConnectionStatus
import com.agrathava.sdk.model.DirectionCommand
import com.agrathava.sdk.model.DockStation
import com.agrathava.sdk.model.DockingStatus
import com.agrathava.sdk.model.MapData
import com.agrathava.sdk.model.NavigationStatus
import com.agrathava.sdk.model.RobotPosition
import com.agrathava.sdk.model.RobotSimulationConfig
import com.agrathava.sdk.model.RobotTelemetry
import com.agrathava.sdk.model.ThermalState
import com.agrathava.sdk.model.WifiNetwork
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

interface RobotRepository {
    val batteryStatus: StateFlow<BatteryStatus>
    val position: StateFlow<RobotPosition>
    val navigationStatus: StateFlow<NavigationStatus>
    val mapData: StateFlow<MapData>
    val connectionStatus: StateFlow<ConnectionStatus>
    val connectionConfig: StateFlow<ConnectionConfig>
    val availableNetworks: StateFlow<List<WifiNetwork>>
    val telemetry: StateFlow<RobotTelemetry>
    val simulationConfig: StateFlow<RobotSimulationConfig>
    val dockingStatus: StateFlow<DockingStatus>
    val dockStation: StateFlow<DockStation>

    fun move(direction: DirectionCommand)
    fun goToCharge()
    fun cancelNavigation()
    fun refreshPosition()
    fun moveToPosition(x: Double, y: Double)
    fun fetchMap()
    fun saveMap()
    fun refreshBattery()
    fun connectToRobot(ip: String, port: Int, ssid: String)
    fun disconnectRobot()
    fun refreshAvailableNetworks()
    fun triggerEmergencyStop()
    fun resetEmergencyStop()
    fun updateSimulationConfig(config: RobotSimulationConfig)
    fun dock()
    fun undock()
    fun cancelDocking()
}

@Singleton
class DefaultRobotRepository @Inject constructor(
    private val robotLogDao: RobotLogDao,
    private val deviceBatteryDataSource: DeviceBatteryDataSource
) : RobotRepository {

    private val scope = CoroutineScope(Dispatchers.IO)

    private val _batteryStatus = MutableStateFlow(BatteryStatus(levelPercent = 85, isCharging = false))
    override val batteryStatus: StateFlow<BatteryStatus> = _batteryStatus.asStateFlow()

    private val _position = MutableStateFlow(RobotPosition(x = 0.0, y = 0.0))
    override val position: StateFlow<RobotPosition> = _position.asStateFlow()

    private val _navigationStatus = MutableStateFlow(NavigationStatus.IDLE)
    override val navigationStatus: StateFlow<NavigationStatus> = _navigationStatus.asStateFlow()

    private val _mapData = MutableStateFlow(MapData(name = "Main Floor Map", isAvailable = true))
    override val mapData: StateFlow<MapData> = _mapData.asStateFlow()

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    override val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _connectionConfig = MutableStateFlow(ConnectionConfig())
    override val connectionConfig: StateFlow<ConnectionConfig> = _connectionConfig.asStateFlow()

    private val _telemetry = MutableStateFlow(RobotTelemetry())
    override val telemetry: StateFlow<RobotTelemetry> = _telemetry.asStateFlow()

    private val _simulationConfig = MutableStateFlow(RobotSimulationConfig())
    override val simulationConfig: StateFlow<RobotSimulationConfig> = _simulationConfig.asStateFlow()

    private val _dockingStatus = MutableStateFlow(DockingStatus.UNDOCKED)
    override val dockingStatus: StateFlow<DockingStatus> = _dockingStatus.asStateFlow()

    private val _dockStation = MutableStateFlow(
        DockStation(
            id = "DOCK_MAIN",
            name = "Primary Charging Dock",
            position = RobotPosition(x = 0.0, y = 0.0)
        )
    )
    override val dockStation: StateFlow<DockStation> = _dockStation.asStateFlow()

    private val defaultNetworksList = listOf(
        WifiNetwork(ssid = "ROBOT_HOTSPOT_5G", signalPercent = 95, isSecured = true, frequency = "5.0 GHz"),
        WifiNetwork(ssid = "LAB_ROBOTICS_NET", signalPercent = 82, isSecured = true, frequency = "5.0 GHz"),
        WifiNetwork(ssid = "OFFICE_GUEST_WIFI", signalPercent = 68, isSecured = false, frequency = "2.4 GHz"),
        WifiNetwork(ssid = "HOME_NETWORK_EXT", signalPercent = 54, isSecured = true, frequency = "2.4 GHz")
    )

    private val _availableNetworks = MutableStateFlow(defaultNetworksList)
    override val availableNetworks: StateFlow<List<WifiNetwork>> = _availableNetworks.asStateFlow()

    init {
        scope.launch {
            deviceBatteryDataSource.observeBatteryStatus().collect { status ->
                processBatteryStatusUpdate(status)
            }
        }
    }

    private fun processBatteryStatusUpdate(status: BatteryStatus) {
        val prevStatus = _batteryStatus.value
        _batteryStatus.value = status

        val thermalState = when {
            status.temperatureCelsius >= 65.0 -> ThermalState.CRITICAL
            status.temperatureCelsius >= 55.0 -> ThermalState.OVERHEATING
            status.temperatureCelsius >= 40.0 -> ThermalState.WARM
            else -> ThermalState.NORMAL
        }

        _telemetry.update {
            it.copy(
                batteryTempCelsius = status.temperatureCelsius,
                thermalState = thermalState
            )
        }

        if (thermalState == ThermalState.CRITICAL && !_telemetry.value.isEmergencyStopped) {
            triggerEmergencyStop()
            logAction("SAFETY", "THERMAL EMERGENCY! Temperature reached ${status.temperatureCelsius}°C.")
        }

        if (status.isCharging && !prevStatus.isCharging) {
            _navigationStatus.value = NavigationStatus.CHARGING
            _dockingStatus.value = DockingStatus.DOCKED
            _telemetry.update { it.copy(isDocked = true, speedMps = 0.0) }
            logAction("BATTERY", "Device plugged into ${status.plugType} (${status.levelPercent}%). Digital Twin set to CHARGING/DOCKED.")
        } else if (!status.isCharging && prevStatus.isCharging) {
            if (_navigationStatus.value == NavigationStatus.CHARGING) {
                _navigationStatus.value = NavigationStatus.IDLE
            }
            if (_dockingStatus.value == DockingStatus.DOCKED) {
                _dockingStatus.value = DockingStatus.UNDOCKED
            }
            _telemetry.update { it.copy(isDocked = false) }
            logAction("BATTERY", "Device unplugged (${status.levelPercent}%). Digital Twin set to IDLE/UNDOCKED.")
        }

        if (status.levelPercent <= 10 && !status.isCharging && _dockingStatus.value == DockingStatus.UNDOCKED) {
            logAction("BATTERY", "CRITICAL LOW BATTERY (${status.levelPercent}%). Auto-docking initiated.")
            dock()
        }
    }

    override fun move(direction: DirectionCommand) {
        if (_telemetry.value.isEmergencyStopped) {
            logAction("MOVE_REJECTED", "Cannot move - Emergency Stop is ACTIVE")
            return
        }

        val step = 0.5
        val baseSpeed = _simulationConfig.value.movementSpeedMps
        val effectiveSpeed = if (_telemetry.value.thermalState == ThermalState.OVERHEATING) {
            min(baseSpeed, 0.25)
        } else {
            baseSpeed
        }

        val current = _position.value
        val newPos = when (direction) {
            DirectionCommand.FORWARD -> current.copy(y = current.y + step, headingDegrees = 0.0)
            DirectionCommand.BACKWARD -> current.copy(y = current.y - step, headingDegrees = 180.0)
            DirectionCommand.LEFT -> current.copy(x = current.x - step, headingDegrees = 270.0)
            DirectionCommand.RIGHT -> current.copy(x = current.x + step, headingDegrees = 90.0)
        }
        _position.value = newPos
        _navigationStatus.value = NavigationStatus.NAVIGATING
        _dockingStatus.value = DockingStatus.UNDOCKED
        _telemetry.update { it.copy(speedMps = effectiveSpeed, isDocked = false) }
        logAction("MOVE", "Moved $direction to (${newPos.x}, ${newPos.y}) at ${effectiveSpeed}m/s")
    }

    override fun goToCharge() {
        if (_telemetry.value.isEmergencyStopped) {
            logAction("CHARGE_REJECTED", "Cannot navigate to charge - Emergency Stop is ACTIVE")
            return
        }
        dock()
    }

    override fun cancelNavigation() {
        _navigationStatus.value = NavigationStatus.CANCELLED
        _telemetry.update { it.copy(speedMps = 0.0) }
        logAction("NAVIGATION", "Navigation cancelled")
    }

    override fun refreshPosition() {
        logAction("STATUS", "Position refreshed")
    }

    override fun moveToPosition(x: Double, y: Double) {
        if (_telemetry.value.isEmergencyStopped) {
            logAction("MOVE_REJECTED", "Cannot move - Emergency Stop is ACTIVE")
            return
        }
        val baseSpeed = _simulationConfig.value.movementSpeedMps
        val effectiveSpeed = if (_telemetry.value.thermalState == ThermalState.OVERHEATING) {
            min(baseSpeed, 0.25)
        } else {
            baseSpeed
        }
        _position.value = RobotPosition(x, y)
        _navigationStatus.value = NavigationStatus.NAVIGATING
        _dockingStatus.value = DockingStatus.UNDOCKED
        _telemetry.update { it.copy(speedMps = effectiveSpeed, isDocked = false) }
        logAction("MOVE", "Moving to position ($x, $y) at ${effectiveSpeed}m/s")
    }

    override fun fetchMap() {
        _navigationStatus.value = NavigationStatus.FETCHING_MAP
        _mapData.value = MapData(name = "Main Floor Map v2", isAvailable = true)
        logAction("MAP", "Fetched map data")
    }

    override fun saveMap() {
        _navigationStatus.value = NavigationStatus.SAVING_MAP
        logAction("MAP", "Saved map data")
    }

    override fun refreshBattery() {
        val status = deviceBatteryDataSource.getCurrentBatteryStatus()
        processBatteryStatusUpdate(status)
        logAction("BATTERY", "Refreshed device battery: ${status.displayText} (${status.plugType})")
    }

    override fun connectToRobot(ip: String, port: Int, ssid: String) {
        _connectionStatus.value = ConnectionStatus.CONNECTING
        _connectionConfig.value = ConnectionConfig(ipAddress = ip, port = port, selectedSsid = ssid)
        
        _availableNetworks.update { networks ->
            networks.map { it.copy(isConnected = (it.ssid == ssid)) }
        }

        _connectionStatus.value = ConnectionStatus.CONNECTED
        logAction("CONNECTION", "Connected to Robot at $ip:$port on network '$ssid'")
    }

    override fun disconnectRobot() {
        _connectionStatus.value = ConnectionStatus.DISCONNECTED
        _availableNetworks.update { networks ->
            networks.map { it.copy(isConnected = false) }
        }
        logAction("CONNECTION", "Disconnected from Robot")
    }

    override fun refreshAvailableNetworks() {
        logAction("WIFI", "Scanned for available Wi-Fi networks")
    }

    override fun triggerEmergencyStop() {
        _telemetry.update { it.copy(isEmergencyStopped = true, speedMps = 0.0) }
        _navigationStatus.value = NavigationStatus.EMERGENCY_STOP
        logAction("SAFETY", "EMERGENCY STOP TRIGGERED!")
    }

    override fun resetEmergencyStop() {
        _telemetry.update { it.copy(isEmergencyStopped = false) }
        _navigationStatus.value = NavigationStatus.IDLE
        logAction("SAFETY", "Emergency stop reset. System restored to IDLE.")
    }

    override fun updateSimulationConfig(config: RobotSimulationConfig) {
        _simulationConfig.value = config
        logAction("SIMULATION", "Updated simulation config: speed=${config.movementSpeedMps}m/s")
    }

    override fun dock() {
        if (_telemetry.value.isEmergencyStopped) {
            logAction("DOCK_REJECTED", "Cannot dock - Emergency Stop is ACTIVE")
            return
        }

        _dockingStatus.value = DockingStatus.NAVIGATING_TO_DOCK
        _navigationStatus.value = NavigationStatus.NAVIGATING
        
        val dockPos = _dockStation.value.position
        _position.value = dockPos

        _dockingStatus.value = DockingStatus.ALIGNING_WITH_DOCK
        _dockingStatus.value = DockingStatus.DOCKED
        _navigationStatus.value = NavigationStatus.CHARGING
        _batteryStatus.update { it.copy(isCharging = true) }
        _telemetry.update { it.copy(isDocked = true, speedMps = 0.0) }

        logAction("DOCKING", "Robot successfully docked at '${_dockStation.value.name}'")
    }

    override fun undock() {
        _dockingStatus.value = DockingStatus.UNDOCKED
        _navigationStatus.value = NavigationStatus.IDLE
        _batteryStatus.update { it.copy(isCharging = false) }
        _telemetry.update { it.copy(isDocked = false) }
        val cur = _position.value
        _position.value = cur.copy(x = cur.x + 0.5)

        logAction("DOCKING", "Robot disengaged from dock")
    }

    override fun cancelDocking() {
        _dockingStatus.value = DockingStatus.UNDOCKED
        _navigationStatus.value = NavigationStatus.CANCELLED
        _telemetry.update { it.copy(speedMps = 0.0) }

        logAction("DOCKING", "Docking procedure cancelled")
    }

    private fun logAction(type: String, message: String) {
        scope.launch {
            robotLogDao.insertLog(RobotLogEntity(logType = type, message = message))
        }
    }
}
