package com.alokrathava.sdk

import java.util.concurrent.ConcurrentHashMap

class RobotManager : AutoCloseable {

    private val clients = ConcurrentHashMap<String, RobotClient>()

    fun getOrCreateClient(robotId: String, config: RobotSdkConfig): RobotClient {
        return clients.computeIfAbsent(robotId) {
            RobotSdk.create(config)
        }
    }

    fun getClient(robotId: String): RobotClient? {
        return clients[robotId]
    }

    fun removeClient(robotId: String): RobotClient? {
        val client = clients.remove(robotId)
        client?.close()
        return client
    }

    val activeRobotIds: Set<String>
        get() = clients.keys.toSet()

    override fun close() {
        val currentClients = clients.values.toList()
        clients.clear()
        for (client in currentClients) {
            try {
                client.close()
            } catch (_: Exception) { }
        }
    }
}
