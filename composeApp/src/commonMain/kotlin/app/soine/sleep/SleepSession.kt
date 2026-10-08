package app.soine.sleep

enum class SleepState { READY, SLEEPING, FINISHED }

data class SleepSession(
    val state: SleepState = SleepState.READY,
    val startedAtEpochMillis: Long? = null,
    val endedAtEpochMillis: Long? = null,
) {
    fun start(now: Long = currentTimeMillis()): SleepSession =
        // Only the READY state can transition to SLEEPING. Calling
        // start on an in-flight or finished session is a no-op so a
        // stray CTA tap (e.g. one triggered by a timezone-change
        // re-render) cannot reset the persisted record.
        if (state != SleepState.READY) this
        else copy(state = SleepState.SLEEPING, startedAtEpochMillis = now, endedAtEpochMillis = null)

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
