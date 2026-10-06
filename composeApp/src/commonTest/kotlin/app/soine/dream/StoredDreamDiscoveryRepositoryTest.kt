package app.soine.dream

import app.soine.night.RarityBand
import app.soine.relationship.CompanionProgressRepository
import app.soine.relationship.RelationshipState
import app.soine.sleep.SleepSessionRecord
import app.soine.sleep.SleepSessionSource
import app.soine.sleep.SleepSessionStatus
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull

class StoredDreamDiscoveryRepositoryTest {
    @Test
    fun missingSnapshotReturnsEmptyState() = runSuspend {
        val repository = StoredDreamDiscoveryRepository(MemoryDreamDiscoveryStore())

        assertEquals(DreamDiscoverySnapshot(), repository.get())
    }

    @Test
    fun roundTripPreservesDecisionDiscoveryAndNoDreamDecision() = runSuspend {
        val repository = StoredDreamDiscoveryRepository(MemoryDreamDiscoveryStore())
        val found = DreamDiscoveryDecision(
            sessionId = "night-1",
            algorithmVersion = 1,
            dreamId = "moon",
            evaluatedAtEpochMillis = 1_000L,
        )
        val discovery = DreamDiscovery(
            dreamId = "moon",
            sessionId = "night-1",
            discoveredAtEpochMillis = 1_000L,
        )
        val missed = DreamDiscoveryDecision(
            sessionId = "夜-2",
            algorithmVersion = 1,
            dreamId = null,
            evaluatedAtEpochMillis = 2_000L,
        )

        repository.saveEvaluation(found, discovery)
        repository.saveEvaluation(missed, null)

        val snapshot = repository.get()
        assertEquals(listOf(found, missed), snapshot.decisions)
        assertEquals(listOf(discovery), snapshot.discoveries)
    }

    @Test
    fun sameSessionCannotOverwriteItsFirstDecision() = runSuspend {
        val repository = StoredDreamDiscoveryRepository(MemoryDreamDiscoveryStore())
        val first = DreamDiscoveryDecision("night-1", 1, null, 1_000L)
        val retry = DreamDiscoveryDecision("night-1", 1, "moon", 2_000L)

        repository.saveEvaluation(first, null)
        val afterRetry = repository.saveEvaluation(
            retry,
            DreamDiscovery("moon", "night-1", 2_000L),
        )

        assertEquals(listOf(first), afterRetry.decisions)
        assertEquals(emptyList(), afterRetry.discoveries)
    }

    @Test
    fun corruptSnapshotFailsWithoutClearingOriginalValue() = runSuspend {
        val store = MemoryDreamDiscoveryStore("broken")
        val repository = StoredDreamDiscoveryRepository(store)

        assertFailsWith<DreamDiscoveryStorageCorruptedException> {
            repository.get()
        }
        assertEquals("broken", store.value)
    }

    @Test
    fun discoveryMustMatchItsDecision() = runSuspend {
        val repository = StoredDreamDiscoveryRepository(MemoryDreamDiscoveryStore())
        val decision = DreamDiscoveryDecision("night-1", 1, "moon", 1_000L)

        assertFailsWith<IllegalArgumentException> {
            repository.saveEvaluation(
                decision,
                DreamDiscovery("cloud", "night-1", 1_000L),
            )
        }
    }
}

class DreamDiscoveryCoordinatorTest {
    @Test
    fun completedSessionIsEvaluatedPersistedAndRetryUsesExistingDecision() = runSuspend {
        val store = MemoryDreamDiscoveryStore()
        val repository = StoredDreamDiscoveryRepository(store)
        val catalog = InMemoryDreamDefinitionRepository(
            listOf(
                DreamDefinition(
                    id = "moon",
                    title = "月の夢",
                    shortLine = "月を見ていたみたい",
                    rarity = RarityBand.COMMON,
                )
            )
        )
        val coordinator = DreamDiscoveryCoordinator(
            repository = repository,
            relationshipRepository = FixedRelationshipRepository(RelationshipState()),
            engine = DeterministicDreamDiscoveryEngine(
                repository = catalog,
                discoveryChancePercent = 100,
            ),
        )
        val session = completedSession("night-1", endedAt = 10_000L)

        val first = coordinator.evaluateIfNeeded(session)
        val retry = coordinator.evaluateIfNeeded(session)

        assertIs<DreamDiscoveryOutcome.Evaluated>(first)
        assertEquals("moon", first.discovery?.dreamId)
        assertIs<DreamDiscoveryOutcome.Existing>(retry)
        assertEquals(first.decision, retry.decision)

        val snapshot = repository.get()
        assertEquals(1, snapshot.decisions.size)
        assertEquals(1, snapshot.discoveries.size)
        assertEquals("moon", coordinator.discoveries().single().dreamId)
    }

    @Test
    fun zeroChanceStillPersistsNoDreamDecisionForIdempotency() = runSuspend {
        val repository = StoredDreamDiscoveryRepository(MemoryDreamDiscoveryStore())
        val catalog = InMemoryDreamDefinitionRepository(
            listOf(
                DreamDefinition(
                    id = "moon",
                    title = "月の夢",
                    shortLine = "月を見ていたみたい",
                    rarity = RarityBand.COMMON,
                )
            )
        )
        val coordinator = DreamDiscoveryCoordinator(
            repository = repository,
            relationshipRepository = FixedRelationshipRepository(RelationshipState()),
            engine = DeterministicDreamDiscoveryEngine(
                repository = catalog,
                discoveryChancePercent = 0,
            ),
        )

        val outcome = coordinator.evaluateIfNeeded(completedSession("night-2", 20_000L))

        assertIs<DreamDiscoveryOutcome.Evaluated>(outcome)
        assertNull(outcome.discovery)
        assertNull(outcome.decision.dreamId)
        assertEquals(1, repository.get().decisions.size)
    }
}

private class MemoryDreamDiscoveryStore(
    var value: String? = null,
) : DreamDiscoveryStore {
    override fun read(): String? = value

    override fun write(value: String) {
        this.value = value
    }

    override fun clear() {
        value = null
    }
}

private class FixedRelationshipRepository(
    private var state: RelationshipState,
) : CompanionProgressRepository {
    override suspend fun get(): RelationshipState = state

    override suspend fun save(state: RelationshipState) {
        this.state = state
    }
}

private fun completedSession(
    id: String,
    endedAt: Long,
): SleepSessionRecord = SleepSessionRecord(
    id = id,
    startedAtEpochMillis = endedAt - 3_600_000L,
    endedAtEpochMillis = endedAt,
    status = SleepSessionStatus.COMPLETED,
    source = SleepSessionSource.MANUAL,
    createdAtEpochMillis = endedAt - 3_600_000L,
    updatedAtEpochMillis = endedAt,
)

private fun <T> runSuspend(block: suspend () -> T): T {
    var outcome: Result<T>? = null
    block.startCoroutine(
        object : Continuation<T> {
            override val context: CoroutineContext = EmptyCoroutineContext

            override fun resumeWith(result: Result<T>) {
                outcome = result
            }
        }
    )
    return checkNotNull(outcome) {
        "Test coroutine suspended unexpectedly; this helper only supports synchronous suspend code."
    }.getOrThrow()
}
