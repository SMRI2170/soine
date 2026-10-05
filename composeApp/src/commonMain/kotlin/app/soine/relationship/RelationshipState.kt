package app.soine.relationship

data class RelationshipState(
    val totalCompletedSleepMillis: Long = 0,
    val completedSessions: Int = 0,
    val familiarity: Int = 0,
    val discoveredBehaviorIds: Set<String> = emptySet(),
) {
    fun completeSession(durationMillis: Long): RelationshipState {
        val safeDuration = durationMillis.coerceAtLeast(0)
        return copy(
            totalCompletedSleepMillis = totalCompletedSleepMillis + safeDuration,
            completedSessions = completedSessions + 1,
            familiarity = familiarityFor(totalCompletedSleepMillis + safeDuration, completedSessions + 1),
        )
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
