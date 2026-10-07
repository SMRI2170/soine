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

    override fun write(preferencesValue: SoundAnalysisPreferences) {
        check(
            preferences.edit()
                .putBoolean("enabled", preferencesValue.enabled)
                .commit()
        ) { "Failed to persist sound-analysis preferences." }
    }

    override fun clear() {
        check(preferences.edit().clear().commit()) {
            "Failed to clear sound-analysis preferences."
        }
    }
}
