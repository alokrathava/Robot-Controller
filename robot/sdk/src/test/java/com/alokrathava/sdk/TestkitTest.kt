package com.alokrathava.sdk

import com.alokrathava.sdk.error.RobotResult
import com.alokrathava.sdk.model.ConnectionState
import com.alokrathava.sdk.model.Pose2D
import com.alokrathava.sdk.testkit.FakeRobotClient
import com.alokrathava.sdk.testkit.ProtocolSimulator
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TestkitTest {

    @Test
    fun testFakeRobotClient() = runBlocking {
        val fake = FakeRobotClient()
        val connectResult = fake.connect()

        assertTrue(connectResult is RobotResult.Success)
        assertTrue(fake.connectionState.value is ConnectionState.Connected)

        val navResult = fake.navigateTo(Pose2D(1.0, 1.0, 0.0))
        assertTrue(navResult is RobotResult.Success)

        fake.close()
        assertTrue(fake.connectionState.value is ConnectionState.Disconnected)
    }

    @Test
    fun testProtocolSimulator() {
        val sim = ProtocolSimulator()
        val telemJson = sim.generateTelemetryEnvelope(Pose2D(3.0, 4.0, 0.0), "NAVIGATING", "map_lab")

        assertTrue(telemJson.contains("NAVIGATING"))
        assertTrue(telemJson.contains("map_lab"))

        val helloJson = sim.generateHelloAckEnvelope("0.2.0", listOf("navigation"))
        assertTrue(helloJson.contains("hello_ack"))
    }
}
