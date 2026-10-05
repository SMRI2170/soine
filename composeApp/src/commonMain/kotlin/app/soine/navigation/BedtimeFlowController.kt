package app.soine.navigation

import app.soine.sleep.*

sealed interface BedtimeDestination {
    data object Loading : BedtimeDestination
    data object Bedtime : BedtimeDestination
    data class Sleeping(val session: SleepSessionRecord) : BedtimeDestination
    data class Morning(val session: SleepSessionRecord) : BedtimeDestination
    data class Error(val cause: Throwable) : BedtimeDestination
}

class BedtimeFlowController(
    private val repository: SleepSessionRepository,
    private val startSleep: StartSleepSessionUseCase = StartSleepSessionUseCase(repository),
    private val finishSleep: FinishSleepSessionUseCase = FinishSleepSessionUseCase(repository),
    private val recoverSleep: RecoverSleepSessionUseCase = RecoverSleepSessionUseCase(repository),
) {
    suspend fun initialDestination(): BedtimeDestination =
        when (val recovered = recoverSleep()) {
            RecoverSleepSessionResult.None -> BedtimeDestination.Bedtime
            is RecoverSleepSessionResult.Recovered -> BedtimeDestination.Sleeping(recovered.session)
            is RecoverSleepSessionResult.Failed -> BedtimeDestination.Error(recovered.cause)
        }

    suspend fun start(): BedtimeDestination =
        when (val result = startSleep()) {
            is StartSleepSessionResult.Started -> BedtimeDestination.Sleeping(result.session)
            is StartSleepSessionResult.AlreadyActive -> BedtimeDestination.Sleeping(result.session)
            is StartSleepSessionResult.Failed -> BedtimeDestination.Error(result.cause)
        }

    suspend fun finish(): BedtimeDestination =
        when (val result = finishSleep()) {
            is FinishSleepSessionResult.Finished -> BedtimeDestination.Morning(result.session)
            FinishSleepSessionResult.NoActiveSession -> latestCompletedOrBedtime()
            is FinishSleepSessionResult.Failed -> BedtimeDestination.Error(result.cause)
        }

    suspend fun latestCompletedOrBedtime(): BedtimeDestination =
        repository.getCompletedSessions().firstOrNull()
            ?.let(BedtimeDestination::Morning)
            ?: BedtimeDestination.Bedtime

    fun dismissMorning(): BedtimeDestination = BedtimeDestination.Bedtime
}
