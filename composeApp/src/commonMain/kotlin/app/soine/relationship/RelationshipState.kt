package app.soine.relationship

data class RelationshipState(
    val totalCompletedSleepMillis: Long = 0,
    val completedSessions: Int = 0,
    /** Persisted enum value. Prefer [familiarityStage] in domain/UI code. */
    val familiarity: Int = FamiliarityStage.NEW.persistedValue,
    val familiarityRuleVersion: Int = FamiliarityPolicy.CURRENT_VERSION,
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
        require(familiarityRuleVersion > 0) { "Familiarity rule version must be positive." }
        FamiliarityStage.fromPersistedValue(familiarity)
    }

    val familiarityStage: FamiliarityStage
        get() = FamiliarityStage.fromPersistedValue(familiarity)

    fun completeSession(sessionId: String, durationMillis: Long): RelationshipState {
        require(sessionId.isNotBlank()) { "Session id must not be blank." }
        if (sessionId in processedSessionIds) return this

        val safeDuration = durationMillis.coerceAtLeast(0)
        val nextSessions = completedSessions + 1
        val nextTotal = totalCompletedSleepMillis + safeDuration
        val nextStage = FamiliarityPolicy.stageFor(nextTotal, nextSessions)
        return copy(
            totalCompletedSleepMillis = nextTotal,
            completedSessions = nextSessions,
            familiarity = nextStage.persistedValue,
            familiarityRuleVersion = FamiliarityPolicy.CURRENT_VERSION,
            processedSessionIds = processedSessionIds + sessionId,
            schemaVersion = CURRENT_SCHEMA_VERSION,
        )
    }

    @Deprecated(
        message = "Use completeSession(sessionId, durationMillis) so progression is retry-safe.",
        level = DeprecationLevel.ERROR,
    )
    fun completeSession(durationMillis: Long): RelationshipState =
        error("A session id is required for exactly-once progression.")

    companion object {
        const val CURRENT_SCHEMA_VERSION: Int = 2
    }
}
