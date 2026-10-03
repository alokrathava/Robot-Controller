package com.alokrathava.robotstatus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alokrathava.sdk.RobotRepository
import com.alokrathava.sdk.model.ConnectionStatus
import com.alokrathava.sdk.model.NavigationStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@HiltViewModel
class RobotStatusViewModel @Inject constructor(
    private val robotRepository: RobotRepository
) : ViewModel() {

    val uiState: StateFlow<RobotStatusUiState> = combine(
        robotRepository.batteryStatus,
        robotRepository.position,
        combine(
            robotRepository.telemetry,
            robotRepository.connectionStatus,
            robotRepository.connectionConfig,
            robotRepository.navigationStatus
        ) { telem, connStatus, connConfig, nav -> Quad(telem, connStatus, connConfig, nav) }
    ) { battery, position, telemGroup ->
        val (telemetry, connStatus, connConfig, navStatus) = telemGroup
        val isConnected = connStatus == ConnectionStatus.CONNECTED
        val address = if (connConfig.ipAddress.isNotEmpty()) "${connConfig.ipAddress}:${connConfig.port}" else "192.168.1.108:8080"

        val totalMins = (battery.levelPercent * 1.8).toInt()
        val hours = totalMins / 60
        val mins = totalMins % 60
        val estimatedRuntimeStr = if (battery.isCharging) {
            "Charging..."
        } else {
            "Estimated ${hours}h ${mins}m remaining"
        }

        val formattedStateStr = when {
            telemetry.isEmergencyStopped -> "Emergency Stop"
            navStatus == NavigationStatus.NAVIGATING -> "Navigating"
            navStatus == NavigationStatus.CHARGING -> "Charging"
            navStatus == NavigationStatus.SAVING_MAP -> "Saving Map"
            else -> "Idle"
        }

        RobotStatusUiState(
            batteryPercent = battery.levelPercent,
            isCharging = battery.isCharging,
            estimatedTimeRemaining = estimatedRuntimeStr,
            positionX = position.x,
            positionY = position.y,
            yawDegrees = position.headingDegrees,
            robotState = formattedStateStr,
            isConnected = isConnected,
            connectionAddress = address,
            speedMps = telemetry.speedMps,
            temperatureCelsius = telemetry.internalTempCelsius,
            batteryStatus = battery,
            robotPosition = position,
            telemetry = telemetry,
            connectionStatus = connStatus,
            navigationStatus = navStatus
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RobotStatusUiState()
    )
}
