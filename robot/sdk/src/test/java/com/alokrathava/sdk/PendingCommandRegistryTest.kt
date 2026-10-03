package com.alokrathava.sdk

import com.alokrathava.sdk.error.ErrorSeverity
import com.alokrathava.sdk.error.RobotError
import com.alokrathava.sdk.error.RobotResult
import com.alokrathava.sdk.internal.protocol.PendingCommandRegistry
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PendingCommandRegistryTest {

    @Test
    fun testGenerateCommandId() {
        val registry = PendingCommandRegistry()
        val id1 = registry.generateCommandId()
        val id2 = registry.generateCommandId()
        assertTrue(id1.startsWith("cmd-"))
        assertTrue(id2.startsWith("cmd-"))
        assertTrue(id1 != id2)
    }

    @Test
    fun testSuccessfulCommandAck() = runTest {
        val registry = PendingCommandRegistry()
        val cmdId = registry.generateCommandId()

        val deferred = async {
            registry.registerAndAwait(cmdId, timeoutMs = 2000) {
                // Sent successfully
            }
        }

        delay(50)
        registry.completeSuccess(cmdId)

        val result = deferred.await()
        assertTrue(result is RobotResult.Success)
    }

    @Test
    fun testCommandTimeout() = runTest {
        val registry = PendingCommandRegistry()
        val cmdId = registry.generateCommandId()

        val result = registry.registerAndAwait(cmdId, timeoutMs = 100) {
            // No response sent
        }

        assertTrue(result is RobotResult.Failure)
        val error = (result as RobotResult.Failure).error
        assertEquals("COMMAND_TIMEOUT", error.code)
    }

    @Test
    fun testCommandErrorResponse() = runTest {
        val registry = PendingCommandRegistry()
        val cmdId = registry.generateCommandId()

        val deferred = async {
            registry.registerAndAwait(cmdId, timeoutMs = 2000) {}
        }

        delay(50)
        val err = RobotError("LOCALIZATION_LOST", "nav", ErrorSeverity.ERROR, "Lost", true)
        registry.completeError(cmdId, err)

        val result = deferred.await()
        assertTrue(result is RobotResult.Failure)
        assertEquals("LOCALIZATION_LOST", (result as RobotResult.Failure).error.code)
    }
}
