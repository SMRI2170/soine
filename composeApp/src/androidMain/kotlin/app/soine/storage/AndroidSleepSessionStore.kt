package app.soine.storage

import android.content.Context

/**
 * Durable Android adapter for the shared sleep-session snapshot.
 *
 * commit() is intentionally used instead of apply(): entering the overnight
 * session must not race an asynchronous preference write.
 */
class AndroidSleepSessionStore(
    context: Context,
    preferencesName: String = DEFAULT_PREFERENCES_NAME,
) : SleepSessionStore {

    private val preferences =
        context.applicationContext.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)

    override fun read(): String? =
        preferences.getString(SNAPSHOT_KEY, null)

    override fun write(value: String) {
        check(preferences.edit().putString(SNAPSHOT_KEY, value).commit()) {
            "Failed to persist sleep-session snapshot."
        }
    }

    override fun clear() {
        check(preferences.edit().remove(SNAPSHOT_KEY).commit()) {
            "Failed to clear sleep-session snapshot."
        }
    }

    companion object {
        private const val DEFAULT_PREFERENCES_NAME = "soine_sleep"
        private const val SNAPSHOT_KEY = "sleep_session_snapshot"
    }
}
