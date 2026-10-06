package app.soine.relationship

import platform.Foundation.NSUserDefaults

class IosRelationshipStateStore(
    private val defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults,
) : RelationshipStateStore {
    override fun read(): String? = defaults.stringForKey(SNAPSHOT_KEY)

    override fun write(value: String) {
        defaults.setObject(value, forKey = SNAPSHOT_KEY)
    }

    override fun clear() {
        defaults.removeObjectForKey(SNAPSHOT_KEY)
    }

    companion object {
        private const val SNAPSHOT_KEY = "soine.relationship_state_snapshot"
    }
}
