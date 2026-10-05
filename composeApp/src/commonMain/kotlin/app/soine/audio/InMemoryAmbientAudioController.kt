package app.soine.audio

/**
 * Deterministic in-memory implementation for common tests and previews.
 */
class InMemoryAmbientAudioController : AmbientAudioController {
    private val observers = mutableSetOf<AmbientAudioStateObserver>()
    override var state: AmbientAudioState = AmbientAudioState()
        private set

    override fun observe(observer: AmbientAudioStateObserver): AutoCloseable {
        observers += observer
        observer.onStateChanged(state)
        return AutoCloseable { observers -= observer }
    }

    override fun play(sound: AmbientSound) {
        update(state.copy(status = AmbientPlaybackStatus.PLAYING, sound = sound, volume = sound.defaultVolume))
    }

    override fun pause() {
        if (state.status == AmbientPlaybackStatus.PLAYING) update(state.copy(status = AmbientPlaybackStatus.PAUSED))
    }

    override fun resume() {
        if (state.status == AmbientPlaybackStatus.PAUSED && state.sound != null) update(state.copy(status = AmbientPlaybackStatus.PLAYING))
    }

    override fun stop() {
        update(state.copy(status = AmbientPlaybackStatus.STOPPED, sound = null, stopAtEpochMillis = null))
    }

    override fun setVolume(volume: Float) {
        require(volume in 0f..1f) { "Volume must be between 0 and 1." }
        update(state.copy(volume = volume))
    }

    override fun setStopAt(epochMillis: Long?) {
        update(state.copy(stopAtEpochMillis = epochMillis))
    }

    private fun update(next: AmbientAudioState) {
        state = next
        observers.toList().forEach { it.onStateChanged(next) }
    }
}
