package app.soine.privacy

import app.soine.sleep.SleepSessionRepository

fun interface LocalDataClearer {
    fun clear()
}

sealed interface LocalDataDeletionResult {
    data object Deleted : LocalDataDeletionResult
    data object BlockedByActiveSession : LocalDataDeletionResult
    data class Failed(val cause: Throwable) : LocalDataDeletionResult
}

class LocalDataDeletionService(
    private val repository: SleepSessionRepository,
    private val clearers: List<LocalDataClearer>,
) {
    suspend fun deleteAll(): LocalDataDeletionResult {
        return try {
            if (repository.getActiveSession() != null) {
                LocalDataDeletionResult.BlockedByActiveSession
            } else {
                var firstFailure: Throwable? = null
                clearers.forEach { clearer ->
                    try {
                        clearer.clear()
                    } catch (cause: Throwable) {
                        if (firstFailure == null) firstFailure = cause
                    }
                }
                firstFailure?.let(LocalDataDeletionResult::Failed)
                    ?: LocalDataDeletionResult.Deleted
            }
        } catch (cause: Throwable) {
            LocalDataDeletionResult.Failed(cause)
        }
    }
}
