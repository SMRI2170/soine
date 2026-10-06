package app.soine.relationship

enum class RelationshipMilestone(
    val id: String,
    val thresholdMillis: Long,
) {
    HOURS_100("hours-100", 100L * 3_600_000L),
    HOURS_300("hours-300", 300L * 3_600_000L),
    HOURS_500("hours-500", 500L * 3_600_000L),
}

sealed interface RelationshipProgressEvent {
    data class MilestoneReached(
        val milestone: RelationshipMilestone,
    ) : RelationshipProgressEvent
}

data class RelationshipProgressResult(
    val state: RelationshipState,
    val events: List<RelationshipProgressEvent>,
)

/**
 * Applies completed-session progression and reports only newly crossed
 * relationship milestones. Session IDs remain the idempotency boundary.
 */
object RelationshipProgression {
    fun completeSession(
        state: RelationshipState,
        sessionId: String,
        durationMillis: Long,
    ): RelationshipProgressResult {
        if (sessionId in state.processedSessionIds) {
            return RelationshipProgressResult(state, emptyList())
        }

        val next = state.completeSession(sessionId, durationMillis)
        val newlyReached = RelationshipMilestone.entries.filter { milestone ->
            state.totalCompletedSleepMillis < milestone.thresholdMillis &&
                next.totalCompletedSleepMillis >= milestone.thresholdMillis &&
                milestone.id !in state.achievedMilestoneIds
        }

        if (newlyReached.isEmpty()) {
            return RelationshipProgressResult(next, emptyList())
        }

        val finalState = next.copy(
            achievedMilestoneIds = next.achievedMilestoneIds + newlyReached.map { it.id },
        )
        return RelationshipProgressResult(
            state = finalState,
            events = newlyReached.map(RelationshipProgressEvent::MilestoneReached),
        )
    }
}
