package app.soine.sound

data class SoundAnalysisPreferences(
    val enabled: Boolean = false,
)

interface SoundAnalysisPreferencesStore {
    fun read(): SoundAnalysisPreferences
    fun write(preferences: SoundAnalysisPreferences)
    fun clear()
}
