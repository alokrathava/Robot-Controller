package com.alokrathava.robotcontroller.di

import android.annotation.SuppressLint
import com.alokrathava.sdk.RobotClient
import com.alokrathava.sdk.RobotEndpoint
import com.alokrathava.sdk.RobotRepository
import com.alokrathava.sdk.RobotPositionSource
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
import com.alokrathava.sdk.model.LiveRobotPosition
import com.alokrathava.sdk.model.MotionLimits
import com.alokrathava.sdk.model.NavigationStatus
import com.alokrathava.sdk.model.Pose2D
import com.alokrathava.sdk.model.PositionAvailability
import com.alokrathava.sdk.model.RobotMap
import com.alokrathava.sdk.model.RobotPosition
import com.alokrathava.sdk.model.RobotSimulationConfig
import com.alokrathava.sdk.model.RobotTelemetry
import com.alokrathava.sdk.model.TelemetryFreshness
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
import com.alokrathava.sdk.discovery.RobotDiscoveryManager
import com.alokrathava.sdk.discovery.RobotDiscoveryManagerImpl
import javax.inject.Singleton
import kotlin.math.hypot

@Module
@InstallIn(SingletonComponent::class)
object AppRobotModule {

    @Provides
    @Singleton
    fun provideRobotDiscoveryManager(): RobotDiscoveryManager {
        return RobotDiscoveryManagerImpl()
    }

    @Provides
    @Singleton
    fun provideRobotConnectionManager(): RobotConnectionManager {
        return RobotConnectionManager()
    }

    @Provides
    @Singleton
    fun provideRobotClient(connectionManager: RobotConnectionManager): RobotClient {
        return com.alokrathava.sdk.DelegatingRobotClient(connectionManager)
    }

    @Provides
    @Singleton
    fun provideRobotRepository(
        connectionManager: RobotConnectionManager,
        @dagger.hilt.android.qualifiers.ApplicationContext context: android.content.Context
    ): RobotRepository {
        return RobotClientRepositoryImpl(connectionManager, context)
    }
}

