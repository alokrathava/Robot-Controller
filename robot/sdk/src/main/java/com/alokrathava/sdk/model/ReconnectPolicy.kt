package com.alokrathava.sdk.model

data class ReconnectPolicy(
    val enabled: Boolean = true,
    val initialDelayMs: Long = 1_000,
    val maxDelayMs: Long = 30_000,
    val multiplier: Double = 2.0,
    val jitterRatio: Double = 0.1
)
