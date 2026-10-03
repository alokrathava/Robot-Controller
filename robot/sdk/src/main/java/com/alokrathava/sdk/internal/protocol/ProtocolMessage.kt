package com.alokrathava.sdk.internal.protocol

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

internal const val ROBOT_PROTOCOL_VERSION = 1

@Serializable
internal data class ProtocolEnvelope(
    val type: String,
    val id: String? = null,
    val protocolVersion: Int = ROBOT_PROTOCOL_VERSION,
    val payload: JsonObject? = null,
    val error: ProtocolErrorDto? = null
)

@Serializable
internal data class ProtocolErrorDto(
    val code: String,
    val subsystem: String = "system",
    val severity: String = "ERROR",
    val message: String,
    val recoverable: Boolean = true,
    val sourceCode: Int? = null
)

@Serializable
internal data class HelloPayloadDto(
    val sdkVersion: String = "0.2.0",
    val clientId: String = "android-client",
    val token: String? = null
)

@Serializable
internal data class HelloAckPayloadDto(
    val gatewayVersion: String = "0.2.0",
    val capabilities: List<String> = emptyList(),
    val sessionId: String? = null
)

@Serializable
internal data class NavigateToPayloadDto(
    val xMeters: Double,
    val yMeters: Double,
    val yawRadians: Double
)

@Serializable
internal data class ManualVelocityPayloadDto(
    val linearMps: Double,
    val angularRadPerSec: Double
)

@Serializable
internal data class MapInfoDto(
    val id: String,
    val name: String,
    val isActive: Boolean = false,
    val resolution: Float? = null,
    val width: Int? = null,
    val height: Int? = null
)

@Serializable
internal data class ListMapsAckPayloadDto(
    val maps: List<MapInfoDto> = emptyList()
)

@Serializable
internal data class SwitchMapPayloadDto(
    val mapId: String
)

@Serializable
internal data class TelemetryPayloadDto(
    val poseValid: Boolean = false,
    val frame: String = "map",
    val xMeters: Double = 0.0,
    val yMeters: Double = 0.0,
    val yawRadians: Double = 0.0,
    val linearVelocityMps: Double = 0.0,
    val angularVelocityRadPerSec: Double = 0.0,
    val isMoving: Boolean = false,
    val navigationState: String = "IDLE",
    val distanceRemainingMeters: Float? = null,
    val navigationElapsedSeconds: Float = 0f,
    val hasGoal: Boolean = false,
    val navigationErrorCode: Int = 0,
    val navigationErrorMessage: String = "",
    val localizationState: String = "UNINITIALIZED",
    val localizationPositionStddevMeters: Float? = null,
    val localizationYawStddevRadians: Float? = null,
    val safetyState: Int = 0,
    val emergencyStopped: Boolean = false,
    val collisionStopped: Boolean = false,
    val controlSource: String = "none",
    val nearestObstacleDistanceMeters: Float? = null,
    val nearestObstacleBearingRadians: Float? = null,
    val dockingState: Int = 0,
    val isDocked: Boolean = false,
    val dockingTimeSeconds: Float = 0f,
    val batteryState: Int = 0,
    val batteryPercentage: Float = 0f,
    val isCharging: Boolean = false,
    val batteryVoltageVolts: Float = 0f,
    val batteryTemperatureCelsius: Float = 0f,
    val activeMap: String = ""
)

@Serializable
internal data class BatteryStatePayloadDto(
    val state: Int = 0,
    val percentage: Float = 0f,
    val isCharging: Boolean = false,
    val voltageVolts: Float = 0f,
    val currentAmps: Float? = null,
    val temperatureCelsius: Float? = null,
    val dataAgeSeconds: Float? = null,
    val detail: String? = null
)

@Serializable
internal data class SafetyStatePayloadDto(
    val state: Int = 0,
    val emergencyStopActive: Boolean = false,
    val motionInhibited: Boolean = false,
    val controlSource: String = "none",
    val detail: String = "",
    val deadManStopCount: Long = 0
)

@Serializable
internal data class DockingStatePayloadDto(
    val state: Int = 0,
    val dockId: String = "",
    val isDocked: Boolean = false,
    val isCharging: Boolean = false,
    val dockingTimeSeconds: Float = 0f,
    val numRetries: Int = 0,
    val errorCode: Int = 0,
    val errorName: String = "",
    val errorMsg: String = "",
    val detail: String = ""
)

@Serializable
internal data class SubsystemHealthDto(
    val name: String,
    val level: Int
)

@Serializable
internal data class RobotHealthPayloadDto(
    val overall: Int = 0,
    val subsystems: List<SubsystemHealthDto> = emptyList(),
    val activeErrors: List<ProtocolErrorDto> = emptyList()
)
