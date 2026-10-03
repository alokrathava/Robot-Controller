package com.alokrathava.sdk

import com.alokrathava.sdk.model.ConnectionMetrics
import com.alokrathava.sdk.model.ConnectionState
import com.alokrathava.sdk.model.ReconnectPolicy
import com.alokrathava.sdk.model.RobotCapability
import com.alokrathava.sdk.model.TelemetryFreshness
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RobotClientReconnectionTest {

    @Test
    fun testReconnectPolicyDefaults() {
        val policy = ReconnectPolicy()
        assertTrue(policy.enabled)
        assertEquals(1_000L, policy.initialDelayMs)
        assertEquals(30_000L, policy.maxDelayMs)
        assertEquals(2.0, policy.multiplier, 0.001)
    }

    @Test
    fun testConnectionStateReconnecting() {
        val state: ConnectionState = ConnectionState.Reconnecting(attempt = 2, retryInMs = 2000L)
        assertTrue(state is ConnectionState.Reconnecting)
        assertEquals(2, (state as ConnectionState.Reconnecting).attempt)
        assertEquals(2000L, state.retryInMs)
    }

    @Test
    fun testConnectedStateCapabilities() {
        val state = ConnectionState.Connected(
            gatewayVersion = "0.2.0",
            protocolVersion = 1,
            capabilities = setOf(RobotCapability.NAVIGATION, RobotCapability.MAP_SWITCHING),
            sessionId = "sess-123"
        )
        assertEquals("0.2.0", state.gatewayVersion)
        assertEquals("sess-123", state.sessionId)
        assertTrue(state.capabilities.contains(RobotCapability.MAP_SWITCHING))
    }

    @Test
    fun testTelemetryFreshness() {
        val freshness = TelemetryFreshness(isStale = true, ageMs = 4500L, receivedAtMillis = 1000000L)
        assertTrue(freshness.isStale)
        assertEquals(4500L, freshness.ageMs)
    }

    @Test
    fun testConnectionMetrics() {
        val metrics = ConnectionMetrics(latencyMs = 25L, connectedDurationMs = 12000L, lastMessageAgeMs = 100L, reconnectCount = 1)
        assertEquals(25L, metrics.latencyMs)
        assertEquals(12000L, metrics.connectedDurationMs)
        assertEquals(1, metrics.reconnectCount)
    }
}
