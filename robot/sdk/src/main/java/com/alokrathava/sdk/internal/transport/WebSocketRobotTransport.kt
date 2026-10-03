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

internal interface TransportListener {
    fun onOpen()
    fun onMessage(text: String)
    fun onFailure(t: Throwable, response: Response?)
    fun onClose(code: Int, reason: String)
}

internal class WebSocketRobotTransport(
    private val endpoint: RobotEndpoint,
    private val logger: RobotLogger,
    private val listener: TransportListener
) {
    private var client: OkHttpClient? = null
    private var webSocket: WebSocket? = null
    @Volatile private var isConnected = false

    fun connect() {
        if (isConnected) return

        client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .pingInterval(15, TimeUnit.SECONDS)
            .build()

        val url = "ws://${endpoint.host}:${endpoint.port}"
        logger.log(RobotLogEvent(RobotLogLevel.INFO, "TRANSPORT", "Connecting to WebSocket at $url"))

        val request = Request.Builder()
            .url(url)
            .build()

        webSocket = client?.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                isConnected = true
                logger.log(RobotLogEvent(RobotLogLevel.INFO, "TRANSPORT", "WebSocket connected successfully"))
                listener.onOpen()
            }

            override fun onMessage(ws: WebSocket, text: String) {
                listener.onMessage(text)
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                isConnected = false
                logger.log(RobotLogEvent(RobotLogLevel.ERROR, "TRANSPORT", "WebSocket failure: ${t.message}"))
                listener.onFailure(t, response)
            }

            override fun onClosing(ws: WebSocket, code: Int, reason: String) {
                ws.close(code, reason)
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                isConnected = false
                logger.log(RobotLogEvent(RobotLogLevel.INFO, "TRANSPORT", "WebSocket closed: $code $reason"))
                listener.onClose(code, reason)
            }
        })
    }

    fun send(text: String): Boolean {
        val ws = webSocket
        if (!isConnected || ws == null) {
            return false
        }
        return ws.send(text)
    }

    fun disconnect() {
        isConnected = false
        try {
            webSocket?.close(1000, "Client disconnect")
        } catch (_: Exception) { }
        webSocket = null
        try {
            client?.dispatcher?.executorService?.shutdown()
        } catch (_: Exception) { }
        client = null
    }
}
