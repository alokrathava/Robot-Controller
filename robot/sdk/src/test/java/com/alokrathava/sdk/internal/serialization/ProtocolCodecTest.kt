package com.alokrathava.sdk.internal.serialization

import com.alokrathava.sdk.internal.protocol.ProtocolEnvelope
import com.alokrathava.sdk.model.RobotOccupancyGrid
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying serialization and deserialization of occupancy grid map snapshot
 * and partial update messages in [ProtocolCodec].
 */
class ProtocolCodecTest {

    @Test
    fun decodeMapSnapshot_parsesGridDataCorrectly() {
        val payload = buildJsonObject {
            put("mapId", "map_office")
            put("widthCells", 10)
            put("heightCells", 10)
            put("resolutionMetersPerCell", 0.05)
            put("originX", -2.5)
            put("originY", -2.5)
            put("originYaw", 0.0)
            put("dataBase64", "AQID")
        }

        val grid = ProtocolCodec.decodeMapSnapshot(payload)

        assertNotNull(grid)
        assertEquals("map_office", grid!!.mapId)
        assertEquals(10, grid.widthCells)
        assertEquals(10, grid.heightCells)
        assertEquals(0.05, grid.resolutionMetersPerCell, 0.001)
        assertEquals(-2.5, grid.originX, 0.001)
        assertTrue(grid.isValid)
        assertArrayEquals(byteArrayOf(1, 2, 3), grid.data)
    }

    @Test
    fun decodeMapUpdate_appliesPartialPatchToGrid() {
        val initialData = ByteArray(16) { 0 }
        val baseGrid = RobotOccupancyGrid(
            mapId = "map_lab",
            widthCells = 4,
            heightCells = 4,
            resolutionMetersPerCell = 0.1,
            originX = 0.0,
            originY = 0.0,
            originYaw = 0.0,
            data = initialData
        )

        val updatePayload = buildJsonObject {
            put("mapId", "map_lab")
            put("startX", 1)
            put("startY", 1)
            put("width", 2)
            put("height", 2)
            put("dataBase64", "f39/f39/")
        }

        val updatedGrid = ProtocolCodec.decodeMapUpdate(updatePayload, baseGrid)

        assertNotNull(updatedGrid)
        assertEquals("map_lab", updatedGrid!!.mapId)
        assertEquals(16, updatedGrid.data.size)
    }

    @Test
    fun encodeEnvelope_normalizesNullPayloadToEmptyJsonObject() {
        val simpleEnvelope = ProtocolCodec.createSimpleCommandEnvelope("list_maps", "cmd-1001")
        val jsonString = ProtocolCodec.encodeEnvelope(simpleEnvelope)

        assertTrue(jsonString.contains("\"payload\":{}"))
        assertTrue(jsonString.contains("\"type\":\"list_maps\""))
        assertTrue(jsonString.contains("\"id\":\"cmd-1001\""))
        assertTrue(jsonString.contains("\"protocolVersion\":1"))
        assertTrue(!jsonString.contains("\"error\""))
    }

    @Test
    fun encodeStartMappingEnvelope_serializesRecoveryOptionsPayload() {
        val envelope = ProtocolCodec.createStartMappingEnvelope("cmd-1002", resumeUnsaved = true, discardUnsaved = false)
        val jsonString = ProtocolCodec.encodeEnvelope(envelope)

        assertTrue(jsonString.contains("\"type\":\"start_mapping\""))
        assertTrue(jsonString.contains("\"resumeUnsaved\":true"))
        assertTrue(jsonString.contains("\"discardUnsaved\":false"))
    }
}
