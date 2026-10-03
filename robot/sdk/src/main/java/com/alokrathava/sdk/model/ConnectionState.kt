package com.alokrathava.sdk.model

import com.alokrathava.sdk.error.RobotError

sealed interface ConnectionState {
    data object Disconnected : ConnectionState
    data object Connecting : ConnectionState
    data object Authenticating : ConnectionState
    data class Connected(
        val gatewayVersion: String,
        val protocolVersion: Int
    ) : ConnectionState
    data class Failed(
        val error: RobotError
    ) : ConnectionState
}
