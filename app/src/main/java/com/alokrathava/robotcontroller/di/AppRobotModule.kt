package com.alokrathava.robotcontroller.di

import com.alokrathava.sdk.RobotAuthentication
import com.alokrathava.sdk.RobotClient
import com.alokrathava.sdk.RobotEndpoint
import com.alokrathava.sdk.RobotRepository
import com.alokrathava.sdk.RobotSdk
import com.alokrathava.sdk.RobotSdkConfig
import com.alokrathava.sdk.model.BatteryStatus
import com.alokrathava.sdk.model.ConnectionConfig
import com.alokrathava.sdk.model.ConnectionState
import com.alokrathava.sdk.model.ConnectionStatus
import com.alokrathava.sdk.model.DirectionCommand
import com.alokrathava.sdk.model.DockStation
import com.alokrathava.sdk.model.DockingStatus
import com.alokrathava.sdk.model.MapData
import com.alokrathava.sdk.model.NavigationStatus
import com.alokrathava.sdk.model.Pose2D
import com.alokrathava.sdk.model.RobotPosition
import com.alokrathava.sdk.model.RobotSimulationConfig
import com.alokrathava.sdk.model.RobotTelemetry
import com.alokrathava.sdk.model.ThermalState
import com.alokrathava.sdk.model.WifiNetwork
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppRobotModule {

    @Provides
    @Singleton
    fun provideRobotClient(): RobotClient {
        return RobotSdk.create(
            RobotSdkConfig(
                endpoint = RobotEndpoint(
                    host = "127.0.0.1",
                    port = 8080
                ),
                authentication = RobotAuthentication.Token("alpha-token")
            )
        )
    }

    @Provides
    @Singleton
    fun provideRobotRepository(robotClient: RobotClient): RobotRepository {
        return RobotClientRepositoryImpl(robotClient)
    }
}

@Singleton
internal class RobotClientRepositoryImpl(
    private val robotClient: RobotClient
) : RobotRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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
        scope.launch {
            robotClient.connectionState.collect { state ->
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

        scope.launch {
            robotClient.telemetry.collect { telem ->
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

        scope.launch {
            robotClient.batteryState.collect { batt ->
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

        scope.launch {
            robotClient.dockingState.collect { dockState ->
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
    }

    override fun move(direction: DirectionCommand) {
        val (linear, angular) = when (direction) {
            DirectionCommand.FORWARD -> Pair(0.5, 0.0)
            DirectionCommand.BACKWARD -> Pair(-0.5, 0.0)
            DirectionCommand.LEFT -> Pair(0.0, 0.5)
            DirectionCommand.RIGHT -> Pair(0.0, -0.5)
        }
        scope.launch {
            robotClient.setManualVelocity(linear, angular)
        }
    }

    override fun goToCharge() {
        scope.launch { robotClient.dock() }
    }

    override fun cancelNavigation() {
        scope.launch { robotClient.cancelNavigation() }
    }

    override fun refreshPosition() {}

    override fun moveToPosition(x: Double, y: Double) {
        scope.launch {
            robotClient.navigateTo(Pose2D(xMeters = x, yMeters = y, yawRadians = 0.0))
        }
    }

    override fun fetchMap() {}

    override fun saveMap() {}

    override fun refreshBattery() {}

    override fun connectToRobot(ip: String, port: Int, ssid: String) {
        _connectionConfig.value = ConnectionConfig(ipAddress = ip, port = port, selectedSsid = ssid)
        scope.launch {
            robotClient.connect()
        }
    }

    override fun disconnectRobot() {
        scope.launch {
            robotClient.disconnect()
        }
    }

    override fun refreshAvailableNetworks() {}

    override fun triggerEmergencyStop() {
        scope.launch {
            robotClient.emergencyStop()
        }
    }

    override fun resetEmergencyStop() {
        scope.launch {
            robotClient.releaseEmergencyStop()
        }
    }

    override fun updateSimulationConfig(config: RobotSimulationConfig) {
        _simulationConfig.value = config
    }

    override fun dock() {
        scope.launch { robotClient.dock() }
    }

    override fun undock() {
        scope.launch { robotClient.undock() }
    }

    override fun cancelDocking() {
        scope.launch { robotClient.cancelDocking() }
    }
}
