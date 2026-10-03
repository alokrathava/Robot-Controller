package com.alokrathava.sdk

import com.alokrathava.sdk.internal.serialization.ProtocolCodec
import com.alokrathava.sdk.model.MapPoint
import com.alokrathava.sdk.model.VirtualWall
import com.alokrathava.sdk.model.VirtualWallDraft
import com.alokrathava.sdk.model.VirtualWallType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VirtualWallTest {

    @Test
    fun testVirtualWallModel() {
        val pts = listOf(MapPoint(0.0, 0.0), MapPoint(5.0, 0.0))
        val wall = VirtualWall(
            id = "wall_1",
            mapId = "map_office",
            name = "No Entry Glass Door",
            type = VirtualWallType.LINE,
            points = pts,
            thicknessMeters = 0.2,
            enabled = true
        )

        assertEquals("wall_1", wall.id)
        assertEquals("map_office", wall.mapId)
        assertEquals("No Entry Glass Door", wall.name)
        assertEquals(VirtualWallType.LINE, wall.type)
        assertEquals(2, wall.points.size)
        assertEquals(0.2, wall.thicknessMeters!!, 0.001)
        assertTrue(wall.enabled)
    }

    @Test
    fun testCreateVirtualWallEnvelopeSerialization() {
        val draft = VirtualWallDraft(
            mapId = "map_office",
            name = "Restricted Area",
            type = VirtualWallType.POLYGON,
            points = listOf(MapPoint(1.0, 1.0), MapPoint(3.0, 1.0), MapPoint(2.0, 3.0)),
            thicknessMeters = null,
            enabled = true
        )

        val env = ProtocolCodec.createCreateVirtualWallEnvelope("cmd-vw-1", draft)
        val jsonText = ProtocolCodec.encodeEnvelope(env)
        val decoded = ProtocolCodec.decodeEnvelope(jsonText)

        assertEquals("create_virtual_wall", decoded.type)
        assertEquals("cmd-vw-1", decoded.id)
        assertNotNull(decoded.payload)
    }

    @Test
    fun testDecodeListVirtualWallsAck() {
        val jsonText = """
            {
                "type": "list_virtual_walls_ack",
                "id": "cmd-vw-list",
                "protocolVersion": 1,
                "payload": {
                    "mapId": "map_office",
                    "walls": [
                        {
                            "id": "wall_101",
                            "mapId": "map_office",
                            "name": "Stairwell Keepout",
                            "type": "LINE",
                            "points": [
                                {"xMeters": 10.0, "yMeters": 5.0},
                                {"xMeters": 10.0, "yMeters": 8.0}
                            ],
                            "thicknessMeters": 0.3,
                            "enabled": true
                        }
                    ]
                }
            }
        """.trimIndent()

        val envelope = ProtocolCodec.decodeEnvelope(jsonText)
        val walls = ProtocolCodec.decodeListVirtualWallsAck(envelope)

        assertNotNull(walls)
        assertEquals(1, walls!!.size)
        val w = walls[0]
        assertEquals("wall_101", w.id)
        assertEquals("Stairwell Keepout", w.name)
        assertEquals(VirtualWallType.LINE, w.type)
        assertEquals(2, w.points.size)
        assertEquals(10.0, w.points[0].xMeters, 0.001)
        assertEquals(5.0, w.points[0].yMeters, 0.001)
    }
}
