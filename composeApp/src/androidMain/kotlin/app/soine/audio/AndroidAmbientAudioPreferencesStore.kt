package app.soine.audio

import android.content.Context

class AndroidAmbientAudioPreferencesStore(context: Context) : AmbientAudioPreferencesStore {
    private val preferences = context.applicationContext
        .getSharedPreferences("soine_ambient_audio", Context.MODE_PRIVATE)

    override fun read(): AmbientAudioPreferences = AmbientAudioPreferences.safe(
        soundId = preferences.getString("sound_id", null),
        volume = if (preferences.contains("volume")) preferences.getFloat("volume", 0f) else null,
        timerPreset = preferences.getString("timer_preset", null),
        muted = if (preferences.contains("muted")) preferences.getBoolean("muted", false) else null,
    )

    override fun write(preferencesValue: AmbientAudioPreferences) {
        check(preferences.edit()
            .putString("sound_id", preferencesValue.soundId)
            .putFloat("volume", preferencesValue.volume)
            .apply { preferencesValue.timerPreset?.let { putString("timer_preset", it.name) } ?: remove("timer_preset") }
            .putBoolean("muted", preferencesValue.muted)
            .commit()
        ) { "Failed to persist ambient audio preferences." }
    }

    override fun clear() {
        check(preferences.edit().clear().commit()) { "Failed to clear ambient audio preferences." }
    }
}
