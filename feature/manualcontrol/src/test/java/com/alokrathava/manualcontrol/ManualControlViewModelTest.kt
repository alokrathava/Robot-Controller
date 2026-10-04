package com.alokrathava.manualcontrol

import com.alokrathava.sdk.RobotRepository
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ManualControlViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeRepository: FakeRobotRepository
    private lateinit var viewModel: ManualControlViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeRobotRepository()
        viewModel = ManualControlViewModel(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialUiStateConnected() = runTest {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        fakeRepository.setConnectionStatus(ConnectionStatus.CONNECTED)
        val state = viewModel.uiState.value
        assertEquals(ConnectionStatus.CONNECTED, state.connectionStatus)
        assertFalse(state.isConnectionLost)
        assertEquals("192.168.1.100:8080", state.connectionAddress)
    }

    @Test
    fun testDisconnectedStateAndReconnect() = runTest {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        fakeRepository.disconnectRobot()
        var state = viewModel.uiState.value
        assertEquals(ConnectionStatus.DISCONNECTED, state.connectionStatus)
        assertTrue(state.isConnectionLost)

        viewModel.reconnect()
        state = viewModel.uiState.value
        assertEquals(ConnectionStatus.CONNECTED, state.connectionStatus)
        assertFalse(state.isConnectionLost)
        assertEquals("192.168.1.100:8080", state.connectionAddress)
    }

    @Test
    fun testEmergencyBrakeTriggerAndReset() = runTest {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        viewModel.triggerEmergencyBrake()
        assertTrue(viewModel.uiState.value.isEmergencyStopped)

        viewModel.resetEmergencyBrake()
        assertFalse(viewModel.uiState.value.isEmergencyStopped)
    }

    @Test
    fun testUpdateSpeed() = runTest {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        viewModel.updateSpeed(80f)
        assertEquals(80f, viewModel.uiState.value.speedPercent)
        assertEquals(ManualControlSpeedPreset.CUSTOM, viewModel.uiState.value.speedPreset)
    }

    private class FakeRobotRepository : RobotRepository {
        private val _batteryStatus = MutableStateFlow(BatteryStatus())
        override val batteryStatus: StateFlow<BatteryStatus> = _batteryStatus.asStateFlow()

        private val _position = MutableStateFlow(RobotPosition())
        override val position: StateFlow<RobotPosition> = _position.asStateFlow()

        private val _navigationStatus = MutableStateFlow(NavigationStatus.IDLE)
        override val navigationStatus: StateFlow<NavigationStatus> = _navigationStatus.asStateFlow()

        private val _mapData = MutableStateFlow(MapData())
        override val mapData: StateFlow<MapData> = _mapData.asStateFlow()

        private val _connectionStatus = MutableStateFlow(ConnectionStatus.CONNECTED)
        override val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

        private val _connectionConfig = MutableStateFlow(ConnectionConfig(ipAddress = "192.168.1.100", port = 8080))
        override val connectionConfig: StateFlow<ConnectionConfig> = _connectionConfig.asStateFlow()

        private val _availableNetworks = MutableStateFlow<List<WifiNetwork>>(emptyList())
        override val availableNetworks: StateFlow<List<WifiNetwork>> = _availableNetworks.asStateFlow()

        private val _telemetry = MutableStateFlow(RobotTelemetry())
        override val telemetry: StateFlow<RobotTelemetry> = _telemetry.asStateFlow()

        private val _simulationConfig = MutableStateFlow(RobotSimulationConfig())
        override val simulationConfig: StateFlow<RobotSimulationConfig> = _simulationConfig.asStateFlow()

        private val _dockingStatus = MutableStateFlow(DockingStatus.UNDOCKED)
        override val dockingStatus: StateFlow<DockingStatus> = _dockingStatus.asStateFlow()

        private val _dockStation = MutableStateFlow(DockStation())
        override val dockStation: StateFlow<DockStation> = _dockStation.asStateFlow()

        fun setConnectionStatus(status: ConnectionStatus) {
            _connectionStatus.value = status
        }

        override fun move(direction: DirectionCommand) {}
        override fun goToCharge() {}
        override fun cancelNavigation() {}
        override fun refreshPosition() {}
        override fun moveToPosition(x: Double, y: Double) {}
        override fun fetchMap() {}
        override fun saveMap() {}
        override fun refreshBattery() {}

        override fun connectToRobot(ip: String, port: Int, ssid: String) {
            _connectionConfig.value = ConnectionConfig(ipAddress = ip, port = port, selectedSsid = ssid)
            _connectionStatus.value = ConnectionStatus.CONNECTED
        }

        override fun disconnectRobot() {
            _connectionStatus.value = ConnectionStatus.DISCONNECTED
        }

        override fun refreshAvailableNetworks() {}
        override fun triggerEmergencyStop() {
            _telemetry.value = _telemetry.value.copy(isEmergencyStopped = true)
        }

        override fun resetEmergencyStop() {
            _telemetry.value = _telemetry.value.copy(isEmergencyStopped = false)
        }

        override fun updateSimulationConfig(config: RobotSimulationConfig) {
            _simulationConfig.value = config
        }

        override fun dock() {}
        override fun undock() {}
        override fun cancelDocking() {}
    }
}

