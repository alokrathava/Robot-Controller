package com.alokrathava.sdk.discovery

import com.alokrathava.sdk.model.DiscoveredRobot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

interface RobotDiscoveryManager {
    fun discoverRobots(port: Int = 8088, timeoutMs: Long = 5000L): Flow<DiscoveredRobot>
}

class RobotDiscoveryManagerImpl : RobotDiscoveryManager {

    private val json = Json { ignoreUnknownKeys = true }

    override fun discoverRobots(port: Int, timeoutMs: Long): Flow<DiscoveredRobot> = flow {
        val seen = mutableSetOf<String>()
        var socket: DatagramSocket? = null
        try {
            socket = DatagramSocket(port)
            socket.soTimeout = 1000
            val buffer = ByteArray(2048)
            val startTime = System.currentTimeMillis()

            while (System.currentTimeMillis() - startTime < timeoutMs) {
                try {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket.receive(packet)
                    val text = String(packet.data, 0, packet.length, Charsets.UTF_8)
                    val robot = parseBeacon(text, packet.address)
                    if (robot != null && seen.add(robot.id)) {
                        emit(robot)
                    }
                } catch (_: Exception) {
                    // Timeout or decode error
                }
            }
        } catch (_: Exception) {
            // Socket bind or network error
        } finally {
            socket?.close()
        }
    }.flowOn(Dispatchers.IO)

    internal fun parseBeacon(jsonText: String, remoteAddress: InetAddress): DiscoveredRobot? {
        return try {
            val element = json.parseToJsonElement(jsonText) as? kotlinx.serialization.json.JsonObject ?: return null
            val type = element["type"]?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
            if (type != "robot_discovery_beacon") return null

            val id = element["robotId"]?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content } ?: return null
            val name = element["name"]?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
            val port = element["port"]?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content?.toIntOrNull() } ?: 8080
            val protoVer = element["protocolVersion"]?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content?.toIntOrNull() } ?: 1
            val host = element["host"]?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content } ?: remoteAddress.hostAddress

            DiscoveredRobot(
                id = id,
                name = name,
                host = host,
                port = port,
                protocolVersion = protoVer
            )
        } catch (_: Exception) {
            null
        }
    }
}
