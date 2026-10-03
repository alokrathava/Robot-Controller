package com.alokrathava.sdk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityAndTransportTest {

    @Test
    fun testEndpointTlsConfiguration() {
        val wsEndpoint = RobotEndpoint(host = "192.168.1.50", port = 8080, useTls = false)
        val wssEndpoint = RobotEndpoint(host = "192.168.1.50", port = 8443, useTls = true)

        assertFalse(wsEndpoint.useTls)
        assertEquals("192.168.1.50", wsEndpoint.host)
        assertEquals(8080, wsEndpoint.port)

        assertTrue(wssEndpoint.useTls)
        assertEquals(8443, wssEndpoint.port)
    }

    @Test
    fun testRobotSdkConfigTlsOptions() {
        val endpoint = RobotEndpoint("robot.local", 8443, useTls = true)
        val config = RobotSdkConfig(
            endpoint = endpoint,
            commandTimeoutMs = 5000L
        )

        assertTrue(config.endpoint.useTls)
        assertEquals(5000L, config.commandTimeoutMs)
    }
}
