package com.alokrathava.sdk.model

enum class DockingState {
    UNDOCKED,
    NAVIGATING_TO_DOCK,
    PRESTAGING,
    ALIGNING,
    DOCKING,
    DOCKED,
    UNDOCKING,
    FAILED,
    CANCELLED
}

data class DockingStateDetails(
    val state: DockingState,
    val dockId: String = "",
    val isDocked: Boolean = false,
    val isCharging: Boolean = false,
    val dockingTimeSeconds: Float = 0f,
    val numRetries: Int = 0,
    val errorCode: Int = 0,
    val errorName: String = "",
    val errorMsg: String = "",
    val detail: String = ""
)
