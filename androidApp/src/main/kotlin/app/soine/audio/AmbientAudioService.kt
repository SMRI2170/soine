package app.soine.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import app.soine.R

internal object AndroidAmbientAudioRuntime {
    private val observers = mutableSetOf<AmbientAudioStateObserver>()
    var serviceActive: Boolean = false
    var state: AmbientAudioState = AmbientAudioState()
        private set

    fun observe(observer: AmbientAudioStateObserver): AutoCloseable {
        observers += observer
        observer.onStateChanged(state)
        return AutoCloseable { observers -= observer }
    }

    fun update(next: AmbientAudioState) {
        state = next
        observers.toList().forEach { it.onStateChanged(next) }
    }
}

class AmbientAudioService : Service() {
    private lateinit var audioManager: AudioManager
    private val handler = Handler(Looper.getMainLooper())
    private var player: MediaPlayer? = null
    private var focusRequest: AudioFocusRequest? = null
    private var resumeAfterFocusGain = false
    private var currentSoundId: String? = null
    private var currentVolume: Float = 1f

    private val timerStop = Runnable { stopPlayback(stopSelfAfter = true) }

    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                resumeAfterFocusGain = false
                stopPlayback(stopSelfAfter = true)
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                resumeAfterFocusGain = player?.isPlaying == true
                pausePlayback()
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                if (resumeAfterFocusGain) {
                    resumeAfterFocusGain = false
                    resumePlayback()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        AndroidAmbientAudioRuntime.serviceActive = true
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> play(
                resourceId = intent.getIntExtra(EXTRA_RESOURCE_ID, 0),
                soundId = intent.getStringExtra(EXTRA_SOUND_ID),
                loop = intent.getBooleanExtra(EXTRA_LOOP, true),
                volume = intent.getFloatExtra(EXTRA_VOLUME, 1f),
                stopAtEpochMillis = intent.getLongExtra(EXTRA_STOP_AT, NO_DEADLINE)
                    .takeUnless { it == NO_DEADLINE },
            )
            ACTION_PAUSE -> pausePlayback()
            ACTION_RESUME -> resumePlayback()
            ACTION_STOP -> stopPlayback(stopSelfAfter = true)
            ACTION_SET_VOLUME -> setPlaybackVolume(intent.getFloatExtra(EXTRA_VOLUME, 1f))
            ACTION_SET_STOP_AT -> scheduleStop(
                intent.getLongExtra(EXTRA_STOP_AT, NO_DEADLINE).takeUnless { it == NO_DEADLINE },
            )
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        handler.removeCallbacks(timerStop)
        releasePlayer()
        abandonFocus()
        AndroidAmbientAudioRuntime.serviceActive = false
        super.onDestroy()
    }

    private fun play(
        resourceId: Int,
        soundId: String?,
        loop: Boolean,
        volume: Float,
        stopAtEpochMillis: Long?,
    ) {
        if (resourceId == 0 || soundId == null) {
            stopPlayback(stopSelfAfter = true)
            return
        }

        releasePlayer()
        if (!requestFocus()) {
            stopSelf()
            return
        }

        val created = MediaPlayer.create(applicationContext, resourceId) ?: run {
            abandonFocus()
            stopSelf()
            return
        }
        currentSoundId = soundId
        currentVolume = volume.coerceIn(0f, 1f)
        player = created.apply {
            isLooping = loop
            setVolume(currentVolume, currentVolume)
            setOnCompletionListener {
                if (!loop) stopPlayback(stopSelfAfter = true)
            }
            start()
        }

        startForeground(NOTIFICATION_ID, buildNotification(isPlaying = true))
        AndroidAmbientAudioRuntime.update(
            AmbientAudioState(
                status = AmbientPlaybackStatus.PLAYING,
                sound = AmbientSounds.find(soundId),
                volume = currentVolume,
                stopAtEpochMillis = stopAtEpochMillis,
            )
        )
        scheduleStop(stopAtEpochMillis)
    }

