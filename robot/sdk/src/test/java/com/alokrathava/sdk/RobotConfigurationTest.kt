package com.alokrathava.sdk

import com.alokrathava.sdk.internal.serialization.ProtocolCodec
import com.alokrathava.sdk.model.MotionLimits
import com.alokrathava.sdk.model.RobotCapability
import com.alokrathava.sdk.model.RobotConfiguration
import com.alokrathava.sdk.model.RobotInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RobotConfigurationTest {

    @Test
    fun testMotionLimitsAndConfigurationModels() {
        val limits = MotionLimits(maxLinearVelocityMps = 1.2, maxAngularVelocityRadPerSec = 2.0)
        val config = RobotConfiguration(motionLimits = limits, telemetryFrequencyHz = 20.0)

        assertEquals(1.2, config.motionLimits.maxLinearVelocityMps, 0.001)
        assertEquals(2.0, config.motionLimits.maxAngularVelocityRadPerSec, 0.001)
        assertEquals(20.0, config.telemetryFrequencyHz, 0.001)
    }

    @Test
    fun testRobotInfoModel() {
        val caps = setOf(RobotCapability.NAVIGATION, RobotCapability.VIRTUAL_WALLS, RobotCapability.MISSIONS)
        val info = RobotInfo(
            robotId = "bot-007",
            model = "GraceBot v2",
            firmwareVersion = "2.1.0",
            navigationVersion = "0.2.0",
            gatewayVersion = "0.2.0",
            capabilities = caps
        )

        assertEquals("bot-007", info.robotId)
        assertEquals("GraceBot v2", info.model)
        assertEquals(3, info.capabilities.size)
        assertTrue(info.capabilities.contains(RobotCapability.VIRTUAL_WALLS))
    }

    @Test
    fun testUpdateMotionLimitsEnvelopeSerialization() {
        val limits = MotionLimits(maxLinearVelocityMps = 0.8, maxAngularVelocityRadPerSec = 1.0)
        val env = ProtocolCodec.createUpdateMotionLimitsEnvelope("cmd-cfg-1", limits)
        val jsonText = ProtocolCodec.encodeEnvelope(env)
        val decoded = ProtocolCodec.decodeEnvelope(jsonText)

        assertEquals("update_motion_limits", decoded.type)
        assertEquals("cmd-cfg-1", decoded.id)
        assertNotNull(decoded.payload)
    }
}
