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
import com.alokrathava.sdk.model.ConnectionState
import com.alokrathava.sdk.model.DockingState
import com.alokrathava.sdk.model.Pose2D
import com.alokrathava.sdk.model.RobotBatteryState
import com.alokrathava.sdk.model.RobotHealth
import com.alokrathava.sdk.model.RobotTelemetry
import com.alokrathava.sdk.model.SafetyState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.Response

internal class RobotClientImpl(
    private val config: RobotSdkConfig
) : RobotClient, TransportListener {

    private val sdkJob = SupervisorJob()
    private val scope = CoroutineScope(sdkJob + Dispatchers.IO)

    private val commandRegistry = PendingCommandRegistry()
    private val transport = WebSocketRobotTransport(config.endpoint, config.logger, this)

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

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

    private var connectHandshakeDeferred: CompletableDeferred<RobotResult<Unit>>? = null

    override suspend fun connect(): RobotResult<Unit> {
        if (_connectionState.value is ConnectionState.Connected) {
            return RobotResult.Success(Unit)
        }

        _connectionState.value = ConnectionState.Connecting
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
        _connectionState.value = ConnectionState.Disconnected
        transport.disconnect()
        clearStatesAndPending("Disconnected by user")
    }

    override suspend fun navigateTo(pose: Pose2D): RobotResult<CommandId> {
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
        return sendSimpleCommand("cancel_navigation")
    }

    override suspend fun setManualVelocity(linearMps: Double, angularRadPerSec: Double): RobotResult<Unit> {
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
        return sendSimpleCommand("dock")
    }

    override suspend fun undock(): RobotResult<Unit> {
        return sendSimpleCommand("undock")
    }

    override suspend fun cancelDocking(): RobotResult<Unit> {
        return sendSimpleCommand("cancel_docking")
    }

    override suspend fun emergencyStop(): RobotResult<Unit> {
        return sendSimpleCommand("emergency_stop")
    }

    override suspend fun releaseEmergencyStop(): RobotResult<Unit> {
        return sendSimpleCommand("release_emergency_stop")
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
        try {
            val envelope = ProtocolCodec.decodeEnvelope(text)
            when (envelope.type) {
                "hello_ack" -> {
                    val ack = ProtocolCodec.decodeHelloAck(envelope)
                    val gatewayVer = ack?.gatewayVersion ?: "0.1.0"
                    _connectionState.value = ConnectionState.Connected(gatewayVer, envelope.protocolVersion)
                    connectHandshakeDeferred?.complete(RobotResult.Success(Unit))
                    config.logger.log(RobotLogEvent(RobotLogLevel.INFO, "CLIENT", "Connected & authenticated to Gateway $gatewayVer"))
                }
                "command_ack" -> {
                    envelope.id?.let { commandRegistry.completeSuccess(it) }
                }
                "command_error" -> {
                    val err = envelope.error?.let { ProtocolCodec.mapRobotError(it) }
                        ?: RobotError("UNKNOWN_ERROR", "gateway", ErrorSeverity.ERROR, "Command error", true)
                    envelope.id?.let { commandRegistry.completeError(it, err) }
                }
                "telemetry" -> {
                    ProtocolCodec.decodeTelemetry(envelope)?.let { _telemetry.value = it }
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
    }

    override fun onFailure(t: Throwable, response: Response?) {
        val err = RobotError("TRANSPORT_ERROR", "transport", ErrorSeverity.ERROR, t.message ?: "Transport error", true)
        _connectionState.value = ConnectionState.Failed(err)
        connectHandshakeDeferred?.complete(RobotResult.Failure(err))
        clearStatesAndPending(t.message ?: "Transport error")
    }

    override fun onClose(code: Int, reason: String) {
        val err = RobotError("CONNECTION_CLOSED", "transport", ErrorSeverity.INFO, "Closed: $code $reason", true)
        _connectionState.value = ConnectionState.Disconnected
        connectHandshakeDeferred?.complete(RobotResult.Failure(err))
        clearStatesAndPending("Connection closed")
    }

    private fun clearStatesAndPending(reason: String) {
        val err = RobotError("CONNECTION_LOST", "transport", ErrorSeverity.WARN, reason, true)
        commandRegistry.clearAll(err)
        _telemetry.value = null
        _batteryState.value = null
        _safetyState.value = null
        _dockingState.value = null
        _health.value = null
    }

    override fun close() {
        _connectionState.value = ConnectionState.Disconnected
        transport.disconnect()
        clearStatesAndPending("Client closed")
        scope.cancel()
    }
}
