package app.soine.audio

class PersistentSleepTimer(
    private val timer: SleepTimer,
    private val preferencesStore: AmbientAudioPreferencesStore,
) {
    val state: SleepTimerState get() = timer.state

    fun restore() {
        val preferences = preferencesStore.read()
        timer.restore(preferences.timerStopAtEpochMillis)
        if (timer.state.stopAtEpochMillis == null && preferences.timerStopAtEpochMillis != null) {
            preferencesStore.write(preferences.copy(timerStopAtEpochMillis = null))
        }
    }

    fun start(preset: SleepTimerPreset) {
        timer.start(preset)
        val current = preferencesStore.read()
        preferencesStore.write(
            current.copy(
                timerPreset = preset,
                timerStopAtEpochMillis = timer.state.stopAtEpochMillis,
            )
        )
    }

    fun startCustom(minutes: Int) {
        timer.startCustom(minutes)
        val current = preferencesStore.read()
        preferencesStore.write(
            current.copy(
                timerPreset = null,
                timerStopAtEpochMillis = timer.state.stopAtEpochMillis,
            )
        )
    }

    fun cancel() {
        timer.cancel()
        val current = preferencesStore.read()
        preferencesStore.write(current.copy(timerStopAtEpochMillis = null))
    }

    fun tick(): Boolean {
        val expired = timer.tick()
        if (expired) {
            val current = preferencesStore.read()
            preferencesStore.write(current.copy(timerStopAtEpochMillis = null))
        }
        return expired
    }
}
