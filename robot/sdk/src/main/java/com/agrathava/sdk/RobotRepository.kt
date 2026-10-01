package com.agrathava.sdk

import com.agrathava.database.dao.RobotLogDao
import com.agrathava.database.entity.RobotLogEntity
import com.agrathava.sdk.model.BatteryStatus
import com.agrathava.sdk.model.DirectionCommand
import com.agrathava.sdk.model.MapData
import com.agrathava.sdk.model.NavigationStatus
import com.agrathava.sdk.model.RobotPosition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

interface RobotRepository {
    val batteryStatus: StateFlow<BatteryStatus>
    val position: StateFlow<RobotPosition>
    val navigationStatus: StateFlow<NavigationStatus>
    val mapData: StateFlow<MapData>

    fun move(direction: DirectionCommand)
    fun goToCharge()
    fun cancelNavigation()
    fun refreshPosition()
    fun moveToPosition(x: Double, y: Double)
    fun fetchMap()
    fun saveMap()
    fun refreshBattery()
}

class DefaultRobotRepository @Inject constructor(
    private val robotLogDao: RobotLogDao
) : RobotRepository {

    private val scope = CoroutineScope(Dispatchers.IO)

    private val _batteryStatus = MutableStateFlow(BatteryStatus(levelPercent = 85, isCharging = false))
    override val batteryStatus: StateFlow<BatteryStatus> = _batteryStatus.asStateFlow()

    private val _position = MutableStateFlow(RobotPosition(x = 0.0, y = 0.0))
    override val position: StateFlow<RobotPosition> = _position.asStateFlow()

    private val _navigationStatus = MutableStateFlow(NavigationStatus.IDLE)
    override val navigationStatus: StateFlow<NavigationStatus> = _navigationStatus.asStateFlow()

    private val _mapData = MutableStateFlow(MapData(name = "Main Floor Map", isAvailable = true))
    override val mapData: StateFlow<MapData> = _mapData.asStateFlow()

    override fun move(direction: DirectionCommand) {
        val step = 0.5
        val current = _position.value
        val newPos = when (direction) {
            DirectionCommand.FORWARD -> current.copy(y = current.y + step)
            DirectionCommand.BACKWARD -> current.copy(y = current.y - step)
            DirectionCommand.LEFT -> current.copy(x = current.x - step)
            DirectionCommand.RIGHT -> current.copy(x = current.x + step)
        }
        _position.value = newPos
        _navigationStatus.value = NavigationStatus.NAVIGATING
        logAction("MOVE", "Moved $direction to (${newPos.x}, ${newPos.y})")
    }

    override fun goToCharge() {
        _navigationStatus.value = NavigationStatus.CHARGING
        _batteryStatus.update { it.copy(isCharging = true) }
        logAction("NAVIGATION", "Robot sent to charging station")
    }

    override fun cancelNavigation() {
        _navigationStatus.value = NavigationStatus.CANCELLED
        logAction("NAVIGATION", "Navigation cancelled")
    }

    override fun refreshPosition() {
        logAction("STATUS", "Position refreshed")
    }

    override fun moveToPosition(x: Double, y: Double) {
        _position.value = RobotPosition(x, y)
        _navigationStatus.value = NavigationStatus.NAVIGATING
        logAction("MOVE", "Moving to position ($x, $y)")
    }

    override fun fetchMap() {
        _navigationStatus.value = NavigationStatus.FETCHING_MAP
        _mapData.value = MapData(name = "Main Floor Map v2", isAvailable = true)
        logAction("MAP", "Fetched map data")
    }

    override fun saveMap() {
        _navigationStatus.value = NavigationStatus.SAVING_MAP
        logAction("MAP", "Saved map data")
    }

    override fun refreshBattery() {
        _batteryStatus.update { current ->
            val nextLevel = if (current.levelPercent > 10) current.levelPercent - 1 else 100
            current.copy(levelPercent = nextLevel)
        }
        logAction("BATTERY", "Refreshed battery status")
    }

    private fun logAction(type: String, message: String) {
        scope.launch {
            robotLogDao.insertLog(RobotLogEntity(logType = type, message = message))
        }
    }
}
