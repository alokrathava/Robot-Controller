package com.alokrathava.settings

import com.alokrathava.sdk.model.ConnectionStatus
import com.alokrathava.sdk.model.RobotSimulationConfig
import com.alokrathava.sdk.model.RobotTelemetry

data class SettingsUiState(
    val selectedTab: SettingsTab = SettingsTab.CONNECTION,
    val robotIpAddress: String = "192.168.1.108",
    val port: String = "6080",
    val autoConnect: Boolean = true,
    val reconnectionAttempts: Int = 3,
    val connectionTimeoutSeconds: Int = 10,
    val connectionStatus: ConnectionStatus = ConnectionStatus.CONNECTED,
    val connectionAddress: String = "192.168.1.108:8080",
    val latencyMs: Int = 12,
    val lastConnected: String = "Sep 30, 2026 9:41 AM",
    val availableReconnectionAttempts: List<Int> = listOf(1, 2, 3, 5, 10),
    val availableTimeoutSeconds: List<Int> = listOf(5, 10, 15, 30, 60),
    val simulationConfig: RobotSimulationConfig = RobotSimulationConfig(),
    val telemetry: RobotTelemetry = RobotTelemetry()
) {
    val isConnected: Boolean
        get() = connectionStatus == ConnectionStatus.CONNECTED
}
