package com.alokrathava.sdk.testkit

import com.alokrathava.sdk.model.Pose2D

class ProtocolSimulator {

    fun generateTelemetryEnvelope(
        pose: Pose2D = Pose2D(1.0, 2.0, 0.5),
        navigationState: String = "IDLE",
        activeMap: String = "map_office"
    ): String {
        return """
            {
                "type": "telemetry",
                "protocolVersion": 1,
                "payload": {
                    "poseValid": true,
                    "frame": "map",
                    "xMeters": ${pose.xMeters},
                    "yMeters": ${pose.yMeters},
                    "yawRadians": ${pose.yawRadians},
                    "linearVelocityMps": 0.0,
                    "angularVelocityRadPerSec": 0.0,
                    "isMoving": false,
                    "navigationState": "$navigationState",
                    "activeMap": "$activeMap"
                }
            }
        """.trimIndent()
    }

    fun generateHelloAckEnvelope(
        gatewayVersion: String = "0.2.0",
        capabilities: List<String> = listOf("navigation", "map_switching", "virtual_walls", "missions")
    ): String {
        val capsJson = capabilities.joinToString(",") { "\"$it\"" }
        return """
            {
                "type": "hello_ack",
                "protocolVersion": 1,
                "payload": {
                    "gatewayVersion": "$gatewayVersion",
                    "capabilities": [$capsJson],
                    "sessionId": "sim-session-001"
                }
            }
        """.trimIndent()
    }
}
