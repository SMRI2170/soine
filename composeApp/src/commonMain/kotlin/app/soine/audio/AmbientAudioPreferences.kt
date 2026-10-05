package app.soine.audio

data class AmbientAudioPreferences(
    val soundId: String = AmbientSounds.Rain.id,
    val volume: Float = AmbientSounds.Rain.defaultVolume,
    val timerPreset: SleepTimerPreset? = null,
    val timerStopAtEpochMillis: Long? = null,
    val muted: Boolean = true,
) {
    companion object {
        fun safe(
            soundId: String?,
            volume: Float?,
            timerPreset: String?,
            timerStopAtEpochMillis: Long?,
            muted: Boolean?,
        ): AmbientAudioPreferences {
            val sound = soundId?.let(AmbientSounds::find) ?: AmbientSounds.Rain
            return AmbientAudioPreferences(
                soundId = sound.id,
                volume = volume?.takeIf { it in 0f..1f } ?: sound.defaultVolume,
                timerPreset = timerPreset?.let { name ->
                    SleepTimerPreset.entries.firstOrNull { it.name == name }
                },
                timerStopAtEpochMillis = timerStopAtEpochMillis?.takeIf { it >= 0L },
                muted = muted ?: true,
            )
        }
    }
}

interface AmbientAudioPreferencesStore {
    fun read(): AmbientAudioPreferences
    fun write(preferences: AmbientAudioPreferences)
    fun clear()
}
