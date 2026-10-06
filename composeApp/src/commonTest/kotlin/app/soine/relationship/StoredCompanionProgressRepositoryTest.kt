package app.soine.relationship

import kotlin.coroutines.*
import kotlin.test.*

class StoredCompanionProgressRepositoryTest {
    @Test fun missingSnapshotReturnsDefaultState() = runSuspend {
        val repository = StoredCompanionProgressRepository(MemoryRelationshipStore())
        assertEquals(RelationshipState(), repository.get())
    }

    @Test fun roundTripPreservesProgressBehaviorsSessionsAndMilestones() = runSuspend {
        val store = MemoryRelationshipStore()
        val repository = StoredCompanionProgressRepository(store)
        val state = RelationshipState(
            totalCompletedSleepMillis = 12_345_678L,
            completedSessions = 4,
            familiarity = 1,
            discoveredBehaviorIds = setOf("sleep-close", "ear-twitch"),
            achievedMilestoneIds = setOf("first-night", "100-hours"),
            processedSessionIds = setOf("night-1", "夜-2"),
        )

        repository.save(state)

        assertEquals(state, repository.get())
    }

    @Test fun laterSaveReplacesPreviousSnapshot() = runSuspend {
        val store = MemoryRelationshipStore()
        val repository = StoredCompanionProgressRepository(store)
        repository.save(RelationshipState(completedSessions = 1, familiarity = 1))
        val next = RelationshipState(
            totalCompletedSleepMillis = 3_600_000L,
            completedSessions = 2,
            familiarity = 1,
            discoveredBehaviorIds = setOf("curl-up"),
        )

        repository.save(next)

        assertEquals(next, repository.get())
    }

    @Test fun corruptSnapshotFailsWithoutClearingOriginalValue() = runSuspend {
        val store = MemoryRelationshipStore("broken")
        val repository = StoredCompanionProgressRepository(store)

        assertFailsWith<RelationshipStateStorageCorruptedException> { repository.get() }
        assertEquals("broken", store.value)
    }
}

private class MemoryRelationshipStore(
    var value: String? = null,
) : RelationshipStateStore {
    override fun read(): String? = value
    override fun write(value: String) { this.value = value }
    override fun clear() { value = null }
}

private fun runSuspend(block: suspend () -> Unit) {
    block.startCoroutine(object : Continuation<Unit> {
        override val context: CoroutineContext = EmptyCoroutineContext
        override fun resumeWith(result: Result<Unit>) = result.getOrThrow()
    })
}
