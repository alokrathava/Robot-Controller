package com.agrathava.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agrathava.sdk.RobotRepository
import com.agrathava.sdk.model.DirectionCommand
import com.agrathava.sdk.model.NavigationStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val robotRepository: RobotRepository
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        robotRepository.batteryStatus,
        robotRepository.position,
        robotRepository.navigationStatus,
        robotRepository.mapData
    ) { battery, position, navStatus, mapData ->
        HomeUiState(
            batteryStatus = battery,
            position = position,
            navigationStatus = navStatus,
            mapData = mapData,
            statusMessage = getStatusMessage(navStatus)
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    fun moveForward() {
        robotRepository.move(DirectionCommand.FORWARD)
    }

    fun moveBackward() {
        robotRepository.move(DirectionCommand.BACKWARD)
    }

    fun moveLeft() {
        robotRepository.move(DirectionCommand.LEFT)
    }

    fun moveRight() {
        robotRepository.move(DirectionCommand.RIGHT)
    }

    fun goToCharge() {
        robotRepository.goToCharge()
    }

    fun cancelNavigation() {
        robotRepository.cancelNavigation()
    }

    fun getPosition() {
        robotRepository.refreshPosition()
    }

    fun moveToPosition() {
        robotRepository.moveToPosition(1.0, 2.0)
    }

    fun getMap() {
        robotRepository.fetchMap()
    }

    fun saveMap() {
        robotRepository.saveMap()
    }

    fun getBatteryLevel() {
        robotRepository.refreshBattery()
    }

    private fun getStatusMessage(status: NavigationStatus): String {
        return when (status) {
            NavigationStatus.IDLE -> "Robot Idle"
            NavigationStatus.NAVIGATING -> "Robot Navigating"
            NavigationStatus.CHARGING -> "Robot Charging"
            NavigationStatus.CANCELLED -> "Navigation Cancelled"
            NavigationStatus.SAVING_MAP -> "Saving Map Data"
            NavigationStatus.FETCHING_MAP -> "Fetching Map Data"
        }
    }
}
