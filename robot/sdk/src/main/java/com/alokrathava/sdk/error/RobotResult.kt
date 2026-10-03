package com.alokrathava.sdk.error

sealed interface RobotResult<out T> {
    data class Success<T>(val value: T) : RobotResult<T>
    data class Failure(val error: RobotError) : RobotResult<Nothing>
}

inline fun <T, R> RobotResult<T>.map(transform: (T) -> R): RobotResult<R> {
    return when (this) {
        is RobotResult.Success -> RobotResult.Success(transform(value))
        is RobotResult.Failure -> RobotResult.Failure(error)
    }
}
