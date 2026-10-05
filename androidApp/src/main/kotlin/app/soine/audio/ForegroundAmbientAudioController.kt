package app.soine.audio

import android.content.Context
import android.content.Intent

class ForegroundAmbientAudioController(
    context: Context,
    private val resourceFor: (AmbientSound) -> Int,
) : AmbientAudioController {
    private val appContext = context.applicationContext

    override val state: AmbientAudioState
        get() = AndroidAmbientAudioRuntime.state

    override fun observe(observer: AmbientAudioStateObserver): AutoCloseable =
        AndroidAmbientAudioRuntime.observe(observer)

    override fun play(sound: AmbientSound) {
        val resourceId = resourceFor(sound)
        if (resourceId == 0) return
        val current = AndroidAmbientAudioRuntime.state
        AndroidAmbientAudioRuntime.update(
            current.copy(
                status = AmbientPlaybackStatus.PLAYING,
                sound = sound,
                volume = sound.defaultVolume,
            )
        )
        val intent = Intent(appContext, AmbientAudioService::class.java)
            .setAction(AmbientAudioService.ACTION_PLAY)
            .putExtra(AmbientAudioService.EXTRA_RESOURCE_ID, resourceId)
            .putExtra(AmbientAudioService.EXTRA_SOUND_ID, sound.id)
            .putExtra(AmbientAudioService.EXTRA_LOOP, sound.loop)
            .putExtra(AmbientAudioService.EXTRA_VOLUME, sound.defaultVolume)
            .putExtra(
                AmbientAudioService.EXTRA_STOP_AT,
                current.stopAtEpochMillis ?: Long.MIN_VALUE,
            )
        appContext.startForegroundService(intent)
    }

    override fun pause() {
        if (!AndroidAmbientAudioRuntime.serviceActive) return
        appContext.startService(
            Intent(appContext, AmbientAudioService::class.java)
                .setAction(AmbientAudioService.ACTION_PAUSE)
        )
    }

    override fun resume() {
        if (!AndroidAmbientAudioRuntime.serviceActive) return
        appContext.startService(
            Intent(appContext, AmbientAudioService::class.java)
                .setAction(AmbientAudioService.ACTION_RESUME)
        )
    }

    override fun stop() {
        if (!AndroidAmbientAudioRuntime.serviceActive) {
            AndroidAmbientAudioRuntime.update(AmbientAudioState(volume = state.volume))
            return
        }
        appContext.startService(
            Intent(appContext, AmbientAudioService::class.java)
                .setAction(AmbientAudioService.ACTION_STOP)
        )
    }

    override fun setVolume(volume: Float) {
        require(volume in 0f..1f) { "Volume must be between 0 and 1." }
        AndroidAmbientAudioRuntime.update(state.copy(volume = volume))
        if (!AndroidAmbientAudioRuntime.serviceActive) return
        appContext.startService(
            Intent(appContext, AmbientAudioService::class.java)
                .setAction(AmbientAudioService.ACTION_SET_VOLUME)
                .putExtra(AmbientAudioService.EXTRA_VOLUME, volume)
        )
    }

    override fun setStopAt(epochMillis: Long?) {
        AndroidAmbientAudioRuntime.update(state.copy(stopAtEpochMillis = epochMillis))
        if (!AndroidAmbientAudioRuntime.serviceActive) return
        appContext.startService(
            Intent(appContext, AmbientAudioService::class.java)
                .setAction(AmbientAudioService.ACTION_SET_STOP_AT)
                .putExtra(
                    AmbientAudioService.EXTRA_STOP_AT,
                    epochMillis ?: Long.MIN_VALUE,
                )
        )
    }
}
