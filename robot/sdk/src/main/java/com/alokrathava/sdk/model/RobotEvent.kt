package com.alokrathava.sdk.model

import com.alokrathava.sdk.error.RobotError

sealed interface RobotEvent {
    data class ErrorRaised(val error: RobotError) : RobotEvent
    data class ErrorCleared(val code: String) : RobotEvent
    data class ConnectionLost(val reason: String) : RobotEvent
    data class ConnectionRestored(val durationMs: Long) : RobotEvent
}
