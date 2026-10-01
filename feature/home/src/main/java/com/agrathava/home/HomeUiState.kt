package com.agrathava.home

import com.agrathava.sdk.model.BatteryStatus
import com.agrathava.sdk.model.ConnectionStatus
import com.agrathava.sdk.model.MapData
import com.agrathava.sdk.model.NavigationStatus
import com.agrathava.sdk.model.RobotPosition
import com.agrathava.sdk.model.WifiNetwork

data class HomeUiState(
    val batteryStatus: BatteryStatus = BatteryStatus(),
    val position: RobotPosition = RobotPosition(),
    val navigationStatus: NavigationStatus = NavigationStatus.IDLE,
    val mapData: MapData = MapData(),
    val statusMessage: String = "Ready",
    val connectionStep: RobotConnectionStep = RobotConnectionStep.NETWORK_SELECTION,
    val availableNetworks: List<WifiNetwork> = emptyList(),
    val selectedNetwork: WifiNetwork? = null,
    val ipAddress: String = "192.168.1.100",
    val port: String = "8080",
    val ipError: String? = null,
    val portError: String? = null,
    val connectionStatus: ConnectionStatus = ConnectionStatus.DISCONNECTED
)
