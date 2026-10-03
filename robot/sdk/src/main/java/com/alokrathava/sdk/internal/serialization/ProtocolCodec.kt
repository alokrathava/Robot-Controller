package com.alokrathava.sdk.internal.serialization

import com.alokrathava.sdk.error.ErrorSeverity
import com.alokrathava.sdk.error.RobotError
import com.alokrathava.sdk.internal.protocol.BatteryStatePayloadDto
import com.alokrathava.sdk.internal.protocol.DockingStatePayloadDto
import com.alokrathava.sdk.internal.protocol.HelloAckPayloadDto
import com.alokrathava.sdk.internal.protocol.HelloPayloadDto
import com.alokrathava.sdk.internal.protocol.ManualVelocityPayloadDto
import com.alokrathava.sdk.internal.protocol.NavigateToPayloadDto
import com.alokrathava.sdk.internal.protocol.ProtocolEnvelope
import com.alokrathava.sdk.internal.protocol.ProtocolErrorDto
import com.alokrathava.sdk.internal.protocol.ROBOT_PROTOCOL_VERSION
import com.alokrathava.sdk.internal.protocol.RobotHealthPayloadDto
import com.alokrathava.sdk.internal.protocol.SafetyStatePayloadDto
import com.alokrathava.sdk.internal.protocol.TelemetryPayloadDto
import com.alokrathava.sdk.model.BatteryState
import com.alokrathava.sdk.model.DockingState
import com.alokrathava.sdk.model.Pose2D
import com.alokrathava.sdk.model.RobotBatteryState
import com.alokrathava.sdk.model.RobotHealth
import com.alokrathava.sdk.model.RobotHealthStatus
import com.alokrathava.sdk.model.RobotTelemetry
import com.alokrathava.sdk.model.SafetyState
import com.alokrathava.sdk.model.SubsystemHealth
import com.alokrathava.sdk.model.SubsystemHealthLevel
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement

internal object ProtocolCodec {

    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    fun encodeEnvelope(envelope: ProtocolEnvelope): String {
        return json.encodeToString(ProtocolEnvelope.serializer(), envelope)
    }

    fun decodeEnvelope(text: String): ProtocolEnvelope {
        return json.decodeFromString(ProtocolEnvelope.serializer(), text)
    }

    fun createHelloEnvelope(id: String, token: String?): ProtocolEnvelope {
        val payload = json.encodeToJsonElement(HelloPayloadDto(token = token)) as? kotlinx.serialization.json.JsonObject
        return ProtocolEnvelope(
            type = "hello",
            id = id,
            protocolVersion = ROBOT_PROTOCOL_VERSION,
            payload = payload
        )
    }

    fun createNavigateToEnvelope(id: String, pose: Pose2D): ProtocolEnvelope {
        val dto = NavigateToPayloadDto(pose.xMeters, pose.yMeters, pose.yawRadians)
        val payload = json.encodeToJsonElement(dto) as? kotlinx.serialization.json.JsonObject
        return ProtocolEnvelope(
            type = "navigate_to",
            id = id,
            protocolVersion = ROBOT_PROTOCOL_VERSION,
            payload = payload
        )
    }

    fun createManualVelocityEnvelope(id: String, linearMps: Double, angularRadPerSec: Double): ProtocolEnvelope {
        val dto = ManualVelocityPayloadDto(linearMps, angularRadPerSec)
        val payload = json.encodeToJsonElement(dto) as? kotlinx.serialization.json.JsonObject
        return ProtocolEnvelope(
            type = "manual_velocity",
            id = id,
            protocolVersion = ROBOT_PROTOCOL_VERSION,
            payload = payload
        )
    }

    fun createSimpleCommandEnvelope(type: String, id: String): ProtocolEnvelope {
        return ProtocolEnvelope(
            type = type,
            id = id,
            protocolVersion = ROBOT_PROTOCOL_VERSION
        )
    }

    fun decodeHelloAck(envelope: ProtocolEnvelope): HelloAckPayloadDto? {
        val payload = envelope.payload ?: return null
        return try {
            json.decodeFromJsonElement<HelloAckPayloadDto>(payload)
        } catch (_: Exception) {
            null
        }
    }

    fun decodeTelemetry(envelope: ProtocolEnvelope): RobotTelemetry? {
        val payload = envelope.payload ?: return null
        return try {
            val dto = json.decodeFromJsonElement<TelemetryPayloadDto>(payload)
            RobotTelemetry(
                poseValid = dto.poseValid,
                frame = dto.frame,
                xMeters = dto.xMeters,
                yMeters = dto.yMeters,
                yawRadians = dto.yawRadians,
                linearVelocityMps = dto.linearVelocityMps,
                angularVelocityRadPerSec = dto.angularVelocityRadPerSec,
                isMoving = dto.isMoving,
                navigationState = dto.navigationState,
                distanceRemainingMeters = dto.distanceRemainingMeters,
                navigationElapsedSeconds = dto.navigationElapsedSeconds,
                hasGoal = dto.hasGoal,
                navigationErrorCode = dto.navigationErrorCode,
                navigationErrorMessage = dto.navigationErrorMessage,
                localizationState = dto.localizationState,
                localizationPositionStddevMeters = dto.localizationPositionStddevMeters,
                localizationYawStddevRadians = dto.localizationYawStddevRadians,
                safetyState = mapSafetyState(dto.safetyState),
                emergencyStopped = dto.emergencyStopped,
                collisionStopped = dto.collisionStopped,
                controlSource = dto.controlSource,
                nearestObstacleDistanceMeters = dto.nearestObstacleDistanceMeters,
                nearestObstacleBearingRadians = dto.nearestObstacleBearingRadians,
                dockingState = mapDockingState(dto.dockingState),
                isDocked = dto.isDocked,
                dockingTimeSeconds = dto.dockingTimeSeconds,
                batteryState = mapBatteryState(dto.batteryState),
                batteryPercentage = dto.batteryPercentage,
                isCharging = dto.isCharging,
                batteryVoltageVolts = dto.batteryVoltageVolts,
                batteryTemperatureCelsius = dto.batteryTemperatureCelsius,
                activeMap = dto.activeMap
            )
        } catch (_: Exception) {
            null
        }
    }

