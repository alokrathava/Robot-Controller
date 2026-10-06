package com.alokrathava.sdk

import com.alokrathava.sdk.internal.serialization.ProtocolCodec
import com.alokrathava.sdk.error.RobotResult
import com.alokrathava.sdk.model.MapOperationState
import com.alokrathava.sdk.model.ReconnectPolicy
import com.alokrathava.sdk.model.RobotHealthStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
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
                    },
                    "mapState": {
                        "state": "ACTIVE",
                        "activeMapId": "office_ground_floor"
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
        assertNotNull(snapshot.mapState)
        assertEquals("office_ground_floor", snapshot.mapState!!.activeMapId)
    }

    @Test
    fun testReconnectResynchronizesState() = runBlocking {
        val transport = FakeTransport()
        val config = RobotSdkConfig(
            endpoint = RobotEndpoint("127.0.0.1", 8080),
            reconnectPolicy = ReconnectPolicy(initialDelayMs = 100, jitterRatio = 0.0)
        )
        val client = FakeTransport.client(config, transport)

        assertTrue(client.connect() is RobotResult.Success)
        assertEquals(listOf("hello", "request_state_snapshot"), transport.types())

        client.onClose(1006, "lost")
        withTimeout(5_000) {
            while (transport.types().count { it == "request_state_snapshot" } < 2) delay(20)
        }
        assertEquals(2, transport.connectCount)
        assertEquals(2, transport.types().count { it == "hello" })

        val snapshotId = transport.last("request_state_snapshot")["id"]!!.jsonPrimitive.content
        client.onMessage("""
            {"type":"robot_state_snapshot","id":"$snapshotId","protocolVersion":1,"payload":{
              "telemetry":{"poseValid":true,"xMeters":10.5,"yMeters":-2.5,"yawRadians":1.57,"activeMap":"map_first_floor"},
              "battery":{"percentage":88.0,"isCharging":true},
              "safety":{"state":1},
              "docking":{"state":2},
              "health":{"overall":1,"subsystems":[],"activeErrors":[]},
              "mapState":{"state":"ACTIVE","mode":"LOCALIZATION","activeMapId":"map_first_floor","mapAvailable":true}
            }}
        """.trimIndent())

        assertEquals(10.5, client.telemetry.value!!.xMeters, 0.001)
        assertEquals(88.0f, client.batteryState.value!!.percentage, 0.1f)
        assertTrue(client.batteryState.value!!.isCharging)
        assertNotNull(client.safetyState.value)
        assertNotNull(client.dockingState.value)
        assertEquals(RobotHealthStatus.DEGRADED, client.health.value!!.overall)
        assertEquals("map_first_floor", client.activeMap.value!!.id)
        assertEquals(MapOperationState.Idle, client.mapOperationState.value)
        client.close()
    }
}
