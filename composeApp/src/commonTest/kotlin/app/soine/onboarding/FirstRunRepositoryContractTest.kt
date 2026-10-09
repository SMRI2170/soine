package app.soine.onboarding

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Smoke test for the [FirstRunRepository] contract.
 *
 * The contract:
 *
 *   - [read] returns a stable value across calls. A fresh
 *     repository returns the NOT_STARTED default.
 *   - [markCompleted] / [markSkipped] persist the new end state
 *     synchronously so the next [read] reflects it.
 *   - [resetForReplay] flips the state back to NOT_STARTED so the
 *     next launch surfaces the onboarding flow again.
 *   - The platform-agnostic [NoOpFirstRunRepository] honours the
 *     same contract so UI tests can swap implementations freely.
 *
 * The [NoOpFirstRunRepository] is exercised here; the platform
 * adapters (`AndroidFirstRunRepository`, `IosFirstRunRepository`)
 * are exercised through instrumented tests when a device or
 * simulator is available, and through the local-build smoke run
 * that the main merge flow performs.
 */
class FirstRunRepositoryContractTest {

    @Test
    fun freshRepositoryReportsNotStarted() {
        val repository = NoOpFirstRunRepository
        // Reset so other tests in the same JVM do not leak state.
        repository.resetForReplay()

        val state = repository.read()

        assertEquals(FirstRunState.EndState.NOT_STARTED, state.endState)
        assertTrue(state.isFirstRun)
    }

    @Test
    fun markCompletedFlipsToCompleted() {
        val repository = NoOpFirstRunRepository
        repository.resetForReplay()

        repository.markCompleted()

        val state = repository.read()
        assertEquals(FirstRunState.EndState.COMPLETED, state.endState)
        assertFalse(state.isFirstRun)
    }

    @Test
    fun markSkippedFlipsToSkipped() {
        val repository = NoOpFirstRunRepository
        repository.resetForReplay()

        repository.markSkipped()

        val state = repository.read()
        assertEquals(FirstRunState.EndState.SKIPPED, state.endState)
        assertFalse(state.isFirstRun)
    }

    @Test
    fun resetForReplayReturnsToNotStarted() {
        val repository = NoOpFirstRunRepository
        repository.markCompleted()
        assertEquals(FirstRunState.EndState.COMPLETED, repository.read().endState)

        repository.resetForReplay()

        assertEquals(FirstRunState.EndState.NOT_STARTED, repository.read().endState)
    }

    @Test
    fun freshStateExposesCurrentVersion() {
        val state = FirstRunState.FRESH
        assertEquals(FirstRunState.CURRENT_VERSION, state.version)
        assertEquals(1, FirstRunState.CURRENT_VERSION)
    }

    @Test
    fun readIsStableAcrossCalls() {
        val repository = NoOpFirstRunRepository
        repository.markCompleted()

        val first = repository.read()
        val second = repository.read()

        assertEquals(first, second)
    }
}
