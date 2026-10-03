package com.alokrathava.sdk.model

import kotlinx.serialization.Serializable

@Serializable
data class NavigationRoute(
    val waypoints: List<Pose2D>,
    val stopOnFailure: Boolean = true
)

@Serializable
data class NavigationState(
    val state: String = "IDLE",
    val targetPose: Pose2D? = null,
    val distanceRemainingMeters: Float? = null,
    val elapsedTimeSeconds: Float = 0f,
    val currentWaypointIndex: Int = 0,
    val totalWaypoints: Int = 0,
    val errorMsg: String = ""
)
