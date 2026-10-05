package app.soine.audio

enum class AmbientPlaybackStatus { STOPPED, PLAYING, PAUSED }

data class AmbientAudioState(
    val status: AmbientPlaybackStatus = AmbientPlaybackStatus.STOPPED,
    val sound: AmbientSound? = null,
    val volume: Float = 1f,
    val stopAtEpochMillis: Long? = null,
) {
    init {
        require(volume in 0f..1f) { "Volume must be between 0 and 1." }
    }
}

fun interface AmbientAudioStateObserver {
    fun onStateChanged(state: AmbientAudioState)
}

/**
 * Platform-neutral ambient audio boundary.
 *
 * Implementations own AVAudioSession/AudioFocus, looping and lifecycle details.
 * The common layer only expresses user intent and observable playback state.
 */
interface AmbientAudioController {
    val state: AmbientAudioState

    fun observe(observer: AmbientAudioStateObserver): AutoCloseable
    fun play(sound: AmbientSound)
    fun pause()
    fun resume()
    fun stop()
    fun setVolume(volume: Float)
    fun setStopAt(epochMillis: Long?)
}
