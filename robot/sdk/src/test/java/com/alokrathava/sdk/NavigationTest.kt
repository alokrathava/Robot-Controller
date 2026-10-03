package com.alokrathava.sdk

import com.alokrathava.sdk.internal.serialization.ProtocolCodec
import com.alokrathava.sdk.model.NavigationRoute
import com.alokrathava.sdk.model.NavigationState
import com.alokrathava.sdk.model.Pose2D
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationTest {

    @Test
    fun testNavigationRouteModel() {
        val waypoints = listOf(
            Pose2D(1.0, 0.0, 0.0),
            Pose2D(2.0, 2.0, 1.57),
            Pose2D(0.0, 2.0, 3.14)
        )
        val route = NavigationRoute(waypoints = waypoints, stopOnFailure = true)

        assertEquals(3, route.waypoints.size)
        assertTrue(route.stopOnFailure)
        assertEquals(2.0, route.waypoints[1].xMeters, 0.001)
    }

    @Test
    fun testNavigateThroughEnvelopeSerialization() {
        val poses = listOf(
            Pose2D(0.5, 0.5, 0.0),
            Pose2D(1.0, 1.0, 0.78)
        )

        val env = ProtocolCodec.createNavigateThroughEnvelope("cmd-nav-through-1", poses)
        val jsonText = ProtocolCodec.encodeEnvelope(env)
        val decoded = ProtocolCodec.decodeEnvelope(jsonText)

        assertEquals("navigate_through", decoded.type)
        assertEquals("cmd-nav-through-1", decoded.id)
        assertNotNull(decoded.payload)
    }

    @Test
    fun testNavigationStateFromTelemetry() {
        val jsonText = """
            {
                "type": "telemetry",
                "protocolVersion": 1,
                "payload": {
                    "poseValid": true,
                    "frame": "map",
                    "xMeters": 3.0,
                    "yMeters": 4.0,
                    "yawRadians": 1.2,
                    "navigationState": "NAVIGATING",
                    "distanceRemainingMeters": 2.5,
                    "navigationElapsedSeconds": 14.2,
                    "hasGoal": true
                }
            }
        """.trimIndent()

        val envelope = ProtocolCodec.decodeEnvelope(jsonText)
        val telemetry = ProtocolCodec.decodeTelemetry(envelope)

        assertNotNull(telemetry)
        val navState = NavigationState(
            state = telemetry!!.navigationState,
            targetPose = if (telemetry.hasGoal) Pose2D(telemetry.xMeters, telemetry.yMeters, telemetry.yawRadians) else null,
            distanceRemainingMeters = telemetry.distanceRemainingMeters,
            elapsedTimeSeconds = telemetry.navigationElapsedSeconds,
            errorMsg = telemetry.navigationErrorMessage
        )

        assertEquals("NAVIGATING", navState.state)
        assertNotNull(navState.targetPose)
        assertEquals(3.0, navState.targetPose!!.xMeters, 0.001)
        assertEquals(2.5f, navState.distanceRemainingMeters!!, 0.001f)
        assertEquals(14.2f, navState.elapsedTimeSeconds, 0.001f)
    }
}
