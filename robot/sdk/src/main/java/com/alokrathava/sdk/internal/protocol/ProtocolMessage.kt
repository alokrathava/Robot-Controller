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
internal data class NavigateThroughPayloadDto(
    val waypoints: List<NavigateToPayloadDto>,
    val stopOnFailure: Boolean = true
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
    val resolutionMetersPerCell: Double? = null,
    val width: Int? = null,
    val widthCells: Int? = null,
    val height: Int? = null,
    val heightCells: Int? = null,
    val createdAtEpochMs: Long? = null,
    val updatedAtEpochMs: Long? = null
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
internal data class SaveMapPayloadDto(
    val name: String
)

@Serializable
internal data class SaveMapAckPayloadDto(
    val map: MapInfoDto
)

@Serializable
internal data class RenameMapPayloadDto(
    val mapId: String,
    val newName: String
)

@Serializable
internal data class RenameMapAckPayloadDto(
    val map: MapInfoDto
)

@Serializable
internal data class DeleteMapPayloadDto(
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

@Serializable
internal data class MapPointDto(
    val xMeters: Double,
    val yMeters: Double
)

@Serializable
internal data class VirtualWallDto(
    val id: String = "",
    val mapId: String,
    val name: String,
    val type: String,
    val points: List<MapPointDto>,
    val thicknessMeters: Double? = null,
    val enabled: Boolean = true
)

@Serializable
internal data class ListVirtualWallsPayloadDto(
    val mapId: String
)

@Serializable
internal data class ListVirtualWallsAckPayloadDto(
    val mapId: String = "",
    val walls: List<VirtualWallDto> = emptyList()
)

@Serializable
internal data class CreateVirtualWallPayloadDto(
    val wall: VirtualWallDto
)

@Serializable
internal data class CreateVirtualWallAckPayloadDto(
    val wall: VirtualWallDto
)

@Serializable
internal data class UpdateVirtualWallPayloadDto(
    val wall: VirtualWallDto
)

@Serializable
internal data class UpdateVirtualWallAckPayloadDto(
    val wall: VirtualWallDto
)

@Serializable
internal data class DeleteVirtualWallPayloadDto(
    val wallId: String
)

@Serializable
internal data class SetVirtualWallEnabledPayloadDto(
    val wallId: String,
    val enabled: Boolean
)

@Serializable
internal data class ClearVirtualWallsPayloadDto(
    val mapId: String
)

@Serializable
internal data class SavedLocationDto(
    val id: String = "",
    val name: String,
    val mapId: String,
    val xMeters: Double,
    val yMeters: Double,
    val yawRadians: Double = 0.0
)

@Serializable
internal data class ListSavedLocationsPayloadDto(
    val mapId: String
)

@Serializable
internal data class ListSavedLocationsAckPayloadDto(
    val mapId: String = "",
    val locations: List<SavedLocationDto> = emptyList()
)

@Serializable
internal data class SaveLocationPayloadDto(
    val name: String,
    val pose: NavigateToPayloadDto,
    val mapId: String? = null
)

@Serializable
internal data class SaveLocationAckPayloadDto(
    val location: SavedLocationDto
)

@Serializable
internal data class RenameLocationPayloadDto(
    val id: String,
    val name: String
)

@Serializable
internal data class RenameLocationAckPayloadDto(
    val location: SavedLocationDto
)

@Serializable
internal data class DeleteLocationPayloadDto(
    val id: String
)

@Serializable
internal data class NavigateToLocationPayloadDto(
    val id: String
)

@Serializable
internal data class CheckMapPointPayloadDto(
    val xMeters: Double,
    val yMeters: Double
)

@Serializable
internal data class CheckMapPointAckPayloadDto(
    val isValid: Boolean,
    val reason: String? = null
)

@Serializable
internal data class MissionStepDto(
    val type: String,
    val xMeters: Double? = null,
    val yMeters: Double? = null,
    val yawRadians: Double? = null,
    val locationId: String? = null,
    val durationMillis: Long? = null
)

@Serializable
internal data class RobotMissionDto(
    val id: String? = null,
    val name: String,
    val steps: List<MissionStepDto> = emptyList()
)

@Serializable
internal data class SubmitMissionPayloadDto(
    val mission: RobotMissionDto
)

@Serializable
internal data class MissionProgressPayloadDto(
    val missionId: String,
    val status: String,
    val currentStepIndex: Int = 0,
    val totalSteps: Int = 0,
    val detail: String = ""
)

@Serializable
internal data class MotionLimitsDto(
    val maxLinearVelocityMps: Double,
    val maxAngularVelocityRadPerSec: Double
)

@Serializable
internal data class RobotConfigurationDto(
    val motionLimits: MotionLimitsDto,
    val telemetryFrequencyHz: Double = 10.0
)

@Serializable
internal data class UpdateMotionLimitsPayloadDto(
    val limits: MotionLimitsDto
)

@Serializable
internal data class RobotLogEntryDto(
    val timestampEpochMs: Long,
    val level: String,
    val logger: String,
    val message: String
)

@Serializable
internal data class GetRecentLogsPayloadDto(
    val limit: Int = 100
)

@Serializable
internal data class GetRecentLogsAckPayloadDto(
    val logs: List<RobotLogEntryDto> = emptyList()
)

@Serializable
internal data class RobotMapStateDto(
    val state: String = "",
    val mode: String = "",
    val activeMapId: String? = null,
    val mapAvailable: Boolean = false,
    val localizationReady: Boolean = false,
    val navigationRuntimeReady: Boolean = false,
    val errorCode: String? = null,
    val detail: String? = null
)

@Serializable
internal data class StopMappingPayloadDto(
    val discardUnsaved: Boolean = false
)

@Serializable
internal data class RobotStateSnapshotPayloadDto(
    val telemetry: TelemetryPayloadDto? = null,
    val battery: BatteryStatePayloadDto? = null,
    val safety: SafetyStatePayloadDto? = null,
    val docking: DockingStatePayloadDto? = null,
    val health: RobotHealthPayloadDto? = null,
    val mapState: RobotMapStateDto? = null
)
