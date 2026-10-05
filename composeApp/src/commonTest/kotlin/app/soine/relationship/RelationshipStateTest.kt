package app.soine.relationship

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class RelationshipStateTest {
    @Test
    fun firstCompletedNightCreatesFamiliarity() {
        val state = RelationshipState().completeSession("night-1", 8 * 3_600_000L)
        assertEquals(1, state.completedSessions)
        assertEquals(8 * 3_600_000L, state.totalCompletedSleepMillis)
        assertEquals(1, state.familiarity)
        assertEquals(setOf("night-1"), state.processedSessionIds)
    }

    @Test
    fun duplicateCompletionIsIgnored() {
        val first = RelationshipState().completeSession("night-1", 8 * 3_600_000L)
        val retry = first.completeSession("night-1", 9 * 3_600_000L)

        assertSame(first, retry)
        assertEquals(1, retry.completedSessions)
        assertEquals(8 * 3_600_000L, retry.totalCompletedSleepMillis)
    }

    @Test
    fun processedIdSurvivesStateRecreation() {
        val persisted = RelationshipState(
            totalCompletedSleepMillis = 8 * 3_600_000L,
            completedSessions = 1,
            familiarity = 1,
            processedSessionIds = setOf("night-1"),
        )

        val afterRestart = persisted.completeSession("night-1", 8 * 3_600_000L)
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
    }
}