    fun decodeBatteryState(envelope: ProtocolEnvelope): RobotBatteryState? {
        val payload = envelope.payload ?: return null
        return try {
            val dto = json.decodeFromJsonElement<BatteryStatePayloadDto>(payload)
            RobotBatteryState(
                state = mapBatteryState(dto.state),
                percentage = dto.percentage,
                isCharging = dto.isCharging,
                voltageVolts = dto.voltageVolts,
                currentAmps = dto.currentAmps,
                temperatureCelsius = dto.temperatureCelsius,
                dataAgeSeconds = dto.dataAgeSeconds,
                detail = dto.detail
            )
        } catch (_: Exception) {
            null
        }
    }

    fun decodeSafetyState(envelope: ProtocolEnvelope): SafetyState? {
        val payload = envelope.payload ?: return null
        return try {
            val dto = json.decodeFromJsonElement<SafetyStatePayloadDto>(payload)
            mapSafetyState(dto.state)
        } catch (_: Exception) {
            null
        }
    }

    fun decodeDockingState(envelope: ProtocolEnvelope): DockingState? {
        val payload = envelope.payload ?: return null
        return try {
            val dto = json.decodeFromJsonElement<DockingStatePayloadDto>(payload)
            mapDockingState(dto.state)
        } catch (_: Exception) {
            null
        }
    }

    fun decodeHealth(envelope: ProtocolEnvelope): RobotHealth? {
        val payload = envelope.payload ?: return null
        return try {
            val dto = json.decodeFromJsonElement<RobotHealthPayloadDto>(payload)
            RobotHealth(
                overall = when (dto.overall) {
                    0 -> RobotHealthStatus.OK
                    1 -> RobotHealthStatus.DEGRADED
                    2 -> RobotHealthStatus.ERROR
                    3 -> RobotHealthStatus.EMERGENCY
                    else -> RobotHealthStatus.ERROR
                },
                subsystems = dto.subsystems.map {
                    SubsystemHealth(
                        name = it.name,
                        level = when (it.level) {
                            0 -> SubsystemHealthLevel.OK
                            1 -> SubsystemHealthLevel.WARN
                            2 -> SubsystemHealthLevel.ERROR
                            else -> SubsystemHealthLevel.STALE
                        }
                    )
                },
                activeErrors = dto.activeErrors.map { mapRobotError(it) }
            )
        } catch (_: Exception) {
            null
        }
    }

    fun mapRobotError(dto: ProtocolErrorDto): RobotError {
        val severity = when (dto.severity.uppercase()) {
            "INFO" -> ErrorSeverity.INFO
            "WARN" -> ErrorSeverity.WARN
            "ERROR" -> ErrorSeverity.ERROR
            "FATAL" -> ErrorSeverity.FATAL
            else -> ErrorSeverity.ERROR
        }
        return RobotError(
            code = dto.code,
            subsystem = dto.subsystem,
            severity = severity,
            message = dto.message,
            recoverable = dto.recoverable,
            sourceCode = dto.sourceCode
        )
    }

    private fun mapBatteryState(code: Int): BatteryState {
        return when (code) {
            1 -> BatteryState.DISCHARGING
            2 -> BatteryState.LOW
            3 -> BatteryState.CRITICAL
            4 -> BatteryState.CHARGING
            5 -> BatteryState.FULL
            6 -> BatteryState.FAULT
            else -> BatteryState.UNKNOWN
        }
    }

    private fun mapSafetyState(code: Int): SafetyState {
        return when (code) {
            0 -> SafetyState.NORMAL
            1 -> SafetyState.CAUTION
            2 -> SafetyState.COLLISION_STOP
            3 -> SafetyState.EMERGENCY_STOP
            4 -> SafetyState.FAULT
            else -> SafetyState.FAULT
        }
    }

    private fun mapDockingState(code: Int): DockingState {
        return when (code) {
            0 -> DockingState.UNDOCKED
            1 -> DockingState.NAVIGATING_TO_DOCK
            2 -> DockingState.PRESTAGING
            3 -> DockingState.ALIGNING
            4 -> DockingState.DOCKING
            5 -> DockingState.DOCKED
            6 -> DockingState.UNDOCKING
            7 -> DockingState.FAILED
            8 -> DockingState.CANCELLED
            else -> DockingState.FAILED
        }
    }
}
