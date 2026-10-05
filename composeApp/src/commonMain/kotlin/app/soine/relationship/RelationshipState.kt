package app.soine.relationship

data class RelationshipState(
    val totalCompletedSleepMillis: Long = 0,
    val completedSessions: Int = 0,
    val familiarity: Int = 0,
    val discoveredBehaviorIds: Set<String> = emptySet(),
    /** Persisted idempotency keys for sessions already applied to progression. */
    val processedSessionIds: Set<String> = emptySet(),
) {
    /**
     * Applies one completed sleep session exactly once.
     *
     * The session id is the idempotency key, so UI double taps, process
     * recreation and retries after a successful persisted update cannot add
     * progression twice.
     */
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
