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

interface RobotClient : AutoCloseable {

    val connectionState: StateFlow<ConnectionState>

    val connectionMetrics: StateFlow<ConnectionMetrics>

    val telemetryFreshness: StateFlow<TelemetryFreshness>

    val telemetry: StateFlow<RobotTelemetry?>

    val batteryState: StateFlow<RobotBatteryState?>

    val safetyState: StateFlow<SafetyState?>

    val dockingState: StateFlow<DockingState?>

    val health: StateFlow<RobotHealth?>

    val activeMap: StateFlow<RobotMap?>

    val mapOperationState: StateFlow<MapOperationState>

    val navigation: StateFlow<NavigationState>

    val missionProgress: StateFlow<MissionProgress?>

    val configuration: StateFlow<RobotConfiguration?>

    val robotInfo: StateFlow<RobotInfo?>

    val diagnostics: StateFlow<RobotDiagnostics>

    val events: SharedFlow<RobotEvent>

    suspend fun connect(): RobotResult<Unit>

    suspend fun disconnect()

    suspend fun navigateTo(pose: Pose2D): RobotResult<CommandId>

    suspend fun navigateThrough(poses: List<Pose2D>): RobotResult<CommandId>

    suspend fun navigateRoute(route: NavigationRoute): RobotResult<CommandId>

    suspend fun cancelNavigation(): RobotResult<Unit>

    suspend fun setManualVelocity(linearMps: Double, angularRadPerSec: Double): RobotResult<Unit>

    suspend fun stop(): RobotResult<Unit>

    suspend fun dock(): RobotResult<Unit>

    suspend fun undock(): RobotResult<Unit>

    suspend fun cancelDocking(): RobotResult<Unit>

    suspend fun emergencyStop(): RobotResult<Unit>

    suspend fun releaseEmergencyStop(): RobotResult<Unit>

    suspend fun listMaps(): RobotResult<List<RobotMap>>

    suspend fun switchMap(mapId: String): RobotResult<Unit>

    suspend fun saveCurrentMap(name: String): RobotResult<RobotMap>

    suspend fun renameMap(mapId: String, newName: String): RobotResult<RobotMap>

    suspend fun deleteMap(mapId: String): RobotResult<Unit>

    suspend fun startMapping(): RobotResult<Unit>

    suspend fun stopMapping(discardUnsaved: Boolean = false): RobotResult<Unit>

    suspend fun listVirtualWalls(mapId: String): RobotResult<List<VirtualWall>>

    suspend fun createVirtualWall(wall: VirtualWallDraft): RobotResult<VirtualWall>

    suspend fun updateVirtualWall(wall: VirtualWall): RobotResult<VirtualWall>

    suspend fun deleteVirtualWall(wallId: String): RobotResult<Unit>

    suspend fun setVirtualWallEnabled(wallId: String, enabled: Boolean): RobotResult<Unit>

    suspend fun clearVirtualWalls(mapId: String): RobotResult<Unit>

    suspend fun listSavedLocations(mapId: String): RobotResult<List<SavedLocation>>

    suspend fun saveLocation(name: String, pose: Pose2D, mapId: String? = null): RobotResult<SavedLocation>

    suspend fun renameLocation(id: String, name: String): RobotResult<SavedLocation>

    suspend fun deleteLocation(id: String): RobotResult<Unit>

    suspend fun navigateToLocation(id: String): RobotResult<CommandId>

    suspend fun checkMapPoint(point: MapPoint): RobotResult<MapPointValidity>

    suspend fun submitMission(mission: RobotMission): RobotResult<CommandId>

    suspend fun cancelMission(): RobotResult<Unit>

    suspend fun updateMotionLimits(limits: MotionLimits): RobotResult<Unit>

    suspend fun collectDiagnosticReport(): RobotResult<RobotDiagnosticReport>

    suspend fun getRecentRobotLogs(limit: Int = 100): RobotResult<List<RobotLogEntry>>

    override fun close()
}
