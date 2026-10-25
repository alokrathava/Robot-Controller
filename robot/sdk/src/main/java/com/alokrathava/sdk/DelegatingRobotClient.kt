package com.alokrathava.sdk

import com.alokrathava.sdk.error.RobotResult
import com.alokrathava.sdk.model.CommandId
import com.alokrathava.sdk.model.ConnectionMetrics
import com.alokrathava.sdk.model.ConnectionState
import com.alokrathava.sdk.model.DockingState
import com.alokrathava.sdk.model.MapOperationState
import com.alokrathava.sdk.model.Pose2D
import com.alokrathava.sdk.model.RobotBatteryState
import com.alokrathava.sdk.model.RobotEvent
import com.alokrathava.sdk.model.RobotHealth
import com.alokrathava.sdk.model.RobotMap
import com.alokrathava.sdk.model.RobotTelemetry
import com.alokrathava.sdk.model.SafetyState
import com.alokrathava.sdk.model.TelemetryFreshness
import com.alokrathava.sdk.model.VirtualWall
import com.alokrathava.sdk.model.VirtualWallDraft
import com.alokrathava.sdk.model.SavedLocation
import com.alokrathava.sdk.model.MapPoint
import com.alokrathava.sdk.model.MapPointValidity
import com.alokrathava.sdk.model.NavigationRoute
import com.alokrathava.sdk.model.NavigationState
import com.alokrathava.sdk.model.MissionProgress
import com.alokrathava.sdk.model.RobotMission
import com.alokrathava.sdk.model.MotionLimits
import com.alokrathava.sdk.model.RobotConfiguration
import com.alokrathava.sdk.model.RobotInfo
import com.alokrathava.sdk.model.RobotDiagnostics
import com.alokrathava.sdk.model.RobotDiagnosticReport
import com.alokrathava.sdk.model.RobotLogEntry
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

