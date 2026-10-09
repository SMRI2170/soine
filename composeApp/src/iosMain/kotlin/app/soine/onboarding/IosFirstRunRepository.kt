package app.soine.onboarding

import platform.Foundation.NSUserDefaults

/**
 * iOS-backed [FirstRunRepository]. Persists the first-run gate to
 * a dedicated NSUserDefaults suite so a future privacy export can
 * surface the data without scanning the whole preferences file.
 */
class IosFirstRunRepository : FirstRunRepository {
    private val defaults: NSUserDefaults = NSUserDefaults(suiteName = SUITE_NAME)

    override fun read(): FirstRunState {
        val version = if (defaults.objectForKey(KEY_VERSION) != null) {
            defaults.integerForKey(KEY_VERSION).toInt()
        } else {
            FirstRunState.CURRENT_VERSION
        }
        val endStateName = defaults.stringForKey(KEY_END_STATE)
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
        defaults.removeObjectForKey(KEY_VERSION)
        defaults.removeObjectForKey(KEY_END_STATE)
    }

    private fun write(endState: FirstRunState.EndState) {
        defaults.setInteger(FirstRunState.CURRENT_VERSION.toLong(), forKey = KEY_VERSION)
        defaults.setObject(endState.name, forKey = KEY_END_STATE)
    }

    private companion object {
        const val SUITE_NAME: String = "app.soine.first_run"
        const val KEY_VERSION: String = "version"
        const val KEY_END_STATE: String = "end_state"
    }
}
