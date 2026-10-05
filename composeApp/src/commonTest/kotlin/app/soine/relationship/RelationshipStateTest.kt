package app.soine.relationship

import kotlin.test.Test
import kotlin.test.assertEquals

class RelationshipStateTest {
    @Test
    fun firstCompletedNightCreatesFamiliarity() {
        val state = RelationshipState().completeSession(8 * 3_600_000L)
        assertEquals(1, state.completedSessions)
        assertEquals(8 * 3_600_000L, state.totalCompletedSleepMillis)
        assertEquals(1, state.familiarity)
    }

    @Test
    fun negativeDurationNeverAddsProgress() {
        val state = RelationshipState().completeSession(-1)
        assertEquals(0, state.totalCompletedSleepMillis)
    }
}
