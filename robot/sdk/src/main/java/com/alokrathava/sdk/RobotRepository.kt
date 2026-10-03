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

    fun move(direction: DirectionCommand)
    fun goToCharge()
    fun cancelNavigation()
    fun refreshPosition()
    fun moveToPosition(x: Double, y: Double)
    fun fetchMap()
    fun saveMap()
    fun refreshBattery()
    fun connectToRobot(ip: String, port: Int, ssid: String)
    fun disconnectRobot()
    fun refreshAvailableNetworks()
    fun triggerEmergencyStop()
    fun resetEmergencyStop()
    fun updateSimulationConfig(config: RobotSimulationConfig)
    fun dock()
    fun undock()
    fun cancelDocking()
}
