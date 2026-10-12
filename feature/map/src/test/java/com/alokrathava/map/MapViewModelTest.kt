package com.alokrathava.map

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
import com.alokrathava.sdk.model.MapOperationState
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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import com.alokrathava.sdk.error.RobotError
import com.alokrathava.sdk.error.ErrorSeverity

@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeTestRepository
    private lateinit var viewModel: MapViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeTestRepository()
        viewModel = MapViewModel(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun refreshMapsFromRobot_populatesMapListInUiState() = runTest(testDispatcher) {
        val testMaps = listOf(
            RobotMap(id = "m1", name = "Main Floor", isActive = true),
            RobotMap(id = "m2", name = "Second Floor", isActive = false)
        )
        fakeRepository.mapList = testMaps

        viewModel.refreshMapsFromRobot()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.maps.size)
        assertEquals("m1", state.selectedMapId)
        assertEquals("Main Floor", state.activeMap?.name)
    }

    @Test
    fun startMapping_updatesMappingActiveState() = runTest(testDispatcher) {
        viewModel.startMapping()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isMappingActive)
        assertTrue(state.mapOperationState is MapOperationState.Mapping)
    }

    @Test
    fun startMapping_whenFailure_setsErrorMessageAndFailedState() = runTest(testDispatcher) {
        fakeRepository.startMappingResult = RobotResult.Failure(
            RobotError("NOT_CONNECTED", "sdk", ErrorSeverity.ERROR, "Client is not connected", true)
        )

        viewModel.startMapping()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Client is not connected", state.errorMessage)
        assertTrue(state.mapOperationState is MapOperationState.Failed)
    }

    @Test
    fun dismissError_clearsErrorMessage() = runTest(testDispatcher) {
        fakeRepository.startMappingResult = RobotResult.Failure(
            RobotError("NOT_CONNECTED", "sdk", ErrorSeverity.ERROR, "Client is not connected", true)
        )

        viewModel.startMapping()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Client is not connected", viewModel.uiState.value.errorMessage)

        viewModel.dismissError()

        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun setMapActive_switchesMapWithoutSaving() = runTest(testDispatcher) {
        viewModel.setMapActive("m2")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("m2", fakeRepository.lastSwitchedMapId)
        assertEquals("m2", viewModel.uiState.value.selectedMapId)
    }
}

private class FakeTestRepository : RobotRepository {
    var mapList: List<RobotMap> = emptyList()
    var lastSwitchedMapId: String? = null
    var startMappingResult: RobotResult<Unit> = RobotResult.Success(Unit)

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
    override fun cancelNavigation() {}
    override fun refreshPosition() {}
    override fun moveToPosition(x: Double, y: Double) {}
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

    override suspend fun startMapping(): RobotResult<Unit> = startMappingResult
    override suspend fun stopMapping(discardUnsaved: Boolean): RobotResult<Unit> = RobotResult.Success(Unit)
    override suspend fun listMaps(): RobotResult<List<RobotMap>> = RobotResult.Success(mapList)
    override suspend fun switchMap(mapId: String): RobotResult<Unit> {
        lastSwitchedMapId = mapId
        return RobotResult.Success(Unit)
    }
    override suspend fun saveCurrentMap(name: String): RobotResult<RobotMap> =
        RobotResult.Success(RobotMap("new_id", name, isActive = true))
    override suspend fun renameMap(mapId: String, newName: String): RobotResult<RobotMap> =
        RobotResult.Success(RobotMap(mapId, newName, isActive = true))
    override suspend fun deleteMap(mapId: String): RobotResult<Unit> = RobotResult.Success(Unit)
    override suspend fun listSavedLocations(mapId: String): RobotResult<List<SavedLocation>> = RobotResult.Success(emptyList())
    override suspend fun saveLocation(name: String, pose: Pose2D, mapId: String?): RobotResult<SavedLocation> =
        RobotResult.Success(SavedLocation("l1", name, mapId ?: "m1", pose))
    override suspend fun deleteLocation(id: String): RobotResult<Unit> = RobotResult.Success(Unit)
    override suspend fun navigateToLocation(id: String): RobotResult<com.alokrathava.sdk.model.CommandId> =
        RobotResult.Success(com.alokrathava.sdk.model.CommandId("c1"))
}
