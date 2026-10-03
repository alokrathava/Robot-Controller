package com.alokrathava.sdk.model

import kotlinx.serialization.Serializable

@Serializable
data class Pose2D(
    val xMeters: Double,
    val yMeters: Double,
    val yawRadians: Double
)
