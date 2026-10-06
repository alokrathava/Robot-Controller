package com.alokrathava.sdk

import com.alokrathava.sdk.internal.RobotClientImpl
import com.alokrathava.sdk.internal.serialization.ProtocolCodec
import com.alokrathava.sdk.error.RobotResult
import com.alokrathava.sdk.model.MapOperationState
import com.alokrathava.sdk.model.NavigationRoute
import com.alokrathava.sdk.model.Pose2D
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import com.alokrathava.sdk.model.RobotMap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MapLifecycleTest {

    @Test
    fun testRobotMapModelExtension() {
        val map = RobotMap(
            id = "map_office",
            name = "Main Office",
            isActive = true,
            resolutionMetersPerCell = 0.05,
            widthCells = 800,
            heightCells = 600,
            createdAtEpochMs = 1700000000000L,
            updatedAtEpochMs = 1700000050000L
        )

        assertEquals("map_office", map.id)
        assertEquals("Main Office", map.name)
        assertTrue(map.isActive)
        assertEquals(0.05f, map.resolution!!, 0.001f)
        assertEquals(800, map.width)
        assertEquals(600, map.height)
        assertEquals(1700000000000L, map.createdAtEpochMs)
        assertEquals(1700000050000L, map.updatedAtEpochMs)
    }

    @Test
    fun testSaveMapEnvelopeSerialization() {
        val env = ProtocolCodec.createSaveMapEnvelope("cmd-save-1", "New Building Map")
        val jsonText = ProtocolCodec.encodeEnvelope(env)
        val decoded = ProtocolCodec.decodeEnvelope(jsonText)

        assertEquals("save_map", decoded.type)
        assertEquals("cmd-save-1", decoded.id)
        assertNotNull(decoded.payload)
    }

    @Test
    fun testRenameMapEnvelopeSerialization() {
        val env = ProtocolCodec.createRenameMapEnvelope("cmd-rename-1", "map_old", "Renamed Map")
        val jsonText = ProtocolCodec.encodeEnvelope(env)
        val decoded = ProtocolCodec.decodeEnvelope(jsonText)

        assertEquals("rename_map", decoded.type)
        assertEquals("cmd-rename-1", decoded.id)
        assertNotNull(decoded.payload)
    }

    @Test
    fun testDeleteMapEnvelopeSerialization() {
        val env = ProtocolCodec.createDeleteMapEnvelope("cmd-del-1", "map_deprecated")
        val jsonText = ProtocolCodec.encodeEnvelope(env)
        val decoded = ProtocolCodec.decodeEnvelope(jsonText)

        assertEquals("delete_map", decoded.type)
        assertEquals("cmd-del-1", decoded.id)
        assertNotNull(decoded.payload)
    }

    @Test
    fun testDecodeSaveMapAck() {
        val jsonText = """
            {
                "type": "save_map_ack",
                "id": "cmd-save-1",
                "protocolVersion": 1,
                "payload": {
                    "map": {
                        "id": "map_warehouse_1",
                        "name": "Warehouse 1",
                        "isActive": false,
                        "resolutionMetersPerCell": 0.05,
                        "widthCells": 1000,
                        "heightCells": 800,
                        "createdAtEpochMs": 1700000000000
                    }
                }
            }
        """.trimIndent()

        val envelope = ProtocolCodec.decodeEnvelope(jsonText)
        val map = ProtocolCodec.decodeSaveMapAck(envelope)

        assertNotNull(map)
        assertEquals("map_warehouse_1", map!!.id)
        assertEquals("Warehouse 1", map.name)
        assertFalse(map.isActive)
        assertEquals(1000, map.widthCells)
        assertEquals(800, map.heightCells)
    }

    @Test
    fun testMapOperationState() {
        val stateSaving: MapOperationState = MapOperationState.Saving("Warehouse")
        val stateFailed: MapOperationState = MapOperationState.Failed("map_1", "Cannot delete active map")

        assertTrue(stateSaving is MapOperationState.Saving)
        assertEquals("Warehouse", (stateSaving as MapOperationState.Saving).mapName)
        assertTrue(stateFailed is MapOperationState.Failed)
        assertEquals("map_1", (stateFailed as MapOperationState.Failed).mapId)
    }

    private fun mapState(state: String, id: String = "building_a", detail: String = "") =
        """{"type":"map_state","protocolVersion":1,"payload":{"state":"$state","mode":"LOCALIZATION","activeMapId":"$id","errorCode":"","detail":"$detail"}}"""

    private fun newClient(
        transport: FakeTransport = FakeTransport()
    ) = FakeTransport.client(RobotSdkConfig(endpoint = RobotEndpoint("127.0.0.1", 8080)), transport)

    @Test
    fun testMapStateDrivesOperationState() {
        val client = newClient()
        client.onMessage(mapState("SAVING"))
        assertEquals("building_a", client.activeMap.value?.id)
        assertEquals(MapOperationState.Saving("building_a"), client.mapOperationState.value)

        client.onMessage(mapState("MAPPING"))
        assertEquals(MapOperationState.Mapping("building_a"), client.mapOperationState.value)

        client.onMessage(mapState("ERROR", detail = "slam died"))
        assertEquals(MapOperationState.Failed("building_a", "slam died"), client.mapOperationState.value)

        client.onMessage(mapState("ACTIVE"))
        assertEquals(MapOperationState.Idle, client.mapOperationState.value)
        client.close()
    }

    @Test
    fun testNoMapClearsActiveMapAndTelemetryDoesNotRestoreIt() {
        val client = newClient()
        client.onMessage(mapState("ACTIVE"))
        client.onMessage("""{"type":"map_state","protocolVersion":1,"payload":{"state":"NO_MAP","activeMapId":""}}""")
        assertEquals(null, client.activeMap.value)
        client.onMessage("""{"type":"telemetry","protocolVersion":1,"payload":{"activeMap":"building_a"}}""")
        assertEquals(null, client.activeMap.value)
        client.close()
    }

    @Test
    fun testSwitchMapAckDoesNotOverrideRobotMapState() = runBlocking {
        lateinit var client: RobotClientImpl
        val transport = FakeTransport { type, id ->
            if (type == "switch_map") {
                client.onMessage(mapState("SWITCHING", id = "building_b"))
                """{"type":"command_ack","id":"$id","protocolVersion":1}"""
            } else null
        }
        client = newClient(transport)
        client.connect()
        assertTrue(client.switchMap("building_b") is RobotResult.Success)
        assertEquals(MapOperationState.Switching("building_b"), client.mapOperationState.value)
        client.close()
    }

    @Test
    fun testSaveMapCommandAckReturnsDerivedMap() = runBlocking {
        val transport = FakeTransport { type, id ->
            if (type == "save_map") """{"type":"command_ack","id":"$id","protocolVersion":1}""" else null
        }
        val client = newClient(transport)
        client.connect()
        val result = client.saveCurrentMap("Main Floor")
        assertEquals("main_floor", (result as RobotResult.Success).value.id)
        client.close()
    }

    @Test
    fun testMappingCommandsAndCapabilityGate() = runBlocking {
        val ack = { type: String, id: String? ->
            if (type.endsWith("_mapping")) """{"type":"command_ack","id":"$id","protocolVersion":1}""" else null
        }
        val transport = FakeTransport(autoReply = ack)
        val client = newClient(transport)
        client.connect()
        assertTrue(client.startMapping() is RobotResult.Success)
        assertTrue(client.stopMapping(discardUnsaved = true) is RobotResult.Success)
        val stop = transport.last("stop_mapping")["payload"]!!.jsonObject
        assertEquals("true", stop["discardUnsaved"]!!.jsonPrimitive.content)
        client.close()

        val noMapping = FakeTransport(capabilities = listOf("navigation"), autoReply = ack)
        val client2 = newClient(noMapping)
        client2.connect()
        val failure = client2.startMapping() as RobotResult.Failure
        assertEquals("UNSUPPORTED_CAPABILITY", failure.error.code)
        assertFalse(noMapping.types().contains("start_mapping"))
        client2.close()
    }

    @Test
    fun testNavigateRoutePassesStopOnFailure() = runBlocking {
        val transport = FakeTransport { type, id ->
            if (type == "navigate_through") """{"type":"command_ack","id":"$id","protocolVersion":1}""" else null
        }
        val client = newClient(transport)
        client.connect()
        val route = NavigationRoute(listOf(Pose2D(1.0, 2.0, 0.0)), stopOnFailure = false)
        assertTrue(client.navigateRoute(route) is RobotResult.Success)
        val payload = transport.last("navigate_through")["payload"]!!.jsonObject
        assertEquals("false", payload["stopOnFailure"]!!.jsonPrimitive.content)
        client.close()
    }

    @Test
    fun testMappingEnvelopes() {
        val startEnv = ProtocolCodec.createStartMappingEnvelope("cmd-map-1")
        val startJson = ProtocolCodec.encodeEnvelope(startEnv)
        val startDecoded = ProtocolCodec.decodeEnvelope(startJson)
        assertEquals("start_mapping", startDecoded.type)
        assertEquals("cmd-map-1", startDecoded.id)

        val stopEnv = ProtocolCodec.createStopMappingEnvelope("cmd-map-2", discardUnsaved = true)
        val stopJson = ProtocolCodec.encodeEnvelope(stopEnv)
        val stopDecoded = ProtocolCodec.decodeEnvelope(stopJson)
        assertEquals("stop_mapping", stopDecoded.type)
        assertEquals("cmd-map-2", stopDecoded.id)
        assertEquals("true", stopDecoded.payload!!["discardUnsaved"]!!.jsonPrimitive.content)
    }
}
