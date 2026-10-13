package com.alokrathava.navigation

import com.alokrathava.sdk.RobotRepository
import com.alokrathava.sdk.error.RobotResult
import com.alokrathava.sdk.model.BatteryStatus
import com.alokrathava.sdk.model.ConnectionConfig
import com.alokrathava.sdk.model.ConnectionMetrics
import com.alokrathava.sdk.model.ConnectionStatus
import com.alokrathava.sdk.model.DirectionCommand
import com.alokrathava.sdk.model.DockStation
import com.alokrathava.sdk.model.DockingStatus
import com.alokrathava.sdk.model.MapData
import com.alokrathava.sdk.model.NavigationStatus
import com.alokrathava.sdk.model.Pose2D
import com.alokrathava.sdk.model.RobotMap
import com.alokrathava.sdk.model.RobotOccupancyGrid
import com.alokrathava.sdk.model.RobotPosition
import com.alokrathava.sdk.model.RobotSimulationConfig
import com.alokrathava.sdk.model.RobotTelemetry
import com.alokrathava.sdk.model.SavedLocation
import com.alokrathava.sdk.model.WifiNetwork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NavigationViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeNavTestRepository
    private lateinit var viewModel: NavigationViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeNavTestRepository()
        viewModel = NavigationViewModel(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun onMapTapped_setsCoordinateDestinationAndTargetPose() = runTest(testDispatcher) {
        val collectJob = launch { viewModel.uiState.collect {} }

        viewModel.onMapTapped(3.5, -1.2)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        val dest = state.selectedDestination
        assertEquals(NavigationDestinationUi.Coordinates("3.50", "-1.20", "0.0"), dest)
        assertNotNull(state.targetPose)
        assertEquals(3.5, state.targetPose!!.xMeters, 0.01)
        assertEquals(-1.2, state.targetPose!!.yMeters, 0.01)

        collectJob.cancel()
    }

    @Test
    fun startNavigation_dispatchesMoveCommandToRepository() = runTest(testDispatcher) {
        val collectJob = launch { viewModel.uiState.collect {} }

        viewModel.onMapTapped(2.0, 4.0)
        viewModel.startNavigation()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2.0, fakeRepository.lastMovedX, 0.01)
        assertEquals(4.0, fakeRepository.lastMovedY, 0.01)

        collectJob.cancel()
    }

    @Test
    fun cancelNavigation_triggersCancelOnRepository() = runTest(testDispatcher) {
        val collectJob = launch { viewModel.uiState.collect {} }

        viewModel.cancelNavigation()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, fakeRepository.cancelCount)

        collectJob.cancel()
    }
}

private class FakeNavTestRepository : RobotRepository {
    var lastMovedX: Double = 0.0
    var lastMovedY: Double = 0.0
    var cancelCount: Int = 0

    override val batteryStatus = MutableStateFlow(BatteryStatus())
    override val position = MutableStateFlow(RobotPosition())
    override val navigationStatus = MutableStateFlow(NavigationStatus.IDLE)
    override val mapData = MutableStateFlow(MapData())
    override val connectionStatus = MutableStateFlow(ConnectionStatus.CONNECTED)
    override val connectionConfig = MutableStateFlow(ConnectionConfig())
    override val availableNetworks = MutableStateFlow(emptyList<WifiNetwork>())
    override val telemetry = MutableStateFlow(RobotTelemetry())
    override val simulationConfig = MutableStateFlow(RobotSimulationConfig())
    override val dockingStatus = MutableStateFlow(DockingStatus.UNDOCKED)
    override val dockStation = MutableStateFlow(DockStation())
    override val connectionMetrics = MutableStateFlow(ConnectionMetrics())
    override val occupancyGrid = MutableStateFlow<RobotOccupancyGrid?>(null)

    override fun move(direction: DirectionCommand) {}
    override fun goToCharge() {}
    override fun cancelNavigation() {
        cancelCount++
    }
    override fun refreshPosition() {}
    override fun moveToPosition(x: Double, y: Double) {
        lastMovedX = x
        lastMovedY = y
    }
    override fun fetchMap() {}
    override fun saveMap() {}
    override fun refreshBattery() {}
    override fun connectToRobot(ip: String, port: Int, token: String, ssid: String) {}
    override fun disconnectRobot() {}
    override fun refreshAvailableNetworks() {}
    override fun triggerEmergencyStop() {}
    override fun resetEmergencyStop() {}
    override fun updateSimulationConfig(config: RobotSimulationConfig) {}
    override fun dock() {}
    override fun undock() {}
    override fun cancelDocking() {}

    override suspend fun startMapping(
        resumeUnsaved: Boolean,
        discardUnsaved: Boolean
    ): RobotResult<Unit> = RobotResult.Success(Unit)
    override suspend fun stopMapping(discardUnsaved: Boolean): RobotResult<Unit> = RobotResult.Success(Unit)
    override suspend fun listMaps(): RobotResult<List<RobotMap>> = RobotResult.Success(emptyList())
    override suspend fun switchMap(mapId: String): RobotResult<Unit> = RobotResult.Success(Unit)
    override suspend fun saveCurrentMap(name: String): RobotResult<RobotMap> =
        RobotResult.Success(RobotMap("m1", name))
    override suspend fun renameMap(mapId: String, newName: String): RobotResult<RobotMap> =
        RobotResult.Success(RobotMap(mapId, newName))
    override suspend fun deleteMap(mapId: String): RobotResult<Unit> = RobotResult.Success(Unit)
    override suspend fun listSavedLocations(mapId: String): RobotResult<List<SavedLocation>> = RobotResult.Success(emptyList())
    override suspend fun saveLocation(name: String, pose: Pose2D, mapId: String?): RobotResult<SavedLocation> =
        RobotResult.Success(SavedLocation("l1", name, mapId ?: "m1", pose))
    override suspend fun deleteLocation(id: String): RobotResult<Unit> = RobotResult.Success(Unit)
    override suspend fun navigateToLocation(id: String): RobotResult<com.alokrathava.sdk.model.CommandId> =
        RobotResult.Success(com.alokrathava.sdk.model.CommandId("c1"))
}
