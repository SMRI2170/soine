package app.soine.relationship

import kotlin.test.*

class RelationshipProgressionTest {
    private val hour = 3_600_000L

    @Test fun crossing100HoursEmitsMilestoneOnce() {
        val before = RelationshipState(
            totalCompletedSleepMillis = 99 * hour,
            completedSessions = 20,
            familiarity = 2,
        )

        val first = RelationshipProgression.completeSession(before, "night-100", hour)
        val retry = RelationshipProgression.completeSession(first.state, "night-100", hour)

        assertEquals(100 * hour, first.state.totalCompletedSleepMillis)
        assertEquals(setOf(RelationshipMilestone.HOURS_100.id), first.state.achievedMilestoneIds)
        assertEquals(
            listOf(RelationshipProgressEvent.MilestoneReached(RelationshipMilestone.HOURS_100)),
            first.events,
        )
        assertSame(first.state, retry.state)
        assertTrue(retry.events.isEmpty())
    }

    @Test fun crossing300HoursEmitsOnly300When100AlreadyAchieved() {
        val before = RelationshipState(
            totalCompletedSleepMillis = 299 * hour,
            completedSessions = 40,
            familiarity = 3,
            achievedMilestoneIds = setOf(RelationshipMilestone.HOURS_100.id),
        )

        val result = RelationshipProgression.completeSession(before, "night-300", hour)

        assertEquals(
            listOf(RelationshipProgressEvent.MilestoneReached(RelationshipMilestone.HOURS_300)),
            result.events,
        )
        assertEquals(
            setOf(RelationshipMilestone.HOURS_100.id, RelationshipMilestone.HOURS_300.id),
            result.state.achievedMilestoneIds,
        )
    }

    @Test fun crossing500HoursEmits500() {
        val before = RelationshipState(
            totalCompletedSleepMillis = 499 * hour,
            completedSessions = 70,
            familiarity = 3,
            achievedMilestoneIds = setOf(
                RelationshipMilestone.HOURS_100.id,
                RelationshipMilestone.HOURS_300.id,
            ),
        )

        val result = RelationshipProgression.completeSession(before, "night-500", hour)

        assertEquals(
            listOf(RelationshipProgressEvent.MilestoneReached(RelationshipMilestone.HOURS_500)),
            result.events,
        )
    }

    @Test fun aLargeCrossingEmitsEachUnseenMilestoneInOrder() {
        val result = RelationshipProgression.completeSession(
            RelationshipState(),
            "imported-night",
            501 * hour,
        )

        assertEquals(
            listOf(
                RelationshipMilestone.HOURS_100,
                RelationshipMilestone.HOURS_300,
                RelationshipMilestone.HOURS_500,
            ),
            result.events.map { (it as RelationshipProgressEvent.MilestoneReached).milestone },
        )
    }

    @Test fun negativeDurationDoesNotCreateTimeMilestone() {
        val result = RelationshipProgression.completeSession(
            RelationshipState(
                totalCompletedSleepMillis = 99 * hour,
                completedSessions = 12,
                familiarity = 2,
            ),
            "negative-night",
            -hour,
        )

        assertTrue(result.events.isEmpty())
        assertEquals(99 * hour, result.state.totalCompletedSleepMillis)
        assertEquals(13, result.state.completedSessions)
    }

    @Test fun storedMilestoneNeverRefiresEvenIfStateIsNearThreshold() {
        val before = RelationshipState(
            totalCompletedSleepMillis = 99 * hour,
            completedSessions = 20,
            familiarity = 2,
            achievedMilestoneIds = setOf(RelationshipMilestone.HOURS_100.id),
        )

        val result = RelationshipProgression.completeSession(before, "night-next", 2 * hour)

        assertTrue(result.events.isEmpty())
        assertEquals(setOf(RelationshipMilestone.HOURS_100.id), result.state.achievedMilestoneIds)
    }
}
