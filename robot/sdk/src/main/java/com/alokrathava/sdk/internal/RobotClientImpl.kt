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
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.Response
import kotlin.math.pow
import kotlin.random.Random

internal class RobotClientImpl(
    private val config: RobotSdkConfig
) : RobotClient, TransportListener {

    private val sdkJob = SupervisorJob()
    private val scope = CoroutineScope(sdkJob + Dispatchers.IO)

    private val commandRegistry = PendingCommandRegistry()
    private val transport = WebSocketRobotTransport(
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

    private var heartbeatJob: Job? = null
    private var staleCheckJob: Job? = null
    private var reconnectJob: Job? = null

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
                message = "Connection handshake timed out after ${config.commandTimeoutMs}ms",
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
            if (!transport.send(jsonText)) {
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
        checkCapability(RobotCapability.WAYPOINT_NAVIGATION)?.let { return RobotResult.Failure((it as RobotResult.Failure).error) }

        if (poses.isEmpty()) {
            return RobotResult.Failure(RobotError("INVALID_ARGUMENT", "sdk", ErrorSeverity.ERROR, "Waypoints list cannot be empty", false))
        }

        val cmdId = commandRegistry.generateCommandId()
        val envelope = ProtocolCodec.createNavigateThroughEnvelope(cmdId, poses)
        val jsonText = ProtocolCodec.encodeEnvelope(envelope)

        val result = commandRegistry.registerAndAwait(cmdId, config.commandTimeoutMs) {
            if (!transport.send(jsonText)) {
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
        return navigateThrough(route.waypoints)
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
            if (!transport.send(jsonText)) {
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

        return commandRegistry.registerAndAwaitTyped<List<RobotMap>>(cmdId, config.commandTimeoutMs) {
            if (!transport.send(jsonText)) {
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

        val result = commandRegistry.registerAndAwait(cmdId, config.commandTimeoutMs) {
            if (!transport.send(jsonText)) {
                commandRegistry.completeError(
                    cmdId,
                    RobotError("SEND_FAILED", "transport", ErrorSeverity.ERROR, "Failed to send WebSocket message", true)
                )
            }
        }
        when (result) {
            is RobotResult.Success -> _mapOperationState.value = MapOperationState.Idle
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

        val result = commandRegistry.registerAndAwaitTyped<RobotMap>(cmdId, config.commandTimeoutMs) {
            if (!transport.send(jsonText)) {
                commandRegistry.completeError(
                    cmdId,
                    RobotError("SEND_FAILED", "transport", ErrorSeverity.ERROR, "Failed to send WebSocket message", true)
                )
            }
        }
        when (result) {
            is RobotResult.Success -> _mapOperationState.value = MapOperationState.Idle
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

        return commandRegistry.registerAndAwaitTyped<RobotMap>(cmdId, config.commandTimeoutMs) {
            if (!transport.send(jsonText)) {
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

        val result = commandRegistry.registerAndAwait(cmdId, config.commandTimeoutMs) {
            if (!transport.send(jsonText)) {
                commandRegistry.completeError(
                    cmdId,
                    RobotError("SEND_FAILED", "transport", ErrorSeverity.ERROR, "Failed to send WebSocket message", true)
                )
            }
        }
        when (result) {
            is RobotResult.Success -> _mapOperationState.value = MapOperationState.Idle
            is RobotResult.Failure -> _mapOperationState.value = MapOperationState.Failed(mapId, result.error.message)
        }
        return result
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
            if (!transport.send(jsonText)) {
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
            if (!transport.send(jsonText)) {
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
            if (!transport.send(jsonText)) {
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
            if (!transport.send(jsonText)) {
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
            if (!transport.send(jsonText)) {
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
            if (!transport.send(jsonText)) {
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
            if (!transport.send(jsonText)) {
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
            if (!transport.send(jsonText)) {
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
            if (!transport.send(jsonText)) {
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
            if (!transport.send(jsonText)) {
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
            if (!transport.send(jsonText)) {
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
            if (!transport.send(jsonText)) {
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
            if (!transport.send(jsonText)) {
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
            if (!transport.send(jsonText)) {
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
            if (!transport.send(jsonText)) {
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
            if (!transport.send(jsonText)) {
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

    private suspend fun sendSimpleCommand(type: String): RobotResult<Unit> {
        val cmdId = commandRegistry.generateCommandId()
        val envelope = ProtocolCodec.createSimpleCommandEnvelope(type, cmdId)
        val jsonText = ProtocolCodec.encodeEnvelope(envelope)

        return commandRegistry.registerAndAwait(cmdId, config.commandTimeoutMs) {
            if (!transport.send(jsonText)) {
                commandRegistry.completeError(
                    cmdId,
                    RobotError("SEND_FAILED", "transport", ErrorSeverity.ERROR, "Failed to send WebSocket message", true)
                )
            }
        }
    }

    // ---- TransportListener Callbacks ----
    override fun onOpen() {
        _connectionState.value = ConnectionState.Authenticating
        val token = when (val auth = config.authentication) {
            is RobotAuthentication.Token -> auth.token
            null -> null
        }
        val handshakeId = commandRegistry.generateCommandId()
        val envelope = ProtocolCodec.createHelloEnvelope(handshakeId, token)
        transport.send(ProtocolCodec.encodeEnvelope(envelope))
    }

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

                    val snapshotReqId = commandRegistry.generateCommandId()
                    val snapshotEnv = ProtocolCodec.createRequestStateSnapshotEnvelope(snapshotReqId)
                    transport.send(ProtocolCodec.encodeEnvelope(snapshotEnv))
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
                    envelope.id?.let { commandRegistry.completeSuccess(it) }
                }
                "command_error" -> {
                    val err = envelope.error?.let { ProtocolCodec.mapRobotError(it) }
                        ?: RobotError("UNKNOWN_ERROR", "gateway", ErrorSeverity.ERROR, "Command error", true)

                    if (err.code == "INVALID_AUTHENTICATION" || err.code == "PROTOCOL_VERSION_MISMATCH") {
                        _connectionState.value = ConnectionState.Failed(err)
                        connectHandshakeDeferred?.complete(RobotResult.Failure(err))
                        return
                    }

                    envelope.id?.let { commandRegistry.completeError(it, err) }
                }
                "telemetry" -> {
                    lastTelemetryTimeMillis = now
                    _telemetryFreshness.value = TelemetryFreshness(isStale = false, ageMs = 0L, receivedAtMillis = now)
                    ProtocolCodec.decodeTelemetry(envelope)?.let { telem ->
                        _telemetry.value = telem
                        if (telem.activeMap.isNotBlank()) {
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

    override fun onFailure(t: Throwable, response: Response?) {
        val err = RobotError("TRANSPORT_ERROR", "transport", ErrorSeverity.ERROR, t.message ?: "Transport error", true)
        connectHandshakeDeferred?.complete(RobotResult.Failure(err))
        clearStatesAndPending(t.message ?: "Transport error")

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

    private fun clearStatesAndPending(reason: String) {
        val err = RobotError("CONNECTION_LOST", "transport", ErrorSeverity.WARN, reason, true)
        commandRegistry.clearAll(err)
        _telemetry.value = null
        _batteryState.value = null
        _safetyState.value = null
        _dockingState.value = null
        _health.value = null
        _activeMap.value = null
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
