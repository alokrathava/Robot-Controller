package com.alokrathava.sdk.model

sealed interface MapOperationState {
    data object Idle : MapOperationState
    data object Connected : MapOperationState
    data class WaitingForMap(val mapId: String?) : MapOperationState
    data class InitializingMapping(val mapId: String?) : MapOperationState
    data class Mapping(val mapId: String?) : MapOperationState
    data class MapUpdating(val mapId: String?) : MapOperationState
    data class Saving(val mapName: String) : MapOperationState
    data class Switching(val mapId: String) : MapOperationState
    data class Deleting(val mapId: String) : MapOperationState
    data class Failed(val mapId: String?, val error: String) : MapOperationState
}
