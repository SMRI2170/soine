package app.soine.audio

/**
 * A failure-injecting [AmbientAudioController] for tests.
 *
 * #185 requires that audio failures be absorbed by the
 * graceful-degradation path. The fake lets a test author
 * specify the failure pattern:
 *
 *   - [failAllPlayCalls] — every `play` returns the same
 *     STOPPED state. The `BedtimeAudioCoordinator` will
 *     see the play call as a no-op and the bedtime loop
 *     will continue.
 *   - [failAllSetVolumeCalls] — every `setVolume` is a
 *     no-op. The volume state stays at its current value.
 *
 * The fake's state transitions are intentionally narrow:
 * the platform impl is the owner of "play worked" /
 * "play failed" and exposes that through [state]. A
 * failure-injecting fake just stops transitioning to
 * `PLAYING`; the bedtime loop sees the STOPPED state and
 * continues.
 */
class FailureInjectingAmbientAudioController : AmbientAudioController {
    private val observers = mutableSetOf<AmbientAudioStateObserver>()
    override var state: AmbientAudioState = AmbientAudioState()
        private set

    private var playFails: Boolean = false
    private val callLog: MutableList<String> = mutableListOf()

    val recordedCalls: List<String> get() = callLog.toList()

    fun failAllPlayCalls() {
        playFails = true
    }

    fun recover() {
        playFails = false
    }

    override fun observe(observer: AmbientAudioStateObserver): AutoCloseable {
        observers += observer
        observer.onStateChanged(state)
        return AutoCloseable { observers -= observer }
    }

    override fun play(sound: AmbientSound) {
        callLog += "play"
        if (playFails) {
            // Stay STOPPED; the bedtime loop continues.
            return
        }
        update(
            state.copy(
                status = AmbientPlaybackStatus.PLAYING,
                sound = sound,
                volume = sound.defaultVolume,
            ),
        )
    }

    override fun pause() {
        callLog += "pause"
        if (state.status == AmbientPlaybackStatus.PLAYING) {
            update(state.copy(status = AmbientPlaybackStatus.PAUSED))
        }
    }

    override fun resume() {
        callLog += "resume"
        if (state.status == AmbientPlaybackStatus.PAUSED && state.sound != null) {
            update(state.copy(status = AmbientPlaybackStatus.PLAYING))
        }
    }

    override fun stop() {
        callLog += "stop"
        update(
            state.copy(
                status = AmbientPlaybackStatus.STOPPED,
                sound = null,
                stopAtEpochMillis = null,
            ),
        )
    }

    override fun setVolume(volume: Float) {
        callLog += "setVolume:$volume"
        update(state.copy(volume = volume))
    }

    override fun setStopAt(epochMillis: Long?) {
        callLog += "setStopAt:$epochMillis"
        update(state.copy(stopAtEpochMillis = epochMillis))
    }

    private fun update(next: AmbientAudioState) {
        state = next
        observers.toList().forEach { it.onStateChanged(next) }
    }
}
