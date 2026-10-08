package app.soine.sound

import android.content.Context

class AndroidSoundAnalysisPreferencesStore(
    context: Context,
) : SoundAnalysisPreferencesStore {
    private val preferences = context.applicationContext
        .getSharedPreferences("soine_sound_analysis", Context.MODE_PRIVATE)

    override fun read(): SoundAnalysisPreferences =
        SoundAnalysisPreferences(
            enabled = preferences.getBoolean("enabled", false),
        )

    override fun write(preferences: SoundAnalysisPreferences) {
        check(
            this.preferences.edit()
                .putBoolean("enabled", preferences.enabled)
                .commit()
        ) { "Failed to persist sound-analysis preferences." }
    }

    override fun clear() {
        check(preferences.edit().clear().commit()) {
            "Failed to clear sound-analysis preferences."
        }
    }
}
