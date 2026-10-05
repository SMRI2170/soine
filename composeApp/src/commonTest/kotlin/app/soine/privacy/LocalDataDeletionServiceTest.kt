package app.soine.privacy

import app.soine.sleep.SleepSessionRecord
import app.soine.sleep.SleepSessionRepository
import app.soine.sleep.SleepSessionStatus
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.test.*

class LocalDataDeletionServiceTest {
    @Test fun activeSessionBlocksDeletion() = runSuspend {
        val repository = TestRepository(active = record(SleepSessionStatus.SLEEPING))
        var clears = 0
        val service = LocalDataDeletionService(repository, listOf(LocalDataClearer { clears++ }))

        assertEquals(LocalDataDeletionResult.BlockedByActiveSession, service.deleteAll())
        assertEquals(0, clears)
    }

    @Test fun allRegisteredStoresAreClearedWhenIdle() = runSuspend {
        val repository = TestRepository()
        var first = false
        var second = false
        val service = LocalDataDeletionService(
            repository,
            listOf(
                LocalDataClearer { first = true },
                LocalDataClearer { second = true },
            ),
        )

        assertEquals(LocalDataDeletionResult.Deleted, service.deleteAll())
        assertTrue(first)
        assertTrue(second)
    }

    @Test fun remainingStoresAreAttemptedAfterFailure() = runSuspend {
        val repository = TestRepository()
        var second = false
        val service = LocalDataDeletionService(
            repository,
            listOf(
                LocalDataClearer { error("first clear failed") },
                LocalDataClearer { second = true },
            ),
        )

        assertIs<LocalDataDeletionResult.Failed>(service.deleteAll())
        assertTrue(second)
    }
}

private class TestRepository(
    private var active: SleepSessionRecord? = null,
) : SleepSessionRepository {
    override suspend fun getActiveSession(): SleepSessionRecord? = active
    override suspend fun saveSession(session: SleepSessionRecord) {
        active = session.takeIf { it.isActive }
    }
    override suspend fun completeSession(
        sessionId: String,
        endedAtEpochMillis: Long,
        updatedAtEpochMillis: Long,
    ): SleepSessionRecord? = null
    override suspend fun getCompletedSessions(): List<SleepSessionRecord> = emptyList()
}

private fun record(status: SleepSessionStatus) = SleepSessionRecord(
    id = "night",
    startedAtEpochMillis = 1_000L,
    status = status,
    createdAtEpochMillis = 1_000L,
    updatedAtEpochMillis = 1_000L,
)

private fun runSuspend(block: suspend () -> Unit) {
    block.startCoroutine(object : Continuation<Unit> {
        override val context: CoroutineContext = EmptyCoroutineContext
        override fun resumeWith(result: Result<Unit>) = result.getOrThrow()
    })
}
