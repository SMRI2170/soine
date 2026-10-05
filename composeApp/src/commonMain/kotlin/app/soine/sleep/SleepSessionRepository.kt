package app.soine.sleep

/**
 * Persistence boundary for sleep sessions.
 *
 * The contract intentionally uses only common Kotlin/domain types so the
 * Android and iOS storage implementations can evolve independently.
 */
interface SleepSessionRepository {
    /**
     * Returns the single active session, or null when no session is running.
     */
    suspend fun getActiveSession(): SleepSessionRecord?

    /**
     * Inserts or updates a session.
     *
     * Implementations must never allow more than one active session.
     */
    suspend fun saveSession(session: SleepSessionRecord)

    /**
     * Completes the session identified by [sessionId].
     *
     * Implementations should make this operation idempotent: completing an
     * already-completed session must return the persisted completed record
     * without creating a duplicate.
     *
     * Returns null when [sessionId] does not exist.
     */
    suspend fun completeSession(
        sessionId: String,
        endedAtEpochMillis: Long,
        updatedAtEpochMillis: Long,
    ): SleepSessionRecord?

    /**
     * Returns completed sessions, newest first.
     */
    suspend fun getCompletedSessions(): List<SleepSessionRecord>
}

enum class SleepSessionStatus {
    PREPARING,
    SLEEPING,
    COMPLETED,
}

enum class SleepSessionSource {
    MANUAL,
    RECOVERED,
}

/**
 * Storage-facing representation of a sleep session.
 *
 * This remains a domain model: platform database/SDK types must be converted
 * at the adapter boundary rather than leaking into commonMain.
 */
data class SleepSessionRecord(
    val id: String,
    val startedAtEpochMillis: Long,
    val endedAtEpochMillis: Long? = null,
    val status: SleepSessionStatus,
    val source: SleepSessionSource = SleepSessionSource.MANUAL,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
) {
    init {
        require(id.isNotBlank()) { "Sleep session id must not be blank." }
        require(schemaVersion > 0) { "Schema version must be positive." }
    }

    val isActive: Boolean
        get() = status != SleepSessionStatus.COMPLETED

    companion object {
        const val CURRENT_SCHEMA_VERSION: Int = 1
    }
}
