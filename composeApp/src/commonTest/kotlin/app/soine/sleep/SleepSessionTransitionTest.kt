package app.soine.sleep

import app.soine.storage.SleepSessionStore
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class SleepSessionTransitionTest {

    @Test
    fun fullLifecyclePersistsStartAndCompletion() = runTransitionTest {
        val store = TransitionMemoryStore()
        val repository = StoredSleepSessionRepository(store)
        val start = StartSleepSessionUseCase(
            repository = repository,
            nowEpochMillis = SequenceClock(1_000, 1_001)::next,
            sessionIdGenerator = { "night-1" },
        )
        val finish = FinishSleepSessionUseCase(
            repository = repository,
            nowEpochMillis = { 8_000 },
        )

        val started = start()
        assertIs<StartSleepSessionResult.Started>(started)
        assertEquals(SleepSessionStatus.SLEEPING, repository.getActiveSession()?.status)

        val finished = finish()
        assertIs<FinishSleepSessionResult.Finished>(finished)
        assertNull(repository.getActiveSession())
        assertEquals(listOf("night-1"), repository.getCompletedSessions().map { it.id })
    }

    @Test
    fun processRestartRecoversSameSessionThenCompletesOnce() = runTransitionTest {
        val store = TransitionMemoryStore()
        val firstRepository = StoredSleepSessionRepository(store)
        val start = StartSleepSessionUseCase(
            repository = firstRepository,
            nowEpochMillis = SequenceClock(1_000, 1_001)::next,
            sessionIdGenerator = { "night-1" },
        )
        assertIs<StartSleepSessionResult.Started>(start())

        // Simulate process recreation by constructing a new repository instance.
        val recreatedRepository = StoredSleepSessionRepository(store)
        val recovery = RecoverSleepSessionUseCase(
            repository = recreatedRepository,
            nowEpochMillis = { 2_000 },
        )
        val recovered = recovery()

        assertIs<RecoverSleepSessionResult.Recovered>(recovered)
        assertEquals("night-1", recovered.session.id)
        assertEquals(SleepSessionSource.RECOVERED, recovered.session.source)

        val finish = FinishSleepSessionUseCase(
            repository = recreatedRepository,
            nowEpochMillis = { 8_000 },
        )
        assertIs<FinishSleepSessionResult.Finished>(finish())
        assertIs<FinishSleepSessionResult.NoActiveSession>(finish())
        assertEquals(1, recreatedRepository.getCompletedSessions().size)
    }

    @Test
    fun failedSleepingTransitionLeavesPreparingRecordRecoverable() = runTransitionTest {
        val store = FailOnWriteStore(failOnWrite = 2)
        val repository = StoredSleepSessionRepository(store)
        val start = StartSleepSessionUseCase(
            repository = repository,
            nowEpochMillis = SequenceClock(1_000, 1_001)::next,
            sessionIdGenerator = { "night-1" },
        )

        assertIs<StartSleepSessionResult.Failed>(start())

        store.allowWrites()
        val recreatedRepository = StoredSleepSessionRepository(store)
        assertEquals(SleepSessionStatus.PREPARING, recreatedRepository.getActiveSession()?.status)

        val recovered = RecoverSleepSessionUseCase(
            repository = recreatedRepository,
            nowEpochMillis = { 2_000 },
        )()
        assertIs<RecoverSleepSessionResult.Recovered>(recovered)
        assertEquals(SleepSessionStatus.SLEEPING, recovered.session.status)
    }

    @Test
    fun duplicateStartReturnsSameActiveSession() = runTransitionTest {
        val repository = StoredSleepSessionRepository(TransitionMemoryStore())
        var generatedIds = 0
        val start = StartSleepSessionUseCase(
            repository = repository,
            nowEpochMillis = { 1_000 },
            sessionIdGenerator = {
                generatedIds += 1
                "night-$generatedIds"
            },
        )

        val first = start()
        val second = start()

        assertIs<StartSleepSessionResult.Started>(first)
        assertIs<StartSleepSessionResult.AlreadyActive>(second)
        assertEquals(first.session.id, second.session.id)
        assertEquals(1, generatedIds)
    }
}

private open class TransitionMemoryStore(
    protected var snapshot: String? = null,
) : SleepSessionStore {
    override fun read(): String? = snapshot
    override fun write(value: String) { snapshot = value }
    override fun clear() { snapshot = null }
}

private class FailOnWriteStore(
    private val failOnWrite: Int,
) : TransitionMemoryStore() {
    private var writes = 0
    private var failuresEnabled = true

    override fun write(value: String) {
        writes += 1
        if (failuresEnabled && writes == failOnWrite) {
            throw IllegalStateException("simulated persistence failure")
        }
        super.write(value)
    }

    fun allowWrites() {
        failuresEnabled = false
    }
}

private class SequenceClock(
    vararg values: Long,
) {
    private val queue = ArrayDeque(values.toList())
    fun next(): Long = queue.removeFirst()
}

private fun <T> runTransitionTest(block: suspend () -> T): T {
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
