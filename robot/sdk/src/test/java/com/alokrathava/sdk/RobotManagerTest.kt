package com.alokrathava.sdk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RobotManagerTest {

    @Test
    fun testRobotManagerMultiClientIsolation() {
        val manager = RobotManager()

        val config1 = RobotSdkConfig(endpoint = RobotEndpoint("192.168.1.10", 8080))
        val config2 = RobotSdkConfig(endpoint = RobotEndpoint("192.168.1.20", 8080))

        val client1 = manager.getOrCreateClient("robot-a", config1)
        val client2 = manager.getOrCreateClient("robot-b", config2)

        assertNotNull(client1)
        assertNotNull(client2)
        assertEquals(2, manager.activeRobotIds.size)
        assertTrue(manager.activeRobotIds.contains("robot-a"))
        assertTrue(manager.activeRobotIds.contains("robot-b"))

        assertEquals(client1, manager.getClient("robot-a"))
        assertEquals(client2, manager.getClient("robot-b"))

        manager.removeClient("robot-a")
        assertNull(manager.getClient("robot-a"))
        assertEquals(1, manager.activeRobotIds.size)

        manager.close()
        assertEquals(0, manager.activeRobotIds.size)
    }
}
