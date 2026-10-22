package com.alokrathava.sdk.model

import kotlinx.serialization.Serializable

@Serializable
data class NavigationRoute(
    val waypoints: List<Pose2D>,
    val stopOnFailure: Boolean = true
)

/** Live progress plus the correlated terminal Nav2 result, retained until the next request. */
@Serializable
data class NavigationState(
    val state: String = "IDLE",
    val targetPose: Pose2D? = null,
    val distanceRemainingMeters: Float? = null,
    val elapsedTimeSeconds: Float = 0f,
    val currentWaypointIndex: Int = 0,
    val totalWaypoints: Int = 0,
    val errorMsg: String = "",
    val requestId: String? = null,
    val goalId: String? = null,
    val resultStatus: String? = null,
    val errorCode: Int? = null
)
