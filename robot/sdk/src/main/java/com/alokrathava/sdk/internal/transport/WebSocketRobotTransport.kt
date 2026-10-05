package com.alokrathava.sdk.internal.transport

import com.alokrathava.sdk.RobotEndpoint
import com.alokrathava.sdk.RobotLogEvent
import com.alokrathava.sdk.RobotLogLevel
import com.alokrathava.sdk.RobotLogger
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager

internal interface TransportListener {
    fun onOpen()
    fun onMessage(text: String)
    fun onFailure(t: Throwable, response: Response?)
    fun onClose(code: Int, reason: String)
}

internal class WebSocketRobotTransport(
    private val endpoint: RobotEndpoint,
    private val logger: RobotLogger,
    private val listener: TransportListener,
    private val sslSocketFactory: SSLSocketFactory? = null,
    private val trustManager: X509TrustManager? = null
) {
    private var client: OkHttpClient? = null
    private var webSocket: WebSocket? = null
    @Volatile private var _isConnected = false

    val isConnected: Boolean
        get() = _isConnected

    fun connect() {
        if (_isConnected) return

        disconnectInternal(silent = true)

        val builder = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .pingInterval(15, TimeUnit.SECONDS)

        if (sslSocketFactory != null && trustManager != null) {
            builder.sslSocketFactory(sslSocketFactory, trustManager)
        }

        client = builder.build()

        val scheme = if (endpoint.useTls) "wss" else "ws"
        val url = "$scheme://${endpoint.host}:${endpoint.port}"
        logger.log(RobotLogEvent(RobotLogLevel.INFO, "TRANSPORT", "Connecting to WebSocket at $url"))

        val request = Request.Builder()
            .url(url)
            .build()

        webSocket = client?.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                _isConnected = true
                logger.log(RobotLogEvent(RobotLogLevel.INFO, "TRANSPORT", "WebSocket connected successfully"))
                listener.onOpen()
            }

            override fun onMessage(ws: WebSocket, text: String) {
                listener.onMessage(text)
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                _isConnected = false
                val responseInfo = response?.let { " [HTTP ${it.code} ${it.message}]" } ?: ""
                val causeInfo = t.cause?.message?.let { " (Cause: $it)" } ?: ""
                val detailedMessage = "${t.javaClass.simpleName}: ${t.message ?: "Unknown transport error"}$causeInfo$responseInfo"
                logger.log(RobotLogEvent(RobotLogLevel.ERROR, "TRANSPORT", "WebSocket failure: $detailedMessage"))
                listener.onFailure(t, response)
            }

            override fun onClosing(ws: WebSocket, code: Int, reason: String) {
                ws.close(code, reason)
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                _isConnected = false
                logger.log(RobotLogEvent(RobotLogLevel.INFO, "TRANSPORT", "WebSocket closed: $code $reason"))
                listener.onClose(code, reason)
            }
        })
    }

    fun send(text: String): Boolean {
        val ws = webSocket
        if (!_isConnected || ws == null) {
            return false
        }
        return ws.send(text)
    }

    private fun disconnectInternal(silent: Boolean) {
        _isConnected = false
        try {
            webSocket?.close(1000, "Disconnect")
        } catch (_: Exception) { }
        webSocket = null
        try {
            client?.dispatcher?.executorService?.shutdown()
        } catch (_: Exception) { }
        client = null
    }

    fun disconnect() {
        disconnectInternal(silent = false)
    }
}
