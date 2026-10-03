package com.alokrathava.sdk.model

import kotlinx.serialization.Serializable

@Serializable
data class SavedLocation(
    val id: String,
    val name: String,
    val mapId: String,
    val pose: Pose2D
)

@Serializable
data class MapPointValidity(
    val isValid: Boolean,
    val reason: String? = null
)
