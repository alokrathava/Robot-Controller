package com.alokrathava.sdk

import com.alokrathava.sdk.error.RobotResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RobotConnectionManager {

    private val _connectionProfile = MutableStateFlow(RobotConnectionProfile())
    val connectionProfile: StateFlow<RobotConnectionProfile> = _connectionProfile.asStateFlow()

    private val _activeClient = MutableStateFlow<RobotClient?>(null)
    val activeClient: StateFlow<RobotClient?> = _activeClient.asStateFlow()

    @Volatile
    private var currentClient: RobotClient? = null

    suspend fun connect(profile: RobotConnectionProfile): RobotResult<Unit> {
        _connectionProfile.value = profile

        currentClient?.let { oldClient ->
            try {
                oldClient.disconnect()
                oldClient.close()
            } catch (_: Exception) { }
        }

        val auth = if (profile.token.isNotBlank()) {
            RobotAuthentication.Token(profile.token)
        } else {
            null
        }

        val sdkConfig = RobotSdkConfig(
            endpoint = RobotEndpoint(
                host = profile.host,
                port = profile.port,
                useTls = profile.useTls
            ),
            authentication = auth,
            commandTimeoutMs = 15_000
        )

        val newClient = RobotSdk.create(sdkConfig)
        currentClient = newClient
        _activeClient.value = newClient

        return newClient.connect()
    }

    suspend fun disconnect() {
        currentClient?.disconnect()
    }

    fun getActiveClient(): RobotClient? = currentClient
}
