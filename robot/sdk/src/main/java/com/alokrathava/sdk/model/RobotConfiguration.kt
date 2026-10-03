package com.alokrathava.sdk.model

import kotlinx.serialization.Serializable

@Serializable
data class MotionLimits(
    val maxLinearVelocityMps: Double,
    val maxAngularVelocityRadPerSec: Double
)

@Serializable
data class RobotConfiguration(
    val motionLimits: MotionLimits,
    val telemetryFrequencyHz: Double = 10.0
)

@Serializable
data class RobotInfo(
    val robotId: String,
    val model: String? = null,
    val firmwareVersion: String? = null,
    val navigationVersion: String? = null,
    val gatewayVersion: String,
    val capabilities: Set<RobotCapability>
)
