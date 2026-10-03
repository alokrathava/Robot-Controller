package com.alokrathava.sdk.internal.protocol

import com.alokrathava.sdk.error.ErrorSeverity
import com.alokrathava.sdk.error.RobotError
import com.alokrathava.sdk.error.RobotResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

internal class PendingCommandRegistry {

    private val idCounter = AtomicLong(1000)
    private val pendingMap = ConcurrentHashMap<String, CompletableDeferred<RobotResult<Unit>>>()

    fun generateCommandId(): String {
        return "cmd-${idCounter.incrementAndGet()}"
    }

    suspend fun registerAndAwait(
        commandId: String,
        timeoutMs: Long,
        onSend: () -> Unit
    ): RobotResult<Unit> {
        val deferred = CompletableDeferred<RobotResult<Unit>>()
        pendingMap[commandId] = deferred

        try {
            onSend()
            val result = withTimeoutOrNull(timeoutMs) {
                deferred.await()
            }
            if (result != null) {
                return result
            }
            pendingMap.remove(commandId)
            return RobotResult.Failure(
                RobotError(
                    code = "COMMAND_TIMEOUT",
                    subsystem = "sdk",
                    severity = ErrorSeverity.WARN,
                    message = "Command '$commandId' timed out after ${timeoutMs}ms",
                    recoverable = true
                )
            )
        } catch (e: Exception) {
            pendingMap.remove(commandId)
            return RobotResult.Failure(
                RobotError(
                    code = "COMMAND_FAILED",
                    subsystem = "sdk",
                    severity = ErrorSeverity.ERROR,
                    message = e.message ?: "Failed to execute command",
                    recoverable = true
                )
            )
        }
    }

    fun completeSuccess(commandId: String) {
        val deferred = pendingMap.remove(commandId)
        deferred?.complete(RobotResult.Success(Unit))
    }

    fun completeError(commandId: String, error: RobotError) {
        val deferred = pendingMap.remove(commandId)
        deferred?.complete(RobotResult.Failure(error))
    }

    fun clearAll(reason: RobotError) {
        val keys = pendingMap.keys().toList()
        for (key in keys) {
            val deferred = pendingMap.remove(key)
            deferred?.complete(RobotResult.Failure(reason))
        }
    }
}
