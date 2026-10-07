package app.soine.sound

import platform.Foundation.NSUserDefaults

class IosSoundEventStore(
    private val defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults,
) : SoundEventStore {
    override fun read(): String? = defaults.stringForKey(SNAPSHOT_KEY)

    override fun write(value: String) {
        defaults.setObject(value, forKey = SNAPSHOT_KEY)
    }

    override fun clear() {
        defaults.removeObjectForKey(SNAPSHOT_KEY)
    }

    companion object {
        private const val SNAPSHOT_KEY = "soine.derived_sound_event_snapshot"
    }
}
