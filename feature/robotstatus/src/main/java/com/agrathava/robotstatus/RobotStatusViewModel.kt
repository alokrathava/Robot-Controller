package com.agrathava.robotstatus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agrathava.sdk.RobotRepository
import com.agrathava.sdk.model.ConnectionStatus
import com.agrathava.sdk.model.NavigationStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class RobotStatusViewModel @Inject constructor(
    private val robotRepository: RobotRepository
) : ViewModel() {

    val uiState: StateFlow<RobotStatusUiState> = combine(
        robotRepository.batteryStatus,
        robotRepository.position,
        robotRepository.telemetry,
        robotRepository.connectionStatus,
        robotRepository.navigationStatus
    ) { battery, position, telemetry, connStatus, navStatus ->
        val isConnected = connStatus == ConnectionStatus.CONNECTED

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
            connectionAddress = "192.168.1.108:8080",
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
