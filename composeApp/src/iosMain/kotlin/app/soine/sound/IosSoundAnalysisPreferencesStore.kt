package app.soine.sound

import platform.Foundation.NSUserDefaults

class IosSoundAnalysisPreferencesStore(
    private val defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults,
) : SoundAnalysisPreferencesStore {
    override fun read(): SoundAnalysisPreferences =
        SoundAnalysisPreferences(
            enabled = defaults.boolForKey(KEY_ENABLED),
        )

    override fun write(preferences: SoundAnalysisPreferences) {
        defaults.setBool(preferences.enabled, forKey = KEY_ENABLED)
    }

    override fun clear() {
        defaults.removeObjectForKey(KEY_ENABLED)
    }

    companion object {
        private const val KEY_ENABLED = "soine.sound_analysis.enabled"
    }
}
