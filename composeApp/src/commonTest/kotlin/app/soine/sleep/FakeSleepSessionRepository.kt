package app.soine.sleep

/**
 * In-memory repository for commonMain use-case tests.
 *
 * Keep this deliberately small: it mirrors the repository contract rather
 * than platform persistence behavior.
 */
internal class FakeSleepSessionRepository(
    initialSessions: List<SleepSessionRecord> = emptyList(),
) : SleepSessionRepository {

    private val sessions = linkedMapOf<String, SleepSessionRecord>().apply {
        initialSessions.forEach { put(it.id, it) }
    }

    override suspend fun getActiveSession(): SleepSessionRecord? =
        sessions.values.firstOrNull { it.isActive }

    override suspend fun saveSession(session: SleepSessionRecord) {
        val otherActive = sessions.values.firstOrNull {
            it.id != session.id && it.isActive
        }

        require(!session.isActive || otherActive == null) {
            "Only one active sleep session is allowed."
        }

        sessions[session.id] = session
    }

    override suspend fun completeSession(
        sessionId: String,
        endedAtEpochMillis: Long,
        updatedAtEpochMillis: Long,
    ): SleepSessionRecord? {
        val current = sessions[sessionId] ?: return null
        if (current.status == SleepSessionStatus.COMPLETED) return current

        val completed = current.copy(
            endedAtEpochMillis = endedAtEpochMillis,
            status = SleepSessionStatus.COMPLETED,
            updatedAtEpochMillis = updatedAtEpochMillis,
        )
        sessions[sessionId] = completed
        return completed
    }

    override suspend fun getCompletedSessions(): List<SleepSessionRecord> =
        sessions.values
            .asSequence()
            .filter { it.status == SleepSessionStatus.COMPLETED }
            .sortedByDescending { it.startedAtEpochMillis }
            .toList()
}
