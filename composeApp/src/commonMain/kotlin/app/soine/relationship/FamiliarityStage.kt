package app.soine.relationship

enum class FamiliarityStage(
    val persistedValue: Int,
) {
    NEW(0),
    WARMING_UP(1),
    FAMILIAR(2),
    CLOSE(3);

    companion object {
        fun fromPersistedValue(value: Int): FamiliarityStage =
            entries.firstOrNull { it.persistedValue == value }
                ?: error("Unknown familiarity stage value: " + value)
    }
}

/**
 * Relationship-stage rules are explicitly versioned so persisted progress can
 * be re-evaluated when thresholds evolve without exposing XP-like numbers.
 *
 * Version 2 intentionally requires both repeated nights and time together for
 * later stages. A single unusually long session can therefore never jump the
 * relationship directly to FAMILIAR or CLOSE.
 */
object FamiliarityPolicy {
    const val CURRENT_VERSION: Int = 2

    private const val HOUR_MILLIS = 3_600_000L
    private const val FAMILIAR_MIN_SESSIONS = 7
    private const val FAMILIAR_MIN_MILLIS = 24L * HOUR_MILLIS
    private const val CLOSE_MIN_SESSIONS = 30
    private const val CLOSE_MIN_MILLIS = 100L * HOUR_MILLIS

    fun stageFor(
        totalCompletedSleepMillis: Long,
        completedSessions: Int,
    ): FamiliarityStage {
        val safeMillis = totalCompletedSleepMillis.coerceAtLeast(0)
        val safeSessions = completedSessions.coerceAtLeast(0)
        return when {
            safeSessions >= CLOSE_MIN_SESSIONS && safeMillis >= CLOSE_MIN_MILLIS ->
                FamiliarityStage.CLOSE
            safeSessions >= FAMILIAR_MIN_SESSIONS && safeMillis >= FAMILIAR_MIN_MILLIS ->
                FamiliarityStage.FAMILIAR
            safeSessions >= 1 -> FamiliarityStage.WARMING_UP
            else -> FamiliarityStage.NEW
        }
    }
}
