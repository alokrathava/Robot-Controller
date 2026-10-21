package com.alokrathava.sdk.model

/** Describes whether the last received map-frame pose may be presented as live. */
enum class PositionAvailability { FRESH, STALE, UNAVAILABLE }

/** Holds a validated pose and its bounded, received-only travel history. */
data class LiveRobotPosition(
    val pose: Pose2D? = null,
    val availability: PositionAvailability = PositionAvailability.UNAVAILABLE,
    val localizationState: String = "UNINITIALIZED",
    val activeMap: String = "",
    val linearVelocityMps: Double? = null,
    val receivedAtMillis: Long = 0L,
    val trailSegments: List<List<Pose2D>> = emptyList(),
    val motionEpoch: Long = 0L
)
