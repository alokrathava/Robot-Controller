package com.alokrathava.sdk.testkit

import com.alokrathava.sdk.RobotClient
import com.alokrathava.sdk.error.ErrorSeverity
import com.alokrathava.sdk.error.RobotError
import com.alokrathava.sdk.error.RobotResult
import com.alokrathava.sdk.model.CommandId
import com.alokrathava.sdk.model.ConnectionMetrics
import com.alokrathava.sdk.model.ConnectionState
import com.alokrathava.sdk.model.DockingState
import com.alokrathava.sdk.model.MapOperationState
import com.alokrathava.sdk.model.MapPoint
import com.alokrathava.sdk.model.MapPointValidity
import com.alokrathava.sdk.model.MissionProgress
import com.alokrathava.sdk.model.MotionLimits
import com.alokrathava.sdk.model.NavigationRoute
import com.alokrathava.sdk.model.NavigationState
import com.alokrathava.sdk.model.Pose2D
import com.alokrathava.sdk.model.RobotBatteryState
import com.alokrathava.sdk.model.RobotConfiguration
import com.alokrathava.sdk.model.RobotDiagnosticReport
import com.alokrathava.sdk.model.RobotDiagnostics
import com.alokrathava.sdk.model.RobotEvent
import com.alokrathava.sdk.model.RobotHealth
import com.alokrathava.sdk.model.RobotInfo
import com.alokrathava.sdk.model.RobotLogEntry
import com.alokrathava.sdk.model.RobotMap
import com.alokrathava.sdk.model.RobotMission
import com.alokrathava.sdk.model.RobotTelemetry
import com.alokrathava.sdk.model.SafetyState
import com.alokrathava.sdk.model.SavedLocation
import com.alokrathava.sdk.model.TelemetryFreshness
import com.alokrathava.sdk.model.VirtualWall
import com.alokrathava.sdk.model.VirtualWallDraft
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeRobotClient : RobotClient {

    val mutableConnectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val connectionState: StateFlow<ConnectionState> = mutableConnectionState.asStateFlow()

    val mutableConnectionMetrics = MutableStateFlow(ConnectionMetrics())
    override val connectionMetrics: StateFlow<ConnectionMetrics> = mutableConnectionMetrics.asStateFlow()

    val mutableTelemetryFreshness = MutableStateFlow(TelemetryFreshness())
    override val telemetryFreshness: StateFlow<TelemetryFreshness> = mutableTelemetryFreshness.asStateFlow()

    val mutableTelemetry = MutableStateFlow<RobotTelemetry?>(null)
    override val telemetry: StateFlow<RobotTelemetry?> = mutableTelemetry.asStateFlow()

    val mutableBatteryState = MutableStateFlow<RobotBatteryState?>(null)
    override val batteryState: StateFlow<RobotBatteryState?> = mutableBatteryState.asStateFlow()

    val mutableSafetyState = MutableStateFlow<SafetyState?>(null)
    override val safetyState: StateFlow<SafetyState?> = mutableSafetyState.asStateFlow()

    val mutableDockingState = MutableStateFlow<DockingState?>(null)
    override val dockingState: StateFlow<DockingState?> = mutableDockingState.asStateFlow()

    val mutableHealth = MutableStateFlow<RobotHealth?>(null)
    override val health: StateFlow<RobotHealth?> = mutableHealth.asStateFlow()

    val mutableActiveMap = MutableStateFlow<RobotMap?>(null)
    override val activeMap: StateFlow<RobotMap?> = mutableActiveMap.asStateFlow()

    val mutableMapOperationState = MutableStateFlow<MapOperationState>(MapOperationState.Idle)
    override val mapOperationState: StateFlow<MapOperationState> = mutableMapOperationState.asStateFlow()

    val mutableNavigation = MutableStateFlow(NavigationState())
    override val navigation: StateFlow<NavigationState> = mutableNavigation.asStateFlow()

    val mutableMissionProgress = MutableStateFlow<MissionProgress?>(null)
    override val missionProgress: StateFlow<MissionProgress?> = mutableMissionProgress.asStateFlow()

    val mutableConfiguration = MutableStateFlow<RobotConfiguration?>(RobotConfiguration(MotionLimits(1.0, 1.5)))
    override val configuration: StateFlow<RobotConfiguration?> = mutableConfiguration.asStateFlow()

    val mutableRobotInfo = MutableStateFlow<RobotInfo?>(null)
    override val robotInfo: StateFlow<RobotInfo?> = mutableRobotInfo.asStateFlow()

    val mutableDiagnostics = MutableStateFlow(RobotDiagnostics(ConnectionMetrics(), TelemetryFreshness()))
    override val diagnostics: StateFlow<RobotDiagnostics> = mutableDiagnostics.asStateFlow()

    val mutableEvents = MutableSharedFlow<RobotEvent>(extraBufferCapacity = 64)
    override val events: SharedFlow<RobotEvent> = mutableEvents.asSharedFlow()

    var shouldSucceed: Boolean = true

    override suspend fun connect(): RobotResult<Unit> {
        mutableConnectionState.value = ConnectionState.Connected("fake-0.2.0", 1, emptySet(), "fake-session")
        return RobotResult.Success(Unit)
    }

    override suspend fun disconnect() {
        mutableConnectionState.value = ConnectionState.Disconnected
    }

    override suspend fun navigateTo(pose: Pose2D): RobotResult<CommandId> {
        return if (shouldSucceed) RobotResult.Success(CommandId("cmd-fake-nav")) else RobotResult.Failure(RobotError("FAKE_ERROR", "fake", ErrorSeverity.ERROR, "Fake failure", false))
    }

    override suspend fun navigateThrough(poses: List<Pose2D>): RobotResult<CommandId> {
        return if (shouldSucceed) RobotResult.Success(CommandId("cmd-fake-through")) else RobotResult.Failure(RobotError("FAKE_ERROR", "fake", ErrorSeverity.ERROR, "Fake failure", false))
    }

    override suspend fun navigateRoute(route: NavigationRoute): RobotResult<CommandId> {
        return navigateThrough(route.waypoints)
    }

    override suspend fun cancelNavigation(): RobotResult<Unit> {
        return RobotResult.Success(Unit)
    }

    override suspend fun setManualVelocity(linearMps: Double, angularRadPerSec: Double): RobotResult<Unit> {
        return RobotResult.Success(Unit)
    }

    override suspend fun stop(): RobotResult<Unit> {
        return RobotResult.Success(Unit)
    }

    override suspend fun dock(): RobotResult<Unit> {
        return RobotResult.Success(Unit)
    }

    override suspend fun undock(): RobotResult<Unit> {
        return RobotResult.Success(Unit)
    }

    override suspend fun cancelDocking(): RobotResult<Unit> {
        return RobotResult.Success(Unit)
    }

    override suspend fun emergencyStop(): RobotResult<Unit> {
        return RobotResult.Success(Unit)
    }

    override suspend fun releaseEmergencyStop(): RobotResult<Unit> {
        return RobotResult.Success(Unit)
    }

    override suspend fun listMaps(): RobotResult<List<RobotMap>> {
        val maps = listOf(RobotMap("map_1", "Main Map", isActive = true))
        return RobotResult.Success(maps)
    }

    override suspend fun switchMap(mapId: String): RobotResult<Unit> {
        mutableActiveMap.value = RobotMap(mapId, mapId, isActive = true)
        return RobotResult.Success(Unit)
    }

    override suspend fun saveCurrentMap(name: String): RobotResult<RobotMap> {
        val newMap = RobotMap("map_${name.lowercase()}", name, isActive = true)
        mutableActiveMap.value = newMap
        return RobotResult.Success(newMap)
    }

    override suspend fun renameMap(mapId: String, newName: String): RobotResult<RobotMap> {
        val renamed = RobotMap(mapId, newName, isActive = true)
        return RobotResult.Success(renamed)
    }

    override suspend fun deleteMap(mapId: String): RobotResult<Unit> {
        return RobotResult.Success(Unit)
    }

    override suspend fun startMapping(): RobotResult<Unit> {
        mutableMapOperationState.value = MapOperationState.Mapping(null)
        return RobotResult.Success(Unit)
    }

    override suspend fun stopMapping(discardUnsaved: Boolean): RobotResult<Unit> {
        mutableMapOperationState.value = MapOperationState.Idle
        return RobotResult.Success(Unit)
    }

    override suspend fun listVirtualWalls(mapId: String): RobotResult<List<VirtualWall>> {
        return RobotResult.Success(emptyList())
    }

    override suspend fun createVirtualWall(wall: VirtualWallDraft): RobotResult<VirtualWall> {
        val created = VirtualWall("vw_1", wall.mapId, wall.name, wall.type, wall.points, wall.thicknessMeters, wall.enabled)
        return RobotResult.Success(created)
    }

    override suspend fun updateVirtualWall(wall: VirtualWall): RobotResult<VirtualWall> {
        return RobotResult.Success(wall)
    }

    override suspend fun deleteVirtualWall(wallId: String): RobotResult<Unit> {
        return RobotResult.Success(Unit)
    }

    override suspend fun setVirtualWallEnabled(wallId: String, enabled: Boolean): RobotResult<Unit> {
        return RobotResult.Success(Unit)
    }

    override suspend fun clearVirtualWalls(mapId: String): RobotResult<Unit> {
        return RobotResult.Success(Unit)
    }

    override suspend fun listSavedLocations(mapId: String): RobotResult<List<SavedLocation>> {
        return RobotResult.Success(emptyList())
    }

    override suspend fun saveLocation(name: String, pose: Pose2D, mapId: String?): RobotResult<SavedLocation> {
        val loc = SavedLocation("loc_1", name, mapId ?: "map_1", pose)
        return RobotResult.Success(loc)
    }

    override suspend fun renameLocation(id: String, name: String): RobotResult<SavedLocation> {
        val loc = SavedLocation(id, name, "map_1", Pose2D(0.0, 0.0, 0.0))
        return RobotResult.Success(loc)
    }

    override suspend fun deleteLocation(id: String): RobotResult<Unit> {
        return RobotResult.Success(Unit)
    }

    override suspend fun navigateToLocation(id: String): RobotResult<CommandId> {
        return RobotResult.Success(CommandId("cmd-fake-nav-loc"))
    }

    override suspend fun checkMapPoint(point: MapPoint): RobotResult<MapPointValidity> {
        return RobotResult.Success(MapPointValidity(isValid = true))
    }

    override suspend fun submitMission(mission: RobotMission): RobotResult<CommandId> {
        return RobotResult.Success(CommandId("cmd-fake-mission"))
    }

    override suspend fun cancelMission(): RobotResult<Unit> {
        return RobotResult.Success(Unit)
    }

    override suspend fun updateMotionLimits(limits: MotionLimits): RobotResult<Unit> {
        mutableConfiguration.value = RobotConfiguration(motionLimits = limits)
        return RobotResult.Success(Unit)
    }

    override suspend fun collectDiagnosticReport(): RobotResult<RobotDiagnosticReport> {
        val report = RobotDiagnosticReport(diagnostics = mutableDiagnostics.value)
        return RobotResult.Success(report)
    }

    override suspend fun getRecentRobotLogs(limit: Int): RobotResult<List<RobotLogEntry>> {
        return RobotResult.Success(emptyList())
    }

    override fun close() {
        mutableConnectionState.value = ConnectionState.Disconnected
    }
}
