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
import com.alokrathava.geospatial.model.GeoTelemetry
import com.alokrathava.geospatial.model.GeoWaypoint
import com.alokrathava.geospatial.model.VehicleCategory
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface RobotClient : AutoCloseable {

    /** Emits real-time geographic telemetry and localization status. */
    val geoTelemetry: StateFlow<GeoTelemetry?>
        get() = kotlinx.coroutines.flow.MutableStateFlow(null)

    /** Emits the active vehicle category (UGV, UAV, UUV, Space Vehicle, etc.). */
    val vehicleCategory: StateFlow<VehicleCategory?>
        get() = kotlinx.coroutines.flow.MutableStateFlow(VehicleCategory.UGV)

    val connectionState: StateFlow<ConnectionState>

    val connectionMetrics: StateFlow<ConnectionMetrics>

    val telemetryFreshness: StateFlow<TelemetryFreshness>

    val telemetry: StateFlow<RobotTelemetry?>

    val batteryState: StateFlow<RobotBatteryState?>

    val safetyState: StateFlow<SafetyState?>

    /** Detailed docking result; existing client implementations retain a compatible default. */
    val dockingDetails: StateFlow<com.alokrathava.sdk.model.DockingStateDetails?>
        get() = kotlinx.coroutines.flow.MutableStateFlow(null)

    val dockingState: StateFlow<DockingState?>

    val health: StateFlow<RobotHealth?>

    val activeMap: StateFlow<RobotMap?>

    val occupancyGrid: StateFlow<com.alokrathava.sdk.model.RobotOccupancyGrid?>

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

    /**
     * Initiates SLAM mapping session on the robot.
     * Optionally resumes an unsaved previous session or discards it to start fresh.
     */
    suspend fun startMapping(
        resumeUnsaved: Boolean = false,
        discardUnsaved: Boolean = false
    ): RobotResult<Unit>

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

    /** Navigates the robot to a specified geospatial waypoint. */
    suspend fun navigateToGeo(waypoint: GeoWaypoint): RobotResult<CommandId> =
        RobotResult.Failure(com.alokrathava.sdk.error.RobotError("UNSUPPORTED", "sdk", com.alokrathava.sdk.error.ErrorSeverity.ERROR, "Geospatial navigation not supported", false))

    /** Navigates the robot through a sequence of geospatial waypoints. */
    suspend fun navigateThroughGeo(waypoints: List<GeoWaypoint>): RobotResult<CommandId> =
        RobotResult.Failure(com.alokrathava.sdk.error.RobotError("UNSUPPORTED", "sdk", com.alokrathava.sdk.error.ErrorSeverity.ERROR, "Geospatial navigation not supported", false))

    /** Cancels active geospatial navigation. */
    suspend fun cancelGeoNavigation(): RobotResult<Unit> =
        RobotResult.Success(Unit)

    override fun close()
}
