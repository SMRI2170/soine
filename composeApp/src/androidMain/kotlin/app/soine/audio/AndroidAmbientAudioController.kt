package app.soine.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer

class AndroidAmbientAudioController(
    context: Context,
    private val resourceFor: (AmbientSound) -> Int,
) : AmbientAudioController, AutoCloseable {
    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val observers = mutableSetOf<AmbientAudioStateObserver>()
    private var player: MediaPlayer? = null
    private var focusRequest: AudioFocusRequest? = null
    private var resumeAfterFocusGain = false

    override var state: AmbientAudioState = AmbientAudioState()
        private set

    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                resumeAfterFocusGain = false
                pauseInternal()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                resumeAfterFocusGain = state.status == AmbientPlaybackStatus.PLAYING
                pauseInternal()
            }
            AudioManager.AUDIOFOCUS_GAIN -> if (resumeAfterFocusGain) {
                resumeAfterFocusGain = false
                resume()
            }
        }
    }

    override fun observe(observer: AmbientAudioStateObserver): AutoCloseable {
        observers += observer
        observer.onStateChanged(state)
        return AutoCloseable { observers -= observer }
    }

    override fun play(sound: AmbientSound) {
        stop()
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(audioAttributes())
            .setOnAudioFocusChangeListener(focusListener)
            .build()
        if (audioManager.requestAudioFocus(request) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) return
        focusRequest = request

        val resourceId = resourceFor(sound)
        if (resourceId == 0) {
            abandonFocus()
            return
        }
        val created = MediaPlayer.create(appContext, resourceId) ?: run {
            abandonFocus()
            return
        }
        player = created.apply {
            isLooping = sound.loop
            setVolume(sound.defaultVolume, sound.defaultVolume)
            setOnCompletionListener { if (!sound.loop) stop() }
            start()
        }
        update(AmbientAudioState(AmbientPlaybackStatus.PLAYING, sound, sound.defaultVolume))
    }

    override fun pause() = pauseInternal()

    private fun pauseInternal() {
        if (state.status != AmbientPlaybackStatus.PLAYING) return
        player?.pause()
        update(state.copy(status = AmbientPlaybackStatus.PAUSED))
    }

    override fun resume() {
        if (state.status != AmbientPlaybackStatus.PAUSED || player == null) return
        player?.start()
        update(state.copy(status = AmbientPlaybackStatus.PLAYING))
    }

    override fun stop() {
        player?.runCatching { stop() }
        player?.release()
        player = null
        abandonFocus()
        update(AmbientAudioState(volume = state.volume))
    }

    override fun setVolume(volume: Float) {
        require(volume in 0f..1f) { "Volume must be between 0 and 1." }
        player?.setVolume(volume, volume)
        update(state.copy(volume = volume))
    }

    override fun setStopAt(epochMillis: Long?) {
        update(state.copy(stopAtEpochMillis = epochMillis))
    }

    override fun close() = stop()

    private fun abandonFocus() {
        focusRequest?.let(audioManager::abandonAudioFocusRequest)
        focusRequest = null
    }

    private fun update(next: AmbientAudioState) {
        state = next
        observers.toList().forEach { it.onStateChanged(next) }
    }

    private fun audioAttributes() = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()
}
