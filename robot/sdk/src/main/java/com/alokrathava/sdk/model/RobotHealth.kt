package com.alokrathava.sdk.model

import com.alokrathava.sdk.error.RobotError

enum class RobotHealthStatus {
    OK,
    DEGRADED,
    ERROR,
    EMERGENCY
}

enum class SubsystemHealthLevel {
    OK,
    WARN,
    ERROR,
    STALE
}

data class SubsystemHealth(
    val name: String,
    val level: SubsystemHealthLevel
)

data class RobotHealth(
    val overall: RobotHealthStatus,
    val subsystems: List<SubsystemHealth>,
    val activeErrors: List<RobotError>
)
