package com.alokrathava.settings

import com.alokrathava.sdk.model.ConnectionStatus
import com.alokrathava.sdk.model.RobotSimulationConfig
import com.alokrathava.sdk.model.RobotTelemetry

data class SettingsUiState(
    val selectedTab: SettingsTab = SettingsTab.CONNECTION,
    val robotIpAddress: String = "192.168.1.100",
    val port: String = "8080",
    val token: String = "",
    val autoConnect: Boolean = true,
    val reconnectionAttempts: Int = 3,
    val connectionTimeoutSeconds: Int = 10,
    val connectionStatus: ConnectionStatus = ConnectionStatus.DISCONNECTED,
    val connectionAddress: String = "192.168.1.100:8080",
    val latencyMs: Long? = null,
    val reconnectCount: Int = 0,
    val lastMessageAgeMs: Long = 0L,
    val lastConnected: String = "Not connected",
    val availableReconnectionAttempts: List<Int> = listOf(1, 2, 3, 5, 10),
    val availableTimeoutSeconds: List<Int> = listOf(5, 10, 15, 30, 60),
    val simulationConfig: RobotSimulationConfig = RobotSimulationConfig(),
    val telemetry: RobotTelemetry = RobotTelemetry()
) {
    val isConnected: Boolean
        get() = connectionStatus == ConnectionStatus.CONNECTED
}
