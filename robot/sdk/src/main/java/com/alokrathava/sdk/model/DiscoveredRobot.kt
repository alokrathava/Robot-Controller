package com.alokrathava.sdk.model

import kotlinx.serialization.Serializable

@Serializable
data class DiscoveredRobot(
    val id: String,
    val name: String? = null,
    val host: String,
    val port: Int = 8080,
    val protocolVersion: Int? = 1
)
