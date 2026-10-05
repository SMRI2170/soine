package app.soine.audio

import platform.Foundation.NSUserDefaults

class IosAmbientAudioPreferencesStore(
    private val defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults,
) : AmbientAudioPreferencesStore {
    override fun read(): AmbientAudioPreferences {
        val soundId = defaults.stringForKey("soine.audio.sound_id")
        val volume = defaults.objectForKey("soine.audio.volume")?.let { defaults.floatForKey("soine.audio.volume") }
        val preset = defaults.stringForKey("soine.audio.timer_preset")
        val stopAt = defaults.objectForKey("soine.audio.timer_stop_at")?.let { defaults.doubleForKey("soine.audio.timer_stop_at").toLong() }
        val muted = defaults.objectForKey("soine.audio.muted")?.let { defaults.boolForKey("soine.audio.muted") }
        return AmbientAudioPreferences.safe(soundId, volume, preset, stopAt, muted)
    }

    override fun write(preferences: AmbientAudioPreferences) {
        defaults.setObject(preferences.soundId, forKey = "soine.audio.sound_id")
        defaults.setFloat(preferences.volume, forKey = "soine.audio.volume")
        preferences.timerPreset?.let { defaults.setObject(it.name, forKey = "soine.audio.timer_preset") }
            ?: defaults.removeObjectForKey("soine.audio.timer_preset")
        preferences.timerStopAtEpochMillis?.let { defaults.setDouble(it.toDouble(), forKey = "soine.audio.timer_stop_at") }
            ?: defaults.removeObjectForKey("soine.audio.timer_stop_at")
        defaults.setBool(preferences.muted, forKey = "soine.audio.muted")
    }

    override fun clear() {
        listOf("sound_id", "volume", "timer_preset", "timer_stop_at", "muted").forEach {
            defaults.removeObjectForKey("soine.audio.$it")
        }
    }
}
