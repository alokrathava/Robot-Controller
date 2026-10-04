package com.alokrathava.robotcontroller.di

import com.alokrathava.sdk.RobotAuthentication
import com.alokrathava.sdk.RobotClient
import com.alokrathava.sdk.RobotEndpoint
import com.alokrathava.sdk.RobotRepository
import com.alokrathava.sdk.RobotSdk
import com.alokrathava.sdk.RobotSdkConfig
import com.alokrathava.sdk.error.RobotResult
import com.alokrathava.sdk.model.BatteryStatus
import com.alokrathava.sdk.model.CommandId
import com.alokrathava.sdk.model.ConnectionConfig
import com.alokrathava.sdk.model.ConnectionState
import com.alokrathava.sdk.model.ConnectionStatus
import com.alokrathava.sdk.model.DirectionCommand
import com.alokrathava.sdk.model.DockStation
import com.alokrathava.sdk.model.DockingStatus
import com.alokrathava.sdk.model.MapData
import com.alokrathava.sdk.model.MotionLimits
import com.alokrathava.sdk.model.NavigationStatus
import com.alokrathava.sdk.model.Pose2D
import com.alokrathava.sdk.model.RobotMap
import com.alokrathava.sdk.model.RobotPosition
import com.alokrathava.sdk.model.RobotSimulationConfig
import com.alokrathava.sdk.model.RobotTelemetry
import com.alokrathava.sdk.model.SavedLocation
import com.alokrathava.sdk.model.WifiNetwork
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.alokrathava.sdk.RobotConnectionManager
import com.alokrathava.sdk.RobotConnectionProfile
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppRobotModule {

    @Provides
    @Singleton
    fun provideRobotConnectionManager(): RobotConnectionManager {
        return RobotConnectionManager()
    }

    @Provides
    @Singleton
    fun provideRobotClient(connectionManager: RobotConnectionManager): RobotClient {
        return connectionManager.getActiveClient() ?: RobotSdk.create(
            RobotSdkConfig(
                endpoint = RobotEndpoint(
                    host = "192.168.1.100",
                    port = 8080
                )
            )
        )
    }

    @Provides
    @Singleton
    fun provideRobotRepository(connectionManager: RobotConnectionManager): RobotRepository {
        return RobotClientRepositoryImpl(connectionManager)
    }
}

