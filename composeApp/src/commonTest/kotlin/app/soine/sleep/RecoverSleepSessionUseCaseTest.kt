package app.soine.sleep

import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class RecoverSleepSessionUseCaseTest {

    @Test
    fun returnsNoneWhenThereIsNoActiveSession() = runRecoveryTest {
        val repository = RecoveryRepository()
        val useCase = RecoverSleepSessionUseCase(repository) { 2_000 }

        assertIs<RecoverSleepSessionResult.None>(useCase())
        assertEquals(0, repository.saveCalls)
    }

    @Test
    fun preparingSessionIsRecoveredAsSleeping() = runRecoveryTest {
        val repository = RecoveryRepository(record(SleepSessionStatus.PREPARING))
        val useCase = RecoverSleepSessionUseCase(repository) { 2_000 }

        val result = useCase()

        assertIs<RecoverSleepSessionResult.Recovered>(result)
        assertEquals(SleepSessionStatus.SLEEPING, result.session.status)
        assertEquals(SleepSessionSource.RECOVERED, result.session.source)
        assertEquals(1, repository.saveCalls)
    }

    @Test
    fun sleepingSessionIsMarkedRecovered() = runRecoveryTest {
        val repository = RecoveryRepository(record(SleepSessionStatus.SLEEPING))
        val useCase = RecoverSleepSessionUseCase(repository) { 2_000 }

        val result = useCase()

        assertIs<RecoverSleepSessionResult.Recovered>(result)
        assertEquals(SleepSessionSource.RECOVERED, result.session.source)
        assertEquals(1, repository.saveCalls)
    }

    @Test
    fun alreadyRecoveredSessionDoesNotRewriteWhenTimestampDoesNotAdvance() = runRecoveryTest {
        val existing = record(
            status = SleepSessionStatus.SLEEPING,
            source = SleepSessionSource.RECOVERED,
            updatedAt = 2_000,
        )
        val repository = RecoveryRepository(existing)
        val useCase = RecoverSleepSessionUseCase(repository) { 1_000 }

        val result = useCase()

        assertIs<RecoverSleepSessionResult.Recovered>(result)
        assertEquals(existing, result.session)
        assertEquals(0, repository.saveCalls)
    }

    @Test
    fun persistenceFailureIsReportedAndOriginalSessionRemains() = runRecoveryTest {
        val original = record(SleepSessionStatus.PREPARING)
        val repository = RecoveryRepository(original, failSave = true)
        val useCase = RecoverSleepSessionUseCase(repository) { 2_000 }

        assertIs<RecoverSleepSessionResult.Failed>(useCase())
        assertEquals(original, repository.getActiveSession())
    }

    private fun record(
        status: SleepSessionStatus,
        source: SleepSessionSource = SleepSessionSource.MANUAL,
        updatedAt: Long = 1_000,
    ) = SleepSessionRecord(
        id = "night-1",
        startedAtEpochMillis = 1_000,
        status = status,
        source = source,
        createdAtEpochMillis = 1_000,
        updatedAtEpochMillis = updatedAt,
    )
}

private class RecoveryRepository(
    private var active: SleepSessionRecord? = null,
    private val failSave: Boolean = false,
) : SleepSessionRepository {

    var saveCalls: Int = 0
        private set

    override suspend fun getActiveSession(): SleepSessionRecord? = active

    override suspend fun saveSession(session: SleepSessionRecord) {
        saveCalls += 1
        if (failSave) throw IllegalStateException("simulated storage failure")
        active = session
    }

    override suspend fun completeSession(
        sessionId: String,
        endedAtEpochMillis: Long,
        updatedAtEpochMillis: Long,
    ): SleepSessionRecord? = null

    override suspend fun getCompletedSessions(): List<SleepSessionRecord> = emptyList()
}

private fun <T> runRecoveryTest(block: suspend () -> T): T {
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
