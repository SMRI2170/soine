package app.soine.sleep

import app.soine.storage.SleepSessionStore
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class StoredSleepSessionRepositoryTest {

    @Test
    fun activeSessionSurvivesRepositoryRecreation() = runSuspendTest {
        val store = MemorySleepSessionStore()
        val firstRepository = StoredSleepSessionRepository(store)
        val session = record(id = "night-1", status = SleepSessionStatus.SLEEPING)

        firstRepository.saveSession(session)

        val recreatedRepository = StoredSleepSessionRepository(store)
        assertEquals(session, recreatedRepository.getActiveSession())
    }

    @Test
    fun completeSessionIsIdempotent() = runSuspendTest {
        val repository = StoredSleepSessionRepository(MemorySleepSessionStore())
        repository.saveSession(record(id = "night-1", status = SleepSessionStatus.SLEEPING))

        val first = repository.completeSession(
            sessionId = "night-1",
            endedAtEpochMillis = 8_000,
            updatedAtEpochMillis = 8_000,
        )
        val second = repository.completeSession(
            sessionId = "night-1",
            endedAtEpochMillis = 9_000,
            updatedAtEpochMillis = 9_000,
        )

        assertEquals(first, second)
        assertNull(repository.getActiveSession())
        assertEquals(listOf(first), repository.getCompletedSessions())
    }

    @Test
    fun completionNeverProducesNegativeDuration() = runSuspendTest {
        val repository = StoredSleepSessionRepository(MemorySleepSessionStore())
        repository.saveSession(
            record(
                id = "night-1",
                status = SleepSessionStatus.SLEEPING,
                startedAt = 5_000,
            ),
        )

        val completed = repository.completeSession(
            sessionId = "night-1",
            endedAtEpochMillis = 1_000,
            updatedAtEpochMillis = 6_000,
        )

        assertEquals(5_000, completed?.endedAtEpochMillis)
    }

    @Test
    fun secondActiveSessionIsRejected() {
        val repository = StoredSleepSessionRepository(MemorySleepSessionStore())

        runSuspendTest {
            repository.saveSession(record(id = "night-1", status = SleepSessionStatus.SLEEPING))
        }

        assertFailsWith<IllegalArgumentException> {
            runSuspendTest {
                repository.saveSession(record(id = "night-2", status = SleepSessionStatus.PREPARING))
            }
        }
    }

    @Test
    fun corruptedSnapshotIsNotSilentlyOverwritten() {
        val store = MemorySleepSessionStore("not-a-valid-snapshot")
        val repository = StoredSleepSessionRepository(store)

        assertFailsWith<SleepSessionStorageCorruptedException> {
            runSuspendTest { repository.getActiveSession() }
        }
        assertEquals("not-a-valid-snapshot", store.read())
    }

    private fun record(
        id: String,
        status: SleepSessionStatus,
        startedAt: Long = 1_000,
    ) = SleepSessionRecord(
        id = id,
        startedAtEpochMillis = startedAt,
        status = status,
        createdAtEpochMillis = startedAt,
        updatedAtEpochMillis = startedAt,
    )
}

private class MemorySleepSessionStore(
    private var value: String? = null,
) : SleepSessionStore {
    override fun read(): String? = value
    override fun write(value: String) { this.value = value }
    override fun clear() { value = null }
}

private fun <T> runSuspendTest(block: suspend () -> T): T {
    var outcome: Result<T>? = null
    block.startCoroutine(
        object : Continuation<T> {
            override val context = EmptyCoroutineContext
            override fun resumeWith(result: Result<T>) {
                outcome = result
            }
        },
    )
    return checkNotNull(outcome) {
        "Test coroutine suspended unexpectedly; this helper only supports synchronous suspend code."
    }.getOrThrow()
}
