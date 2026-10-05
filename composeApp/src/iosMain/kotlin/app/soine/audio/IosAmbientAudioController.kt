package app.soine.audio

import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFAudio.AVAudioPlayer
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.Foundation.NSBundle
import platform.Foundation.NSURL

@OptIn(ExperimentalForeignApi::class)
class IosAmbientAudioController(
    private val resourceNameFor: (AmbientSound) -> String?,
) : AmbientAudioController, AutoCloseable {
    private val session = AVAudioSession.sharedInstance()
    private val observers = mutableSetOf<AmbientAudioStateObserver>()
    private var player: AVAudioPlayer? = null

    override var state: AmbientAudioState = AmbientAudioState()
        private set

    override fun observe(observer: AmbientAudioStateObserver): AutoCloseable {
        observers += observer
        observer.onStateChanged(state)
        return AutoCloseable { observers -= observer }
    }

    override fun play(sound: AmbientSound) {
        stop()
        val name = resourceNameFor(sound) ?: return
        val dot = name.lastIndexOf('.')
        val base = if (dot >= 0) name.substring(0, dot) else name
        val ext = if (dot >= 0) name.substring(dot + 1) else null
        val path = NSBundle.mainBundle.pathForResource(base, ext) ?: return
        val url = NSURL.fileURLWithPath(path)

        session.setCategory(AVAudioSessionCategoryPlayback, error = null)
        if (!session.setActiveWithOptions(true, 0u, null)) return

        val created = AVAudioPlayer(contentsOfURL = url, error = null) ?: run {
            session.setActiveWithOptions(false, 0u, null)
            return
        }
        created.numberOfLoops = if (sound.loop) -1 else 0
        created.volume = sound.defaultVolume
        created.prepareToPlay()
        if (!created.play()) {
            session.setActiveWithOptions(false, 0u, null)
            return
        }
        player = created
        update(AmbientAudioState(AmbientPlaybackStatus.PLAYING, sound, sound.defaultVolume))
    }

    override fun pause() {
        if (state.status != AmbientPlaybackStatus.PLAYING) return
        player?.pause()
        update(state.copy(status = AmbientPlaybackStatus.PAUSED))
    }

    override fun resume() {
        if (state.status != AmbientPlaybackStatus.PAUSED || player == null) return
        if (player?.play() == true) update(state.copy(status = AmbientPlaybackStatus.PLAYING))
    }

    override fun stop() {
        player?.stop()
        player = null
        session.setActiveWithOptions(false, 0u, null)
        update(AmbientAudioState(volume = state.volume))
    }

    override fun setVolume(volume: Float) {
        require(volume in 0f..1f) { "Volume must be between 0 and 1." }
        player?.volume = volume
        update(state.copy(volume = volume))
    }

    override fun setStopAt(epochMillis: Long?) {
        update(state.copy(stopAtEpochMillis = epochMillis))
    }

    /** Called by the iOS notification/lifecycle bridge on audio interruption. */
    fun handleInterruptionBegan() = pause()

    /** Resume only when the OS indicates resumption is appropriate. */
    fun handleInterruptionEnded(shouldResume: Boolean) {
        if (shouldResume) resume()
    }

    override fun close() = stop()

    private fun update(next: AmbientAudioState) {
        state = next
        observers.toList().forEach { it.onStateChanged(next) }
    }
}
