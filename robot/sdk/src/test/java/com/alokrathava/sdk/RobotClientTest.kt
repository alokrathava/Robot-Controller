package com.alokrathava.sdk

import com.alokrathava.sdk.model.ConnectionState
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class RobotClientTest {

    @Test
    fun testRobotSdkCreate() {
        val config = RobotSdkConfig(
            endpoint = RobotEndpoint("127.0.0.1", 8080)
        )
        val client = RobotSdk.create(config)
        assertNotNull(client)
        assertEquals(ConnectionState.Disconnected, client.connectionState.value)
        client.close()
    }

    @Test
    fun testClientDisconnect() = runTest {
        val config = RobotSdkConfig(
            endpoint = RobotEndpoint("127.0.0.1", 8080)
        )
        val client = RobotSdk.create(config)
        client.disconnect()
        assertEquals(ConnectionState.Disconnected, client.connectionState.value)
        client.close()
    }
}
