package com.alokrathava.sdk

import com.alokrathava.sdk.model.ReconnectPolicy
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager

data class RobotEndpoint(
    val host: String,
    val port: Int,
    val useTls: Boolean = false
)

sealed interface RobotAuthentication {
    data class Token(
        val token: String
    ) : RobotAuthentication
}

fun interface RobotLogger {
    fun log(event: RobotLogEvent)

    companion object {
        val NONE = RobotLogger { }
    }
}

enum class RobotLogLevel {
    DEBUG,
    INFO,
    WARN,
    ERROR
}

data class RobotLogEvent(
    val level: RobotLogLevel,
    val category: String,
    val message: String
)

data class RobotSdkConfig(
    val endpoint: RobotEndpoint,
    val authentication: RobotAuthentication? = null,
    val commandTimeoutMs: Long = 15_000,
    val reconnectPolicy: ReconnectPolicy = ReconnectPolicy(),
    val heartbeatIntervalMs: Long = 5_000,
    val telemetryStaleTimeoutMs: Long = 3_000,
    val sslSocketFactory: SSLSocketFactory? = null,
    val trustManager: X509TrustManager? = null,
    val logger: RobotLogger = RobotLogger.NONE
)
