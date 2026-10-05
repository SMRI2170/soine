package app.soine.sleep

import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class StartSleepSessionUseCaseTest {

    @Test
    fun createsPreparingBeforeSleeping() = runStartTest {
        val repository = RecordingSleepSessionRepository()
        val times = ArrayDeque(listOf(1_000L, 1_001L))
        val useCase = StartSleepSessionUseCase(
            repository = repository,
            nowEpochMillis = { times.removeFirst() },
            sessionIdGenerator = { "night-1" },
        )

        val result = useCase()

        assertIs<StartSleepSessionResult.Started>(result)
        assertEquals(
            listOf(SleepSessionStatus.PREPARING, SleepSessionStatus.SLEEPING),
            repository.saved.map { it.status },
        )
        assertEquals("night-1", result.session.id)
    }

    @Test
    fun returnsExistingActiveSessionInsteadOfCreatingAnother() = runStartTest {
        val active = record("existing", SleepSessionStatus.SLEEPING)
        val repository = RecordingSleepSessionRepository(active = active)
        val useCase = StartSleepSessionUseCase(
            repository = repository,
            nowEpochMillis = { 2_000 },
            sessionIdGenerator = { "should-not-be-used" },
        )

        val result = useCase()

        assertIs<StartSleepSessionResult.AlreadyActive>(result)
        assertEquals(active, result.session)
        assertEquals(emptyList(), repository.saved)
    }

    @Test
    fun secondSequentialStartDoesNotCreateDuplicateSession() = runStartTest {
        val repository = RecordingSleepSessionRepository()
        var idCalls = 0
        val useCase = StartSleepSessionUseCase(
            repository = repository,
            nowEpochMillis = { 3_000 },
            sessionIdGenerator = {
                idCalls += 1
                "night-$idCalls"
            },
        )

        val first = useCase()
        val second = useCase()

        assertIs<StartSleepSessionResult.Started>(first)
        assertIs<StartSleepSessionResult.AlreadyActive>(second)
        assertEquals(first.session.id, second.session.id)
        assertEquals(1, idCalls)
    }

    @Test
    fun sleepingWriteFailureLeavesPreparingSessionRecoverable() = runStartTest {
        val repository = RecordingSleepSessionRepository(failOnSaveNumber = 2)
        val useCase = StartSleepSessionUseCase(
            repository = repository,
            nowEpochMillis = { 4_000 },
            sessionIdGenerator = { "night-1" },
        )

        val result = useCase()

        assertIs<StartSleepSessionResult.Failed>(result)
        assertEquals(SleepSessionStatus.PREPARING, repository.getActiveSession()?.status)
    }

    private fun record(
        id: String,
        status: SleepSessionStatus,
    ) = SleepSessionRecord(
        id = id,
        startedAtEpochMillis = 1_000,
        status = status,
        createdAtEpochMillis = 1_000,
        updatedAtEpochMillis = 1_000,
    )
}

private class RecordingSleepSessionRepository(
    active: SleepSessionRecord? = null,
    private val failOnSaveNumber: Int? = null,
) : SleepSessionRepository {

    val saved = mutableListOf<SleepSessionRecord>()
    private var activeSession = active
    private val completed = mutableListOf<SleepSessionRecord>()
    private var saveCount = 0

    override suspend fun getActiveSession(): SleepSessionRecord? = activeSession

    override suspend fun saveSession(session: SleepSessionRecord) {
        saveCount += 1
        if (saveCount == failOnSaveNumber) {
            throw IllegalStateException("simulated storage failure")
        }

        saved += session
        if (session.isActive) {
            activeSession = session
        } else {
            activeSession = activeSession?.takeUnless { it.id == session.id }
            completed.removeAll { it.id == session.id }
            completed += session
        }
    }

    override suspend fun completeSession(
        sessionId: String,
        endedAtEpochMillis: Long,
        updatedAtEpochMillis: Long,
    ): SleepSessionRecord? = null

    override suspend fun getCompletedSessions(): List<SleepSessionRecord> = completed
}

private fun <T> runStartTest(block: suspend () -> T): T {
    var outcome: Result<T>? = null
    block.startCoroutine(
        object : Continuation<T> {
            override val context = EmptyCoroutineContext
            override fun resumeWith(result: Result<T>) {
                outcome = result
            }
        },
    )
    return checkNotNull(outcome).getOrThrow()
}
