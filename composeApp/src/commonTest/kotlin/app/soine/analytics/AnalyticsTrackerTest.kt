package app.soine.analytics

import kotlin.test.Test
import kotlin.test.assertEquals

class AnalyticsTrackerTest {

    @Test
    fun eventNamesMatchPublishedVocabulary() {
        assertEquals(
            listOf(
                "bedtime_screen_viewed",
                "sleep_session_started",
                "session_recovered",
                "sleep_session_completed",
                "morning_summary_viewed",
                "night_memory_opened",
                "dream_discovered",
                "onboarding_started",
                "onboarding_step_viewed",
                "onboarding_completed",
                "onboarding_skipped",
                "onboarding_replayed",
            ),
            AnalyticsEvent.entries.map { it.eventName },
        )
    }

    @Test
    fun fakeTrackerRecordsEventsInOrder() {
        val tracker = FakeAnalyticsTracker()

        tracker.track(AnalyticsEvent.BEDTIME_SCREEN_VIEWED)
        tracker.track(AnalyticsEvent.SLEEP_SESSION_STARTED)

        assertEquals(
            listOf(
                AnalyticsEvent.BEDTIME_SCREEN_VIEWED,
                AnalyticsEvent.SLEEP_SESSION_STARTED,
            ),
            tracker.events,
        )
    }

    @Test
    fun noOpTrackerAcceptsEveryEvent() {
        AnalyticsEvent.entries.forEach(NoOpAnalyticsTracker::track)
    }
}
