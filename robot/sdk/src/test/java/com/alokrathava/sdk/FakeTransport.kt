package com.alokrathava.sdk

import com.alokrathava.sdk.internal.RobotClientImpl
import com.alokrathava.sdk.internal.transport.RobotTransport
import com.alokrathava.sdk.internal.transport.TransportListener
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.concurrent.CopyOnWriteArrayList

/** Records outbound frames; replies to `hello` with a hello_ack and to commands via [autoReply]. */
internal class FakeTransport(
    private val capabilities: List<String> = listOf("navigation", "waypoint_navigation", "map_switching", "mapping"),
    private val autoReply: (type: String, id: String?) -> String? = { _, _ -> null }
) : RobotTransport {
    lateinit var listener: TransportListener
    val sent = CopyOnWriteArrayList<JsonObject>()
    @Volatile var connectCount = 0

    override fun connect() {
        connectCount++
        listener.onOpen()
    }

    override fun disconnect() {}

    override fun send(text: String): Boolean {
        val obj = Json.parseToJsonElement(text).jsonObject
        sent.add(obj)
        val type = obj["type"]!!.jsonPrimitive.content
        val id = obj["id"]?.jsonPrimitive?.content
        val reply = if (type == "hello") {
            """{"type":"hello_ack","id":"$id","protocolVersion":1,"payload":{"gatewayVersion":"0.2.0","capabilities":[${capabilities.joinToString(",") { "\"$it\"" }}],"sessionId":"s1"}}"""
        } else autoReply(type, id)
        reply?.let { listener.onMessage(it) }
        return true
    }

    fun types(): List<String> = sent.map { it["type"]!!.jsonPrimitive.content }
    fun last(type: String): JsonObject = sent.last { it["type"]!!.jsonPrimitive.content == type }

    companion object {
        fun client(config: RobotSdkConfig, transport: FakeTransport): RobotClientImpl =
            RobotClientImpl(config) { l -> transport.also { it.listener = l } }
    }
}
