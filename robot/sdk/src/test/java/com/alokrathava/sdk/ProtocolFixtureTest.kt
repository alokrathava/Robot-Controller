package com.alokrathava.sdk

import com.alokrathava.sdk.internal.protocol.ROBOT_PROTOCOL_VERSION
import com.alokrathava.sdk.internal.serialization.ProtocolCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ProtocolFixtureTest {

    private val fixturesDir = File("../../protocol-fixtures")

    @Test
    fun testHelloAckFixture() {
        val file = File(fixturesDir, "hello_ack.json")
        assertTrue("Fixture file must exist: ${file.absolutePath}", file.exists())

        val jsonText = file.readText()
        val envelope = ProtocolCodec.decodeEnvelope(jsonText)

        assertEquals("hello_ack", envelope.type)
        assertEquals(ROBOT_PROTOCOL_VERSION, envelope.protocolVersion)

        val helloAck = ProtocolCodec.decodeHelloAck(envelope)
        assertNotNull(helloAck)
        assertEquals("0.2.0", helloAck?.gatewayVersion)
        assertEquals("sess-1234", helloAck?.sessionId)
        assertTrue(helloAck?.capabilities?.contains("navigation") == true)
        assertTrue(helloAck?.capabilities?.contains("map_switching") == true)
    }

    @Test
    fun testTelemetryFixture() {
        val file = File(fixturesDir, "telemetry.json")
        assertTrue("Fixture file must exist: ${file.absolutePath}", file.exists())

        val jsonText = file.readText()
        val envelope = ProtocolCodec.decodeEnvelope(jsonText)

        assertEquals("telemetry", envelope.type)
        val telemetry = ProtocolCodec.decodeTelemetry(envelope)
        assertNotNull(telemetry)
        assertEquals("main_floor", telemetry?.activeMap)
        assertEquals(1.25, telemetry?.xMeters ?: 0.0, 0.001)
        assertEquals(-3.40, telemetry?.yMeters ?: 0.0, 0.001)
        assertEquals(true, telemetry?.poseValid)
    }

    @Test
    fun testCommandErrorFixture() {
        val file = File(fixturesDir, "command_error.json")
        assertTrue("Fixture file must exist: ${file.absolutePath}", file.exists())

        val jsonText = file.readText()
        val envelope = ProtocolCodec.decodeEnvelope(jsonText)

        assertEquals("command_error", envelope.type)
        assertNotNull(envelope.error)
        assertEquals("CONTROL_SESSION_IN_USE", envelope.error?.code)
        assertEquals(true, envelope.error?.recoverable)
    }

    @Test
    fun testSaveMapFixture() {
        val file = File(fixturesDir, "save_map.json")
        assertTrue("Fixture file must exist: ${file.absolutePath}", file.exists())

        val jsonText = file.readText()
        val envelope = ProtocolCodec.decodeEnvelope(jsonText)

        assertEquals("save_map", envelope.type)
        assertEquals("cmd-save-1", envelope.id)
    }

    @Test
    fun testCreateVirtualWallFixture() {
        val file = File(fixturesDir, "create_virtual_wall.json")
        assertTrue("Fixture file must exist: ${file.absolutePath}", file.exists())

        val jsonText = file.readText()
        val envelope = ProtocolCodec.decodeEnvelope(jsonText)

        assertEquals("create_virtual_wall", envelope.type)
        assertEquals("cmd-vw-1", envelope.id)
    }

    @Test
    fun testSaveLocationFixture() {
        val file = File(fixturesDir, "save_location.json")
        assertTrue("Fixture file must exist: ${file.absolutePath}", file.exists())

        val jsonText = file.readText()
        val envelope = ProtocolCodec.decodeEnvelope(jsonText)

        assertEquals("save_location", envelope.type)
        assertEquals("cmd-loc-1", envelope.id)
    }

    @Test
    fun testSubmitMissionFixture() {
        val file = File(fixturesDir, "submit_mission.json")
        assertTrue("Fixture file must exist: ${file.absolutePath}", file.exists())

        val jsonText = file.readText()
        val envelope = ProtocolCodec.decodeEnvelope(jsonText)

        assertEquals("submit_mission", envelope.type)
        assertEquals("cmd-mis-1", envelope.id)
    }
}
