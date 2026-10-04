package com.alokrathava.sdk

import com.alokrathava.sdk.model.BatteryStatus
import com.alokrathava.sdk.model.ConnectionConfig
import com.alokrathava.sdk.model.ConnectionStatus
import com.alokrathava.sdk.model.DirectionCommand
import com.alokrathava.sdk.model.DockStation
import com.alokrathava.sdk.model.DockingStatus
import com.alokrathava.sdk.model.MapData
import com.alokrathava.sdk.model.NavigationStatus
import com.alokrathava.sdk.model.RobotPosition
import com.alokrathava.sdk.model.RobotSimulationConfig
import com.alokrathava.sdk.model.RobotTelemetry
import com.alokrathava.sdk.model.WifiNetwork
import kotlinx.coroutines.flow.StateFlow

@Deprecated("Use RobotClient instead for direct SDK interactions")
interface RobotRepository {
    val batteryStatus: StateFlow<BatteryStatus>
    val position: StateFlow<RobotPosition>
    val navigationStatus: StateFlow<NavigationStatus>
    val mapData: StateFlow<MapData>
    val connectionStatus: StateFlow<ConnectionStatus>
    val connectionConfig: StateFlow<ConnectionConfig>
    val availableNetworks: StateFlow<List<WifiNetwork>>
    val telemetry: StateFlow<RobotTelemetry>
    val simulationConfig: StateFlow<RobotSimulationConfig>
    val dockingStatus: StateFlow<DockingStatus>
    val dockStation: StateFlow<DockStation>
    val connectionMetrics: StateFlow<com.alokrathava.sdk.model.ConnectionMetrics>
        get() = kotlinx.coroutines.flow.MutableStateFlow(com.alokrathava.sdk.model.ConnectionMetrics())

    fun move(direction: DirectionCommand)
    fun setManualVelocity(linearMps: Double, angularRadPerSec: Double) {}
    fun goToCharge()
    fun cancelNavigation()
    fun refreshPosition()
    fun moveToPosition(x: Double, y: Double)
    fun fetchMap()
    fun saveMap()
    fun refreshBattery()
    fun connectToRobot(ip: String, port: Int, ssid: String) = connectToRobot(ip, port, "", ssid)
    fun connectToRobot(ip: String, port: Int, token: String, ssid: String)
    fun disconnectRobot()
    fun refreshAvailableNetworks()
    fun triggerEmergencyStop()
    fun resetEmergencyStop()
    fun updateSimulationConfig(config: RobotSimulationConfig)
    fun dock()
    fun undock()
    fun cancelDocking()

    suspend fun listMaps(): com.alokrathava.sdk.error.RobotResult<List<com.alokrathava.sdk.model.RobotMap>> =
        com.alokrathava.sdk.error.RobotResult.Success(emptyList())

    suspend fun switchMap(mapId: String): com.alokrathava.sdk.error.RobotResult<Unit> =
        com.alokrathava.sdk.error.RobotResult.Success(Unit)

    suspend fun saveCurrentMap(name: String): com.alokrathava.sdk.error.RobotResult<com.alokrathava.sdk.model.RobotMap> =
        com.alokrathava.sdk.error.RobotResult.Success(
            com.alokrathava.sdk.model.RobotMap(
                id = "map_1", name = name, isActive = true
            )
        )

    suspend fun deleteMap(mapId: String): com.alokrathava.sdk.error.RobotResult<Unit> =
        com.alokrathava.sdk.error.RobotResult.Success(Unit)

    suspend fun listSavedLocations(mapId: String = ""): com.alokrathava.sdk.error.RobotResult<List<com.alokrathava.sdk.model.SavedLocation>> =
        com.alokrathava.sdk.error.RobotResult.Success(emptyList())

    suspend fun saveLocation(name: String, pose: com.alokrathava.sdk.model.Pose2D, mapId: String? = null): com.alokrathava.sdk.error.RobotResult<com.alokrathava.sdk.model.SavedLocation> =
        com.alokrathava.sdk.error.RobotResult.Success(com.alokrathava.sdk.model.SavedLocation("loc_1", name, mapId ?: "map_1", pose))

    suspend fun deleteLocation(id: String): com.alokrathava.sdk.error.RobotResult<Unit> =
        com.alokrathava.sdk.error.RobotResult.Success(Unit)

    suspend fun navigateToLocation(id: String): com.alokrathava.sdk.error.RobotResult<com.alokrathava.sdk.model.CommandId> =
        com.alokrathava.sdk.error.RobotResult.Success(com.alokrathava.sdk.model.CommandId("cmd_1"))

    suspend fun updateMotionLimits(limits: com.alokrathava.sdk.model.MotionLimits): com.alokrathava.sdk.error.RobotResult<Unit> =
        com.alokrathava.sdk.error.RobotResult.Success(Unit)

    fun getActiveRobotClient(): RobotClient? = null
}
