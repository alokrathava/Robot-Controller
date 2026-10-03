package com.alokrathava.sdk.model

import com.alokrathava.sdk.error.RobotError

sealed interface ConnectionState {

    data object Disconnected : ConnectionState

    data class Connecting(
        val attempt: Int = 1
    ) : ConnectionState

    data object Authenticating : ConnectionState

    data class Connected(
        val gatewayVersion: String,
        val protocolVersion: Int,
        val capabilities: Set<RobotCapability> = emptySet(),
        val sessionId: String? = null
    ) : ConnectionState

    data class Reconnecting(
        val attempt: Int,
        val retryInMs: Long
    ) : ConnectionState

    data class Failed(
        val error: RobotError
    ) : ConnectionState
}
