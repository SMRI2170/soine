package app.soine.audio

import android.content.Context

class AndroidAmbientAudioPreferencesStore(context: Context) : AmbientAudioPreferencesStore {
    private val preferences = context.applicationContext
        .getSharedPreferences("soine_ambient_audio", Context.MODE_PRIVATE)

    override fun read(): AmbientAudioPreferences = AmbientAudioPreferences.safe(
        soundId = preferences.getString("sound_id", null),
        volume = if (preferences.contains("volume")) preferences.getFloat("volume", 0f) else null,
        timerPreset = preferences.getString("timer_preset", null),
        timerStopAtEpochMillis = if (preferences.contains("timer_stop_at")) preferences.getLong("timer_stop_at", 0L) else null,
        muted = if (preferences.contains("muted")) preferences.getBoolean("muted", false) else null,
    )

    override fun write(preferences: AmbientAudioPreferences) {
        check(this.preferences.edit()
            .putString("sound_id", preferences.soundId)
            .putFloat("volume", preferences.volume)
            .apply { preferences.timerPreset?.let { putString("timer_preset", it.name) } ?: remove("timer_preset") }
            .apply { preferences.timerStopAtEpochMillis?.let { putLong("timer_stop_at", it) } ?: remove("timer_stop_at") }
            .putBoolean("muted", preferences.muted)
            .commit()
        ) { "Failed to persist ambient audio preferences." }
    }

    override fun clear() {
        check(preferences.edit().clear().commit()) { "Failed to clear ambient audio preferences." }
    }
}
