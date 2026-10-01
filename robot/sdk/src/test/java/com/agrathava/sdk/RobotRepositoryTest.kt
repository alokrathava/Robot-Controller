package com.agrathava.sdk

import android.content.ContextWrapper
import com.agrathava.database.dao.RobotLogDao
import com.agrathava.database.entity.RobotLogEntity
import com.agrathava.sdk.battery.DeviceBatteryDataSource
import com.agrathava.sdk.model.BatteryStatus
import com.agrathava.sdk.model.ConnectionStatus
import com.agrathava.sdk.model.DirectionCommand
import com.agrathava.sdk.model.DockingStatus
import com.agrathava.sdk.model.NavigationStatus
import com.agrathava.sdk.model.RobotSimulationConfig
import com.agrathava.sdk.model.ThermalState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RobotRepositoryTest {

    private lateinit var fakeDao: FakeRobotLogDao
    private lateinit var fakeBatteryDataSource: FakeBatteryDataSource
    private lateinit var repository: DefaultRobotRepository

    @Before
    fun setUp() {
        fakeDao = FakeRobotLogDao()
        fakeBatteryDataSource = FakeBatteryDataSource()
        repository = DefaultRobotRepository(fakeDao, fakeBatteryDataSource)
    }

    @Test
    fun testInitialBatteryStatus() {
        val battery = repository.batteryStatus.value
        assertEquals(90, battery.levelPercent)
        assertFalse(battery.isCharging)
        assertEquals(25.0, battery.temperatureCelsius, 0.001)
        assertEquals(4000, battery.voltageMv)
        assertEquals("Good", battery.health)
        assertFalse(battery.isLowBattery)
    }

    @Test
    fun testMoveForward() {
        repository.move(DirectionCommand.FORWARD)
        assertEquals(0.5, repository.position.value.y, 0.001)
        assertEquals(NavigationStatus.NAVIGATING, repository.navigationStatus.value)
    }

    @Test
    fun testGoToCharge() {
        repository.goToCharge()
        assertEquals(NavigationStatus.CHARGING, repository.navigationStatus.value)
        assertEquals(DockingStatus.DOCKED, repository.dockingStatus.value)
        assertTrue(repository.batteryStatus.value.isCharging)
        assertTrue(repository.telemetry.value.isDocked)
    }

    @Test
    fun testDockAndUndockLifecycle() {
        repository.dock()
        assertEquals(DockingStatus.DOCKED, repository.dockingStatus.value)
        assertEquals(NavigationStatus.CHARGING, repository.navigationStatus.value)
        assertTrue(repository.telemetry.value.isDocked)

        repository.undock()
        assertEquals(DockingStatus.UNDOCKED, repository.dockingStatus.value)
        assertEquals(NavigationStatus.IDLE, repository.navigationStatus.value)
        assertFalse(repository.telemetry.value.isDocked)
    }

    @Test
    fun testThermalOverheatingSpeedThrottling() {
        fakeBatteryDataSource.batteryFlow.value = BatteryStatus(
            levelPercent = 80,
            isCharging = false,
            temperatureCelsius = 58.0
        )
        repository.refreshBattery()

        assertEquals(ThermalState.OVERHEATING, repository.telemetry.value.thermalState)

        repository.move(DirectionCommand.FORWARD)
        assertEquals(0.25, repository.telemetry.value.speedMps, 0.001)
    }

    @Test
    fun testThermalCriticalEmergencyStop() {
        fakeBatteryDataSource.batteryFlow.value = BatteryStatus(
            levelPercent = 80,
            isCharging = false,
            temperatureCelsius = 66.0
        )
        repository.refreshBattery()

        assertEquals(ThermalState.CRITICAL, repository.telemetry.value.thermalState)
        assertTrue(repository.telemetry.value.isEmergencyStopped)
        assertEquals(NavigationStatus.EMERGENCY_STOP, repository.navigationStatus.value)
    }

    @Test
    fun testCriticalLowBatteryAutoDocking() {
        fakeBatteryDataSource.batteryFlow.value = BatteryStatus(
            levelPercent = 8,
            isCharging = false,
            isLowBattery = true
        )
        repository.refreshBattery()

        assertEquals(DockingStatus.DOCKED, repository.dockingStatus.value)
        assertEquals(NavigationStatus.CHARGING, repository.navigationStatus.value)
    }

    @Test
    fun testEmergencyStopBlocksMovement() {
        repository.triggerEmergencyStop()
        assertTrue(repository.telemetry.value.isEmergencyStopped)
        assertEquals(NavigationStatus.EMERGENCY_STOP, repository.navigationStatus.value)

        val initialY = repository.position.value.y
        repository.move(DirectionCommand.FORWARD)
        assertEquals(initialY, repository.position.value.y, 0.001)
    }

    @Test
    fun testResetEmergencyStopRestoresState() {
        repository.triggerEmergencyStop()
        repository.resetEmergencyStop()
        assertFalse(repository.telemetry.value.isEmergencyStopped)
        assertEquals(NavigationStatus.IDLE, repository.navigationStatus.value)

        repository.move(DirectionCommand.FORWARD)
        assertEquals(0.5, repository.position.value.y, 0.001)
    }

    @Test
    fun testSimulationConfigUpdatesSpeed() {
        repository.updateSimulationConfig(RobotSimulationConfig(movementSpeedMps = 1.2))
        assertEquals(1.2, repository.simulationConfig.value.movementSpeedMps, 0.001)

        repository.move(DirectionCommand.RIGHT)
        assertEquals(1.2, repository.telemetry.value.speedMps, 0.001)
    }

    @Test
    fun testConnectAndDisconnectRobot() {
        repository.connectToRobot("192.168.1.50", 9090, "ROBOT_HOTSPOT_5G")
        assertEquals(ConnectionStatus.CONNECTED, repository.connectionStatus.value)
        assertEquals("192.168.1.50", repository.connectionConfig.value.ipAddress)

        val connectedNetwork = repository.availableNetworks.value.find { it.ssid == "ROBOT_HOTSPOT_5G" }
        assertTrue(connectedNetwork?.isConnected == true)

        repository.disconnectRobot()
        assertEquals(ConnectionStatus.DISCONNECTED, repository.connectionStatus.value)
        assertFalse(repository.availableNetworks.value.any { it.isConnected })
    }

    private class FakeRobotLogDao : RobotLogDao {
        val logs = mutableListOf<RobotLogEntity>()
        override suspend fun insertLog(log: RobotLogEntity) {
            logs.add(log)
        }

        override fun getAllLogs(): Flow<List<RobotLogEntity>> {
            return flowOf(logs)
        }

        override suspend fun clearLogs() {
            logs.clear()
        }
    }

    private class FakeBatteryDataSource : DeviceBatteryDataSource(
        context = DummyContext()
    ) {
        val batteryFlow = MutableStateFlow(
            BatteryStatus(
                levelPercent = 90,
                isCharging = false,
                plugType = "Battery",
                temperatureCelsius = 25.0,
                voltageMv = 4000,
                health = "Good",
                isLowBattery = false
            )
        )

        override fun observeBatteryStatus(): Flow<BatteryStatus> = batteryFlow

        override fun getCurrentBatteryStatus(): BatteryStatus = batteryFlow.value
    }

    private class DummyContext : ContextWrapper(null)
}
