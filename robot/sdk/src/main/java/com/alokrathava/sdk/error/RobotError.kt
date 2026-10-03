package com.alokrathava.sdk.error

data class RobotError(
    val code: String,
    val subsystem: String,
    val severity: ErrorSeverity,
    val message: String,
    val recoverable: Boolean,
    val sourceCode: Int? = null
)
