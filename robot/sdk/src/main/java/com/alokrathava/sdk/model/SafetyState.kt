package com.alokrathava.sdk.model

enum class SafetyState {
    NORMAL,
    CAUTION,
    COLLISION_STOP,
    EMERGENCY_STOP,
    FAULT
}

data class SafetyStateDetails(
    val state: SafetyState,
    val emergencyStopActive: Boolean,
    val motionInhibited: Boolean,
    val controlSource: String,
    val detail: String,
    val deadManStopCount: Long = 0
)
