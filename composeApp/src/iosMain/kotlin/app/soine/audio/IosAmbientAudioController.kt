package app.soine.audio

import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFAudio.AVAudioPlayer
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.AVAudioSessionInterruptionNotification
import platform.AVFAudio.AVAudioSessionInterruptionOptionKey
import platform.AVFAudio.AVAudioSessionInterruptionTypeKey
import platform.Foundation.NSBundle
import platform.Foundation.NSNotification
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSNumber
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSURL
import platform.darwin.NSObjectProtocol

@OptIn(ExperimentalForeignApi::class)
class IosAmbientAudioController(
    private val resourceNameFor: (AmbientSound) -> String?,
) : AmbientAudioController, AutoCloseable {
    private val session = AVAudioSession.sharedInstance()
    private val notificationCenter = NSNotificationCenter.defaultCenter
    private val observers = mutableSetOf<AmbientAudioStateObserver>()
    private var player: AVAudioPlayer? = null
    private var interruptionObserver: NSObjectProtocol? = null
    private var resumeAfterInterruption = false

    override var state: AmbientAudioState = AmbientAudioState()
        private set

    init {
        interruptionObserver = notificationCenter.addObserverForName(
            name = AVAudioSessionInterruptionNotification,
            `object` = session,
            queue = NSOperationQueue.mainQueue,
        ) { notification ->
            notification?.let(::handleInterruption)
        }
    }

    override fun observe(observer: AmbientAudioStateObserver): AutoCloseable {
        observers += observer
        observer.onStateChanged(state)
        return AutoCloseable { observers -= observer }
    }

    override fun play(sound: AmbientSound) {
        stopPlayerOnly()
        val name = resourceNameFor(sound) ?: return
        val dot = name.lastIndexOf('.')
        val base = if (dot >= 0) name.substring(0, dot) else name
        val ext = if (dot >= 0) name.substring(dot + 1) else null
        val path = NSBundle.mainBundle.pathForResource(base, ext) ?: return
        val url = NSURL.fileURLWithPath(path)

        session.setCategory(AVAudioSessionCategoryPlayback, error = null)
        val created = AVAudioPlayer(contentsOfURL = url, error = null) ?: return
        created.numberOfLoops = if (sound.loop) -1 else 0
        created.volume = sound.defaultVolume
        created.prepareToPlay()
        if (!created.play()) return

        player = created
        update(
            AmbientAudioState(
                status = AmbientPlaybackStatus.PLAYING,
                sound = sound,
                volume = sound.defaultVolume,
                stopAtEpochMillis = state.stopAtEpochMillis,
            )
        )
    }

    override fun pause() {
        if (state.status != AmbientPlaybackStatus.PLAYING) return
        player?.pause()
        update(state.copy(status = AmbientPlaybackStatus.PAUSED))
    }

    override fun resume() {
        if (state.status != AmbientPlaybackStatus.PAUSED || player == null) return
        if (player?.play() == true) {
            update(state.copy(status = AmbientPlaybackStatus.PLAYING))
        }
    }

    override fun stop() {
        resumeAfterInterruption = false
        stopPlayerOnly()
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

    private fun handleInterruption(notification: NSNotification) {
        val userInfo = notification.userInfo ?: return
        val type = (userInfo[AVAudioSessionInterruptionTypeKey] as? NSNumber)
            ?.unsignedIntegerValue
            ?: return

        when (type.toLong()) {
            1L -> {
                resumeAfterInterruption = state.status == AmbientPlaybackStatus.PLAYING
                pause()
            }
            0L -> {
                val options = (userInfo[AVAudioSessionInterruptionOptionKey] as? NSNumber)
                    ?.unsignedIntegerValue
                    ?.toLong()
                    ?: 0L
                val shouldResume = options and 1L != 0L
                if (resumeAfterInterruption && shouldResume) {
                    resumeAfterInterruption = false
                    resume()
                } else {
                    resumeAfterInterruption = false
                }
            }
        }
    }

    private fun stopPlayerOnly() {
        player?.stop()
        player = null
    }

    override fun close() {
        stop()
        interruptionObserver?.let(notificationCenter::removeObserver)
        interruptionObserver = null
    }

    private fun update(next: AmbientAudioState) {
        state = next
        observers.toList().forEach { it.onStateChanged(next) }
    }
}
