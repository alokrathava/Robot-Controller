package com.alokrathava.sdk.model

data class TelemetryFreshness(
    val isStale: Boolean = false,
    val ageMs: Long = 0L,
    val receivedAtMillis: Long = 0L
)
