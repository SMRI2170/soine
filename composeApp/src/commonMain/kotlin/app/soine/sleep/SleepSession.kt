package app.soine.sleep

enum class SleepState { READY, SLEEPING, FINISHED }

data class SleepSession(
    val state: SleepState = SleepState.READY,
    val startedAtEpochMillis: Long? = null,
    val endedAtEpochMillis: Long? = null,
) {
    fun start(now: Long = currentTimeMillis()): SleepSession =
        copy(state = SleepState.SLEEPING, startedAtEpochMillis = now, endedAtEpochMillis = null)

    fun finish(now: Long = currentTimeMillis()): SleepSession =
        if (state != SleepState.SLEEPING) this
        else copy(state = SleepState.FINISHED, endedAtEpochMillis = now)

    fun durationMillis(): Long? {
        val start = startedAtEpochMillis ?: return null
        val end = endedAtEpochMillis ?: return null
        return (end - start).coerceAtLeast(0)
    }
}

expect fun currentTimeMillis(): Long
