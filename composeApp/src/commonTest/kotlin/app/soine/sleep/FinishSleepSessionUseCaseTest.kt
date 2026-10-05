package app.soine.sleep

import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class FinishSleepSessionUseCaseTest {

    @Test
    fun completesActiveSession() = runFinishTest {
        val active = record("night-1", startedAt = 1_000)
        val repository = FinishRecordingRepository(active)
        val useCase = FinishSleepSessionUseCase(
            repository = repository,
            nowEpochMillis = { 8_000 },
        )

        val result = useCase()

        assertIs<FinishSleepSessionResult.Finished>(result)
        assertEquals("night-1", result.session.id)
        assertEquals(SleepSessionStatus.COMPLETED, result.session.status)
        assertEquals(8_000, result.session.endedAtEpochMillis)
        assertNull(repository.getActiveSession())
        assertEquals(1, repository.completionCalls)
    }

    @Test
    fun finishingTwiceDoesNotCompleteTwice() = runFinishTest {
        val repository = FinishRecordingRepository(record("night-1", 1_000))
        val useCase = FinishSleepSessionUseCase(
            repository = repository,
            nowEpochMillis = { 8_000 },
        )

        val first = useCase()
        val second = useCase()

        assertIs<FinishSleepSessionResult.Finished>(first)
        assertIs<FinishSleepSessionResult.NoActiveSession>(second)
        assertEquals(1, repository.completionCalls)
        assertEquals(1, repository.getCompletedSessions().size)
    }

    @Test
    fun noActiveSessionIsExplicitResult() = runFinishTest {
        val repository = FinishRecordingRepository()
        val useCase = FinishSleepSessionUseCase(
            repository = repository,
            nowEpochMillis = { 8_000 },
        )

        assertIs<FinishSleepSessionResult.NoActiveSession>(useCase())
        assertEquals(0, repository.completionCalls)
    }

    @Test
    fun clockGoingBackwardDoesNotCreateNegativeDuration() = runFinishTest {
        val repository = FinishRecordingRepository(record("night-1", 5_000))
        val useCase = FinishSleepSessionUseCase(
            repository = repository,
            nowEpochMillis = { 1_000 },
        )

        val result = useCase()

        assertIs<FinishSleepSessionResult.Finished>(result)
        assertEquals(5_000, result.session.endedAtEpochMillis)
    }

    @Test
    fun persistenceFailureIsReturnedWithoutPretendingSuccess() = runFinishTest {
        val repository = FinishRecordingRepository(
            active = record("night-1", 1_000),
            failCompletion = true,
        )
        val useCase = FinishSleepSessionUseCase(
            repository = repository,
            nowEpochMillis = { 8_000 },
        )

        val result = useCase()

        assertIs<FinishSleepSessionResult.Failed>(result)
        assertEquals("night-1", repository.getActiveSession()?.id)
    }

    private fun record(id: String, startedAt: Long) = SleepSessionRecord(
        id = id,
        startedAtEpochMillis = startedAt,
        status = SleepSessionStatus.SLEEPING,
        createdAtEpochMillis = startedAt,
        updatedAtEpochMillis = startedAt,
    )
}

private class FinishRecordingRepository(
    private var active: SleepSessionRecord? = null,
    private val failCompletion: Boolean = false,
) : SleepSessionRepository {

    private val completed = mutableListOf<SleepSessionRecord>()
    var completionCalls: Int = 0
        private set

    override suspend fun getActiveSession(): SleepSessionRecord? = active

    override suspend fun saveSession(session: SleepSessionRecord) {
        active = session.takeIf { it.isActive }
        if (!session.isActive) {
            completed.removeAll { it.id == session.id }
            completed += session
        }
    }

    override suspend fun completeSession(
        sessionId: String,
        endedAtEpochMillis: Long,
        updatedAtEpochMillis: Long,
    ): SleepSessionRecord? {
        completionCalls += 1
        if (failCompletion) throw IllegalStateException("simulated storage failure")

        completed.firstOrNull { it.id == sessionId }?.let { return it }
        val current = active?.takeIf { it.id == sessionId } ?: return null
        val result = current.copy(
            status = SleepSessionStatus.COMPLETED,
            endedAtEpochMillis = endedAtEpochMillis.coerceAtLeast(current.startedAtEpochMillis),
            updatedAtEpochMillis = updatedAtEpochMillis.coerceAtLeast(current.updatedAtEpochMillis),
        )
        active = null
        completed += result
        return result
    }

    override suspend fun getCompletedSessions(): List<SleepSessionRecord> =
        completed.sortedByDescending { it.startedAtEpochMillis }
}

private fun <T> runFinishTest(block: suspend () -> T): T {
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
