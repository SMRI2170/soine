package app.soine.dream

import android.content.Context

class AndroidDreamDiscoveryStore(
    context: Context,
    preferencesName: String = DEFAULT_PREFERENCES_NAME,
) : DreamDiscoveryStore {
    private val preferences =
        context.applicationContext.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)

    override fun read(): String? = preferences.getString(SNAPSHOT_KEY, null)

    override fun write(value: String) {
        check(preferences.edit().putString(SNAPSHOT_KEY, value).commit()) {
            "Failed to persist dream discovery state."
        }
    }

    override fun clear() {
        check(preferences.edit().remove(SNAPSHOT_KEY).commit()) {
            "Failed to clear dream discovery state."
        }
    }

    companion object {
        private const val DEFAULT_PREFERENCES_NAME = "soine_dreams"
        private const val SNAPSHOT_KEY = "dream_discovery_snapshot"
    }
}
