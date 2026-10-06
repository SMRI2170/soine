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
            totalCompletedSleepMillis = 30L * 3_600_000L,
            completedSessions = 8,
            familiarity = FamiliarityStage.FAMILIAR.persistedValue,
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
        repository.save(
            RelationshipState(
                completedSessions = 1,
                familiarity = FamiliarityStage.WARMING_UP.persistedValue,
            )
        )
        val next = RelationshipState(
            totalCompletedSleepMillis = 3_600_000L,
            completedSessions = 2,
            familiarity = FamiliarityStage.WARMING_UP.persistedValue,
            discoveredBehaviorIds = setOf("curl-up"),
        )

        repository.save(next)

        assertEquals(next, repository.get())
    }

    @Test fun v1SnapshotMigratesToBalancedCurrentStageRules() = runSuspend {
        val legacy = listOf(
            "V\t1",
            "S\t1\t360000000\t1\t2",
            "P\t6e696768742d31",
        ).joinToString("\n")
        val repository = StoredCompanionProgressRepository(MemoryRelationshipStore(legacy))

        val migrated = repository.get()

        assertEquals(RelationshipState.CURRENT_SCHEMA_VERSION, migrated.schemaVersion)
        assertEquals(FamiliarityPolicy.CURRENT_VERSION, migrated.familiarityRuleVersion)
        assertEquals(FamiliarityStage.WARMING_UP, migrated.familiarityStage)
        assertEquals(setOf("night-1"), migrated.processedSessionIds)
    }

    @Test fun currentSnapshotRecomputesStageUsingCurrentRuleVersion() = runSuspend {
        val raw = listOf(
            "V\t2",
            "S\t2\t86400000\t7\t1\t1",
        ).joinToString("\n")
        val repository = StoredCompanionProgressRepository(MemoryRelationshipStore(raw))

        val migrated = repository.get()

        assertEquals(FamiliarityStage.FAMILIAR, migrated.familiarityStage)
        assertEquals(FamiliarityPolicy.CURRENT_VERSION, migrated.familiarityRuleVersion)
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
