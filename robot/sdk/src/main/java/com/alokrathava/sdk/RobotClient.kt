package com.alokrathava.sdk

import com.alokrathava.sdk.error.RobotResult
import com.alokrathava.sdk.model.CommandId
import com.alokrathava.sdk.model.ConnectionMetrics
import com.alokrathava.sdk.model.ConnectionState
import com.alokrathava.sdk.model.DockingState
import com.alokrathava.sdk.model.Pose2D
import com.alokrathava.sdk.model.RobotBatteryState
import com.alokrathava.sdk.model.RobotEvent
import com.alokrathava.sdk.model.RobotHealth
import com.alokrathava.sdk.model.RobotMap
import com.alokrathava.sdk.model.RobotTelemetry
import com.alokrathava.sdk.model.SafetyState
import com.alokrathava.sdk.model.TelemetryFreshness
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

    val events: SharedFlow<RobotEvent>

    suspend fun connect(): RobotResult<Unit>

    suspend fun disconnect()

    suspend fun navigateTo(pose: Pose2D): RobotResult<CommandId>

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

    override fun close()
}
