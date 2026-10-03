package com.alokrathava.sdk.model

import kotlinx.serialization.Serializable

@Serializable
data class MapPoint(
    val xMeters: Double,
    val yMeters: Double
)

@Serializable
enum class VirtualWallType {
    LINE,
    POLYGON
}

@Serializable
data class VirtualWall(
    val id: String,
    val mapId: String,
    val name: String,
    val type: VirtualWallType,
    val points: List<MapPoint>,
    val thicknessMeters: Double? = null,
    val enabled: Boolean = true
)

@Serializable
data class VirtualWallDraft(
    val mapId: String,
    val name: String,
    val type: VirtualWallType,
    val points: List<MapPoint>,
    val thicknessMeters: Double? = null,
    val enabled: Boolean = true
)
