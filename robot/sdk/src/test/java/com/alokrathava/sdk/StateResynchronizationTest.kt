package com.alokrathava.sdk

import com.alokrathava.sdk.internal.serialization.ProtocolCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class StateResynchronizationTest {

    @Test
    fun testStateSnapshotRequestEnvelope() {
        val env = ProtocolCodec.createRequestStateSnapshotEnvelope("cmd-resync-1")
        val jsonText = ProtocolCodec.encodeEnvelope(env)
        val decoded = ProtocolCodec.decodeEnvelope(jsonText)

        assertEquals("request_state_snapshot", decoded.type)
        assertEquals("cmd-resync-1", decoded.id)
    }

    @Test
    fun testDecodeStateSnapshot() {
        val jsonText = """
            {
                "type": "robot_state_snapshot",
                "id": "cmd-resync-1",
                "protocolVersion": 1,
                "payload": {
                    "telemetry": {
                        "poseValid": true,
                        "xMeters": 4.5,
                        "yMeters": -1.2,
                        "activeMap": "office_ground_floor"
                    },
                    "battery": {
                        "percentage": 92.0,
                        "isCharging": false
                    }
                }
            }
        """.trimIndent()

        val envelope = ProtocolCodec.decodeEnvelope(jsonText)
        val snapshot = ProtocolCodec.decodeRobotStateSnapshot(envelope)

        assertNotNull(snapshot)
        assertNotNull(snapshot!!.telemetry)
        assertEquals(4.5, snapshot.telemetry!!.xMeters, 0.001)
        assertEquals("office_ground_floor", snapshot.telemetry!!.activeMap)
        assertNotNull(snapshot.battery)
        assertEquals(92.0f, snapshot.battery!!.percentage, 0.1f)
    }
}
