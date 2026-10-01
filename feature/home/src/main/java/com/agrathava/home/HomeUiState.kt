package com.agrathava.home

import com.agrathava.sdk.model.BatteryStatus
import com.agrathava.sdk.model.MapData
import com.agrathava.sdk.model.NavigationStatus
import com.agrathava.sdk.model.RobotPosition

data class HomeUiState(
    val batteryStatus: BatteryStatus = BatteryStatus(),
    val position: RobotPosition = RobotPosition(),
    val navigationStatus: NavigationStatus = NavigationStatus.IDLE,
    val mapData: MapData = MapData(),
    val statusMessage: String = "Ready"
)
