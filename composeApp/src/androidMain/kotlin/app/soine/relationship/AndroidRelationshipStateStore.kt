package app.soine.relationship

import android.content.Context

class AndroidRelationshipStateStore(
    context: Context,
    preferencesName: String = DEFAULT_PREFERENCES_NAME,
) : RelationshipStateStore {
    private val preferences =
        context.applicationContext.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)

    override fun read(): String? = preferences.getString(SNAPSHOT_KEY, null)

    override fun write(value: String) {
        check(preferences.edit().putString(SNAPSHOT_KEY, value).commit()) {
            "Failed to persist relationship state."
        }
    }

    override fun clear() {
        check(preferences.edit().remove(SNAPSHOT_KEY).commit()) {
            "Failed to clear relationship state."
        }
    }

    companion object {
        private const val DEFAULT_PREFERENCES_NAME = "soine_relationship"
        private const val SNAPSHOT_KEY = "relationship_state_snapshot"
    }
}
