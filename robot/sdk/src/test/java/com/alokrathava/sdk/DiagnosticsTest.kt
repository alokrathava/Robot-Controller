package com.alokrathava.sdk

import com.alokrathava.sdk.internal.serialization.ProtocolCodec
import com.alokrathava.sdk.model.ConnectionMetrics
import com.alokrathava.sdk.model.RobotDiagnosticReport
import com.alokrathava.sdk.model.RobotDiagnostics
import com.alokrathava.sdk.model.RobotLogEntry
import com.alokrathava.sdk.model.TelemetryFreshness
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticsTest {

    @Test
    fun testDiagnosticsAndReportModels() {
        val diag = RobotDiagnostics(
            connection = ConnectionMetrics(latencyMs = 12L, connectedDurationMs = 60000L),
            telemetryFreshness = TelemetryFreshness(isStale = false, ageMs = 100L),
            activeMapId = "map_hallway"
        )
        val report = RobotDiagnosticReport(
            sdkVersion = "0.2.0",
            gatewayVersion = "0.2.0",
            protocolVersion = 1,
            diagnostics = diag,
            recentErrorCodes = listOf("WARN_STALE_BATTERY")
        )

        assertEquals("0.2.0", report.sdkVersion)
        assertEquals("map_hallway", report.diagnostics.activeMapId)
        assertEquals(12L, report.diagnostics.connection.latencyMs)
        assertEquals(1, report.recentErrorCodes.size)
    }

    @Test
    fun testGetRecentLogsEnvelopeAndAck() {
        val env = ProtocolCodec.createGetRecentLogsEnvelope("cmd-log-1", 50)
        val jsonText = ProtocolCodec.encodeEnvelope(env)
        val decoded = ProtocolCodec.decodeEnvelope(jsonText)

        assertEquals("get_recent_logs", decoded.type)
        assertEquals("cmd-log-1", decoded.id)

        val ackText = """
            {
                "type": "get_recent_logs_ack",
                "id": "cmd-log-1",
                "protocolVersion": 1,
                "payload": {
                    "logs": [
                        {
                            "timestampEpochMs": 1700000000000,
                            "level": "INFO",
                            "logger": "gateway",
                            "message": "Client connected"
                        }
                    ]
                }
            }
        """.trimIndent()

        val ackEnv = ProtocolCodec.decodeEnvelope(ackText)
        val logs = ProtocolCodec.decodeGetRecentLogsAck(ackEnv)

        assertNotNull(logs)
        assertEquals(1, logs!!.size)
        assertEquals("INFO", logs[0].level)
        assertEquals("Client connected", logs[0].message)
    }
}
