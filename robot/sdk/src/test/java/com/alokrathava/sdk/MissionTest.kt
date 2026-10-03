package com.alokrathava.sdk

import com.alokrathava.sdk.internal.serialization.ProtocolCodec
import com.alokrathava.sdk.model.MissionProgress
import com.alokrathava.sdk.model.MissionStatus
import com.alokrathava.sdk.model.MissionStep
import com.alokrathava.sdk.model.Pose2D
import com.alokrathava.sdk.model.RobotMission
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MissionTest {

    @Test
    fun testRobotMissionModel() {
        val steps = listOf(
            MissionStep.Navigate(Pose2D(1.0, 2.0, 0.0)),
            MissionStep.Wait(3000L),
            MissionStep.NavigateToLocation("loc_breakroom"),
            MissionStep.Dock
        )
        val mission = RobotMission(
            id = "mission_patrol_1",
            name = "Night Patrol",
            steps = steps
        )

        assertEquals("mission_patrol_1", mission.id)
        assertEquals("Night Patrol", mission.name)
        assertEquals(4, mission.steps.size)
        assertTrue(mission.steps[0] is MissionStep.Navigate)
        assertTrue(mission.steps[1] is MissionStep.Wait)
        assertTrue(mission.steps[2] is MissionStep.NavigateToLocation)
        assertTrue(mission.steps[3] is MissionStep.Dock)
    }

    @Test
    fun testSubmitMissionEnvelopeSerialization() {
        val steps = listOf(
            MissionStep.Navigate(Pose2D(5.0, 5.0, 1.57)),
            MissionStep.Undock
        )
        val mission = RobotMission(name = "Clean Room", steps = steps)

        val env = ProtocolCodec.createSubmitMissionEnvelope("cmd-mission-1", mission)
        val jsonText = ProtocolCodec.encodeEnvelope(env)
        val decoded = ProtocolCodec.decodeEnvelope(jsonText)

        assertEquals("submit_mission", decoded.type)
        assertEquals("cmd-mission-1", decoded.id)
        assertNotNull(decoded.payload)
    }

    @Test
    fun testDecodeMissionProgress() {
        val jsonText = """
            {
                "type": "mission_progress",
                "protocolVersion": 1,
                "payload": {
                    "missionId": "m_1001",
                    "status": "RUNNING",
                    "currentStepIndex": 2,
                    "totalSteps": 5,
                    "detail": "Navigating to waypoint 3"
                }
            }
        """.trimIndent()

        val envelope = ProtocolCodec.decodeEnvelope(jsonText)
        val progress = ProtocolCodec.decodeMissionProgress(envelope)

        assertNotNull(progress)
        assertEquals("m_1001", progress!!.missionId)
        assertEquals(MissionStatus.RUNNING, progress.status)
        assertEquals(2, progress.currentStepIndex)
        assertEquals(5, progress.totalSteps)
        assertEquals("Navigating to waypoint 3", progress.detail)
    }
}
