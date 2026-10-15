package com.alokrathava.sdk.internal

import com.alokrathava.sdk.RobotAuthentication
import com.alokrathava.sdk.RobotClient
import com.alokrathava.sdk.RobotLogEvent
import com.alokrathava.sdk.RobotLogLevel
import com.alokrathava.sdk.RobotSdkConfig
import com.alokrathava.sdk.error.ErrorSeverity
import com.alokrathava.sdk.error.RobotError
import com.alokrathava.sdk.error.RobotResult
import com.alokrathava.sdk.internal.protocol.PendingCommandRegistry
import com.alokrathava.sdk.internal.serialization.ProtocolCodec
import com.alokrathava.sdk.internal.transport.RobotTransport
import com.alokrathava.sdk.internal.transport.TransportListener
import com.alokrathava.sdk.internal.transport.WebSocketRobotTransport
import com.alokrathava.sdk.model.CommandId
import com.alokrathava.sdk.model.ConnectionMetrics
import com.alokrathava.sdk.model.ConnectionState
import com.alokrathava.sdk.RobotSdkBuildVersion
import com.alokrathava.sdk.internal.protocol.ROBOT_PROTOCOL_VERSION
import com.alokrathava.sdk.model.RobotDiagnostics
import com.alokrathava.sdk.model.RobotDiagnosticReport
import com.alokrathava.sdk.model.RobotLogEntry
import com.alokrathava.sdk.internal.protocol.RobotMapStateDto
import com.alokrathava.sdk.model.DockingState
import com.alokrathava.sdk.model.MapOperationState
import com.alokrathava.sdk.model.Pose2D
import com.alokrathava.sdk.model.RobotBatteryState
import com.alokrathava.sdk.model.RobotCapability
import com.alokrathava.sdk.model.RobotHealth
import com.alokrathava.sdk.model.RobotMap
import com.alokrathava.sdk.model.RobotTelemetry
import com.alokrathava.sdk.model.SafetyState
import com.alokrathava.sdk.model.TelemetryFreshness
import com.alokrathava.sdk.model.VirtualWall
import com.alokrathava.sdk.model.VirtualWallDraft
import com.alokrathava.sdk.model.VirtualWallType
import com.alokrathava.sdk.model.SavedLocation
import com.alokrathava.sdk.model.MapPointValidity
import com.alokrathava.sdk.model.NavigationRoute
import com.alokrathava.sdk.model.NavigationState
import com.alokrathava.sdk.model.MissionProgress
import com.alokrathava.sdk.model.RobotMission
import com.alokrathava.sdk.model.MotionLimits
import com.alokrathava.sdk.model.RobotConfiguration
import com.alokrathava.sdk.model.RobotInfo
import com.alokrathava.sdk.model.MapPoint
import com.alokrathava.sdk.model.RobotEvent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.Response
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.pow
import kotlin.random.Random

// Tracks the single map-stream subscription across acknowledgement, snapshot, and failure phases.
private enum class MapSubscriptionState {
    IDLE,
    REQUESTING,
    AWAITING_SNAPSHOT,
    ACTIVE,
    FAILED
}

private const val MIN_ORDINARY_COMMAND_INTERVAL_MS = 75L

/**
 * Owns one robot WebSocket session, including paced command dispatch and map subscription lifecycle.
 */
