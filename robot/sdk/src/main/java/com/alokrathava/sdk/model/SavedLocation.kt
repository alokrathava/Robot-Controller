package com.alokrathava.sdk.model

import kotlinx.serialization.Serializable

/** A persistent destination in the coordinate frame of its associated map. */
@Serializable
data class SavedLocation(
    val id: String,
    val name: String,
    val mapId: String,
    val pose: Pose2D,
    val frame: String = "map"
)

@Serializable
data class MapPointValidity(
    val isValid: Boolean,
    val reason: String? = null
)