@Singleton
/** Bridges the active SDK client into shared application flows, including validated position. */
internal class RobotClientRepositoryImpl(
    private val connectionManager: RobotConnectionManager,
    private val context: android.content.Context
) : RobotRepository, RobotPositionSource {

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
    private var gridJob: Job? = null
    private var metricsJob: Job? = null
    private var freshnessJob: Job? = null
    private var mapOpJob: Job? = null
    private var velocityJob: Job? = null

    private val _mapOperationState = MutableStateFlow<com.alokrathava.sdk.model.MapOperationState>(com.alokrathava.sdk.model.MapOperationState.Idle)
    override val mapOperationState: StateFlow<com.alokrathava.sdk.model.MapOperationState> = _mapOperationState.asStateFlow()

    private val _occupancyGrid = MutableStateFlow<com.alokrathava.sdk.model.RobotOccupancyGrid?>(null)
    override val occupancyGrid: StateFlow<com.alokrathava.sdk.model.RobotOccupancyGrid?> = _occupancyGrid.asStateFlow()

    private val _connectionMetrics = MutableStateFlow(com.alokrathava.sdk.model.ConnectionMetrics())
    override val connectionMetrics: StateFlow<com.alokrathava.sdk.model.ConnectionMetrics> = _connectionMetrics.asStateFlow()

    private val _batteryStatus = MutableStateFlow(BatteryStatus(levelPercent = 0, isCharging = false))
    override val batteryStatus: StateFlow<BatteryStatus> = _batteryStatus.asStateFlow()

    private val _position = MutableStateFlow(RobotPosition(x = 0.0, y = 0.0))
    override val position: StateFlow<RobotPosition> = _position.asStateFlow()

    private val positionLock = Any()
    private val _livePosition = MutableStateFlow(LiveRobotPosition())
    override val livePosition: StateFlow<LiveRobotPosition> = _livePosition.asStateFlow()
    private var lastValidSampleMillis = 0L
    private var lastGridMapId = ""
    private var positionConnectionWasActive = false
    private var positionReconnectPending = false
    private var trailBreakPending = false

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

    /** Rebinds all SDK collectors and resets session-owned position and grid state. */
    private fun bindClient(client: RobotClient) {
        connJob?.cancel()
        telemJob?.cancel()
        battJob?.cancel()
        dockJob?.cancel()
        mapJob?.cancel()
        gridJob?.cancel()
        metricsJob?.cancel()
        freshnessJob?.cancel()
        mapOpJob?.cancel()

        // A replacement client begins a separate position history and cannot inherit an old pose.
        synchronized(positionLock) {
            _livePosition.value = LiveRobotPosition(motionEpoch = _livePosition.value.motionEpoch + 1)
            lastValidSampleMillis = 0L
            lastGridMapId = ""
            positionConnectionWasActive = false
            positionReconnectPending = false
            trailBreakPending = false
        }
        _occupancyGrid.value = null
        activeClient = client

        mapOpJob = scope.launch {
            client.mapOperationState.collect { opState ->
                _mapOperationState.value = opState
            }
        }

        gridJob = scope.launch {
            client.occupancyGrid.collect { grid ->
                if (client !== activeClient) return@collect
                // Keep the last grid visible with a stale pose until a new session begins.
                if (grid != null || client.connectionState.value is ConnectionState.Connected) {
                    _occupancyGrid.value = grid
                }
                // A different grid map invalidates the previous map's path immediately.
                synchronized(positionLock) {
                    val mapId = grid?.mapId.orEmpty()
                    if (mapId.isNotBlank() && lastGridMapId.isNotBlank() && mapId != lastGridMapId) {
                        _livePosition.value = _livePosition.value.copy(
                            availability = PositionAvailability.UNAVAILABLE,
                            trailSegments = emptyList(),
                            motionEpoch = _livePosition.value.motionEpoch + 1
                        )
                        lastValidSampleMillis = 0L
                        trailBreakPending = true
                    }
                    if (mapId.isNotBlank()) lastGridMapId = mapId
                }
            }
        }

        connJob = scope.launch {
            client.connectionState.collect { state ->
                if (client !== activeClient) return@collect
                _connectionStatus.value = when (state) {
                    is ConnectionState.Connected -> ConnectionStatus.CONNECTED
                    is ConnectionState.Connecting -> ConnectionStatus.CONNECTING
                    is ConnectionState.Authenticating -> ConnectionStatus.CONNECTING
                    is ConnectionState.Reconnecting -> ConnectionStatus.CONNECTING
                    is ConnectionState.Failed -> ConnectionStatus.FAILED
                    is ConnectionState.Disconnected -> ConnectionStatus.DISCONNECTED
                }
                // Connection loss freezes the last pose; a new session starts with no assumed pose.
                synchronized(positionLock) {
                    val current = _livePosition.value
                    val isActive = state is ConnectionState.Connected
                    if (isActive && positionReconnectPending) {
                        _livePosition.value = LiveRobotPosition(motionEpoch = current.motionEpoch + 1)
                        lastValidSampleMillis = 0L
                        trailBreakPending = false
                        positionReconnectPending = false
                        _occupancyGrid.value = null
                        lastGridMapId = ""
                    } else if (!isActive) {
                        if (positionConnectionWasActive) positionReconnectPending = true
                        if (current.availability == PositionAvailability.FRESH) {
                            _livePosition.value = current.copy(availability = PositionAvailability.STALE, motionEpoch = current.motionEpoch + 1)
                            trailBreakPending = true
                        }
                    }
                    positionConnectionWasActive = isActive
                }
            }
        }

        freshnessJob = scope.launch {
            client.telemetryFreshness.collect { freshness ->
                if (client !== activeClient) return@collect
                // The SDK owns the timeout and timestamps even identical stationary packets.
                synchronized(positionLock) {
                    val current = _livePosition.value
                    if (current.pose != null && freshness.receivedAtMillis > 0L) {
                        if (freshness.isStale && current.availability == PositionAvailability.FRESH) {
                            _livePosition.value = current.copy(availability = PositionAvailability.STALE, motionEpoch = current.motionEpoch + 1)
                            trailBreakPending = true
                        } else if (!freshness.isStale && freshness.receivedAtMillis > current.receivedAtMillis) {
                            // Refresh age without adding a duplicate point; a long pause starts a new trail segment.
                            if (lastValidSampleMillis > 0L && freshness.receivedAtMillis - lastValidSampleMillis > 3_000L) {
                                trailBreakPending = true
                            }
                            lastValidSampleMillis = freshness.receivedAtMillis
                            _livePosition.value = current.copy(
                                receivedAtMillis = freshness.receivedAtMillis,
                                availability = if (client.connectionState.value is ConnectionState.Connected) PositionAvailability.FRESH else current.availability
                            )
                        }
                    }
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
                if (client !== activeClient) return@collect
                if (telem != null) {
                    // Only valid finite map poses enter the position and trail flows.
                    acceptPositionTelemetry(telem, client.telemetryFreshness.value)
                    if (isValidMapPose(telem)) {
                        _position.value = RobotPosition(telem.xMeters, telem.yMeters, Math.toDegrees(telem.yawRadians))
                    }
                    _navigationStatus.value = when (telem.navigationState.uppercase()) {
                        "NAVIGATING" -> NavigationStatus.NAVIGATING
                        "CHARGING" -> NavigationStatus.CHARGING
                        "CANCELLED" -> NavigationStatus.CANCELLED
                        else -> NavigationStatus.IDLE
                    }
                    _telemetry.value = telem
                } else {
                    // The SDK clears telemetry on connection loss; retain only a stale last-known pose.
                    synchronized(positionLock) {
                        val current = _livePosition.value
                        if (current.pose != null && current.availability == PositionAvailability.FRESH) {
                            _livePosition.value = current.copy(availability = PositionAvailability.STALE, motionEpoch = current.motionEpoch + 1)
                            trailBreakPending = true
                        }
                    }
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
                        // Intermediate operations remain in progress rather than appearing failed.
                        com.alokrathava.sdk.model.DockingState.PRESTAGING -> DockingStatus.PRESTAGING
                        com.alokrathava.sdk.model.DockingState.DOCKING -> DockingStatus.DOCKING
                        com.alokrathava.sdk.model.DockingState.UNDOCKING -> DockingStatus.UNDOCKING
                        com.alokrathava.sdk.model.DockingState.CANCELLED -> DockingStatus.CANCELLED
                        com.alokrathava.sdk.model.DockingState.FAILED -> DockingStatus.DOCKING_FAILED
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

    /** Accepts one real SDK telemetry sample and updates the shared pose and bounded trail. */
    private fun acceptPositionTelemetry(telem: RobotTelemetry, freshness: TelemetryFreshness) {
        synchronized(positionLock) {
            val current = _livePosition.value
            val mapChanged = current.activeMap.isNotBlank() && telem.activeMap.isNotBlank() &&
                current.activeMap != telem.activeMap
            val base = if (mapChanged) {
                lastValidSampleMillis = 0L
                trailBreakPending = false
                LiveRobotPosition(motionEpoch = current.motionEpoch + 1)
            } else current
            val connected = activeClient.connectionState.value is ConnectionState.Connected
            val valid = isValidMapPose(telem) && connected && !freshness.isStale && freshness.receivedAtMillis > 0L

            if (!valid) {
                // Invalid coordinates remove the marker; disconnected samples cannot start a trail.
                lastValidSampleMillis = 0L
                trailBreakPending = true
                _livePosition.value = base.copy(
                    pose = null,
                    availability = PositionAvailability.UNAVAILABLE,
                    activeMap = telem.activeMap,
                    localizationState = telem.localizationState,
                    linearVelocityMps = null,
                    motionEpoch = base.motionEpoch + 1
                )
                return
            }

            val pose = Pose2D(telem.xMeters, telem.yMeters, telem.yawRadians)
            val lastPoint = base.trailSegments.lastOrNull()?.lastOrNull()
            val distance = lastPoint?.let { hypot(pose.xMeters - it.xMeters, pose.yMeters - it.yMeters) }
            val gap = lastValidSampleMillis > 0L && freshness.receivedAtMillis - lastValidSampleMillis > 3_000L
            val breakTrail = lastPoint == null || trailBreakPending || gap || (distance != null && distance > 2.0)
            val nextTrail = when {
                breakTrail -> base.trailSegments + listOf(listOf(pose))
                distance != null && distance < 0.05 -> base.trailSegments
                else -> base.trailSegments.dropLast(1) + listOf(base.trailSegments.last() + pose)
            }
            // Keep at most 300 received points, dropping oldest history first.
            var boundedTrail = nextTrail
            while (boundedTrail.sumOf { it.size } > 300) {
                boundedTrail = if (boundedTrail.first().size == 1) boundedTrail.drop(1)
                else listOf(boundedTrail.first().drop(1)) + boundedTrail.drop(1)
            }
            lastValidSampleMillis = freshness.receivedAtMillis
            trailBreakPending = false
            _livePosition.value = base.copy(
                pose = pose,
                availability = PositionAvailability.FRESH,
                localizationState = telem.localizationState,
                activeMap = telem.activeMap,
                linearVelocityMps = telem.linearVelocityMps.takeIf { it.isFinite() },
                receivedAtMillis = freshness.receivedAtMillis,
                trailSegments = boundedTrail,
                motionEpoch = base.motionEpoch + if (breakTrail && lastPoint != null) 1 else 0
            )
        }
    }

    /** Rejects invalid, non-map-frame, and non-finite pose samples before UI use. */
    private fun isValidMapPose(telem: RobotTelemetry): Boolean =
        telem.poseValid && telem.frame.trim().equals("map", ignoreCase = true) &&
            telem.xMeters.isFinite() && telem.yMeters.isFinite() && telem.yawRadians.isFinite()

    override fun setManualVelocity(linearMps: Double, angularRadPerSec: Double) {
        velocityJob?.cancel()
        velocityJob = scope.launch {
            activeClient.setManualVelocity(linearMps, angularRadPerSec)
        }
    }

    override fun move(direction: DirectionCommand) {
        val speed = _simulationConfig.value.movementSpeedMps.coerceAtLeast(0.1)
        val angularSpeed = 1.0
        val (linear, angular) = when (direction) {
            DirectionCommand.FORWARD -> Pair(speed, 0.0)
            DirectionCommand.BACKWARD -> Pair(-speed, 0.0)
            DirectionCommand.LEFT -> Pair(0.0, angularSpeed)
            DirectionCommand.RIGHT -> Pair(0.0, -angularSpeed)
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
        // Legacy callers may refresh the cached pose, but invalid telemetry never replaces it.
        if (telem != null && isValidMapPose(telem)) {
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

    // Scans nearby Wi-Fi networks using WifiManager when location and Wi-Fi state permissions are granted.
    // Falls back gracefully to labeled demonstration network entries if permissions or scan results are unavailable.
    @SuppressLint("MissingPermission")
    override fun refreshAvailableNetworks() {
        try {
            val wifiManager = context.applicationContext.getSystemService(android.content.Context.WIFI_SERVICE) as? android.net.wifi.WifiManager
            val hasFineLocation = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            val hasWifiState = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_WIFI_STATE
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            if (wifiManager != null && hasWifiState) {
                val scanResults = if (hasFineLocation) {
                    try { wifiManager.scanResults } catch (_: Exception) { emptyList() }
                } else {
                    emptyList()
                }

                val networks = if (scanResults.isNotEmpty()) {
                    scanResults.map { result ->
                        val ssid = result.SSID.ifBlank { "Unknown Network" }
                        val rssi = result.level
                        val signalPercent = android.net.wifi.WifiManager.calculateSignalLevel(rssi, 100).coerceIn(0, 100)
                        val isSecured = result.capabilities.contains("WPA") || result.capabilities.contains("WEP") || result.capabilities.contains("RSN")
                        WifiNetwork(ssid = ssid, signalPercent = signalPercent, isSecured = isSecured)
                    }
                } else {
                    listOf(
                        WifiNetwork(ssid = "ROBOT_HOTSPOT_5G (Demo)", signalPercent = 95, isSecured = true),
                        WifiNetwork(ssid = "ROBOT_OFFICE_WIFI (Demo)", signalPercent = 80, isSecured = true),
                        WifiNetwork(ssid = "ROBOT_LAB_2G (Demo)", signalPercent = 65, isSecured = false)
                    )
                }
                _availableNetworks.value = networks
                return
            }
        } catch (e: Exception) {
            android.util.Log.w("RobotRepository", "Failed to scan Wi-Fi networks: ${e.message}")
        }

        _availableNetworks.value = listOf(
            WifiNetwork(ssid = "ROBOT_HOTSPOT_5G (Demo)", signalPercent = 95, isSecured = true),
            WifiNetwork(ssid = "ROBOT_OFFICE_WIFI (Demo)", signalPercent = 80, isSecured = true),
            WifiNetwork(ssid = "ROBOT_LAB_2G (Demo)", signalPercent = 65, isSecured = false)
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

    /**
     * Initiates start mapping request on active client with session recovery options.
     */
    override suspend fun startMapping(
        resumeUnsaved: Boolean,
        discardUnsaved: Boolean
    ): RobotResult<Unit> = activeClient.startMapping(resumeUnsaved, discardUnsaved)

    override suspend fun stopMapping(discardUnsaved: Boolean): RobotResult<Unit> = activeClient.stopMapping(discardUnsaved)

    override suspend fun listMaps(): RobotResult<List<RobotMap>> = activeClient.listMaps()

    override suspend fun switchMap(mapId: String): RobotResult<Unit> = activeClient.switchMap(mapId)

    override suspend fun saveCurrentMap(name: String): RobotResult<RobotMap> = activeClient.saveCurrentMap(name)

    override suspend fun renameMap(mapId: String, newName: String): RobotResult<RobotMap> = activeClient.renameMap(mapId, newName)

    override suspend fun deleteMap(mapId: String): RobotResult<Unit> = activeClient.deleteMap(mapId)

    override suspend fun listSavedLocations(mapId: String): RobotResult<List<SavedLocation>> = activeClient.listSavedLocations(mapId)

    override suspend fun saveLocation(name: String, pose: Pose2D, mapId: String?): RobotResult<SavedLocation> = activeClient.saveLocation(name, pose, mapId)

    override suspend fun deleteLocation(id: String): RobotResult<Unit> = activeClient.deleteLocation(id)

    override suspend fun navigateToLocation(id: String): RobotResult<CommandId> = activeClient.navigateToLocation(id)

    override suspend fun updateMotionLimits(limits: MotionLimits): RobotResult<Unit> = activeClient.updateMotionLimits(limits)

    override fun getActiveRobotClient(): RobotClient = activeClient
}
