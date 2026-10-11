package app.soine.onboarding

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.soine.analytics.AnalyticsEvent
import app.soine.analytics.AnalyticsTracker

/**
 * State machine for the onboarding flow.
 *
 * The flow has three steps by design — the V1 budget is "first
 * sleep within 30 seconds". The controller is the only surface
 * that owns the current step, and it never persists the step
 * itself: a re-launch always restarts at the first step so the
 * user does not see a half-completed flow.
 *
 * Step 0: Soine / companion introduction ("夜の相棒")
 * Step 1: 「一緒に眠る」体験 (what the sleep CTA does, no
 *         permission request, no Health probe)
 * Step 2: local-first / optional features (Health / microphone are
 *         off by default; the user enables them later)
 *
 * The controller emits an [AnalyticsEvent] at every state change
 * so the product analytics layer can observe the funnel. Analytics
 * is best-effort: a failure in the tracker must not block the
 * onboarding transition.
 *
 * The state is backed by Compose's [mutableStateOf] so the screen
 * recomposes when the controller's [currentStep] or [lastResult]
 * changes. A plain `var` would not trigger a recomposition and
 * the UI would not advance even when the click handler fires.
 */
class OnboardingController(
    private val repository: FirstRunRepository,
    private val tracker: AnalyticsTracker = NoOpAnalyticsTracker,
) {
    private val _currentStep = mutableStateOf(0)
    /**
     * The current step the UI should render. Bounded by
     * [TOTAL_STEPS] - 1.
     */
    var currentStep: Int
        get() = _currentStep.value
        private set(value) {
            _currentStep.value = value
        }

    private val _lastResult = mutableStateOf(Result.PENDING)
    /**
     * The result of the last [complete] / [skip] / [replay] call.
     * The UI listens to this to navigate away from the onboarding
     * screen.
     */
    var lastResult: Result
        get() = _lastResult.value
        private set(value) {
            _lastResult.value = value
        }

    /**
     * Step forward. No-op at the last step; the UI should call
     * [complete] from there.
     */
    fun advance() {
        if (currentStep < LAST_STEP) {
            currentStep += 1
            tracker.track(AnalyticsEvent.ONBOARDING_STEP_VIEWED)
        }
    }

    /**
     * Step backward. No-op at the first step.
     */
    fun retreat() {
        if (currentStep > 0) {
            currentStep -= 1
        }
    }

    /**
     * The user finished the flow. The repository must persist the
     * COMPLETED state synchronously so the next launch skips the
     * onboarding screen.
     */
    fun complete() {
        repository.markCompleted()
        lastResult = Result.COMPLETED
        tracker.track(AnalyticsEvent.ONBOARDING_COMPLETED)
    }

    /**
     * The user opted out of the flow. The repository must persist
     * the SKIPPED state synchronously.
     */
    fun skip() {
        repository.markSkipped()
        lastResult = Result.SKIPPED
        tracker.track(AnalyticsEvent.ONBOARDING_SKIPPED)
    }

    /**
     * The user wants to see the flow again. Resets the persisted
     * state and rewinds the local step counter. The caller is
     * expected to navigate the UI back to the first step.
     */
    fun replay() {
        repository.resetForReplay()
        currentStep = 0
        lastResult = Result.PENDING
        tracker.track(AnalyticsEvent.ONBOARDING_REPLAYED)
    }

    /**
     * Mark the flow as having been observed. Called by the UI when
     * the onboarding screen first becomes visible so the analytics
     * funnel can count unique opens.
     */
    fun onShown() {
        tracker.track(AnalyticsEvent.ONBOARDING_STARTED)
    }

    enum class Result { PENDING, COMPLETED, SKIPPED }

    companion object {
        const val TOTAL_STEPS: Int = 3
        const val LAST_STEP: Int = TOTAL_STEPS - 1
    }
}

private object NoOpAnalyticsTracker : AnalyticsTracker {
    override fun track(event: AnalyticsEvent) = Unit
}
