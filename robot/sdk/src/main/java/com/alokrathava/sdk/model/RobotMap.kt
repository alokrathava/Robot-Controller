package com.alokrathava.sdk.model

data class RobotMap(
    val id: String,
    val name: String,
    val isActive: Boolean = false,
    val resolution: Float? = null,
    val width: Int? = null,
    val height: Int? = null
)
