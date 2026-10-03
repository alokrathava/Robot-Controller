package com.alokrathava.sdk.model

sealed interface MapOperationState {
    data object Idle : MapOperationState
    data class Saving(val mapName: String) : MapOperationState
    data class Switching(val mapId: String) : MapOperationState
    data class Deleting(val mapId: String) : MapOperationState
    data class Failed(val mapId: String?, val error: String) : MapOperationState
}
