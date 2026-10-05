package app.soine.sleep

import kotlin.random.Random

sealed interface StartSleepSessionResult {
    data class Started(
        val session: SleepSessionRecord,
    ) : StartSleepSessionResult

    data class AlreadyActive(
        val session: SleepSessionRecord,
    ) : StartSleepSessionResult

    data class Failed(
        val cause: Throwable,
    ) : StartSleepSessionResult
}

/**
 * Starts a sleep session without depending on platform persistence.
 *
 * Critical ordering:
 * 1. persist PREPARING
 * 2. transition to SLEEPING
 * 3. persist SLEEPING
 *
 * This guarantees that a process failure cannot happen after the app has
 * semantically started sleeping without at least a recoverable record.
 */
class StartSleepSessionUseCase(
    private val repository: SleepSessionRepository,
    private val nowEpochMillis: () -> Long = ::currentTimeMillis,
    private val sessionIdGenerator: () -> String = ::defaultSleepSessionId,
) {
    suspend operator fun invoke(): StartSleepSessionResult {
        return try {
            repository.getActiveSession()?.let {
                return StartSleepSessionResult.AlreadyActive(it)
            }

            val now = nowEpochMillis()
            val preparing = SleepSessionRecord(
                id = sessionIdGenerator(),
                startedAtEpochMillis = now,
                status = SleepSessionStatus.PREPARING,
                source = SleepSessionSource.MANUAL,
                createdAtEpochMillis = now,
                updatedAtEpochMillis = now,
            )

            repository.saveSession(preparing)

            val sleeping = preparing.copy(
                status = SleepSessionStatus.SLEEPING,
                updatedAtEpochMillis = nowEpochMillis().coerceAtLeast(now),
            )
            repository.saveSession(sleeping)

            StartSleepSessionResult.Started(sleeping)
        } catch (cause: Throwable) {
            StartSleepSessionResult.Failed(cause)
        }
    }
}

private fun defaultSleepSessionId(): String {
    val now = currentTimeMillis().toString(36)
    val random = Random.nextLong().toULong().toString(36)
    return "sleep-$now-$random"
}
