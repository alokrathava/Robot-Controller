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
import com.alokrathava.sdk.model.DockingState
import com.alokrathava.sdk.model.Pose2D
import com.alokrathava.sdk.model.RobotBatteryState
import com.alokrathava.sdk.model.RobotCapability
import com.alokrathava.sdk.model.RobotHealth
import com.alokrathava.sdk.model.RobotMap
import com.alokrathava.sdk.model.RobotTelemetry
import com.alokrathava.sdk.model.SafetyState
import com.alokrathava.sdk.model.TelemetryFreshness
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
    private val transport = WebSocketRobotTransport(config.endpoint, config.logger, this)

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

        val cmdId = commandRegistry.generateCommandId()
        val envelope = ProtocolCodec.createSwitchMapEnvelope(cmdId, mapId)
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
                    connectHandshakeDeferred?.complete(RobotResult.Success(Unit))
                    config.logger.log(RobotLogEvent(RobotLogLevel.INFO, "CLIENT", "Connected & authenticated to Gateway $gatewayVer (session=$session)"))

                    startBackgroundJobs()
                }
                "pong" -> {
                    val latency = if (pingSentTimeMillis > 0) now - pingSentTimeMillis else null
                    updateMetrics(latency = latency)
                }
                "list_maps_ack" -> {
                    val maps = ProtocolCodec.decodeListMapsAck(envelope) ?: emptyList()
                    envelope.id?.let { commandRegistry.completeSuccessPayload(it, maps) }
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
