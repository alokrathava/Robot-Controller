package com.alokrathava.sdk.model

import kotlinx.serialization.Serializable

@Serializable
data class RobotDiagnostics(
    val connection: ConnectionMetrics,
    val telemetryFreshness: TelemetryFreshness,
    val activeMapId: String? = null,
    val navigationAvailable: Boolean = true,
    val safetyAvailable: Boolean = true,
    val dockingAvailable: Boolean = true
)

@Serializable
data class RobotDiagnosticReport(
    val sdkVersion: String = "0.2.0",
    val gatewayVersion: String = "0.2.0",
    val protocolVersion: Int = 1,
    val diagnostics: RobotDiagnostics,
    val recentErrorCodes: List<String> = emptyList()
)

@Serializable
data class RobotLogEntry(
    val timestampEpochMs: Long,
    val level: String,
    val logger: String,
    val message: String
)

interface RobotMetricsCollector {
    fun recordCommandLatency(commandType: String, latencyMs: Long)
    fun recordReconnectAttempt(attempt: Int)
    fun recordFrameDropped(reason: String)

    companion object {
        val NONE = object : RobotMetricsCollector {
            override fun recordCommandLatency(commandType: String, latencyMs: Long) {}
            override fun recordReconnectAttempt(attempt: Int) {}
            override fun recordFrameDropped(reason: String) {}
        }
    }
}
