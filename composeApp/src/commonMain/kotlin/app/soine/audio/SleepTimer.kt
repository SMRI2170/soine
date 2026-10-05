package app.soine.audio

enum class SleepTimerPreset(val minutes: Int) {
    MINUTES_30(30),
    MINUTES_60(60),
    MINUTES_90(90),
}

data class SleepTimerState(
    val stopAtEpochMillis: Long? = null,
    val durationMinutes: Int? = null,
) {
    fun remainingMillis(nowEpochMillis: Long): Long =
        ((stopAtEpochMillis ?: nowEpochMillis) - nowEpochMillis).coerceAtLeast(0)

    fun isExpired(nowEpochMillis: Long): Boolean =
        stopAtEpochMillis?.let { nowEpochMillis >= it } ?: false
}

class SleepTimer(
    private val audio: AmbientAudioController,
    private val now: () -> Long,
) {
    var state: SleepTimerState = SleepTimerState()
        private set

    fun start(preset: SleepTimerPreset) = startCustom(preset.minutes)

    fun startCustom(minutes: Int) {
        require(minutes > 0) { "Timer duration must be positive." }
        val stopAt = safeDeadline(now(), minutes)
        state = SleepTimerState(stopAt, minutes)
        audio.setStopAt(stopAt)
    }

    fun restore(stopAtEpochMillis: Long?) {
        state = SleepTimerState(stopAtEpochMillis)
        audio.setStopAt(stopAtEpochMillis)
        tick()
    }

    fun cancel() {
        state = SleepTimerState()
        audio.setStopAt(null)
    }

    /**
     * Call from the platform scheduler/lifecycle. Expiry stops audio only;
     * it intentionally has no dependency on the sleep-session repository.
     */
    fun tick(): Boolean {
        if (!state.isExpired(now())) return false
        audio.stop()
        state = SleepTimerState()
        return true
    }

    private fun safeDeadline(start: Long, minutes: Int): Long {
        val delta = minutes.toLong() * 60_000L
        return if (Long.MAX_VALUE - start < delta) Long.MAX_VALUE else start + delta
    }
}
