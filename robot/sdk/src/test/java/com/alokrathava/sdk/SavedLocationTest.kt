package com.alokrathava.sdk

import com.alokrathava.sdk.internal.serialization.ProtocolCodec
import com.alokrathava.sdk.model.MapPoint
import com.alokrathava.sdk.model.Pose2D
import com.alokrathava.sdk.model.SavedLocation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SavedLocationTest {

    @Test
    fun testSavedLocationModel() {
        val pose = Pose2D(xMeters = 5.5, yMeters = -2.0, yawRadians = 1.57)
        val loc = SavedLocation(
            id = "loc_charging",
            name = "Docking Bay A",
            mapId = "map_office",
            pose = pose
        )

        assertEquals("loc_charging", loc.id)
        assertEquals("Docking Bay A", loc.name)
        assertEquals("map_office", loc.mapId)
        assertEquals(5.5, loc.pose.xMeters, 0.001)
        assertEquals(-2.0, loc.pose.yMeters, 0.001)
        assertEquals(1.57, loc.pose.yawRadians, 0.001)
    }

    @Test
    fun testSaveLocationEnvelopeSerialization() {
        val pose = Pose2D(xMeters = 12.0, yMeters = 4.5, yawRadians = 0.0)
        val env = ProtocolCodec.createSaveLocationEnvelope("cmd-loc-1", "Breakroom", pose, "map_office")
        val jsonText = ProtocolCodec.encodeEnvelope(env)
        val decoded = ProtocolCodec.decodeEnvelope(jsonText)

        assertEquals("save_location", decoded.type)
        assertEquals("cmd-loc-1", decoded.id)
        assertNotNull(decoded.payload)
    }

    @Test
    fun testDecodeListSavedLocationsAck() {
        val jsonText = """
            {
                "type": "list_saved_locations_ack",
                "id": "cmd-loc-list",
                "protocolVersion": 1,
                "payload": {
                    "mapId": "map_office",
                    "locations": [
                        {
                            "id": "loc_1",
                            "name": "Front Desk",
                            "mapId": "map_office",
                            "xMeters": 2.0,
                            "yMeters": 1.0,
                            "yawRadians": 0.0
                        }
                    ]
                }
            }
        """.trimIndent()

        val envelope = ProtocolCodec.decodeEnvelope(jsonText)
        val locations = ProtocolCodec.decodeListSavedLocationsAck(envelope)

        assertNotNull(locations)
        assertEquals(1, locations!!.size)
        val loc = locations[0]
        assertEquals("loc_1", loc.id)
        assertEquals("Front Desk", loc.name)
        assertEquals(2.0, loc.pose.xMeters, 0.001)
    }

    @Test
    fun testCheckMapPointEnvelopeAndAck() {
        val env = ProtocolCodec.createCheckMapPointEnvelope("cmd-chk-1", MapPoint(10.0, 20.0))
        val jsonText = ProtocolCodec.encodeEnvelope(env)
        val decoded = ProtocolCodec.decodeEnvelope(jsonText)

        assertEquals("check_map_point", decoded.type)
        assertEquals("cmd-chk-1", decoded.id)

        val ackText = """
            {
                "type": "check_map_point_ack",
                "id": "cmd-chk-1",
                "protocolVersion": 1,
                "payload": {
                    "isValid": true,
                    "reason": null
                }
            }
        """.trimIndent()

        val ackEnv = ProtocolCodec.decodeEnvelope(ackText)
        val validity = ProtocolCodec.decodeCheckMapPointAck(ackEnv)

        assertNotNull(validity)
        assertTrue(validity!!.isValid)
        assertNull(validity.reason)
    }
}
