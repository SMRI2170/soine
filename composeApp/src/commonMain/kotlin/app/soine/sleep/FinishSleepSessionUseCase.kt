package app.soine.sleep

sealed interface FinishSleepSessionResult {
    data class Finished(
        val session: SleepSessionRecord,
    ) : FinishSleepSessionResult

    data object NoActiveSession : FinishSleepSessionResult

    data class Failed(
        val cause: Throwable,
    ) : FinishSleepSessionResult
}

/**
 * Completes the current active sleep session.
 *
 * Completion is delegated to the repository so the persisted state is the
 * source of truth. Calling this use case again after a successful completion
 * returns NoActiveSession and must not create another completed session.
 */
class FinishSleepSessionUseCase(
    private val repository: SleepSessionRepository,
    private val nowEpochMillis: () -> Long = ::currentTimeMillis,
) {
    suspend operator fun invoke(): FinishSleepSessionResult {
        return try {
            val active = repository.getActiveSession()
                ?: return FinishSleepSessionResult.NoActiveSession

            val now = nowEpochMillis()
            val completed = repository.completeSession(
                sessionId = active.id,
                endedAtEpochMillis = now.coerceAtLeast(active.startedAtEpochMillis),
                updatedAtEpochMillis = now.coerceAtLeast(active.updatedAtEpochMillis),
            ) ?: return FinishSleepSessionResult.NoActiveSession

            FinishSleepSessionResult.Finished(completed)
        } catch (cause: Throwable) {
            FinishSleepSessionResult.Failed(cause)
        }
    }
}
