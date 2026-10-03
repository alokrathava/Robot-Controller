package com.alokrathava.sdk.model

import kotlinx.serialization.Serializable

@Serializable
data class TelemetryFreshness(
    val isStale: Boolean = false,
    val ageMs: Long = 0L,
    val receivedAtMillis: Long = 0L
)
