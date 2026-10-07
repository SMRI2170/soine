package app.soine.sound

import android.content.Context

class AndroidSoundEventStore(
    context: Context,
    preferencesName: String = DEFAULT_PREFERENCES_NAME,
) : SoundEventStore {
    private val preferences =
        context.applicationContext.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)

    override fun read(): String? = preferences.getString(SNAPSHOT_KEY, null)

    override fun write(value: String) {
        check(preferences.edit().putString(SNAPSHOT_KEY, value).commit()) {
            "Failed to persist derived sound events."
        }
    }

    override fun clear() {
        check(preferences.edit().remove(SNAPSHOT_KEY).commit()) {
            "Failed to clear derived sound events."
        }
    }

    companion object {
        private const val DEFAULT_PREFERENCES_NAME = "soine_sound_events"
        private const val SNAPSHOT_KEY = "derived_sound_event_snapshot"
    }
}