class DelegatingRobotClient(
    private val connectionManager: RobotConnectionManager,
    fallbackConfig: RobotSdkConfig = RobotSdkConfig(endpoint = RobotEndpoint("192.168.1.100", 8080))
) : RobotClient {
    private val fallbackClient = RobotSdk.create(fallbackConfig)
    private val client: RobotClient
        get() = connectionManager.getActiveClient() ?: fallbackClient

    override val connectionState: StateFlow<ConnectionState> get() = client.connectionState
    override val connectionMetrics: StateFlow<ConnectionMetrics> get() = client.connectionMetrics
    override val telemetryFreshness: StateFlow<TelemetryFreshness> get() = client.telemetryFreshness
    override val telemetry: StateFlow<RobotTelemetry?> get() = client.telemetry
    override val batteryState: StateFlow<RobotBatteryState?> get() = client.batteryState
    override val safetyState: StateFlow<SafetyState?> get() = client.safetyState
    override val dockingDetails: StateFlow<com.alokrathava.sdk.model.DockingStateDetails?> get() = client.dockingDetails
    override val dockingState: StateFlow<DockingState?> get() = client.dockingState
    override val health: StateFlow<RobotHealth?> get() = client.health
    override val activeMap: StateFlow<RobotMap?> get() = client.activeMap
    override val occupancyGrid: StateFlow<com.alokrathava.sdk.model.RobotOccupancyGrid?> get() = client.occupancyGrid
    override val mapOperationState: StateFlow<MapOperationState> get() = client.mapOperationState
    override val navigation: StateFlow<NavigationState> get() = client.navigation
    override val missionProgress: StateFlow<MissionProgress?> get() = client.missionProgress
    override val configuration: StateFlow<RobotConfiguration?> get() = client.configuration
    override val robotInfo: StateFlow<RobotInfo?> get() = client.robotInfo
    override val diagnostics: StateFlow<RobotDiagnostics> get() = client.diagnostics
    override val events: SharedFlow<RobotEvent> get() = client.events

    override suspend fun connect(): RobotResult<Unit> = client.connect()
    override suspend fun disconnect() = client.disconnect()
    override suspend fun navigateTo(pose: Pose2D): RobotResult<CommandId> = client.navigateTo(pose)
    override suspend fun navigateThrough(poses: List<Pose2D>): RobotResult<CommandId> = client.navigateThrough(poses)
    override suspend fun navigateRoute(route: NavigationRoute): RobotResult<CommandId> = client.navigateRoute(route)
    override suspend fun cancelNavigation(): RobotResult<Unit> = client.cancelNavigation()
    override suspend fun setManualVelocity(linearMps: Double, angularRadPerSec: Double): RobotResult<Unit> = client.setManualVelocity(linearMps, angularRadPerSec)
    override suspend fun stop(): RobotResult<Unit> = client.stop()
    override suspend fun dock(): RobotResult<Unit> = client.dock()
    override suspend fun undock(): RobotResult<Unit> = client.undock()
    override suspend fun cancelDocking(): RobotResult<Unit> = client.cancelDocking()
    override suspend fun emergencyStop(): RobotResult<Unit> = client.emergencyStop()
    override suspend fun releaseEmergencyStop(): RobotResult<Unit> = client.releaseEmergencyStop()
    override suspend fun listMaps(): RobotResult<List<RobotMap>> = client.listMaps()
    override suspend fun switchMap(mapId: String): RobotResult<Unit> = client.switchMap(mapId)
    override suspend fun saveCurrentMap(name: String): RobotResult<RobotMap> = client.saveCurrentMap(name)
    override suspend fun renameMap(mapId: String, newName: String): RobotResult<RobotMap> = client.renameMap(mapId, newName)
    override suspend fun deleteMap(mapId: String): RobotResult<Unit> = client.deleteMap(mapId)
    override suspend fun startMapping(resumeUnsaved: Boolean, discardUnsaved: Boolean): RobotResult<Unit> = client.startMapping(resumeUnsaved, discardUnsaved)
    override suspend fun stopMapping(discardUnsaved: Boolean): RobotResult<Unit> = client.stopMapping(discardUnsaved)
    override suspend fun listVirtualWalls(mapId: String): RobotResult<List<VirtualWall>> = client.listVirtualWalls(mapId)
    override suspend fun createVirtualWall(wall: VirtualWallDraft): RobotResult<VirtualWall> = client.createVirtualWall(wall)
    override suspend fun updateVirtualWall(wall: VirtualWall): RobotResult<VirtualWall> = client.updateVirtualWall(wall)
    override suspend fun deleteVirtualWall(wallId: String): RobotResult<Unit> = client.deleteVirtualWall(wallId)
    override suspend fun setVirtualWallEnabled(wallId: String, enabled: Boolean): RobotResult<Unit> = client.setVirtualWallEnabled(wallId, enabled)
    override suspend fun clearVirtualWalls(mapId: String): RobotResult<Unit> = client.clearVirtualWalls(mapId)
    override suspend fun listSavedLocations(mapId: String): RobotResult<List<SavedLocation>> = client.listSavedLocations(mapId)
    override suspend fun saveLocation(name: String, pose: Pose2D, mapId: String?): RobotResult<SavedLocation> = client.saveLocation(name, pose, mapId)
    override suspend fun renameLocation(id: String, name: String): RobotResult<SavedLocation> = client.renameLocation(id, name)
    override suspend fun deleteLocation(id: String): RobotResult<Unit> = client.deleteLocation(id)
    override suspend fun navigateToLocation(id: String): RobotResult<CommandId> = client.navigateToLocation(id)
    override suspend fun checkMapPoint(point: MapPoint): RobotResult<MapPointValidity> = client.checkMapPoint(point)
    override suspend fun submitMission(mission: RobotMission): RobotResult<CommandId> = client.submitMission(mission)
    override suspend fun cancelMission(): RobotResult<Unit> = client.cancelMission()
    override suspend fun updateMotionLimits(limits: MotionLimits): RobotResult<Unit> = client.updateMotionLimits(limits)
    override suspend fun collectDiagnosticReport(): RobotResult<RobotDiagnosticReport> = client.collectDiagnosticReport()
    override suspend fun getRecentRobotLogs(limit: Int): RobotResult<List<RobotLogEntry>> = client.getRecentRobotLogs(limit)
    override fun close() {
        fallbackClient.close()
    }
}
