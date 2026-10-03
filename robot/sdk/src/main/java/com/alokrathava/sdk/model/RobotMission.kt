package com.alokrathava.sdk.model

import kotlinx.serialization.Serializable

@Serializable
sealed interface MissionStep {

    @Serializable
    data class Navigate(val pose: Pose2D) : MissionStep

    @Serializable
    data class NavigateToLocation(val locationId: String) : MissionStep

    @Serializable
    data class Wait(val durationMillis: Long) : MissionStep

    @Serializable
    data object Dock : MissionStep

    @Serializable
    data object Undock : MissionStep
}

@Serializable
enum class MissionStatus {
    IDLE,
    QUEUED,
    RUNNING,
    PAUSED,
    SUCCEEDED,
    FAILED,
    CANCELLED
}

@Serializable
data class RobotMission(
    val id: String? = null,
    val name: String,
    val steps: List<MissionStep>
)

@Serializable
data class MissionProgress(
    val missionId: String,
    val status: MissionStatus,
    val currentStepIndex: Int = 0,
    val totalSteps: Int = 0,
    val detail: String = ""
)
