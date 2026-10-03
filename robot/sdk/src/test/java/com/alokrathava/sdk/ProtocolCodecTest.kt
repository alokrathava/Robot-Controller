package com.alokrathava.sdk

import com.alokrathava.sdk.internal.serialization.ProtocolCodec
import com.alokrathava.sdk.model.BatteryState
import com.alokrathava.sdk.model.DockingState
import com.alokrathava.sdk.model.Pose2D
import com.alokrathava.sdk.model.SafetyState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtocolCodecTest {

    @Test
    fun testHelloEnvelope() {
        val env = ProtocolCodec.createHelloEnvelope("cmd-1", "token123")
        val jsonText = ProtocolCodec.encodeEnvelope(env)
        val decoded = ProtocolCodec.decodeEnvelope(jsonText)

        assertEquals("hello", decoded.type)
        assertEquals("cmd-1", decoded.id)
        assertEquals(1, decoded.protocolVersion)
        assertNotNull(decoded.payload)
    }

    @Test
    fun testNavigateToEnvelope() {
        val pose = Pose2D(xMeters = 1.5, yMeters = 2.0, yawRadians = 1.57)
        val env = ProtocolCodec.createNavigateToEnvelope("cmd-2", pose)
        val jsonText = ProtocolCodec.encodeEnvelope(env)
        val decoded = ProtocolCodec.decodeEnvelope(jsonText)

        assertEquals("navigate_to", decoded.type)
        assertEquals("cmd-2", decoded.id)
    }

    @Test
    fun testTelemetryDecoding() {
        val jsonText = """
            {
                "type": "telemetry",
                "protocolVersion": 1,
                "payload": {
                    "poseValid": true,
                    "frame": "map",
                    "xMeters": 1.2,
                    "yMeters": 3.4,
                    "yawRadians": 0.5,
                    "linearVelocityMps": 0.4,
                    "angularVelocityRadPerSec": 0.1,
                    "isMoving": true,
                    "navigationState": "NAVIGATING",
                    "safetyState": 0,
                    "dockingState": 0,
                    "batteryState": 4,
                    "batteryPercentage": 88.5,
                    "isCharging": true
                }
            }
        """.trimIndent()

        val envelope = ProtocolCodec.decodeEnvelope(jsonText)
        val telemetry = ProtocolCodec.decodeTelemetry(envelope)

        assertNotNull(telemetry)
        assertTrue(telemetry!!.poseValid)
        assertEquals(1.2, telemetry.xMeters, 0.001)
        assertEquals(3.4, telemetry.yMeters, 0.001)
        assertEquals(0.5, telemetry.yawRadians, 0.001)
        assertEquals("NAVIGATING", telemetry.navigationState)
        assertEquals(SafetyState.NORMAL, telemetry.safetyState)
        assertEquals(DockingState.UNDOCKED, telemetry.dockingState)
        assertEquals(BatteryState.CHARGING, telemetry.batteryState)
        assertEquals(88.5f, telemetry.batteryPercentage, 0.1f)
        assertTrue(telemetry.isCharging)
    }
}
