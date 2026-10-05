package app.soine.sleep

sealed interface RecoverSleepSessionResult {
    data object None : RecoverSleepSessionResult

    data class Recovered(
        val session: SleepSessionRecord,
    ) : RecoverSleepSessionResult

    data class Failed(
        val cause: Throwable,
    ) : RecoverSleepSessionResult
}

/**
 * Restores an unfinished session when the app starts.
 *
 * PREPARING is treated as recoverable because start persists it before the
 * final SLEEPING transition. Both PREPARING and SLEEPING are normalized to a
 * SLEEPING record with source=RECOVERED.
 */
class RecoverSleepSessionUseCase(
    private val repository: SleepSessionRepository,
    private val nowEpochMillis: () -> Long = ::currentTimeMillis,
) {
    suspend operator fun invoke(): RecoverSleepSessionResult {
        return try {
            val active = repository.getActiveSession()
                ?: return RecoverSleepSessionResult.None

            val recovered = active.copy(
                status = SleepSessionStatus.SLEEPING,
                source = SleepSessionSource.RECOVERED,
                updatedAtEpochMillis = nowEpochMillis().coerceAtLeast(active.updatedAtEpochMillis),
            )

            if (recovered != active) {
                repository.saveSession(recovered)
            }

            RecoverSleepSessionResult.Recovered(recovered)
        } catch (cause: Throwable) {
            RecoverSleepSessionResult.Failed(cause)
        }
    }
}
