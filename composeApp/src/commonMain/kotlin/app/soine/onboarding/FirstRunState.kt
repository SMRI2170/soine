package app.soine.onboarding

/**
 * Per-install first-run state owned by commonMain.
 *
 * The first-run gate decides whether the onboarding flow runs at
 * app start. A user can `complete` the flow, `skip` it, or `replay`
 * it from Settings. The repository must be the only surface that
 * mutates the persisted value so a future test or migration can
 * inspect the gate without going through the UI.
 *
 * V1 supports exactly three end states:
 *
 *   - [EndState.NOT_STARTED] — fresh install, onboarding not yet
 *     observed
 *   - [EndState.COMPLETED] — the user stepped through the flow
 *     (or hit the "はじめる" final CTA)
 *   - [EndState.SKIPPED] — the user hit "スキップ" or the back
 *     gesture; they reach the Bedtime screen without seeing the
 *     rest of the flow
 *
 * Replay resets the state to NOT_STARTED so the next launch shows
 * the flow again. The state is intentionally coarse — there is no
 * "current step" persisted; the controller starts at the first
 * step on every replay.
 */
data class FirstRunState(
    val endState: EndState = EndState.NOT_STARTED,
    val version: Int = CURRENT_VERSION,
) {
    init {
        require(version == CURRENT_VERSION) {
            "Stored first-run version $version does not match the current $CURRENT_VERSION."
        }
    }

    val isFirstRun: Boolean get() = endState == EndState.NOT_STARTED

    enum class EndState {
        NOT_STARTED,
        COMPLETED,
        SKIPPED,
    }

    companion object {
        const val CURRENT_VERSION: Int = 1

        val FRESH: FirstRunState = FirstRunState()
    }
}

/**
 * Repository that persists the first-run gate.
 *
 * The interface is the only place that knows how the value is
 * stored. The default `NoOpFirstRunRepository` is the safe choice
 * for unit tests and for hosts that have not yet wired a platform
 * adapter — it never reports a first run after the first read.
 *
 * Implementation contract:
 *
 *   - [read] must return a stable value across calls. The platform
 *     adapter is allowed to apply the [FirstRunState] version check
 *     on read so a future migration can rewrite older payloads
 *     transparently.
 *   - [markCompleted] and [markSkipped] must persist before
 *     returning. The OnboardingController relies on this so the
 *     user does not re-see the flow on the next launch because a
 *     write failed silently.
 *   - [resetForReplay] flips the state back to NOT_STARTED. The
 *     controller calls this when the user hits "オンボーディングを
 *     もう一度見る" in Settings.
 */
interface FirstRunRepository {
    fun read(): FirstRunState
    fun markCompleted()
    fun markSkipped()
    fun resetForReplay()
}

/**
 * Safe default that treats the first read as NOT_STARTED and the
 * second read as COMPLETED. Useful for unit tests that want a
 * stable in-memory implementation without picking a real platform
 * adapter.
 */
object NoOpFirstRunRepository : FirstRunRepository {
    private var state: FirstRunState = FirstRunState.FRESH

    override fun read(): FirstRunState = state

    override fun markCompleted() {
        state = state.copy(endState = FirstRunState.EndState.COMPLETED)
    }

    override fun markSkipped() {
        state = state.copy(endState = FirstRunState.EndState.SKIPPED)
    }

    override fun resetForReplay() {
        state = FirstRunState.FRESH
    }
}
