package com.alokrathava.sdk

import com.alokrathava.sdk.discovery.RobotDiscoveryManagerImpl
import com.alokrathava.sdk.security.InMemoryCredentialStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.net.InetAddress

class DiscoveryTest {

    @Test
    fun testCredentialStore() {
        val store = InMemoryCredentialStore()
        store.saveToken("robot-1", "secret-token-123")

        assertEquals("secret-token-123", store.getToken("robot-1"))
        assertNull(store.getToken("robot-2"))

        store.clearToken("robot-1")
        assertNull(store.getToken("robot-1"))
    }

    @Test
    fun testParseBeacon() {
        val manager = RobotDiscoveryManagerImpl()
        val beaconJson = """
            {
                "type": "robot_discovery_beacon",
                "robotId": "gracebot-01",
                "name": "GraceBot Floor 1",
                "host": "192.168.1.120",
                "port": 8080,
                "protocolVersion": 1
            }
        """.trimIndent()

        val mockAddr = InetAddress.getByName("192.168.1.120")
        val robot = manager.parseBeacon(beaconJson, mockAddr)

        assertNotNull(robot)
        assertEquals("gracebot-01", robot!!.id)
        assertEquals("GraceBot Floor 1", robot.name)
        assertEquals("192.168.1.120", robot.host)
        assertEquals(8080, robot.port)
        assertEquals(1, robot.protocolVersion)
    }
}