internal class RobotClientImpl(
    private val config: RobotSdkConfig,
    transportFactory: ((TransportListener) -> RobotTransport)? = null
) : RobotClient, TransportListener {

    private val sdkJob = SupervisorJob()
    private val scope = CoroutineScope(sdkJob + Dispatchers.IO)

    private val commandRegistry = PendingCommandRegistry()
    private val ordinaryCommandMutex = Mutex()
    private val connectionGeneration = AtomicLong(0L)
    private var lastOrdinarySendGeneration = -1L
    private var lastOrdinarySendNanos = 0L
    private val mapSubscriptionRetryDelaysMs = longArrayOf(250L, 500L, 1_000L)

    private val mapSubscriptionLock = Any()
    @Volatile private var mapSubscriptionState = MapSubscriptionState.IDLE
    @Volatile private var mapSubscriptionRequestId: String? = null
    private var rejectedMapSubscriptionRequestId: String? = null
    private val transport: RobotTransport = transportFactory?.invoke(this) ?: WebSocketRobotTransport(
        endpoint = config.endpoint,
        logger = config.logger,
        listener = this,
        sslSocketFactory = config.sslSocketFactory,
        trustManager = config.trustManager
    )

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _connectionMetrics = MutableStateFlow(ConnectionMetrics())
    override val connectionMetrics: StateFlow<ConnectionMetrics> = _connectionMetrics.asStateFlow()

    private val _telemetryFreshness = MutableStateFlow(TelemetryFreshness())
    override val telemetryFreshness: StateFlow<TelemetryFreshness> = _telemetryFreshness.asStateFlow()

    private val _telemetry = MutableStateFlow<RobotTelemetry?>(null)
    override val telemetry: StateFlow<RobotTelemetry?> = _telemetry.asStateFlow()

    private val _batteryState = MutableStateFlow<RobotBatteryState?>(null)
    override val batteryState: StateFlow<RobotBatteryState?> = _batteryState.asStateFlow()

    private val _safetyState = MutableStateFlow<SafetyState?>(null)
    override val safetyState: StateFlow<SafetyState?> = _safetyState.asStateFlow()

    private val _dockingState = MutableStateFlow<DockingState?>(null)
    override val dockingState: StateFlow<DockingState?> = _dockingState.asStateFlow()

    private val _health = MutableStateFlow<RobotHealth?>(null)
    override val health: StateFlow<RobotHealth?> = _health.asStateFlow()

    private val _activeMap = MutableStateFlow<RobotMap?>(null)
    override val activeMap: StateFlow<RobotMap?> = _activeMap.asStateFlow()

    private val _occupancyGrid = MutableStateFlow<com.alokrathava.sdk.model.RobotOccupancyGrid?>(null)
    override val occupancyGrid: StateFlow<com.alokrathava.sdk.model.RobotOccupancyGrid?> = _occupancyGrid.asStateFlow()

    private var mapStreamSequence: Long = -1L

    private val _mapOperationState = MutableStateFlow<MapOperationState>(MapOperationState.Idle)
    override val mapOperationState: StateFlow<MapOperationState> = _mapOperationState.asStateFlow()

    private val _navigation = MutableStateFlow(NavigationState())
    override val navigation: StateFlow<NavigationState> = _navigation.asStateFlow()

    private val _missionProgress = MutableStateFlow<MissionProgress?>(null)
    override val missionProgress: StateFlow<MissionProgress?> = _missionProgress.asStateFlow()

    private val _configuration = MutableStateFlow<RobotConfiguration?>(
        RobotConfiguration(motionLimits = MotionLimits(maxLinearVelocityMps = 1.0, maxAngularVelocityRadPerSec = 1.5))
    )
    override val configuration: StateFlow<RobotConfiguration?> = _configuration.asStateFlow()

    private val _robotInfo = MutableStateFlow<RobotInfo?>(null)
    override val robotInfo: StateFlow<RobotInfo?> = _robotInfo.asStateFlow()

    private val _diagnostics = MutableStateFlow(
        RobotDiagnostics(
            connection = _connectionMetrics.value,
            telemetryFreshness = _telemetryFreshness.value
        )
    )
    override val diagnostics: StateFlow<RobotDiagnostics> = _diagnostics.asStateFlow()

    private val _events = MutableSharedFlow<RobotEvent>(extraBufferCapacity = 64)
    override val events: SharedFlow<RobotEvent> = _events.asSharedFlow()

    private var connectHandshakeDeferred: CompletableDeferred<RobotResult<Unit>>? = null

    @Volatile private var isUserDisconnected = false
    @Volatile private var reconnectAttempt = 0
    @Volatile private var connectedTimeMillis = 0L
    @Volatile private var lastMessageTimeMillis = 0L
    @Volatile private var lastTelemetryTimeMillis = 0L
    @Volatile private var pingSentTimeMillis = 0L
    @Volatile private var hasMapState = false

    private var heartbeatJob: Job? = null
    private var staleCheckJob: Job? = null
    private var reconnectJob: Job? = null
    private var postAuthenticationJob: Job? = null
    private var mapSubscriptionJob: Job? = null

    override suspend fun connect(): RobotResult<Unit> {
        if (_connectionState.value is ConnectionState.Connected) {
            return RobotResult.Success(Unit)
        }

        isUserDisconnected = false
        reconnectAttempt = 0

        return performConnectAttempt(1)
    }

    private suspend fun performConnectAttempt(attempt: Int): RobotResult<Unit> {
        _connectionState.value = ConnectionState.Connecting(attempt)
        val deferred = CompletableDeferred<RobotResult<Unit>>()
        connectHandshakeDeferred = deferred

        transport.connect()

        val result = withTimeoutOrNull(config.commandTimeoutMs) {
            deferred.await()
        }

        return if (result != null) {
            result
        } else {
            val err = RobotError(
                code = "CONNECT_TIMEOUT",
                subsystem = "transport",
                severity = ErrorSeverity.ERROR,
                message = "Connection handshake timed out after ${config.commandTimeoutMs}ms waiting for response from ${config.endpoint.host}:${config.endpoint.port}. Check authentication token, protocol version, and gateway host binding.",
                recoverable = true
            )
            _connectionState.value = ConnectionState.Failed(err)
            RobotResult.Failure(err)
        }
    }

    override suspend fun disconnect() {
        isUserDisconnected = true
        cancelBackgroundJobs()
        _connectionState.value = ConnectionState.Disconnected
        transport.disconnect()
        clearStatesAndPending("Disconnected by user")
    }

    private fun checkCapability(capability: RobotCapability): RobotResult<Unit>? {
        val state = _connectionState.value
        if (state !is ConnectionState.Connected) {
            return RobotResult.Failure(
                RobotError("NOT_CONNECTED", "sdk", ErrorSeverity.ERROR, "Client is not connected", true)
            )
        }
        if (state.capabilities.isNotEmpty() && !state.capabilities.contains(capability)) {
            return RobotResult.Failure(
                RobotError("UNSUPPORTED_CAPABILITY", "sdk", ErrorSeverity.ERROR, "Gateway does not support capability '${capability.name}'", false)
            )
        }
        return null
    }

    override suspend fun navigateTo(pose: Pose2D): RobotResult<CommandId> {
        checkCapability(RobotCapability.NAVIGATION)?.let { return RobotResult.Failure((it as RobotResult.Failure).error) }

        val cmdId = commandRegistry.generateCommandId()
        val envelope = ProtocolCodec.createNavigateToEnvelope(cmdId, pose)
        val jsonText = ProtocolCodec.encodeEnvelope(envelope)

        val result = commandRegistry.registerAndAwait(cmdId, config.commandTimeoutMs) {
            // Keeps this ordinary request in the per-connection send order.
            if (!sendGatewayCommand(jsonText)) {
                commandRegistry.completeError(
                    cmdId,
                    RobotError("SEND_FAILED", "transport", ErrorSeverity.ERROR, "Failed to send WebSocket message", true)
                )
            }
        }

        return when (result) {
            is RobotResult.Success -> RobotResult.Success(CommandId(cmdId))
            is RobotResult.Failure -> RobotResult.Failure(result.error)
        }
    }

    override suspend fun navigateThrough(poses: List<Pose2D>): RobotResult<CommandId> {
        return sendNavigateThrough(poses, stopOnFailure = true)
    }

    private suspend fun sendNavigateThrough(poses: List<Pose2D>, stopOnFailure: Boolean): RobotResult<CommandId> {
        checkCapability(RobotCapability.WAYPOINT_NAVIGATION)?.let { return RobotResult.Failure((it as RobotResult.Failure).error) }

        if (poses.isEmpty()) {
            return RobotResult.Failure(RobotError("INVALID_ARGUMENT", "sdk", ErrorSeverity.ERROR, "Waypoints list cannot be empty", false))
        }

        val cmdId = commandRegistry.generateCommandId()
        val envelope = ProtocolCodec.createNavigateThroughEnvelope(cmdId, poses, stopOnFailure)
        val jsonText = ProtocolCodec.encodeEnvelope(envelope)

        val result = commandRegistry.registerAndAwait(cmdId, config.commandTimeoutMs) {
            // Keeps this ordinary request in the per-connection send order.
            if (!sendGatewayCommand(jsonText)) {
                commandRegistry.completeError(
                    cmdId,
                    RobotError("SEND_FAILED", "transport", ErrorSeverity.ERROR, "Failed to send WebSocket message", true)
                )
            }
        }

        return when (result) {
            is RobotResult.Success -> RobotResult.Success(CommandId(cmdId))
            is RobotResult.Failure -> RobotResult.Failure(result.error)
        }
    }

    override suspend fun navigateRoute(route: NavigationRoute): RobotResult<CommandId> {
        return sendNavigateThrough(route.waypoints, route.stopOnFailure)
    }

    override suspend fun cancelNavigation(): RobotResult<Unit> {
        checkCapability(RobotCapability.NAVIGATION)?.let { return it }
        return sendSimpleCommand("cancel_navigation")
    }

    override suspend fun setManualVelocity(linearMps: Double, angularRadPerSec: Double): RobotResult<Unit> {
        checkCapability(RobotCapability.MANUAL_CONTROL)?.let { return it }

        val cmdId = commandRegistry.generateCommandId()
        val envelope = ProtocolCodec.createManualVelocityEnvelope(cmdId, linearMps, angularRadPerSec)
        val jsonText = ProtocolCodec.encodeEnvelope(envelope)

        return commandRegistry.registerAndAwait(cmdId, config.commandTimeoutMs) {
            // A zero-velocity frame stops manual motion immediately, even while commands are queued.
            if (!sendGatewayCommand(jsonText, bypassPacing = linearMps == 0.0 && angularRadPerSec == 0.0)) {
                commandRegistry.completeError(
                    cmdId,
                    RobotError("SEND_FAILED", "transport", ErrorSeverity.ERROR, "Failed to send WebSocket message", true)
                )
            }
        }
    }

    override suspend fun stop(): RobotResult<Unit> {
        return sendSimpleCommand("stop")
    }

    override suspend fun dock(): RobotResult<Unit> {
        checkCapability(RobotCapability.DOCKING)?.let { return it }
        return sendSimpleCommand("dock")
    }

    override suspend fun undock(): RobotResult<Unit> {
        checkCapability(RobotCapability.DOCKING)?.let { return it }
        return sendSimpleCommand("undock")
    }

    override suspend fun cancelDocking(): RobotResult<Unit> {
        checkCapability(RobotCapability.DOCKING)?.let { return it }
        return sendSimpleCommand("cancel_docking")
    }

    override suspend fun emergencyStop(): RobotResult<Unit> {
        return sendSimpleCommand("emergency_stop")
    }

    override suspend fun releaseEmergencyStop(): RobotResult<Unit> {
        checkCapability(RobotCapability.SAFETY)?.let { return it }
        return sendSimpleCommand("release_emergency_stop")
    }

    override suspend fun listMaps(): RobotResult<List<RobotMap>> {
        checkCapability(RobotCapability.MAP_SWITCHING)?.let { return RobotResult.Failure((it as RobotResult.Failure).error) }

        val cmdId = commandRegistry.generateCommandId()
        val envelope = ProtocolCodec.createSimpleCommandEnvelope("list_maps", cmdId)
        val jsonText = ProtocolCodec.encodeEnvelope(envelope)

        return commandRegistry.registerAndAwaitTyped<List<RobotMap>>(cmdId, config.commandTimeouts.mapQueryMs) {
            // Keeps this ordinary request in the per-connection send order.
            if (!sendGatewayCommand(jsonText)) {
                commandRegistry.completeError(
                    cmdId,
                    RobotError("SEND_FAILED", "transport", ErrorSeverity.ERROR, "Failed to send WebSocket message", true)
                )
            }
        }
    }

    override suspend fun switchMap(mapId: String): RobotResult<Unit> {
        checkCapability(RobotCapability.MAP_SWITCHING)?.let { return it }

        if (mapId.isBlank() || mapId.contains("..") || mapId.contains("/") || mapId.contains("\\")) {
            return RobotResult.Failure(
                RobotError("INVALID_MAP_ID", "sdk", ErrorSeverity.ERROR, "Map ID is invalid or contains directory traversal attempt", false)
            )
        }

        _mapOperationState.value = MapOperationState.Switching(mapId)
        val cmdId = commandRegistry.generateCommandId()
        val envelope = ProtocolCodec.createSwitchMapEnvelope(cmdId, mapId)
        val jsonText = ProtocolCodec.encodeEnvelope(envelope)

        val result = commandRegistry.registerAndAwait(cmdId, config.commandTimeouts.mapSwitchMs) {
            // Keeps this ordinary request in the per-connection send order.
            if (!sendGatewayCommand(jsonText)) {
                commandRegistry.completeError(
                    cmdId,
                    RobotError("SEND_FAILED", "transport", ErrorSeverity.ERROR, "Failed to send WebSocket message", true)
                )
            }
        }
        when (result) {
            is RobotResult.Success -> if (!hasMapState) _mapOperationState.value = MapOperationState.Idle
            is RobotResult.Failure -> _mapOperationState.value = MapOperationState.Failed(mapId, result.error.message)
        }
        return result
    }

    override suspend fun saveCurrentMap(name: String): RobotResult<RobotMap> {
        checkCapability(RobotCapability.MAP_SWITCHING)?.let { return RobotResult.Failure((it as RobotResult.Failure).error) }

        if (name.isBlank()) {
            return RobotResult.Failure(
                RobotError("INVALID_MAP_NAME", "sdk", ErrorSeverity.ERROR, "Map name cannot be blank", false)
            )
        }

        _mapOperationState.value = MapOperationState.Saving(name)
        val cmdId = commandRegistry.generateCommandId()
        val envelope = ProtocolCodec.createSaveMapEnvelope(cmdId, name)
        val jsonText = ProtocolCodec.encodeEnvelope(envelope)

        val raw = commandRegistry.registerAndAwaitTyped<Any>(cmdId, config.commandTimeouts.mapSaveMs) {
            // Keeps this ordinary request in the per-connection send order.
            if (!sendGatewayCommand(jsonText)) {
                commandRegistry.completeError(
                    cmdId,
                    RobotError("SEND_FAILED", "transport", ErrorSeverity.ERROR, "Failed to send WebSocket message", true)
                )
            }
        }
        val result: RobotResult<RobotMap> = when (raw) {
            is RobotResult.Success -> RobotResult.Success(
                raw.value as? RobotMap
                    ?: RobotMap(id = name.lowercase().replace(Regex("[^a-z0-9_\\-]"), "_"), name = name)
            )
            is RobotResult.Failure -> RobotResult.Failure(raw.error)
        }
        when (result) {
            is RobotResult.Success -> if (!hasMapState) _mapOperationState.value = MapOperationState.Idle
            is RobotResult.Failure -> _mapOperationState.value = MapOperationState.Failed(null, result.error.message)
        }
        return result
    }

    override suspend fun renameMap(mapId: String, newName: String): RobotResult<RobotMap> {
        checkCapability(RobotCapability.MAP_SWITCHING)?.let { return RobotResult.Failure((it as RobotResult.Failure).error) }

        if (mapId.isBlank() || newName.isBlank()) {
            return RobotResult.Failure(
                RobotError("INVALID_ARGUMENT", "sdk", ErrorSeverity.ERROR, "Map ID and new name cannot be blank", false)
            )
        }

        val cmdId = commandRegistry.generateCommandId()
        val envelope = ProtocolCodec.createRenameMapEnvelope(cmdId, mapId, newName)
        val jsonText = ProtocolCodec.encodeEnvelope(envelope)

        return commandRegistry.registerAndAwaitTyped<RobotMap>(cmdId, config.commandTimeouts.mapQueryMs) {
            // Keeps this ordinary request in the per-connection send order.
            if (!sendGatewayCommand(jsonText)) {
                commandRegistry.completeError(
                    cmdId,
                    RobotError("SEND_FAILED", "transport", ErrorSeverity.ERROR, "Failed to send WebSocket message", true)
                )
            }
        }
    }

    override suspend fun deleteMap(mapId: String): RobotResult<Unit> {
        checkCapability(RobotCapability.MAP_SWITCHING)?.let { return it }

        if (mapId.isBlank() || mapId.contains("..") || mapId.contains("/") || mapId.contains("\\")) {
            return RobotResult.Failure(
                RobotError("INVALID_MAP_ID", "sdk", ErrorSeverity.ERROR, "Map ID is invalid or contains directory traversal attempt", false)
            )
        }

        if (activeMap.value?.id == mapId) {
            return RobotResult.Failure(
                RobotError("CANNOT_DELETE_ACTIVE_MAP", "sdk", ErrorSeverity.ERROR, "Cannot delete currently active map", false)
            )
        }

        _mapOperationState.value = MapOperationState.Deleting(mapId)
        val cmdId = commandRegistry.generateCommandId()
        val envelope = ProtocolCodec.createDeleteMapEnvelope(cmdId, mapId)
        val jsonText = ProtocolCodec.encodeEnvelope(envelope)

        val result = commandRegistry.registerAndAwait(cmdId, config.commandTimeouts.mapDeleteMs) {
            // Keeps this ordinary request in the per-connection send order.
            if (!sendGatewayCommand(jsonText)) {
                commandRegistry.completeError(
                    cmdId,
                    RobotError("SEND_FAILED", "transport", ErrorSeverity.ERROR, "Failed to send WebSocket message", true)
                )
            }
        }
        when (result) {
            is RobotResult.Success -> if (!hasMapState) _mapOperationState.value = MapOperationState.Idle
            is RobotResult.Failure -> _mapOperationState.value = MapOperationState.Failed(mapId, result.error.message)
        }
        return result
    }

    /**
     * Initiates SLAM mapping on the robot gateway with optional session recovery flags.
     * Logs diagnostic events and tracks request timing and gateway errors.
     */
    override suspend fun startMapping(
        resumeUnsaved: Boolean,
        discardUnsaved: Boolean
    ): RobotResult<Unit> {
        checkCapability(RobotCapability.MAPPING)?.let { return it }

        if (resumeUnsaved && discardUnsaved) {
            return RobotResult.Failure(
                RobotError(
                    code = "INVALID_ARGUMENT",
                    subsystem = "sdk",
                    severity = ErrorSeverity.ERROR,
                    message = "resumeUnsaved and discardUnsaved cannot both be true simultaneously",
                    recoverable = false
                )
            )
        }

        val startTime = System.currentTimeMillis()
        val cmdId = commandRegistry.generateCommandId()
        val envelope = ProtocolCodec.createStartMappingEnvelope(cmdId, resumeUnsaved, discardUnsaved)
        val jsonText = ProtocolCodec.encodeEnvelope(envelope)

        config.logger.log(
            RobotLogEvent(
                RobotLogLevel.INFO,
                "RobotSDK",
                "SEND type=start_mapping id=$cmdId payload=$jsonText"
            )
        )
        android.util.Log.d("RobotSDK", "SEND type=start_mapping id=$cmdId payload=$jsonText")

        val result = commandRegistry.registerAndAwait(cmdId, config.commandTimeouts.mappingMs) {
            // Keeps this ordinary request in the per-connection send order.
            if (!sendGatewayCommand(jsonText)) {
                commandRegistry.completeError(
                    cmdId,
                    RobotError("SEND_FAILED", "transport", ErrorSeverity.ERROR, "Failed to send WebSocket message", true)
                )
            }
        }

        val elapsedMs = System.currentTimeMillis() - startTime
        when (result) {
            is RobotResult.Success -> {
                config.logger.log(
                    RobotLogEvent(
                        RobotLogLevel.INFO,
                        "RobotSDK",
                        "RECEIVE command_ack id=$cmdId in ${elapsedMs}ms"
                    )
                )
                android.util.Log.d("RobotSDK", "RECEIVE type=command_ack id=$cmdId in ${elapsedMs}ms")
            }
            is RobotResult.Failure -> {
                config.logger.log(
                    RobotLogEvent(
                        RobotLogLevel.WARN,
                        "RobotSDK",
                        "ERROR code=${result.error.code} msg=${result.error.message} id=$cmdId in ${elapsedMs}ms"
                    )
                )
                android.util.Log.w(
                    "RobotSDK",
                    "ERROR code=${result.error.code} msg='${result.error.message}' id=$cmdId in ${elapsedMs}ms"
                )
            }
        }

        return result
    }

    override suspend fun stopMapping(discardUnsaved: Boolean): RobotResult<Unit> {
        checkCapability(RobotCapability.MAPPING)?.let { return it }

        val cmdId = commandRegistry.generateCommandId()
        val envelope = ProtocolCodec.createStopMappingEnvelope(cmdId, discardUnsaved)
        val jsonText = ProtocolCodec.encodeEnvelope(envelope)

        return commandRegistry.registerAndAwait(cmdId, config.commandTimeouts.mappingMs) {
            // Keeps this ordinary request in the per-connection send order.
            if (!sendGatewayCommand(jsonText)) {
                commandRegistry.completeError(
                    cmdId,
                    RobotError("SEND_FAILED", "transport", ErrorSeverity.ERROR, "Failed to send WebSocket message", true)
                )
            }
        }
    }

    override suspend fun listVirtualWalls(mapId: String): RobotResult<List<VirtualWall>> {
        checkCapability(RobotCapability.VIRTUAL_WALLS)?.let { return RobotResult.Failure((it as RobotResult.Failure).error) }

        if (mapId.isBlank() || mapId.contains("..") || mapId.contains("/") || mapId.contains("\\")) {
            return RobotResult.Failure(
                RobotError("INVALID_MAP_ID", "sdk", ErrorSeverity.ERROR, "Map ID is invalid or contains path traversal attempt", false)
            )
        }

        val cmdId = commandRegistry.generateCommandId()
        val envelope = ProtocolCodec.createListVirtualWallsEnvelope(cmdId, mapId)
        val jsonText = ProtocolCodec.encodeEnvelope(envelope)

        return commandRegistry.registerAndAwaitTyped<List<VirtualWall>>(cmdId, config.commandTimeoutMs) {
            // Keeps this ordinary request in the per-connection send order.
            if (!sendGatewayCommand(jsonText)) {
                commandRegistry.completeError(
                    cmdId,
                    RobotError("SEND_FAILED", "transport", ErrorSeverity.ERROR, "Failed to send WebSocket message", true)
                )
            }
        }
    }

    override suspend fun createVirtualWall(wall: VirtualWallDraft): RobotResult<VirtualWall> {
        checkCapability(RobotCapability.VIRTUAL_WALLS)?.let { return RobotResult.Failure((it as RobotResult.Failure).error) }

        val validationError = validateVirtualWallPoints(wall.type, wall.points)
        if (validationError != null) return RobotResult.Failure(validationError)

        val cmdId = commandRegistry.generateCommandId()
        val envelope = ProtocolCodec.createCreateVirtualWallEnvelope(cmdId, wall)
        val jsonText = ProtocolCodec.encodeEnvelope(envelope)

        return commandRegistry.registerAndAwaitTyped<VirtualWall>(cmdId, config.commandTimeoutMs) {
            // Keeps this ordinary request in the per-connection send order.
            if (!sendGatewayCommand(jsonText)) {
                commandRegistry.completeError(
                    cmdId,
                    RobotError("SEND_FAILED", "transport", ErrorSeverity.ERROR, "Failed to send WebSocket message", true)
                )
            }
        }
    }

    override suspend fun updateVirtualWall(wall: VirtualWall): RobotResult<VirtualWall> {
        checkCapability(RobotCapability.VIRTUAL_WALLS)?.let { return RobotResult.Failure((it as RobotResult.Failure).error) }

        if (wall.id.isBlank()) {
            return RobotResult.Failure(RobotError("INVALID_WALL_ID", "sdk", ErrorSeverity.ERROR, "Virtual wall ID cannot be blank", false))
        }

        val validationError = validateVirtualWallPoints(wall.type, wall.points)
        if (validationError != null) return RobotResult.Failure(validationError)

        val cmdId = commandRegistry.generateCommandId()
        val envelope = ProtocolCodec.createUpdateVirtualWallEnvelope(cmdId, wall)
        val jsonText = ProtocolCodec.encodeEnvelope(envelope)

        return commandRegistry.registerAndAwaitTyped<VirtualWall>(cmdId, config.commandTimeoutMs) {
            // Keeps this ordinary request in the per-connection send order.
            if (!sendGatewayCommand(jsonText)) {
                commandRegistry.completeError(
                    cmdId,
                    RobotError("SEND_FAILED", "transport", ErrorSeverity.ERROR, "Failed to send WebSocket message", true)
                )
            }
        }
    }

    override suspend fun deleteVirtualWall(wallId: String): RobotResult<Unit> {
        checkCapability(RobotCapability.VIRTUAL_WALLS)?.let { return it }

        if (wallId.isBlank()) {
            return RobotResult.Failure(RobotError("INVALID_WALL_ID", "sdk", ErrorSeverity.ERROR, "Virtual wall ID cannot be blank", false))
        }

        val cmdId = commandRegistry.generateCommandId()
        val envelope = ProtocolCodec.createDeleteVirtualWallEnvelope(cmdId, wallId)
        val jsonText = ProtocolCodec.encodeEnvelope(envelope)

        return commandRegistry.registerAndAwait(cmdId, config.commandTimeoutMs) {
            // Keeps this ordinary request in the per-connection send order.
            if (!sendGatewayCommand(jsonText)) {
                commandRegistry.completeError(
                    cmdId,
                    RobotError("SEND_FAILED", "transport", ErrorSeverity.ERROR, "Failed to send WebSocket message", true)
                )
            }
        }
    }

    override suspend fun setVirtualWallEnabled(wallId: String, enabled: Boolean): RobotResult<Unit> {
        checkCapability(RobotCapability.VIRTUAL_WALLS)?.let { return it }

        if (wallId.isBlank()) {
            return RobotResult.Failure(RobotError("INVALID_WALL_ID", "sdk", ErrorSeverity.ERROR, "Virtual wall ID cannot be blank", false))
        }

        val cmdId = commandRegistry.generateCommandId()
        val envelope = ProtocolCodec.createSetVirtualWallEnabledEnvelope(cmdId, wallId, enabled)
        val jsonText = ProtocolCodec.encodeEnvelope(envelope)

        return commandRegistry.registerAndAwait(cmdId, config.commandTimeoutMs) {
            // Keeps this ordinary request in the per-connection send order.
            if (!sendGatewayCommand(jsonText)) {
                commandRegistry.completeError(
                    cmdId,
                    RobotError("SEND_FAILED", "transport", ErrorSeverity.ERROR, "Failed to send WebSocket message", true)
                )
            }
        }
    }

    override suspend fun clearVirtualWalls(mapId: String): RobotResult<Unit> {
        checkCapability(RobotCapability.VIRTUAL_WALLS)?.let { return it }

        if (mapId.isBlank() || mapId.contains("..") || mapId.contains("/") || mapId.contains("\\")) {
            return RobotResult.Failure(
                RobotError("INVALID_MAP_ID", "sdk", ErrorSeverity.ERROR, "Map ID is invalid or contains path traversal attempt", false)
            )
        }

        val cmdId = commandRegistry.generateCommandId()
        val envelope = ProtocolCodec.createClearVirtualWallsEnvelope(cmdId, mapId)
        val jsonText = ProtocolCodec.encodeEnvelope(envelope)

        return commandRegistry.registerAndAwait(cmdId, config.commandTimeoutMs) {
            // Keeps this ordinary request in the per-connection send order.
            if (!sendGatewayCommand(jsonText)) {
                commandRegistry.completeError(
                    cmdId,
                    RobotError("SEND_FAILED", "transport", ErrorSeverity.ERROR, "Failed to send WebSocket message", true)
                )
            }
        }
    }

    override suspend fun listSavedLocations(mapId: String): RobotResult<List<SavedLocation>> {
        checkCapability(RobotCapability.SAVED_LOCATIONS)?.let { return RobotResult.Failure((it as RobotResult.Failure).error) }

        if (mapId.isBlank() || mapId.contains("..") || mapId.contains("/") || mapId.contains("\\")) {
            return RobotResult.Failure(
                RobotError("INVALID_MAP_ID", "sdk", ErrorSeverity.ERROR, "Map ID is invalid or contains path traversal attempt", false)
            )
        }

        val cmdId = commandRegistry.generateCommandId()
        val envelope = ProtocolCodec.createListSavedLocationsEnvelope(cmdId, mapId)
        val jsonText = ProtocolCodec.encodeEnvelope(envelope)

        return commandRegistry.registerAndAwaitTyped<List<SavedLocation>>(cmdId, config.commandTimeoutMs) {
            // Keeps this ordinary request in the per-connection send order.
            if (!sendGatewayCommand(jsonText)) {
                commandRegistry.completeError(
                    cmdId,
                    RobotError("SEND_FAILED", "transport", ErrorSeverity.ERROR, "Failed to send WebSocket message", true)
                )
            }
        }
    }

    override suspend fun saveLocation(name: String, pose: Pose2D, mapId: String?): RobotResult<SavedLocation> {
        checkCapability(RobotCapability.SAVED_LOCATIONS)?.let { return RobotResult.Failure((it as RobotResult.Failure).error) }

        if (name.isBlank()) {
            return RobotResult.Failure(RobotError("INVALID_ARGUMENT", "sdk", ErrorSeverity.ERROR, "Location name cannot be blank", false))
        }

        val cmdId = commandRegistry.generateCommandId()
        val envelope = ProtocolCodec.createSaveLocationEnvelope(cmdId, name, pose, mapId)
        val jsonText = ProtocolCodec.encodeEnvelope(envelope)

        return commandRegistry.registerAndAwaitTyped<SavedLocation>(cmdId, config.commandTimeoutMs) {
            // Keeps this ordinary request in the per-connection send order.
            if (!sendGatewayCommand(jsonText)) {
                commandRegistry.completeError(
                    cmdId,
                    RobotError("SEND_FAILED", "transport", ErrorSeverity.ERROR, "Failed to send WebSocket message", true)
                )
            }
        }
    }

    override suspend fun renameLocation(id: String, name: String): RobotResult<SavedLocation> {
        checkCapability(RobotCapability.SAVED_LOCATIONS)?.let { return RobotResult.Failure((it as RobotResult.Failure).error) }

        if (id.isBlank() || name.isBlank()) {
            return RobotResult.Failure(RobotError("INVALID_ARGUMENT", "sdk", ErrorSeverity.ERROR, "Location ID and new name cannot be blank", false))
        }

        val cmdId = commandRegistry.generateCommandId()
        val envelope = ProtocolCodec.createRenameLocationEnvelope(cmdId, id, name)
        val jsonText = ProtocolCodec.encodeEnvelope(envelope)

        return commandRegistry.registerAndAwaitTyped<SavedLocation>(cmdId, config.commandTimeoutMs) {
            // Keeps this ordinary request in the per-connection send order.
            if (!sendGatewayCommand(jsonText)) {
                commandRegistry.completeError(
                    cmdId,
                    RobotError("SEND_FAILED", "transport", ErrorSeverity.ERROR, "Failed to send WebSocket message", true)
                )
            }
        }
    }

    override suspend fun deleteLocation(id: String): RobotResult<Unit> {
        checkCapability(RobotCapability.SAVED_LOCATIONS)?.let { return it }

        if (id.isBlank()) {
            return RobotResult.Failure(RobotError("INVALID_ARGUMENT", "sdk", ErrorSeverity.ERROR, "Location ID cannot be blank", false))
        }

        val cmdId = commandRegistry.generateCommandId()
        val envelope = ProtocolCodec.createDeleteLocationEnvelope(cmdId, id)
        val jsonText = ProtocolCodec.encodeEnvelope(envelope)

        return commandRegistry.registerAndAwait(cmdId, config.commandTimeoutMs) {
            // Keeps this ordinary request in the per-connection send order.
            if (!sendGatewayCommand(jsonText)) {
                commandRegistry.completeError(
                    cmdId,
                    RobotError("SEND_FAILED", "transport", ErrorSeverity.ERROR, "Failed to send WebSocket message", true)
                )
            }
        }
    }

    override suspend fun navigateToLocation(id: String): RobotResult<CommandId> {
        checkCapability(RobotCapability.NAVIGATION)?.let { return RobotResult.Failure((it as RobotResult.Failure).error) }

        if (id.isBlank()) {
            return RobotResult.Failure(RobotError("INVALID_ARGUMENT", "sdk", ErrorSeverity.ERROR, "Location ID cannot be blank", false))
        }

        val cmdId = commandRegistry.generateCommandId()
        val envelope = ProtocolCodec.createNavigateToLocationEnvelope(cmdId, id)
        val jsonText = ProtocolCodec.encodeEnvelope(envelope)

        val res = commandRegistry.registerAndAwait(cmdId, config.commandTimeoutMs) {
            // Keeps this ordinary request in the per-connection send order.
            if (!sendGatewayCommand(jsonText)) {
                commandRegistry.completeError(
                    cmdId,
                    RobotError("SEND_FAILED", "transport", ErrorSeverity.ERROR, "Failed to send WebSocket message", true)
                )
            }
        }
        return when (res) {
            is RobotResult.Success -> RobotResult.Success(CommandId(cmdId))
            is RobotResult.Failure -> RobotResult.Failure(res.error)
        }
    }

    override suspend fun checkMapPoint(point: MapPoint): RobotResult<MapPointValidity> {
        val cmdId = commandRegistry.generateCommandId()
        val envelope = ProtocolCodec.createCheckMapPointEnvelope(cmdId, point)
        val jsonText = ProtocolCodec.encodeEnvelope(envelope)

        return commandRegistry.registerAndAwaitTyped<MapPointValidity>(cmdId, config.commandTimeoutMs) {
            // Keeps this ordinary request in the per-connection send order.
            if (!sendGatewayCommand(jsonText)) {
                commandRegistry.completeError(
                    cmdId,
                    RobotError("SEND_FAILED", "transport", ErrorSeverity.ERROR, "Failed to send WebSocket message", true)
                )
            }
        }
    }

    override suspend fun submitMission(mission: RobotMission): RobotResult<CommandId> {
        checkCapability(RobotCapability.MISSIONS)?.let { return RobotResult.Failure((it as RobotResult.Failure).error) }

        if (mission.steps.isEmpty()) {
            return RobotResult.Failure(RobotError("INVALID_ARGUMENT", "sdk", ErrorSeverity.ERROR, "Mission steps cannot be empty", false))
        }

        val cmdId = commandRegistry.generateCommandId()
        val envelope = ProtocolCodec.createSubmitMissionEnvelope(cmdId, mission)
        val jsonText = ProtocolCodec.encodeEnvelope(envelope)

        val result = commandRegistry.registerAndAwait(cmdId, config.commandTimeoutMs) {
            // Keeps this ordinary request in the per-connection send order.
            if (!sendGatewayCommand(jsonText)) {
                commandRegistry.completeError(
                    cmdId,
                    RobotError("SEND_FAILED", "transport", ErrorSeverity.ERROR, "Failed to send WebSocket message", true)
                )
            }
        }

        return when (result) {
            is RobotResult.Success -> RobotResult.Success(CommandId(cmdId))
            is RobotResult.Failure -> RobotResult.Failure(result.error)
        }
    }

    override suspend fun cancelMission(): RobotResult<Unit> {
        checkCapability(RobotCapability.MISSIONS)?.let { return it }

        val cmdId = commandRegistry.generateCommandId()
        val envelope = ProtocolCodec.createCancelMissionEnvelope(cmdId)
        val jsonText = ProtocolCodec.encodeEnvelope(envelope)

        return commandRegistry.registerAndAwait(cmdId, config.commandTimeoutMs) {
            // Mission cancellation can halt autonomous motion and must not wait behind ordinary commands.
            if (!sendGatewayCommand(jsonText, bypassPacing = true)) {
                commandRegistry.completeError(
                    cmdId,
                    RobotError("SEND_FAILED", "transport", ErrorSeverity.ERROR, "Failed to send WebSocket message", true)
                )
            }
        }
    }

    override suspend fun updateMotionLimits(limits: MotionLimits): RobotResult<Unit> {
        checkCapability(RobotCapability.ROBOT_CONFIGURATION)?.let { return it }

        if (limits.maxLinearVelocityMps <= 0.0 || limits.maxAngularVelocityRadPerSec <= 0.0) {
            return RobotResult.Failure(RobotError("INVALID_ARGUMENT", "sdk", ErrorSeverity.ERROR, "Velocity limits must be positive numbers", false))
        }

        val cmdId = commandRegistry.generateCommandId()
        val envelope = ProtocolCodec.createUpdateMotionLimitsEnvelope(cmdId, limits)
        val jsonText = ProtocolCodec.encodeEnvelope(envelope)

        val res = commandRegistry.registerAndAwait(cmdId, config.commandTimeoutMs) {
            // Keeps this ordinary request in the per-connection send order.
            if (!sendGatewayCommand(jsonText)) {
                commandRegistry.completeError(
                    cmdId,
                    RobotError("SEND_FAILED", "transport", ErrorSeverity.ERROR, "Failed to send WebSocket message", true)
                )
            }
        }
        if (res is RobotResult.Success) {
            val current = _configuration.value
            if (current != null) {
                _configuration.value = current.copy(motionLimits = limits)
            } else {
                _configuration.value = RobotConfiguration(motionLimits = limits)
            }
        }
        return res
    }

    override suspend fun collectDiagnosticReport(): RobotResult<RobotDiagnosticReport> {
        val currentDiag = RobotDiagnostics(
            connection = connectionMetrics.value,
            telemetryFreshness = telemetryFreshness.value,
            activeMapId = activeMap.value?.id,
            navigationAvailable = connectionState.value is ConnectionState.Connected,
            safetyAvailable = connectionState.value is ConnectionState.Connected,
            dockingAvailable = connectionState.value is ConnectionState.Connected
        )
        val activeErrors = health.value?.activeErrors?.map { it.code } ?: emptyList()
        val report = RobotDiagnosticReport(
            sdkVersion = RobotSdkBuildVersion.SDK_VERSION,
            gatewayVersion = (connectionState.value as? ConnectionState.Connected)?.gatewayVersion ?: "unknown",
            protocolVersion = ROBOT_PROTOCOL_VERSION,
            diagnostics = currentDiag,
            recentErrorCodes = activeErrors
        )
        return RobotResult.Success(report)
    }

    override suspend fun getRecentRobotLogs(limit: Int): RobotResult<List<RobotLogEntry>> {
        checkCapability(RobotCapability.LOG_RETRIEVAL)?.let { return RobotResult.Failure((it as RobotResult.Failure).error) }

        val cmdId = commandRegistry.generateCommandId()
        val envelope = ProtocolCodec.createGetRecentLogsEnvelope(cmdId, limit)
        val jsonText = ProtocolCodec.encodeEnvelope(envelope)

        return commandRegistry.registerAndAwaitTyped<List<RobotLogEntry>>(cmdId, config.commandTimeoutMs) {
            // Keeps this ordinary request in the per-connection send order.
            if (!sendGatewayCommand(jsonText)) {
                commandRegistry.completeError(
                    cmdId,
                    RobotError("SEND_FAILED", "transport", ErrorSeverity.ERROR, "Failed to send WebSocket message", true)
                )
            }
        }
    }

    private fun validateVirtualWallPoints(type: VirtualWallType, points: List<MapPoint>): RobotError? {
        val minPoints = if (type == VirtualWallType.LINE) 2 else 3
        if (points.size < minPoints) {
            return RobotError("INVALID_GEOMETRY", "sdk", ErrorSeverity.ERROR, "$type requires at least $minPoints points", false)
        }
        for (pt in points) {
            if (!pt.xMeters.isFinite() || !pt.yMeters.isFinite()) {
                return RobotError("INVALID_GEOMETRY", "sdk", ErrorSeverity.ERROR, "Point coordinates must be finite numbers", false)
            }
        }
        return null
    }

    // Serializes ordinary commands with margin above the gateway's 50 ms receive limit.
    // Safety commands bypass this lock so a queued map or control request cannot delay a stop.
    private suspend fun sendGatewayCommand(
        jsonText: String,
        bypassPacing: Boolean = false,
        expectedGeneration: Long = connectionGeneration.get()
    ): Boolean {
        if (bypassPacing) {
            return expectedGeneration == connectionGeneration.get() &&
                _connectionState.value is ConnectionState.Connected && transport.send(jsonText)
        }

        return ordinaryCommandMutex.withLock {
            if (expectedGeneration != connectionGeneration.get() ||
                _connectionState.value !is ConnectionState.Connected
            ) return@withLock false

            if (lastOrdinarySendGeneration == expectedGeneration) {
                val elapsedNanos = System.nanoTime() - lastOrdinarySendNanos
                val remainingNanos = MIN_ORDINARY_COMMAND_INTERVAL_MS * 1_000_000L - elapsedNanos
                if (remainingNanos > 0L) delay((remainingNanos + 999_999L) / 1_000_000L)
            }

            if (expectedGeneration != connectionGeneration.get() ||
                _connectionState.value !is ConnectionState.Connected
            ) return@withLock false

            val sent = transport.send(jsonText)
            if (sent) {
                lastOrdinarySendGeneration = expectedGeneration
                lastOrdinarySendNanos = System.nanoTime()
            }
            sent
        }
    }

    // Registers simple commands while allowing motion-stopping requests to bypass ordinary pacing.
    private suspend fun sendSimpleCommand(type: String): RobotResult<Unit> {
        val cmdId = commandRegistry.generateCommandId()
        val envelope = ProtocolCodec.createSimpleCommandEnvelope(type, cmdId)
        val jsonText = ProtocolCodec.encodeEnvelope(envelope)

        return commandRegistry.registerAndAwait(cmdId, config.commandTimeoutMs) {
            if (!sendGatewayCommand(
                    jsonText,
                    bypassPacing = type == "emergency_stop" || type == "stop" ||
                        type == "cancel_navigation" || type == "cancel_docking"
                )) {
                commandRegistry.completeError(
                    cmdId,
                    RobotError("SEND_FAILED", "transport", ErrorSeverity.ERROR, "Failed to send WebSocket message", true)
                )
            }
        }
    }

    // Starts one subscription cycle after authentication or when an active stream needs resync.
    private fun requestMapSubscription(reason: String, resync: Boolean = false) {
        synchronized(mapSubscriptionLock) {
            if (_connectionState.value !is ConnectionState.Connected ||
                mapSubscriptionState == MapSubscriptionState.REQUESTING ||
                mapSubscriptionState == MapSubscriptionState.AWAITING_SNAPSHOT ||
                mapSubscriptionState == MapSubscriptionState.FAILED ||
                (mapSubscriptionState == MapSubscriptionState.ACTIVE && !resync) ||
                mapSubscriptionJob?.isActive == true
            ) return

            mapSubscriptionState = MapSubscriptionState.REQUESTING
            val generation = connectionGeneration.get()
            val job = scope.launch(start = CoroutineStart.LAZY) {
                runMapSubscription(generation, reason)
            }
            mapSubscriptionJob = job
            job.start()
        }
    }

    // Correlates each attempt with its gateway response and retries only rejected recoverable requests.
    private suspend fun runMapSubscription(generation: Long, reason: String) {
        for (attempt in 0..mapSubscriptionRetryDelaysMs.size) {
            val requestId = commandRegistry.generateCommandId()
            // Claim the request only while its authenticated connection is still current.
            val canSend = synchronized(mapSubscriptionLock) {
                if (generation != connectionGeneration.get() ||
                    _connectionState.value !is ConnectionState.Connected
                ) false else {
                    mapSubscriptionState = MapSubscriptionState.REQUESTING
                    mapSubscriptionRequestId = requestId
                    rejectedMapSubscriptionRequestId = null
                    true
                }
            }
            if (!canSend) return
            val envelope = ProtocolCodec.createSubscribeMapEnvelope(requestId)
            val jsonText = ProtocolCodec.encodeEnvelope(envelope)
            val result = commandRegistry.registerAndAwait(requestId, config.commandTimeoutMs) {
                if (sendGatewayCommand(jsonText, expectedGeneration = generation)) {
                    android.util.Log.i("RobotSDK", "SEND type=subscribe_map id=$requestId attempt=${attempt + 1} reason=$reason")
                    config.logger.log(RobotLogEvent(RobotLogLevel.INFO, "CLIENT", "subscribe_map sent id=$requestId attempt=${attempt + 1} reason=$reason"))
                } else {
                    commandRegistry.completeError(
                        requestId,
                        RobotError("SEND_FAILED", "transport", ErrorSeverity.ERROR, "Failed to send subscribe_map", true)
                    )
                }
            }

            val rejectedByGateway = synchronized(mapSubscriptionLock) {
                val rejected = rejectedMapSubscriptionRequestId == requestId
                if (mapSubscriptionRequestId == requestId) mapSubscriptionRequestId = null
                rejectedMapSubscriptionRequestId = null
                rejected
            }
            if (generation != connectionGeneration.get()) return

            if (result is RobotResult.Success) {
                synchronized(mapSubscriptionLock) {
                    if (generation == connectionGeneration.get() &&
                        _connectionState.value is ConnectionState.Connected &&
                        mapSubscriptionState != MapSubscriptionState.ACTIVE
                    ) {
                        mapSubscriptionState = MapSubscriptionState.AWAITING_SNAPSHOT
                    }
                }
                return
            }

            val error = (result as RobotResult.Failure).error
            if (mapSubscriptionState == MapSubscriptionState.ACTIVE) return
            if (rejectedByGateway && error.recoverable && attempt < mapSubscriptionRetryDelaysMs.size) {
                val retryDelayMs = mapSubscriptionRetryDelaysMs[attempt]
                android.util.Log.w("RobotSDK", "RETRY type=subscribe_map after=${retryDelayMs}ms code=${error.code}")
                config.logger.log(RobotLogEvent(RobotLogLevel.WARN, "CLIENT", "subscribe_map retry in ${retryDelayMs}ms after ${error.code}"))
                delay(retryDelayMs)
                continue
            }

            synchronized(mapSubscriptionLock) {
                if (generation == connectionGeneration.get() &&
                    _connectionState.value is ConnectionState.Connected
                ) mapSubscriptionState = MapSubscriptionState.FAILED
            }
            android.util.Log.e("RobotSDK", "FAILED type=subscribe_map id=$requestId code=${error.code} attempts=${attempt + 1}")
            config.logger.log(RobotLogEvent(RobotLogLevel.ERROR, "CLIENT", "subscribe_map failed id=$requestId code=${error.code} attempts=${attempt + 1}"))
            return
        }
    }

    // Records a matching acknowledgement while leaving snapshot delivery to activate the stream.
    private fun acknowledgeMapSubscription(requestId: String?) {
        val matchedId = synchronized(mapSubscriptionLock) {
            val pendingId = mapSubscriptionRequestId
            if (pendingId == null || (requestId != null && requestId != pendingId)) null else {
                if (mapSubscriptionState != MapSubscriptionState.ACTIVE) {
                    mapSubscriptionState = MapSubscriptionState.AWAITING_SNAPSHOT
                }
                pendingId
            }
        } ?: return
        android.util.Log.i("RobotSDK", "RECEIVE subscribe_map acknowledgement id=$matchedId")
        config.logger.log(RobotLogEvent(RobotLogLevel.INFO, "CLIENT", "subscribe_map acknowledged id=$matchedId"))
        commandRegistry.completeSuccess(matchedId)
    }

    // Cancels session-owned setup work and forgets pending subscription state on connection loss.
    private fun resetMapSubscription() {
        postAuthenticationJob?.cancel()
        postAuthenticationJob = null
        mapSubscriptionJob?.cancel()
        mapSubscriptionJob = null
        synchronized(mapSubscriptionLock) {
            mapSubscriptionState = MapSubscriptionState.IDLE
            mapSubscriptionRequestId = null
            rejectedMapSubscriptionRequestId = null
        }
    }

    // ---- TransportListener Callbacks ----
    // Starts a new transport generation before sending the unpaced authentication handshake.
    override fun onOpen() {
        connectionGeneration.incrementAndGet()
        _connectionState.value = ConnectionState.Authenticating
        val token = when (val auth = config.authentication) {
            is RobotAuthentication.Token -> auth.token
            null -> null
        }
        val handshakeId = commandRegistry.generateCommandId()
        val envelope = ProtocolCodec.createHelloEnvelope(handshakeId, token)
        transport.send(ProtocolCodec.encodeEnvelope(envelope))
    }

    // Routes gateway responses to pending commands and applies authenticated map-stream events.
    override fun onMessage(text: String) {
        val now = System.currentTimeMillis()
        lastMessageTimeMillis = now

        try {
            val envelope = ProtocolCodec.decodeEnvelope(text)
            when (envelope.type) {
                "hello_ack" -> {
                    val ack = ProtocolCodec.decodeHelloAck(envelope)
                    val gatewayVer = ack?.gatewayVersion ?: "0.1.0"
                    val caps = ack?.capabilities?.mapNotNull { parseCapability(it) }?.toSet() ?: emptySet()
                    val session = ack?.sessionId

                    connectedTimeMillis = now
                    reconnectAttempt = 0

                    _connectionState.value = ConnectionState.Connected(gatewayVer, envelope.protocolVersion, caps, session)
                    _robotInfo.value = RobotInfo(
                        robotId = session ?: "robot-1",
                        gatewayVersion = gatewayVer,
                        capabilities = caps
                    )
                    connectHandshakeDeferred?.complete(RobotResult.Success(Unit))
                    config.logger.log(RobotLogEvent(RobotLogLevel.INFO, "CLIENT", "Connected & authenticated to Gateway $gatewayVer (session=$session)"))

                    startBackgroundJobs()

                    // Submit initial ordinary requests in order after the gateway accepts authentication.
                    val generation = connectionGeneration.get()
                    postAuthenticationJob?.cancel()
                    postAuthenticationJob = scope.launch {
                        val snapshotReqId = commandRegistry.generateCommandId()
                        val snapshotEnv = ProtocolCodec.createRequestStateSnapshotEnvelope(snapshotReqId)
                        if (!sendGatewayCommand(ProtocolCodec.encodeEnvelope(snapshotEnv), expectedGeneration = generation)) {
                            config.logger.log(RobotLogEvent(RobotLogLevel.WARN, "CLIENT", "request_state_snapshot send failed"))
                        }
                        if (generation == connectionGeneration.get()) requestMapSubscription("authentication")
                    }
                }
                "pong" -> {
                    val latency = if (pingSentTimeMillis > 0) now - pingSentTimeMillis else null
                    updateMetrics(latency = latency)
                }
                "list_maps_ack" -> {
                    val maps = ProtocolCodec.decodeListMapsAck(envelope) ?: emptyList()
                    envelope.id?.let { commandRegistry.completeSuccessPayload(it, maps) }
                }
                "save_map_ack" -> {
                    val map = ProtocolCodec.decodeSaveMapAck(envelope)
                    if (map != null) {
                        envelope.id?.let { commandRegistry.completeSuccessPayload(it, map) }
                    } else {
                        envelope.id?.let { commandRegistry.completeError(it, RobotError("DECODE_ERROR", "sdk", ErrorSeverity.ERROR, "Failed to decode save_map_ack", true)) }
                    }
                }
                "rename_map_ack" -> {
                    val map = ProtocolCodec.decodeRenameMapAck(envelope)
                    if (map != null) {
                        envelope.id?.let { commandRegistry.completeSuccessPayload(it, map) }
                    } else {
                        envelope.id?.let { commandRegistry.completeError(it, RobotError("DECODE_ERROR", "sdk", ErrorSeverity.ERROR, "Failed to decode rename_map_ack", true)) }
                    }
                }
                "list_virtual_walls_ack" -> {
                    val walls = ProtocolCodec.decodeListVirtualWallsAck(envelope) ?: emptyList()
                    envelope.id?.let { commandRegistry.completeSuccessPayload(it, walls) }
                }
                "create_virtual_wall_ack" -> {
                    val wall = ProtocolCodec.decodeCreateVirtualWallAck(envelope)
                    if (wall != null) {
                        envelope.id?.let { commandRegistry.completeSuccessPayload(it, wall) }
                    } else {
                        envelope.id?.let { commandRegistry.completeError(it, RobotError("DECODE_ERROR", "sdk", ErrorSeverity.ERROR, "Failed to decode create_virtual_wall_ack", true)) }
                    }
                }
                "update_virtual_wall_ack" -> {
                    val wall = ProtocolCodec.decodeUpdateVirtualWallAck(envelope)
                    if (wall != null) {
                        envelope.id?.let { commandRegistry.completeSuccessPayload(it, wall) }
                    } else {
                        envelope.id?.let { commandRegistry.completeError(it, RobotError("DECODE_ERROR", "sdk", ErrorSeverity.ERROR, "Failed to decode update_virtual_wall_ack", true)) }
                    }
                }
                "list_saved_locations_ack" -> {
                    val locations = ProtocolCodec.decodeListSavedLocationsAck(envelope) ?: emptyList()
                    envelope.id?.let { commandRegistry.completeSuccessPayload(it, locations) }
                }
                "save_location_ack" -> {
                    val loc = ProtocolCodec.decodeSaveLocationAck(envelope)
                    if (loc != null) {
                        envelope.id?.let { commandRegistry.completeSuccessPayload(it, loc) }
                    } else {
                        envelope.id?.let { commandRegistry.completeError(it, RobotError("DECODE_ERROR", "sdk", ErrorSeverity.ERROR, "Failed to decode save_location_ack", true)) }
                    }
                }
                "rename_location_ack" -> {
                    val loc = ProtocolCodec.decodeRenameLocationAck(envelope)
                    if (loc != null) {
                        envelope.id?.let { commandRegistry.completeSuccessPayload(it, loc) }
                    } else {
                        envelope.id?.let { commandRegistry.completeError(it, RobotError("DECODE_ERROR", "sdk", ErrorSeverity.ERROR, "Failed to decode rename_location_ack", true)) }
                    }
                }
                "check_map_point_ack" -> {
                    val validity = ProtocolCodec.decodeCheckMapPointAck(envelope)
                    if (validity != null) {
                        envelope.id?.let { commandRegistry.completeSuccessPayload(it, validity) }
                    } else {
                        envelope.id?.let { commandRegistry.completeError(it, RobotError("DECODE_ERROR", "sdk", ErrorSeverity.ERROR, "Failed to decode check_map_point_ack", true)) }
                    }
                }
                "get_recent_logs_ack" -> {
                    val logs = ProtocolCodec.decodeGetRecentLogsAck(envelope) ?: emptyList()
                    envelope.id?.let { commandRegistry.completeSuccessPayload(it, logs) }
                }
                "mission_progress" -> {
                    ProtocolCodec.decodeMissionProgress(envelope)?.let { _missionProgress.value = it }
                }
                "command_ack" -> {
                    config.logger.log(RobotLogEvent(RobotLogLevel.INFO, "RobotSDK", "RECEIVE type=command_ack id=${envelope.id}"))
                    android.util.Log.d("RobotSDK", "RECEIVE type=command_ack id=${envelope.id}")
                    envelope.id?.let { acknowledgeMapSubscription(it) }
                    envelope.id?.let { commandRegistry.completeSuccess(it) }
                }
                "command_error" -> {
                    val err = envelope.error?.let { ProtocolCodec.mapRobotError(it) }
                        ?: RobotError("UNKNOWN_ERROR", "gateway", ErrorSeverity.ERROR, "Command error", true)

                    config.logger.log(RobotLogEvent(RobotLogLevel.WARN, "RobotSDK", "RECEIVE type=command_error id=${envelope.id} code=${err.code} msg=${err.message}"))
                    android.util.Log.w("RobotSDK", "RECEIVE type=command_error id=${envelope.id} code=${err.code} msg='${err.message}'")

                    // Identify subscription rejections before generic authentication errors alter session state.
                    synchronized(mapSubscriptionLock) {
                        if (envelope.id != null && envelope.id == mapSubscriptionRequestId) {
                            rejectedMapSubscriptionRequestId = envelope.id
                            android.util.Log.w("RobotSDK", "RECEIVE subscribe_map command_error id=${envelope.id} code=${err.code} message=${err.message}")
                        }
                    }
                    if (err.code == "INVALID_AUTHENTICATION" || err.code == "PROTOCOL_VERSION_MISMATCH") {
                        _connectionState.value = ConnectionState.Failed(err)
                        connectHandshakeDeferred?.complete(RobotResult.Failure(err))
                        connectionGeneration.incrementAndGet()
                        resetMapSubscription()
                        return
                    }

                    envelope.id?.let { commandRegistry.completeError(it, err) }
                }
                "robot_state_snapshot" -> {
                    ProtocolCodec.decodeRobotStateSnapshot(envelope)?.let { snapshot ->
                        snapshot.telemetry?.let { applyTelemetry(ProtocolCodec.mapTelemetryPayload(it), now) }
                        snapshot.battery?.let { _batteryState.value = ProtocolCodec.mapBatteryStatePayload(it) }
                        snapshot.safety?.let { _safetyState.value = ProtocolCodec.mapSafetyStatePayload(it) }
                        snapshot.docking?.let { _dockingState.value = ProtocolCodec.mapDockingStatePayload(it) }
                        snapshot.health?.let { _health.value = ProtocolCodec.mapHealthPayload(it) }
                        snapshot.mapState?.let { applyMapState(it) }
                    }
                }
                "map_state" -> {
                    ProtocolCodec.decodeMapState(envelope)?.let { applyMapState(it) }
                }
                "subscribe_map_ack" -> {
                    acknowledgeMapSubscription(envelope.id)
                }
                "map_snapshot" -> {
                    val seq = envelope.payload?.get("sequence")?.toString()?.toLongOrNull() ?: -1L
                    ProtocolCodec.decodeMapSnapshot(envelope.payload)?.let {
                        _occupancyGrid.value = it
                        mapStreamSequence = seq
                        synchronized(mapSubscriptionLock) { mapSubscriptionState = MapSubscriptionState.ACTIVE }
                        android.util.Log.i("RobotSDK", "RECEIVE map_snapshot seq=$seq size=${it.widthCells}x${it.heightCells} cells=${it.data.size}")
                        config.logger.log(RobotLogEvent(RobotLogLevel.INFO, "CLIENT", "map_snapshot received (seq=$seq, size=${it.widthCells}x${it.heightCells}, cells=${it.data.size})"))
                    } ?: run {
                        android.util.Log.e("RobotSDK", "RECEIVE map_snapshot decode failure seq=$seq")
                        config.logger.log(RobotLogEvent(RobotLogLevel.ERROR, "CLIENT", "map_snapshot decode failure"))
                    }
                }
                "map_update" -> {
                    val seq = envelope.payload?.get("sequence")?.toString()?.toLongOrNull() ?: -1L
                    val baseSeq = envelope.payload?.get("baseSequence")?.toString()?.toLongOrNull() ?: -1L

                    if (baseSeq != mapStreamSequence) {
                        config.logger.log(RobotLogEvent(RobotLogLevel.WARN, "CLIENT", "map_update sequence mismatch: expected $mapStreamSequence but got base $baseSeq. Resyncing."))
                        requestMapSubscription("sequence_mismatch", resync = true)
                    } else {
                        val updated = ProtocolCodec.decodeMapUpdate(envelope.payload, _occupancyGrid.value)
                        if (updated != null) {
                            _occupancyGrid.value = updated
                            mapStreamSequence = seq
                            config.logger.log(RobotLogEvent(RobotLogLevel.DEBUG, "CLIENT", "map_update applied (seq=$seq)"))
                        } else {
                            config.logger.log(RobotLogEvent(RobotLogLevel.ERROR, "CLIENT", "map_update decode failure"))
                            requestMapSubscription("update_decode_failure", resync = true)
                        }
                    }
                }
                "telemetry" -> {
                    ProtocolCodec.decodeTelemetry(envelope)?.let { applyTelemetry(it, now) }
                }
                "battery_state" -> {
                    ProtocolCodec.decodeBatteryState(envelope)?.let { _batteryState.value = it }
                }
                "safety_state" -> {
                    ProtocolCodec.decodeSafetyState(envelope)?.let { _safetyState.value = it }
                }
                "docking_state" -> {
                    ProtocolCodec.decodeDockingState(envelope)?.let { _dockingState.value = it }
                }
                "robot_health" -> {
                    ProtocolCodec.decodeHealth(envelope)?.let { _health.value = it }
                }
            }
        } catch (e: Exception) {
            config.logger.log(RobotLogEvent(RobotLogLevel.WARN, "CLIENT", "Error parsing message: ${e.message}"))
        }

        updateMetrics()
    }

    private fun applyTelemetry(telem: RobotTelemetry, now: Long) {
        lastTelemetryTimeMillis = now
        _telemetryFreshness.value = TelemetryFreshness(isStale = false, ageMs = 0L, receivedAtMillis = now)
        _telemetry.value = telem
        if (!hasMapState && _activeMap.value == null && telem.activeMap.isNotBlank()) {
            _activeMap.value = RobotMap(id = telem.activeMap, name = telem.activeMap, isActive = true)
        }
        _navigation.value = NavigationState(
            state = telem.navigationState,
            targetPose = if (telem.hasGoal) Pose2D(telem.xMeters, telem.yMeters, telem.yawRadians) else null,
            distanceRemainingMeters = telem.distanceRemainingMeters,
            elapsedTimeSeconds = telem.navigationElapsedSeconds,
            errorMsg = telem.navigationErrorMessage
        )
    }

    /**
     * Applies state updates from map_state telemetry onto internal flows.
     */
    private fun applyMapState(mapState: RobotMapStateDto) {
        hasMapState = true
        config.logger.log(
            RobotLogEvent(
                RobotLogLevel.INFO,
                "RobotMapping",
                "MapState update received: state=${mapState.state}, mode=${mapState.mode}, activeMapId=${mapState.activeMapId}, errorCode=${mapState.errorCode}"
            )
        )
        android.util.Log.d("RobotMapping", "MapState update: state=${mapState.state}, activeMap=${mapState.activeMapId}")

        val activeId = mapState.activeMapId?.takeIf { it.isNotBlank() }
        if (activeId == null) {
            _activeMap.value = null
        } else if (_activeMap.value?.id != activeId) {
            _activeMap.value = RobotMap(id = activeId, name = activeId, isActive = true)
        }

        val previous = _mapOperationState.value
        _mapOperationState.value = when (mapState.state.uppercase()) {
            "CONNECTED" -> MapOperationState.Connected
            "WAITING_FOR_MAP" -> MapOperationState.WaitingForMap(activeId)
            "INITIALIZING_MAPPING" -> MapOperationState.InitializingMapping(activeId)
            "MAPPING" -> MapOperationState.Mapping(activeId)
            "MAP_UPDATING" -> MapOperationState.MapUpdating(activeId)
            "SAVING" -> MapOperationState.Saving(activeId ?: (previous as? MapOperationState.Saving)?.mapName ?: "")
            "SWITCHING", "LOADING" ->
                MapOperationState.Switching((previous as? MapOperationState.Switching)?.mapId ?: activeId ?: "")
            "ERROR" -> MapOperationState.Failed(
                activeId,
                mapState.detail?.takeIf { it.isNotBlank() } ?: mapState.errorCode?.takeIf { it.isNotBlank() } ?: "Map error"
            )
            else -> MapOperationState.Idle
        }
    }

    override fun onFailure(t: Throwable, response: Response?) {
        val responseText = response?.let { " (HTTP ${it.code} ${it.message})" } ?: ""
        val detailedMessage = when {
            t.message?.contains("CLEARTEXT", ignoreCase = true) == true ->
                "Cleartext (HTTP/WS) traffic to ${config.endpoint.host}:${config.endpoint.port} blocked by Android Network Security Policy."
            t is java.net.ConnectException ->
                "Connection refused at ${config.endpoint.host}:${config.endpoint.port}. Ensure the robot gateway server is running and bound to 0.0.0.0."
            t is java.net.SocketTimeoutException ->
                "Socket connection timed out connecting to ${config.endpoint.host}:${config.endpoint.port}."
            else ->
                "${t.javaClass.simpleName}: ${t.message ?: "Transport error"}$responseText"
        }
        val err = RobotError("TRANSPORT_ERROR", "transport", ErrorSeverity.ERROR, detailedMessage, true)
        connectHandshakeDeferred?.complete(RobotResult.Failure(err))
        clearStatesAndPending(detailedMessage)

        handleConnectionLoss(err)
    }

    override fun onClose(code: Int, reason: String) {
        val err = RobotError("CONNECTION_CLOSED", "transport", ErrorSeverity.INFO, "Closed: $code $reason", true)
        connectHandshakeDeferred?.complete(RobotResult.Failure(err))
        clearStatesAndPending("Connection closed")

        if (code != 1000 && !isUserDisconnected) {
            handleConnectionLoss(err)
        } else {
            _connectionState.value = ConnectionState.Disconnected
        }
    }

    private fun handleConnectionLoss(error: RobotError) {
        cancelBackgroundJobs()

        if (isUserDisconnected || !config.reconnectPolicy.enabled) {
            _connectionState.value = ConnectionState.Failed(error)
            return
        }

        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            reconnectAttempt++
            val delayMs = calculateBackoffDelay(reconnectAttempt, config.reconnectPolicy)
            _connectionState.value = ConnectionState.Reconnecting(reconnectAttempt, delayMs)
            config.logger.log(RobotLogEvent(RobotLogLevel.INFO, "RECONNECT", "Attempting reconnection #$reconnectAttempt in ${delayMs}ms"))

            delay(delayMs)

            if (!isActive || isUserDisconnected) return@launch

            val result = performConnectAttempt(reconnectAttempt)
            if (result is RobotResult.Failure && isActive && !isUserDisconnected) {
                handleConnectionLoss(result.error)
            }
        }
    }

    private fun calculateBackoffDelay(attempt: Int, policy: com.alokrathava.sdk.model.ReconnectPolicy): Long {
        val exp = policy.multiplier.pow((attempt - 1).toDouble())
        var delay = (policy.initialDelayMs * exp).toLong()
        if (delay > policy.maxDelayMs) {
            delay = policy.maxDelayMs
        }
        val jitter = (delay * policy.jitterRatio * (Random.nextDouble() * 2 - 1)).toLong()
        return (delay + jitter).coerceAtLeast(100L)
    }

    private fun startBackgroundJobs() {
        cancelBackgroundJobs()

        heartbeatJob = scope.launch {
            while (isActive) {
                delay(config.heartbeatIntervalMs)
                if (_connectionState.value is ConnectionState.Connected) {
                    sendPing()
                }
            }
        }

        staleCheckJob = scope.launch {
            while (isActive) {
                delay(1000L)
                val now = System.currentTimeMillis()
                if (lastTelemetryTimeMillis > 0) {
                    val age = now - lastTelemetryTimeMillis
                    val isStale = age > config.telemetryStaleTimeoutMs
                    _telemetryFreshness.value = TelemetryFreshness(isStale = isStale, ageMs = age, receivedAtMillis = lastTelemetryTimeMillis)
                }
            }
        }
    }

    private fun cancelBackgroundJobs() {
        heartbeatJob?.cancel()
        heartbeatJob = null
        staleCheckJob?.cancel()
        staleCheckJob = null
    }

    private fun sendPing() {
        val pingId = commandRegistry.generateCommandId()
        pingSentTimeMillis = System.currentTimeMillis()
        val envelope = ProtocolCodec.createPingEnvelope(pingId)
        transport.send(ProtocolCodec.encodeEnvelope(envelope))
    }

    private fun updateMetrics(latency: Long? = null) {
        val now = System.currentTimeMillis()
        val connectedDuration = if (connectedTimeMillis > 0) now - connectedTimeMillis else 0L
        val lastMsgAge = if (lastMessageTimeMillis > 0) now - lastMessageTimeMillis else 0L

        val currentMetrics = _connectionMetrics.value
        val newLatency = latency ?: currentMetrics.latencyMs

        _connectionMetrics.value = ConnectionMetrics(
            latencyMs = newLatency,
            connectedDurationMs = connectedDuration,
            lastMessageAgeMs = lastMsgAge,
            reconnectCount = reconnectAttempt
        )
    }

    private fun parseCapability(capString: String): RobotCapability? {
        return try {
            RobotCapability.valueOf(capString.uppercase())
        } catch (_: Exception) {
            null
        }
    }

    // Invalidates queued sends and clears session-owned state before a future connection starts.
    private fun clearStatesAndPending(reason: String) {
        connectionGeneration.incrementAndGet()
        resetMapSubscription()
        val err = RobotError("CONNECTION_LOST", "transport", ErrorSeverity.WARN, reason, true)
        commandRegistry.clearAll(err)
        _telemetry.value = null
        _batteryState.value = null
        _safetyState.value = null
        _dockingState.value = null
        _health.value = null
        _activeMap.value = null
        _occupancyGrid.value = null
        mapStreamSequence = -1L
        _mapOperationState.value = MapOperationState.Idle
        hasMapState = false
    }

    override fun close() {
        isUserDisconnected = true
        cancelBackgroundJobs()
        reconnectJob?.cancel()
        _connectionState.value = ConnectionState.Disconnected
        transport.disconnect()
        clearStatesAndPending("Client closed")
        scope.cancel()
    }
}