@Singleton
internal class RobotClientRepositoryImpl(
    private val connectionManager: RobotConnectionManager
) : RobotRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var activeClient: RobotClient = connectionManager.getActiveClient() ?: RobotSdk.create(
        RobotSdkConfig(
            endpoint = RobotEndpoint(
                host = "192.168.1.100",
                port = 8080
            )
        )
    )

    private var connJob: Job? = null
    private var telemJob: Job? = null
    private var battJob: Job? = null
    private var dockJob: Job? = null
    private var mapJob: Job? = null
    private var metricsJob: Job? = null

    private val _connectionMetrics = MutableStateFlow(com.alokrathava.sdk.model.ConnectionMetrics())
    override val connectionMetrics: StateFlow<com.alokrathava.sdk.model.ConnectionMetrics> = _connectionMetrics.asStateFlow()

    private val _batteryStatus = MutableStateFlow(BatteryStatus(levelPercent = 0, isCharging = false))
    override val batteryStatus: StateFlow<BatteryStatus> = _batteryStatus.asStateFlow()

    private val _position = MutableStateFlow(RobotPosition(x = 0.0, y = 0.0))
    override val position: StateFlow<RobotPosition> = _position.asStateFlow()

    private val _navigationStatus = MutableStateFlow(NavigationStatus.IDLE)
    override val navigationStatus: StateFlow<NavigationStatus> = _navigationStatus.asStateFlow()

    private val _mapData = MutableStateFlow(MapData(name = "Default Map", isAvailable = true))
    override val mapData: StateFlow<MapData> = _mapData.asStateFlow()

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    override val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _connectionConfig = MutableStateFlow(ConnectionConfig())
    override val connectionConfig: StateFlow<ConnectionConfig> = _connectionConfig.asStateFlow()

    private val _availableNetworks = MutableStateFlow(emptyList<WifiNetwork>())
    override val availableNetworks: StateFlow<List<WifiNetwork>> = _availableNetworks.asStateFlow()

    private val _telemetry = MutableStateFlow(RobotTelemetry())
    override val telemetry: StateFlow<RobotTelemetry> = _telemetry.asStateFlow()

    private val _simulationConfig = MutableStateFlow(RobotSimulationConfig())
    override val simulationConfig: StateFlow<RobotSimulationConfig> = _simulationConfig.asStateFlow()

    private val _dockingStatus = MutableStateFlow(DockingStatus.UNDOCKED)
    override val dockingStatus: StateFlow<DockingStatus> = _dockingStatus.asStateFlow()

    private val _dockStation = MutableStateFlow(DockStation())
    override val dockStation: StateFlow<DockStation> = _dockStation.asStateFlow()

    init {
        bindClient(activeClient)
        scope.launch {
            connectionManager.activeClient.collect { client ->
                if (client != null && client != activeClient) {
                    bindClient(client)
                }
            }
        }
    }

    private fun bindClient(client: RobotClient) {
        connJob?.cancel()
        telemJob?.cancel()
        battJob?.cancel()
        dockJob?.cancel()
        mapJob?.cancel()
        metricsJob?.cancel()

        activeClient = client

        connJob = scope.launch {
            client.connectionState.collect { state ->
                _connectionStatus.value = when (state) {
                    is ConnectionState.Connected -> ConnectionStatus.CONNECTED
                    is ConnectionState.Connecting -> ConnectionStatus.CONNECTING
                    is ConnectionState.Authenticating -> ConnectionStatus.CONNECTING
                    is ConnectionState.Reconnecting -> ConnectionStatus.CONNECTING
                    is ConnectionState.Failed -> ConnectionStatus.FAILED
                    is ConnectionState.Disconnected -> ConnectionStatus.DISCONNECTED
                }
            }
        }

        metricsJob = scope.launch {
            client.connectionMetrics.collect { metrics ->
                _connectionMetrics.value = metrics
            }
        }

        telemJob = scope.launch {
            client.telemetry.collect { telem ->
                if (telem != null) {
                    _position.value = RobotPosition(telem.xMeters, telem.yMeters, Math.toDegrees(telem.yawRadians))
                    _navigationStatus.value = when (telem.navigationState.uppercase()) {
                        "NAVIGATING" -> NavigationStatus.NAVIGATING
                        "CHARGING" -> NavigationStatus.CHARGING
                        "CANCELLED" -> NavigationStatus.CANCELLED
                        else -> NavigationStatus.IDLE
                    }
                    _telemetry.value = telem
                }
            }
        }

        battJob = scope.launch {
            client.batteryState.collect { batt ->
                if (batt != null) {
                    _batteryStatus.value = BatteryStatus(
                        levelPercent = batt.percentage.toInt(),
                        isCharging = batt.isCharging,
                        temperatureCelsius = batt.temperatureCelsius?.toDouble() ?: 25.0,
                        voltageMv = (batt.voltageVolts * 1000).toInt()
                    )
                }
            }
        }

        dockJob = scope.launch {
            client.dockingState.collect { dockState ->
                if (dockState != null) {
                    _dockingStatus.value = when (dockState) {
                        com.alokrathava.sdk.model.DockingState.UNDOCKED -> DockingStatus.UNDOCKED
                        com.alokrathava.sdk.model.DockingState.NAVIGATING_TO_DOCK -> DockingStatus.NAVIGATING_TO_DOCK
                        com.alokrathava.sdk.model.DockingState.ALIGNING -> DockingStatus.ALIGNING_WITH_DOCK
                        com.alokrathava.sdk.model.DockingState.DOCKED -> DockingStatus.DOCKED
                        else -> DockingStatus.DOCKING_FAILED
                    }
                }
            }
        }

        mapJob = scope.launch {
            client.activeMap.collect { map ->
                if (map != null) {
                    _mapData.value = MapData(name = map.name, isAvailable = true)
                }
            }
        }
    }

    override fun setManualVelocity(linearMps: Double, angularRadPerSec: Double) {
        scope.launch {
            activeClient.setManualVelocity(linearMps, angularRadPerSec)
        }
    }

    override fun move(direction: DirectionCommand) {
        val (linear, angular) = when (direction) {
            DirectionCommand.FORWARD -> Pair(0.5, 0.0)
            DirectionCommand.BACKWARD -> Pair(-0.5, 0.0)
            DirectionCommand.LEFT -> Pair(0.0, 0.5)
            DirectionCommand.RIGHT -> Pair(0.0, -0.5)
        }
        scope.launch {
            activeClient.setManualVelocity(linear, angular)
        }
    }

    override fun goToCharge() {
        scope.launch { activeClient.dock() }
    }

    override fun cancelNavigation() {
        scope.launch { activeClient.cancelNavigation() }
    }

    override fun refreshPosition() {
        val telem = activeClient.telemetry.value
        if (telem != null) {
            _position.value = RobotPosition(telem.xMeters, telem.yMeters, Math.toDegrees(telem.yawRadians))
        }
    }

    override fun moveToPosition(x: Double, y: Double) {
        scope.launch {
            activeClient.navigateTo(Pose2D(xMeters = x, yMeters = y, yawRadians = 0.0))
        }
    }

    override fun fetchMap() {
        scope.launch {
            when (val result = activeClient.listMaps()) {
                is RobotResult.Success -> {
                    val activeMap = result.value.firstOrNull { it.isActive } ?: result.value.firstOrNull()
                    if (activeMap != null) {
                        _mapData.value = MapData(name = activeMap.name, isAvailable = true)
                    }
                }
                is RobotResult.Failure -> { }
            }
        }
    }

    override fun saveMap() {
        scope.launch {
            activeClient.saveCurrentMap(_mapData.value.name.ifEmpty { "Saved Map" })
        }
    }

    override fun refreshBattery() {
        val batt = activeClient.batteryState.value
        if (batt != null) {
            _batteryStatus.value = BatteryStatus(
                levelPercent = batt.percentage.toInt(),
                isCharging = batt.isCharging,
                temperatureCelsius = batt.temperatureCelsius?.toDouble() ?: 25.0,
                voltageMv = (batt.voltageVolts * 1000).toInt()
            )
        }
    }

    override fun connectToRobot(ip: String, port: Int, ssid: String) {
        connectToRobot(ip, port, "", ssid)
    }

    override fun connectToRobot(ip: String, port: Int, token: String, ssid: String) {
        _connectionConfig.value = ConnectionConfig(ipAddress = ip, port = port, token = token, selectedSsid = ssid)
        scope.launch {
            val profile = RobotConnectionProfile(
                host = ip,
                port = port,
                token = token,
                selectedSsid = ssid
            )
            connectionManager.connect(profile)
        }
    }

    override fun disconnectRobot() {
        scope.launch {
            connectionManager.disconnect()
        }
    }

    override fun refreshAvailableNetworks() {
        _availableNetworks.value = listOf(
            WifiNetwork(ssid = "ROBOT_HOTSPOT_5G", signalPercent = 95, isSecured = true),
            WifiNetwork(ssid = "ROBOT_OFFICE_WIFI", signalPercent = 80, isSecured = true),
            WifiNetwork(ssid = "ROBOT_LAB_2G", signalPercent = 65, isSecured = false)
        )
    }

    override fun triggerEmergencyStop() {
        scope.launch {
            activeClient.emergencyStop()
        }
    }

    override fun resetEmergencyStop() {
        scope.launch {
            activeClient.releaseEmergencyStop()
        }
    }

    override fun updateSimulationConfig(config: RobotSimulationConfig) {
        _simulationConfig.value = config
    }

    override fun dock() {
        scope.launch { activeClient.dock() }
    }

    override fun undock() {
        scope.launch { activeClient.undock() }
    }

    override fun cancelDocking() {
        scope.launch { activeClient.cancelDocking() }
    }

    override suspend fun listMaps(): RobotResult<List<RobotMap>> = activeClient.listMaps()

    override suspend fun switchMap(mapId: String): RobotResult<Unit> = activeClient.switchMap(mapId)

    override suspend fun saveCurrentMap(name: String): RobotResult<RobotMap> = activeClient.saveCurrentMap(name)

    override suspend fun deleteMap(mapId: String): RobotResult<Unit> = activeClient.deleteMap(mapId)

    override suspend fun listSavedLocations(mapId: String): RobotResult<List<SavedLocation>> = activeClient.listSavedLocations(mapId)

    override suspend fun saveLocation(name: String, pose: Pose2D, mapId: String?): RobotResult<SavedLocation> = activeClient.saveLocation(name, pose, mapId)

    override suspend fun deleteLocation(id: String): RobotResult<Unit> = activeClient.deleteLocation(id)

    override suspend fun navigateToLocation(id: String): RobotResult<CommandId> = activeClient.navigateToLocation(id)

    override suspend fun updateMotionLimits(limits: MotionLimits): RobotResult<Unit> = activeClient.updateMotionLimits(limits)

    override fun getActiveRobotClient(): RobotClient = activeClient
}
