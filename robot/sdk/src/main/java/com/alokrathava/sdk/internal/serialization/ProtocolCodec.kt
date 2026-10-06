package com.alokrathava.sdk.internal.serialization

import com.alokrathava.sdk.error.ErrorSeverity
import com.alokrathava.sdk.error.RobotError
import com.alokrathava.sdk.internal.protocol.BatteryStatePayloadDto
import com.alokrathava.sdk.internal.protocol.CheckMapPointAckPayloadDto
import com.alokrathava.sdk.internal.protocol.CheckMapPointPayloadDto
import com.alokrathava.sdk.internal.protocol.ClearVirtualWallsPayloadDto
import com.alokrathava.sdk.internal.protocol.CreateVirtualWallAckPayloadDto
import com.alokrathava.sdk.internal.protocol.CreateVirtualWallPayloadDto
import com.alokrathava.sdk.internal.protocol.DeleteLocationPayloadDto
import com.alokrathava.sdk.internal.protocol.DeleteMapPayloadDto
import com.alokrathava.sdk.internal.protocol.DeleteVirtualWallPayloadDto
import com.alokrathava.sdk.internal.protocol.DockingStatePayloadDto
import com.alokrathava.sdk.internal.protocol.HelloAckPayloadDto
import com.alokrathava.sdk.internal.protocol.HelloPayloadDto
import com.alokrathava.sdk.internal.protocol.ListMapsAckPayloadDto
import com.alokrathava.sdk.internal.protocol.ListSavedLocationsAckPayloadDto
import com.alokrathava.sdk.internal.protocol.ListSavedLocationsPayloadDto
import com.alokrathava.sdk.internal.protocol.ListVirtualWallsAckPayloadDto
import com.alokrathava.sdk.internal.protocol.ListVirtualWallsPayloadDto
import com.alokrathava.sdk.internal.protocol.ManualVelocityPayloadDto
import com.alokrathava.sdk.internal.protocol.MapInfoDto
import com.alokrathava.sdk.internal.protocol.MapPointDto
import com.alokrathava.sdk.internal.protocol.MissionProgressPayloadDto
import com.alokrathava.sdk.internal.protocol.MissionStepDto
import com.alokrathava.sdk.internal.protocol.MotionLimitsDto
import com.alokrathava.sdk.internal.protocol.NavigateToLocationPayloadDto
import com.alokrathava.sdk.internal.protocol.NavigateToPayloadDto
import com.alokrathava.sdk.internal.protocol.NavigateThroughPayloadDto
import com.alokrathava.sdk.internal.protocol.ProtocolEnvelope
import com.alokrathava.sdk.internal.protocol.ProtocolErrorDto
import com.alokrathava.sdk.internal.protocol.ROBOT_PROTOCOL_VERSION
import com.alokrathava.sdk.internal.protocol.RenameLocationAckPayloadDto
import com.alokrathava.sdk.internal.protocol.RenameLocationPayloadDto
import com.alokrathava.sdk.internal.protocol.RenameMapAckPayloadDto
import com.alokrathava.sdk.internal.protocol.RenameMapPayloadDto
import com.alokrathava.sdk.internal.protocol.GetRecentLogsAckPayloadDto
import com.alokrathava.sdk.internal.protocol.GetRecentLogsPayloadDto
import com.alokrathava.sdk.internal.protocol.RobotLogEntryDto
import com.alokrathava.sdk.internal.protocol.RobotHealthPayloadDto
import com.alokrathava.sdk.internal.protocol.RobotMapStateDto
import com.alokrathava.sdk.internal.protocol.RobotMissionDto
import com.alokrathava.sdk.internal.protocol.RobotStateSnapshotPayloadDto
import com.alokrathava.sdk.internal.protocol.StopMappingPayloadDto
import com.alokrathava.sdk.internal.protocol.SafetyStatePayloadDto
import com.alokrathava.sdk.internal.protocol.SaveLocationAckPayloadDto
import com.alokrathava.sdk.internal.protocol.SaveLocationPayloadDto
import com.alokrathava.sdk.internal.protocol.SaveMapAckPayloadDto
import com.alokrathava.sdk.internal.protocol.SaveMapPayloadDto
import com.alokrathava.sdk.internal.protocol.SavedLocationDto
import com.alokrathava.sdk.internal.protocol.SetVirtualWallEnabledPayloadDto
import com.alokrathava.sdk.internal.protocol.SubmitMissionPayloadDto
import com.alokrathava.sdk.internal.protocol.SwitchMapPayloadDto
import com.alokrathava.sdk.internal.protocol.TelemetryPayloadDto
import com.alokrathava.sdk.internal.protocol.UpdateMotionLimitsPayloadDto
import com.alokrathava.sdk.internal.protocol.UpdateVirtualWallAckPayloadDto
import com.alokrathava.sdk.internal.protocol.UpdateVirtualWallPayloadDto
import com.alokrathava.sdk.internal.protocol.VirtualWallDto
import com.alokrathava.sdk.model.BatteryState
import com.alokrathava.sdk.model.DockingState
import com.alokrathava.sdk.model.MapPoint
import com.alokrathava.sdk.model.MapPointValidity
import com.alokrathava.sdk.model.MissionProgress
import com.alokrathava.sdk.model.MissionStatus
import com.alokrathava.sdk.model.MissionStep
import com.alokrathava.sdk.model.MotionLimits
import com.alokrathava.sdk.model.Pose2D
import com.alokrathava.sdk.model.RobotBatteryState
import com.alokrathava.sdk.model.RobotHealth
import com.alokrathava.sdk.model.RobotHealthStatus
import com.alokrathava.sdk.model.RobotLogEntry
import com.alokrathava.sdk.model.RobotMap
import com.alokrathava.sdk.model.RobotMission
import com.alokrathava.sdk.model.RobotTelemetry
import com.alokrathava.sdk.model.SafetyState
import com.alokrathava.sdk.model.SavedLocation
import com.alokrathava.sdk.model.SubsystemHealth
import com.alokrathava.sdk.model.SubsystemHealthLevel
import com.alokrathava.sdk.model.VirtualWall
import com.alokrathava.sdk.model.VirtualWallDraft
import com.alokrathava.sdk.model.VirtualWallType
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

    fun createPingEnvelope(id: String): ProtocolEnvelope {
        return ProtocolEnvelope(
            type = "ping",
            id = id,
            protocolVersion = ROBOT_PROTOCOL_VERSION
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

    internal fun createNavigateThroughEnvelope(id: String, poses: List<Pose2D>, stopOnFailure: Boolean = true): ProtocolEnvelope {
        val waypointsDto = poses.map { NavigateToPayloadDto(it.xMeters, it.yMeters, it.yawRadians) }
        val dto = NavigateThroughPayloadDto(waypoints = waypointsDto, stopOnFailure = stopOnFailure)
        val payload = json.encodeToJsonElement(dto) as? kotlinx.serialization.json.JsonObject
        return ProtocolEnvelope(
            type = "navigate_through",
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

    fun createSwitchMapEnvelope(id: String, mapId: String): ProtocolEnvelope {
        val dto = SwitchMapPayloadDto(mapId = mapId)
        val payload = json.encodeToJsonElement(dto) as? kotlinx.serialization.json.JsonObject
        return ProtocolEnvelope(
            type = "switch_map",
            id = id,
            protocolVersion = ROBOT_PROTOCOL_VERSION,
            payload = payload
        )
    }

    fun createSaveMapEnvelope(id: String, name: String): ProtocolEnvelope {
        val dto = SaveMapPayloadDto(name = name)
        val payload = json.encodeToJsonElement(dto) as? kotlinx.serialization.json.JsonObject
        return ProtocolEnvelope(
            type = "save_map",
            id = id,
            protocolVersion = ROBOT_PROTOCOL_VERSION,
            payload = payload
        )
    }

    internal fun createUpdateMotionLimitsEnvelope(id: String, limits: MotionLimits): ProtocolEnvelope {
        val dto = UpdateMotionLimitsPayloadDto(
            limits = MotionLimitsDto(
                maxLinearVelocityMps = limits.maxLinearVelocityMps,
                maxAngularVelocityRadPerSec = limits.maxAngularVelocityRadPerSec
            )
        )
        val payload = json.encodeToJsonElement(dto) as? kotlinx.serialization.json.JsonObject
        return ProtocolEnvelope(
            type = "update_motion_limits",
            id = id,
            protocolVersion = ROBOT_PROTOCOL_VERSION,
            payload = payload
        )
    }

    fun createRenameMapEnvelope(id: String, mapId: String, newName: String): ProtocolEnvelope {
        val dto = RenameMapPayloadDto(mapId = mapId, newName = newName)
        val payload = json.encodeToJsonElement(dto) as? kotlinx.serialization.json.JsonObject
        return ProtocolEnvelope(
            type = "rename_map",
            id = id,
            protocolVersion = ROBOT_PROTOCOL_VERSION,
            payload = payload
        )
    }

    fun createDeleteMapEnvelope(id: String, mapId: String): ProtocolEnvelope {
        val dto = DeleteMapPayloadDto(mapId = mapId)
        val payload = json.encodeToJsonElement(dto) as? kotlinx.serialization.json.JsonObject
        return ProtocolEnvelope(
            type = "delete_map",
            id = id,
            protocolVersion = ROBOT_PROTOCOL_VERSION,
            payload = payload
        )
    }

    fun mapVirtualWallToDto(wall: VirtualWall): VirtualWallDto {
        return VirtualWallDto(
            id = wall.id,
            mapId = wall.mapId,
            name = wall.name,
            type = wall.type.name,
            points = wall.points.map { MapPointDto(it.xMeters, it.yMeters) },
            thicknessMeters = wall.thicknessMeters,
            enabled = wall.enabled
        )
    }

    fun mapVirtualWallDtoToModel(dto: VirtualWallDto): VirtualWall {
        val type = try {
            VirtualWallType.valueOf(dto.type.uppercase())
        } catch (_: Exception) {
            VirtualWallType.LINE
        }
        return VirtualWall(
            id = dto.id,
            mapId = dto.mapId,
            name = dto.name,
            type = type,
            points = dto.points.map { MapPoint(it.xMeters, it.yMeters) },
            thicknessMeters = dto.thicknessMeters,
            enabled = dto.enabled
        )
    }

    fun createListVirtualWallsEnvelope(id: String, mapId: String): ProtocolEnvelope {
        val dto = ListVirtualWallsPayloadDto(mapId = mapId)
        val payload = json.encodeToJsonElement(dto) as? kotlinx.serialization.json.JsonObject
        return ProtocolEnvelope(
            type = "list_virtual_walls",
            id = id,
            protocolVersion = ROBOT_PROTOCOL_VERSION,
            payload = payload
        )
    }

    fun createCreateVirtualWallEnvelope(id: String, wall: VirtualWallDraft): ProtocolEnvelope {
        val wallDto = VirtualWallDto(
            mapId = wall.mapId,
            name = wall.name,
            type = wall.type.name,
            points = wall.points.map { MapPointDto(it.xMeters, it.yMeters) },
            thicknessMeters = wall.thicknessMeters,
            enabled = wall.enabled
        )
        val dto = CreateVirtualWallPayloadDto(wall = wallDto)
        val payload = json.encodeToJsonElement(dto) as? kotlinx.serialization.json.JsonObject
        return ProtocolEnvelope(
            type = "create_virtual_wall",
            id = id,
            protocolVersion = ROBOT_PROTOCOL_VERSION,
            payload = payload
        )
    }

    fun createUpdateVirtualWallEnvelope(id: String, wall: VirtualWall): ProtocolEnvelope {
        val dto = UpdateVirtualWallPayloadDto(wall = mapVirtualWallToDto(wall))
        val payload = json.encodeToJsonElement(dto) as? kotlinx.serialization.json.JsonObject
        return ProtocolEnvelope(
            type = "update_virtual_wall",
            id = id,
            protocolVersion = ROBOT_PROTOCOL_VERSION,
            payload = payload
        )
    }

    fun createDeleteVirtualWallEnvelope(id: String, wallId: String): ProtocolEnvelope {
        val dto = DeleteVirtualWallPayloadDto(wallId = wallId)
        val payload = json.encodeToJsonElement(dto) as? kotlinx.serialization.json.JsonObject
        return ProtocolEnvelope(
            type = "delete_virtual_wall",
            id = id,
            protocolVersion = ROBOT_PROTOCOL_VERSION,
            payload = payload
        )
    }

    fun createSetVirtualWallEnabledEnvelope(id: String, wallId: String, enabled: Boolean): ProtocolEnvelope {
        val dto = SetVirtualWallEnabledPayloadDto(wallId = wallId, enabled = enabled)
        val payload = json.encodeToJsonElement(dto) as? kotlinx.serialization.json.JsonObject
        return ProtocolEnvelope(
            type = "set_virtual_wall_enabled",
            id = id,
            protocolVersion = ROBOT_PROTOCOL_VERSION,
            payload = payload
        )
    }

    fun createClearVirtualWallsEnvelope(id: String, mapId: String): ProtocolEnvelope {
        val dto = ClearVirtualWallsPayloadDto(mapId = mapId)
        val payload = json.encodeToJsonElement(dto) as? kotlinx.serialization.json.JsonObject
        return ProtocolEnvelope(
            type = "clear_virtual_walls",
            id = id,
            protocolVersion = ROBOT_PROTOCOL_VERSION,
            payload = payload
        )
    }

    fun decodeListVirtualWallsAck(envelope: ProtocolEnvelope): List<VirtualWall>? {
        val payload = envelope.payload ?: return null
        return try {
            val dto = json.decodeFromJsonElement<ListVirtualWallsAckPayloadDto>(payload)
            dto.walls.map { mapVirtualWallDtoToModel(it) }
        } catch (_: Exception) {
            null
        }
    }

    fun decodeCreateVirtualWallAck(envelope: ProtocolEnvelope): VirtualWall? {
        val payload = envelope.payload ?: return null
        return try {
            val dto = json.decodeFromJsonElement<CreateVirtualWallAckPayloadDto>(payload)
            mapVirtualWallDtoToModel(dto.wall)
        } catch (_: Exception) {
            null
        }
    }

    fun decodeUpdateVirtualWallAck(envelope: ProtocolEnvelope): VirtualWall? {
        val payload = envelope.payload ?: return null
        return try {
            val dto = json.decodeFromJsonElement<UpdateVirtualWallAckPayloadDto>(payload)
            mapVirtualWallDtoToModel(dto.wall)
        } catch (_: Exception) {
            null
        }
    }

    internal fun mapSavedLocationDtoToModel(dto: SavedLocationDto): SavedLocation {
        return SavedLocation(
            id = dto.id,
            name = dto.name,
            mapId = dto.mapId,
            pose = Pose2D(xMeters = dto.xMeters, yMeters = dto.yMeters, yawRadians = dto.yawRadians)
        )
    }

    fun createListSavedLocationsEnvelope(id: String, mapId: String): ProtocolEnvelope {
        val dto = ListSavedLocationsPayloadDto(mapId = mapId)
        val payload = json.encodeToJsonElement(dto) as? kotlinx.serialization.json.JsonObject
        return ProtocolEnvelope(
            type = "list_saved_locations",
            id = id,
            protocolVersion = ROBOT_PROTOCOL_VERSION,
            payload = payload
        )
    }

    fun createSaveLocationEnvelope(id: String, name: String, pose: Pose2D, mapId: String?): ProtocolEnvelope {
        val poseDto = NavigateToPayloadDto(xMeters = pose.xMeters, yMeters = pose.yMeters, yawRadians = pose.yawRadians)
        val dto = SaveLocationPayloadDto(name = name, pose = poseDto, mapId = mapId)
        val payload = json.encodeToJsonElement(dto) as? kotlinx.serialization.json.JsonObject
        return ProtocolEnvelope(
            type = "save_location",
            id = id,
            protocolVersion = ROBOT_PROTOCOL_VERSION,
            payload = payload
        )
    }

    fun createRenameLocationEnvelope(id: String, locationId: String, name: String): ProtocolEnvelope {
        val dto = RenameLocationPayloadDto(id = locationId, name = name)
        val payload = json.encodeToJsonElement(dto) as? kotlinx.serialization.json.JsonObject
        return ProtocolEnvelope(
            type = "rename_location",
            id = id,
            protocolVersion = ROBOT_PROTOCOL_VERSION,
            payload = payload
        )
    }

    fun createDeleteLocationEnvelope(id: String, locationId: String): ProtocolEnvelope {
        val dto = DeleteLocationPayloadDto(id = locationId)
        val payload = json.encodeToJsonElement(dto) as? kotlinx.serialization.json.JsonObject
        return ProtocolEnvelope(
            type = "delete_location",
            id = id,
            protocolVersion = ROBOT_PROTOCOL_VERSION,
            payload = payload
        )
    }

    fun createNavigateToLocationEnvelope(id: String, locationId: String): ProtocolEnvelope {
        val dto = NavigateToLocationPayloadDto(id = locationId)
        val payload = json.encodeToJsonElement(dto) as? kotlinx.serialization.json.JsonObject
        return ProtocolEnvelope(
            type = "navigate_to_location",
            id = id,
            protocolVersion = ROBOT_PROTOCOL_VERSION,
            payload = payload
        )
    }

    fun createCheckMapPointEnvelope(id: String, point: MapPoint): ProtocolEnvelope {
        val dto = CheckMapPointPayloadDto(xMeters = point.xMeters, yMeters = point.yMeters)
        val payload = json.encodeToJsonElement(dto) as? kotlinx.serialization.json.JsonObject
        return ProtocolEnvelope(
            type = "check_map_point",
            id = id,
            protocolVersion = ROBOT_PROTOCOL_VERSION,
            payload = payload
        )
    }

    fun decodeListSavedLocationsAck(envelope: ProtocolEnvelope): List<SavedLocation>? {
        val payload = envelope.payload ?: return null
        return try {
            val dto = json.decodeFromJsonElement<ListSavedLocationsAckPayloadDto>(payload)
            dto.locations.map { mapSavedLocationDtoToModel(it) }
        } catch (_: Exception) {
            null
        }
    }

    fun decodeSaveLocationAck(envelope: ProtocolEnvelope): SavedLocation? {
        val payload = envelope.payload ?: return null
        return try {
            val dto = json.decodeFromJsonElement<SaveLocationAckPayloadDto>(payload)
            mapSavedLocationDtoToModel(dto.location)
        } catch (_: Exception) {
            null
        }
    }

    fun decodeRenameLocationAck(envelope: ProtocolEnvelope): SavedLocation? {
        val payload = envelope.payload ?: return null
        return try {
            val dto = json.decodeFromJsonElement<RenameLocationAckPayloadDto>(payload)
            mapSavedLocationDtoToModel(dto.location)
        } catch (_: Exception) {
            null
        }
    }

    fun decodeCheckMapPointAck(envelope: ProtocolEnvelope): MapPointValidity? {
        val payload = envelope.payload ?: return null
        return try {
            val dto = json.decodeFromJsonElement<CheckMapPointAckPayloadDto>(payload)
            MapPointValidity(isValid = dto.isValid, reason = dto.reason)
        } catch (_: Exception) {
            null
        }
    }

    internal fun mapMissionStepToDto(step: MissionStep): MissionStepDto {
        return when (step) {
            is MissionStep.Navigate -> MissionStepDto(
                type = "NAVIGATE",
                xMeters = step.pose.xMeters,
                yMeters = step.pose.yMeters,
                yawRadians = step.pose.yawRadians
            )
            is MissionStep.NavigateToLocation -> MissionStepDto(
                type = "NAVIGATE_TO_LOCATION",
                locationId = step.locationId
            )
            is MissionStep.Wait -> MissionStepDto(
                type = "WAIT",
                durationMillis = step.durationMillis
            )
            is MissionStep.Dock -> MissionStepDto(type = "DOCK")
            is MissionStep.Undock -> MissionStepDto(type = "UNDOCK")
        }
    }

    internal fun mapMissionToDto(mission: RobotMission): RobotMissionDto {
        return RobotMissionDto(
            id = mission.id,
            name = mission.name,
            steps = mission.steps.map { mapMissionStepToDto(it) }
        )
    }

    internal fun createSubmitMissionEnvelope(id: String, mission: RobotMission): ProtocolEnvelope {
        val dto = SubmitMissionPayloadDto(mission = mapMissionToDto(mission))
        val payload = json.encodeToJsonElement(dto) as? kotlinx.serialization.json.JsonObject
        return ProtocolEnvelope(
            type = "submit_mission",
            id = id,
            protocolVersion = ROBOT_PROTOCOL_VERSION,
            payload = payload
        )
    }

    internal fun createCancelMissionEnvelope(id: String): ProtocolEnvelope {
        return ProtocolEnvelope(
            type = "cancel_mission",
            id = id,
            protocolVersion = ROBOT_PROTOCOL_VERSION
        )
    }

    internal fun decodeMissionProgress(envelope: ProtocolEnvelope): MissionProgress? {
        val payload = envelope.payload ?: return null
        return try {
            val dto = json.decodeFromJsonElement<MissionProgressPayloadDto>(payload)
            val status = try {
                MissionStatus.valueOf(dto.status.uppercase())
            } catch (_: Exception) {
                MissionStatus.IDLE
            }
            MissionProgress(
                missionId = dto.missionId,
                status = status,
                currentStepIndex = dto.currentStepIndex,
                totalSteps = dto.totalSteps,
                detail = dto.detail
            )
        } catch (_: Exception) {
            null
        }
    }

    internal fun createRequestStateSnapshotEnvelope(id: String): ProtocolEnvelope {
        return ProtocolEnvelope(
            type = "request_state_snapshot",
            id = id,
            protocolVersion = ROBOT_PROTOCOL_VERSION
        )
    }

    internal fun decodeRobotStateSnapshot(envelope: ProtocolEnvelope): RobotStateSnapshotPayloadDto? {
        val payload = envelope.payload ?: return null
        return try {
            json.decodeFromJsonElement<RobotStateSnapshotPayloadDto>(payload)
        } catch (_: Exception) {
            null
        }
    }

    internal fun decodeMapState(envelope: ProtocolEnvelope): RobotMapStateDto? {
        val payload = envelope.payload ?: return null
        return try {
            json.decodeFromJsonElement<RobotMapStateDto>(payload)
        } catch (_: Exception) {
            null
        }
    }

    internal fun createStartMappingEnvelope(id: String): ProtocolEnvelope {
        return ProtocolEnvelope(
            type = "start_mapping",
            id = id,
            protocolVersion = ROBOT_PROTOCOL_VERSION
        )
    }

    internal fun createStopMappingEnvelope(id: String, discardUnsaved: Boolean = false): ProtocolEnvelope {
        val dto = StopMappingPayloadDto(discardUnsaved = discardUnsaved)
        val payload = json.encodeToJsonElement(dto) as? kotlinx.serialization.json.JsonObject
        return ProtocolEnvelope(
            type = "stop_mapping",
            id = id,
            protocolVersion = ROBOT_PROTOCOL_VERSION,
            payload = payload
        )
    }

    internal fun createGetRecentLogsEnvelope(id: String, limit: Int = 100): ProtocolEnvelope {
        val dto = GetRecentLogsPayloadDto(limit = limit)
        val payload = json.encodeToJsonElement(dto) as? kotlinx.serialization.json.JsonObject
        return ProtocolEnvelope(
            type = "get_recent_logs",
            id = id,
            protocolVersion = ROBOT_PROTOCOL_VERSION,
            payload = payload
        )
    }

    internal fun decodeGetRecentLogsAck(envelope: ProtocolEnvelope): List<RobotLogEntry>? {
        val payload = envelope.payload ?: return null
        return try {
            val dto = json.decodeFromJsonElement<GetRecentLogsAckPayloadDto>(payload)
            dto.logs.map {
                RobotLogEntry(
                    timestampEpochMs = it.timestampEpochMs,
                    level = it.level,
                    logger = it.logger,
                    message = it.message
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    internal fun createSimpleCommandEnvelope(type: String, id: String): ProtocolEnvelope {
        return ProtocolEnvelope(
            type = type,
            id = id,
            protocolVersion = ROBOT_PROTOCOL_VERSION
        )
    }

    internal fun decodeHelloAck(envelope: ProtocolEnvelope): HelloAckPayloadDto? {
        val payload = envelope.payload ?: return null
        return try {
            json.decodeFromJsonElement<HelloAckPayloadDto>(payload)
        } catch (_: Exception) {
            null
        }
    }

    internal fun mapMapInfoDto(dto: MapInfoDto): RobotMap {
        val res = dto.resolutionMetersPerCell ?: dto.resolution?.toDouble()
        val width = dto.widthCells ?: dto.width
        val height = dto.heightCells ?: dto.height
        return RobotMap(
            id = dto.id,
            name = dto.name,
            isActive = dto.isActive,
            resolutionMetersPerCell = res,
            widthCells = width,
            heightCells = height,
            createdAtEpochMs = dto.createdAtEpochMs,
            updatedAtEpochMs = dto.updatedAtEpochMs
        )
    }

    fun decodeListMapsAck(envelope: ProtocolEnvelope): List<RobotMap>? {
        val payload = envelope.payload ?: return null
        return try {
            val dto = json.decodeFromJsonElement<ListMapsAckPayloadDto>(payload)
            dto.maps.map { mapMapInfoDto(it) }
        } catch (_: Exception) {
            null
        }
    }

    fun decodeSaveMapAck(envelope: ProtocolEnvelope): RobotMap? {
        val payload = envelope.payload ?: return null
        return try {
            val dto = json.decodeFromJsonElement<SaveMapAckPayloadDto>(payload)
            mapMapInfoDto(dto.map)
        } catch (_: Exception) {
            null
        }
    }

    fun decodeRenameMapAck(envelope: ProtocolEnvelope): RobotMap? {
        val payload = envelope.payload ?: return null
        return try {
            val dto = json.decodeFromJsonElement<RenameMapAckPayloadDto>(payload)
            mapMapInfoDto(dto.map)
        } catch (_: Exception) {
            null
        }
    }

    fun mapTelemetryPayload(dto: TelemetryPayloadDto): RobotTelemetry {
        return RobotTelemetry(
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
    }

    fun mapBatteryStatePayload(dto: BatteryStatePayloadDto): RobotBatteryState {
        return RobotBatteryState(
            state = mapBatteryState(dto.state),
            percentage = dto.percentage,
            isCharging = dto.isCharging,
            voltageVolts = dto.voltageVolts,
            currentAmps = dto.currentAmps,
            temperatureCelsius = dto.temperatureCelsius,
            dataAgeSeconds = dto.dataAgeSeconds,
            detail = dto.detail
        )
    }

    fun mapSafetyStatePayload(dto: SafetyStatePayloadDto): SafetyState {
        return mapSafetyState(dto.state)
    }

    fun mapDockingStatePayload(dto: DockingStatePayloadDto): DockingState {
        return mapDockingState(dto.state)
    }

    fun mapHealthPayload(dto: RobotHealthPayloadDto): RobotHealth {
        return RobotHealth(
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
    }

    fun decodeTelemetry(envelope: ProtocolEnvelope): RobotTelemetry? {
        val payload = envelope.payload ?: return null
        return try {
            val dto = json.decodeFromJsonElement<TelemetryPayloadDto>(payload)
            mapTelemetryPayload(dto)
        } catch (_: Exception) {
            null
        }
    }

    fun decodeBatteryState(envelope: ProtocolEnvelope): RobotBatteryState? {
        val payload = envelope.payload ?: return null
        return try {
            val dto = json.decodeFromJsonElement<BatteryStatePayloadDto>(payload)
            mapBatteryStatePayload(dto)
        } catch (_: Exception) {
            null
        }
    }

    fun decodeSafetyState(envelope: ProtocolEnvelope): SafetyState? {
        val payload = envelope.payload ?: return null
        return try {
            val dto = json.decodeFromJsonElement<SafetyStatePayloadDto>(payload)
            mapSafetyStatePayload(dto)
        } catch (_: Exception) {
            null
        }
    }

    fun decodeDockingState(envelope: ProtocolEnvelope): DockingState? {
        val payload = envelope.payload ?: return null
        return try {
            val dto = json.decodeFromJsonElement<DockingStatePayloadDto>(payload)
            mapDockingStatePayload(dto)
        } catch (_: Exception) {
            null
        }
    }

    fun decodeHealth(envelope: ProtocolEnvelope): RobotHealth? {
        val payload = envelope.payload ?: return null
        return try {
            val dto = json.decodeFromJsonElement<RobotHealthPayloadDto>(payload)
            mapHealthPayload(dto)
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
