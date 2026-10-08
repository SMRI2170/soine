package app.soine.analytics

import app.soine.navigation.BedtimeDestination

/**
 * Thin wrapper that translates UI/domain transitions into the fixed
 * analytics vocabulary. The wrapper is the only place that calls
 * [AnalyticsTracker.track]; downstream code passes domain-level
 * signals and never has to remember the vocabulary name.
 *
 * The wrapper also enforces the opt-out preference: every method is a
 * no-op when the user has analytics disabled.
 */
class BedtimeAnalytics(
    private val tracker: AnalyticsTracker,
    private val preferencesStore: AnalyticsPreferencesStore,
) {
    fun onDestinationReached(destination: BedtimeDestination) {
        if (!preferencesStore.read().enabled) return
        when (destination) {
            BedtimeDestination.Bedtime -> tracker.track(AnalyticsEvent.BEDTIME_SCREEN_VIEWED)
            is BedtimeDestination.Morning -> tracker.track(AnalyticsEvent.MORNING_SUMMARY_VIEWED)
            else -> Unit
        }
    }

    fun onSleepStarted(sessionId: String) {
        if (!preferencesStore.read().enabled) return
        require(sessionId.isNotBlank()) { "Sleep session id must not be blank." }
        tracker.track(AnalyticsEvent.SLEEP_SESSION_STARTED)
    }

    fun onSleepCompleted(sessionId: String) {
        if (!preferencesStore.read().enabled) return
        require(sessionId.isNotBlank()) { "Sleep session id must not be blank." }
        tracker.track(AnalyticsEvent.SLEEP_SESSION_COMPLETED)
    }

    fun onSessionRecovered(sessionId: String) {
        if (!preferencesStore.read().enabled) return
        require(sessionId.isNotBlank()) { "Sleep session id must not be blank." }
        tracker.track(AnalyticsEvent.SESSION_RECOVERED)
    }

    fun onNightMemoryViewed(entryCount: Int) {
        if (!preferencesStore.read().enabled) return
        require(entryCount >= 0) { "Night memory entry count must not be negative." }
        if (entryCount == 0) return
        tracker.track(AnalyticsEvent.NIGHT_MEMORY_OPENED)
    }

    fun onDreamDiscovered(dreamId: String) {
        if (!preferencesStore.read().enabled) return
        require(dreamId.isNotBlank()) { "Dream id must not be blank." }
        tracker.track(AnalyticsEvent.DREAM_DISCOVERED)
    }
}