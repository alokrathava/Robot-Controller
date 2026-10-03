package com.alokrathava.sdk

import com.alokrathava.sdk.internal.serialization.ProtocolCodec
import com.alokrathava.sdk.model.MapOperationState
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
}
