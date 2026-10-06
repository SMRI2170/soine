package app.soine.dream

import platform.Foundation.NSUserDefaults

class IosDreamDiscoveryStore(
    private val defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults,
) : DreamDiscoveryStore {
    override fun read(): String? = defaults.stringForKey(SNAPSHOT_KEY)

    override fun write(value: String) {
        defaults.setObject(value, forKey = SNAPSHOT_KEY)
    }

    override fun clear() {
        defaults.removeObjectForKey(SNAPSHOT_KEY)
    }

    companion object {
        private const val SNAPSHOT_KEY = "soine.dream_discovery_snapshot"
    }
}
