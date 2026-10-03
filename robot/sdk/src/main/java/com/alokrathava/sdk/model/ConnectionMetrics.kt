package com.alokrathava.sdk.model

data class ConnectionMetrics(
    val latencyMs: Long? = null,
    val connectedDurationMs: Long = 0L,
    val lastMessageAgeMs: Long = 0L,
    val reconnectCount: Int = 0
)
