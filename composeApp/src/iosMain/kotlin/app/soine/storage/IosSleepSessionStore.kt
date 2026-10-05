package app.soine.storage

import platform.Foundation.NSUserDefaults

/**
 * iOS adapter for the shared sleep-session snapshot.
 *
 * NSUserDefaults is sufficient for the small MVP snapshot. If session history
 * grows substantially, the SleepSessionStore boundary allows replacement with
 * a database/file-backed adapter without changing domain use cases.
 */
class IosSleepSessionStore(
    private val defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults,
) : SleepSessionStore {

    override fun read(): String? =
        defaults.stringForKey(SNAPSHOT_KEY)

    override fun write(value: String) {
        defaults.setObject(value, forKey = SNAPSHOT_KEY)
    }

    override fun clear() {
        defaults.removeObjectForKey(SNAPSHOT_KEY)
    }

    companion object {
        private const val SNAPSHOT_KEY = "soine.sleep_session_snapshot"
    }
}
