package app.soine.relationship

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class RelationshipStateTest {
    private val hour = 3_600_000L

    @Test
    fun firstCompletedNightStartsWarmingUp() {
        val state = RelationshipState().completeSession("night-1", 8 * hour)
        assertEquals(1, state.completedSessions)
        assertEquals(8 * hour, state.totalCompletedSleepMillis)
        assertEquals(FamiliarityStage.WARMING_UP, state.familiarityStage)
        assertEquals(setOf("night-1"), state.processedSessionIds)
    }

    @Test
    fun sevenNightsWithoutEnoughTimeDoNotReachFamiliar() {
        val state = RelationshipState(
            totalCompletedSleepMillis = 20 * hour,
            completedSessions = 6,
            familiarity = FamiliarityStage.WARMING_UP.persistedValue,
        ).completeSession("night-7", hour)

        assertEquals(FamiliarityStage.WARMING_UP, state.familiarityStage)
    }

    @Test
    fun familiarRequiresBothSevenNightsAnd24Hours() {
        val state = RelationshipState(
            totalCompletedSleepMillis = 23 * hour,
            completedSessions = 6,
            familiarity = FamiliarityStage.WARMING_UP.persistedValue,
        ).completeSession("night-7", hour)

        assertEquals(FamiliarityStage.FAMILIAR, state.familiarityStage)
    }

    @Test
    fun oneVeryLongNightCannotSkipToClose() {
        val state = RelationshipState().completeSession("very-long", 500 * hour)

        assertEquals(FamiliarityStage.WARMING_UP, state.familiarityStage)
    }

    @Test
    fun closeRequiresBoth30NightsAnd100Hours() {
        val state = RelationshipState(
            totalCompletedSleepMillis = 99 * hour,
            completedSessions = 29,
            familiarity = FamiliarityStage.FAMILIAR.persistedValue,
        ).completeSession("night-30", hour)

        assertEquals(FamiliarityStage.CLOSE, state.familiarityStage)
    }

    @Test
    fun duplicateCompletionIsIgnored() {
        val first = RelationshipState().completeSession("night-1", 8 * hour)
        val retry = first.completeSession("night-1", 9 * hour)

        assertSame(first, retry)
        assertEquals(1, retry.completedSessions)
        assertEquals(8 * hour, retry.totalCompletedSleepMillis)
    }

    @Test
    fun processedIdSurvivesStateRecreation() {
        val persisted = RelationshipState(
            totalCompletedSleepMillis = 8 * hour,
            completedSessions = 1,
            familiarity = FamiliarityStage.WARMING_UP.persistedValue,
            processedSessionIds = setOf("night-1"),
        )

        val afterRestart = persisted.completeSession("night-1", 8 * hour)
        assertEquals(persisted, afterRestart)
    }

    @Test
    fun retryWithDifferentSessionIsApplied() {
        val first = RelationshipState().completeSession("night-1", 1_000)
        val second = first.completeSession("night-2", 2_000)

        assertEquals(2, second.completedSessions)
        assertEquals(3_000, second.totalCompletedSleepMillis)
        assertEquals(setOf("night-1", "night-2"), second.processedSessionIds)
    }

    @Test
    fun negativeDurationDoesNotAddTimeButSessionIsProcessedOnce() {
        val first = RelationshipState().completeSession("night-1", -1)
        val retry = first.completeSession("night-1", 1_000)

        assertEquals(0, retry.totalCompletedSleepMillis)
        assertEquals(1, retry.completedSessions)
        assertEquals(setOf("night-1"), retry.processedSessionIds)
        assertEquals(FamiliarityStage.WARMING_UP, retry.familiarityStage)
    }
}
