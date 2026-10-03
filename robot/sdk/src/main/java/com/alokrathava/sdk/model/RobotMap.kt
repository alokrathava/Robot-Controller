package com.alokrathava.sdk.model

data class RobotMap(
    val id: String,
    val name: String,
    val isActive: Boolean = false,
    val resolutionMetersPerCell: Double? = null,
    val widthCells: Int? = null,
    val heightCells: Int? = null,
    val createdAtEpochMs: Long? = null,
    val updatedAtEpochMs: Long? = null
) {
    val resolution: Float? get() = resolutionMetersPerCell?.toFloat()
    val width: Int? get() = widthCells
    val height: Int? get() = heightCells

    constructor(
        id: String,
        name: String,
        isActive: Boolean = false,
        resolution: Float? = null,
        width: Int? = null,
        height: Int? = null
    ) : this(
        id = id,
        name = name,
        isActive = isActive,
        resolutionMetersPerCell = resolution?.toDouble(),
        widthCells = width,
        heightCells = height,
        createdAtEpochMs = null,
        updatedAtEpochMs = null
    )
}
