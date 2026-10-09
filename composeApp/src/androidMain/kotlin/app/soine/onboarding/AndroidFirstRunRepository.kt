package app.soine.onboarding

import android.content.Context

/**
 * Android-backed [FirstRunRepository]. Persists the first-run gate
 * to a private SharedPreferences file. The file holds at most two
 * keys: the schema version and the end state. The repository must
 * not touch any other Soine data.
 */
class AndroidFirstRunRepository(context: Context) : FirstRunRepository {
    private val preferences = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun read(): FirstRunState {
        val version = if (preferences.contains(KEY_VERSION)) {
            preferences.getInt(KEY_VERSION, 0)
        } else {
            FirstRunState.CURRENT_VERSION
        }
        val endStateName = preferences.getString(KEY_END_STATE, null)
        val endState = when (endStateName) {
            FirstRunState.EndState.COMPLETED.name ->
                FirstRunState.EndState.COMPLETED
            FirstRunState.EndState.SKIPPED.name ->
                FirstRunState.EndState.SKIPPED
            else -> FirstRunState.EndState.NOT_STARTED
        }
        return FirstRunState(endState = endState, version = version)
    }

    override fun markCompleted() {
        write(FirstRunState.EndState.COMPLETED)
    }

    override fun markSkipped() {
        write(FirstRunState.EndState.SKIPPED)
    }

    override fun resetForReplay() {
        // Drop both keys so the next read returns the NOT_STARTED
        // default. A future migration that bumps CURRENT_VERSION
        // will still see version 0 on the read path, which is
        // treated as the current version by [read] (defensive
        // default for upgrades from a future build).
        preferences.edit()
            .remove(KEY_VERSION)
            .remove(KEY_END_STATE)
            .commit()
    }

    private fun write(endState: FirstRunState.EndState) {
        check(
            preferences.edit()
                .putInt(KEY_VERSION, FirstRunState.CURRENT_VERSION)
                .putString(KEY_END_STATE, endState.name)
                .commit()
        ) { "Failed to persist first-run state." }
    }

    private companion object {
        const val PREFS_NAME: String = "soine_first_run"
        const val KEY_VERSION: String = "version"
        const val KEY_END_STATE: String = "end_state"
    }
}
