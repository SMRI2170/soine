package app.soine.audio

class BedtimeAudioCoordinator(
    private val audio: AmbientAudioController,
    private val preferencesStore: AmbientAudioPreferencesStore,
    private val now: () -> Long,
) {
    private val timer = PersistentSleepTimer(SleepTimer(audio, now), preferencesStore)

    val state: AmbientAudioState get() = audio.state

    fun beginNight() {
        val preferences = preferencesStore.read()
        if (preferences.muted) {
            audio.stop()
            timer.cancel()
            return
        }
        playSelected(preferences)
        preferences.timerPreset?.let(timer::start) ?: timer.cancel()
    }

    fun recoverNight() {
        val before = preferencesStore.read()
        timer.restore()
        val expired = before.timerStopAtEpochMillis?.let { it <= now() } == true
        if (before.muted || expired) {
            audio.stop()
            return
        }
        playSelected(preferencesStore.read())
    }

    fun endNight() {
        audio.stop()
        timer.cancel()
    }

    fun togglePlayback() {
        when (audio.state.status) {
            AmbientPlaybackStatus.PLAYING -> audio.pause()
            AmbientPlaybackStatus.PAUSED -> audio.resume()
            AmbientPlaybackStatus.STOPPED -> {
                val preferences = preferencesStore.read()
                playSelected(preferences)
                if (preferences.timerStopAtEpochMillis == null) {
                    preferences.timerPreset?.let(timer::start)
                }
            }
        }
    }

    fun startTimer(minutes: Int) {
        timer.startCustom(minutes)
    }

    fun startTimer(preset: SleepTimerPreset) {
        timer.start(preset)
    }

    fun cancelTimer() {
        timer.cancel()
    }

    fun tick(): Boolean = timer.tick()

    fun remainingMillis(): Long? =
        timer.state.stopAtEpochMillis?.let { timer.state.remainingMillis(now()) }

    private fun playSelected(preferences: AmbientAudioPreferences) {
        val sound = AmbientSounds.find(preferences.soundId) ?: AmbientSounds.Rain
        audio.play(sound)
        audio.setVolume(if (preferences.muted) 0f else preferences.volume)
    }
}
