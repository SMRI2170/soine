package app.soine.relationship

data class RelationshipState(
    val totalCompletedSleepMillis: Long = 0,
    val completedSessions: Int = 0,
    val familiarity: Int = 0,
    val discoveredBehaviorIds: Set<String> = emptySet(),
    val achievedMilestoneIds: Set<String> = emptySet(),
    /** Persisted idempotency keys for sessions already applied to progression. */
    val processedSessionIds: Set<String> = emptySet(),
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
) {
    init {
        require(schemaVersion > 0) { "Relationship schema version must be positive." }
        require(totalCompletedSleepMillis >= 0) { "Total completed sleep time must not be negative." }
        require(completedSessions >= 0) { "Completed sessions must not be negative." }
        require(familiarity >= 0) { "Familiarity must not be negative." }
    }

    fun completeSession(sessionId: String, durationMillis: Long): RelationshipState {
        require(sessionId.isNotBlank()) { "Session id must not be blank." }
        if (sessionId in processedSessionIds) return this

        val safeDuration = durationMillis.coerceAtLeast(0)
        val nextSessions = completedSessions + 1
        val nextTotal = totalCompletedSleepMillis + safeDuration
        return copy(
            totalCompletedSleepMillis = nextTotal,
            completedSessions = nextSessions,
            familiarity = familiarityFor(nextTotal, nextSessions),
            processedSessionIds = processedSessionIds + sessionId,
        )
    }

    @Deprecated(
        message = "Use completeSession(sessionId, durationMillis) so progression is retry-safe.",
        level = DeprecationLevel.ERROR,
    )
    fun completeSession(durationMillis: Long): RelationshipState =
        error("A session id is required for exactly-once progression.")

    companion object {
        const val CURRENT_SCHEMA_VERSION: Int = 1
    }
}

private fun familiarityFor(totalMillis: Long, sessions: Int): Int {
    val hours = totalMillis / 3_600_000
    return when {
        sessions >= 30 || hours >= 300 -> 3
        sessions >= 7 || hours >= 100 -> 2
        sessions >= 1 -> 1
        else -> 0
    }
}