    private fun pausePlayback() {
        if (player?.isPlaying != true) return
        player?.pause()
        AndroidAmbientAudioRuntime.update(
            AndroidAmbientAudioRuntime.state.copy(status = AmbientPlaybackStatus.PAUSED)
        )
        updateNotification(isPlaying = false)
    }

    private fun resumePlayback() {
        val existing = player ?: return
        if (!requestFocus()) return
        existing.start()
        AndroidAmbientAudioRuntime.update(
            AndroidAmbientAudioRuntime.state.copy(status = AmbientPlaybackStatus.PLAYING)
        )
        updateNotification(isPlaying = true)
    }

    private fun stopPlayback(stopSelfAfter: Boolean) {
        handler.removeCallbacks(timerStop)
        releasePlayer()
        abandonFocus()
        AndroidAmbientAudioRuntime.update(
            AmbientAudioState(volume = AndroidAmbientAudioRuntime.state.volume)
        )
        stopForeground(STOP_FOREGROUND_REMOVE)
        if (stopSelfAfter) stopSelf()
    }

    private fun setPlaybackVolume(volume: Float) {
        currentVolume = volume.coerceIn(0f, 1f)
        player?.setVolume(currentVolume, currentVolume)
        AndroidAmbientAudioRuntime.update(
            AndroidAmbientAudioRuntime.state.copy(volume = currentVolume)
        )
    }

    private fun scheduleStop(stopAtEpochMillis: Long?) {
        handler.removeCallbacks(timerStop)
        AndroidAmbientAudioRuntime.update(
            AndroidAmbientAudioRuntime.state.copy(stopAtEpochMillis = stopAtEpochMillis)
        )
        stopAtEpochMillis ?: return
        val delay = (stopAtEpochMillis - System.currentTimeMillis()).coerceAtLeast(0L)
        handler.postDelayed(timerStop, delay)
    }

    private fun requestFocus(): Boolean {
        if (focusRequest != null) return true
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(audioAttributes())
            .setWillPauseWhenDucked(true)
            .setOnAudioFocusChangeListener(focusListener)
            .build()
        if (audioManager.requestAudioFocus(request) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            return false
        }
        focusRequest = request
        return true
    }

    private fun abandonFocus() {
        focusRequest?.let(audioManager::abandonAudioFocusRequest)
        focusRequest = null
    }

    private fun releasePlayer() {
        player?.runCatching { stop() }
        player?.release()
        player = null
        currentSoundId = null
    }

    private fun audioAttributes() = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "睡眠中の環境音",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Soineの環境音を画面消灯中も再生します"
            setSound(null, null)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(isPlaying: Boolean): Notification =
        Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Soine")
            .setContentText(if (isPlaying) "環境音を再生中" else "環境音を一時停止中")
            .setOngoing(isPlaying)
            .build()

    private fun updateNotification(isPlaying: Boolean) {
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, buildNotification(isPlaying))
    }

    companion object {
        const val ACTION_PLAY = "app.soine.audio.PLAY"
        const val ACTION_PAUSE = "app.soine.audio.PAUSE"
        const val ACTION_RESUME = "app.soine.audio.RESUME"
        const val ACTION_STOP = "app.soine.audio.STOP"
        const val ACTION_SET_VOLUME = "app.soine.audio.SET_VOLUME"
        const val ACTION_SET_STOP_AT = "app.soine.audio.SET_STOP_AT"

        const val EXTRA_RESOURCE_ID = "resource_id"
        const val EXTRA_SOUND_ID = "sound_id"
        const val EXTRA_LOOP = "loop"
        const val EXTRA_VOLUME = "volume"
        const val EXTRA_STOP_AT = "stop_at"

        private const val CHANNEL_ID = "soine_ambient_audio"
        private const val NOTIFICATION_ID = 2001
        private const val NO_DEADLINE = Long.MIN_VALUE
    }
}
