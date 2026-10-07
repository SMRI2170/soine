package app.soine.analytics

import app.soine.navigation.BedtimeDestination
import app.soine.sleep.SleepSessionRecord
import app.soine.sleep.SleepSessionSource
import app.soine.sleep.SleepSessionStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class BedtimeAnalyticsTest {

    @Test
    fun optedOutTrackerReceivesNothing() {
        val recorder = FakeAnalyticsTracker()
        val store = InMemoryAnalyticsPreferencesStore(enabled = false)
        val analytics = BedtimeAnalytics(recorder, store)

        analytics.onDestinationReached(BedtimeDestination.Bedtime)
        analytics.onSleepStarted("session-1")
        analytics.onSleepCompleted("session-1")
        analytics.onSessionRecovered("session-1")
        analytics.onNightMemoryViewed(2)
        analytics.onDreamDiscovered("dream-1")

        assertTrue(recorder.events.isEmpty())
    }

    @Test
    fun bedtimeScreenEmitsBedtimeScreenViewed() {
        val recorder = FakeAnalyticsTracker()
        val store = InMemoryAnalyticsPreferencesStore(enabled = true)
        val analytics = BedtimeAnalytics(recorder, store)

        analytics.onDestinationReached(BedtimeDestination.Bedtime)

        assertEquals(
            listOf(AnalyticsEvent.BEDTIME_SCREEN_VIEWED),
            recorder.events,
        )
    }

    @Test
    fun morningDestinationEmitsMorningSummaryViewed() {
        val recorder = FakeAnalyticsTracker()
        val store = InMemoryAnalyticsPreferencesStore(enabled = true)
        val analytics = BedtimeAnalytics(recorder, store)
        val session = sleepSession()

        analytics.onDestinationReached(BedtimeDestination.Morning(session))

        assertEquals(
            listOf(AnalyticsEvent.MORNING_SUMMARY_VIEWED),
            recorder.events,
        )
    }

    @Test
    fun sleepStartedAndCompletedEmitTheirEvents() {
        val recorder = FakeAnalyticsTracker()
        val store = InMemoryAnalyticsPreferencesStore(enabled = true)
        val analytics = BedtimeAnalytics(recorder, store)

        analytics.onSleepStarted("session-1")
        analytics.onSleepCompleted("session-1")

        assertEquals(
            listOf(
                AnalyticsEvent.SLEEP_SESSION_STARTED,
                AnalyticsEvent.SLEEP_SESSION_COMPLETED,
            ),
            recorder.events,
        )
    }

    @Test
    fun sessionRecoveryEmitsSessionRecovered() {
        val recorder = FakeAnalyticsTracker()
        val store = InMemoryAnalyticsPreferencesStore(enabled = true)
        val analytics = BedtimeAnalytics(recorder, store)

        analytics.onSessionRecovered("session-1")

        assertEquals(
            listOf(AnalyticsEvent.SESSION_RECOVERED),
            recorder.events,
        )
    }

    @Test
    fun nightMemoryViewedEmitsOnlyWhenEntriesExist() {
        val recorder = FakeAnalyticsTracker()
        val store = InMemoryAnalyticsPreferencesStore(enabled = true)
        val analytics = BedtimeAnalytics(recorder, store)

        analytics.onNightMemoryViewed(0)
        assertTrue(recorder.events.isEmpty())

        analytics.onNightMemoryViewed(1)
        assertEquals(
            listOf(AnalyticsEvent.NIGHT_MEMORY_OPENED),
            recorder.events,
        )
    }

    @Test
    fun dreamDiscoveredEmitsDreamDiscovered() {
        val recorder = FakeAnalyticsTracker()
        val store = InMemoryAnalyticsPreferencesStore(enabled = true)
        val analytics = BedtimeAnalytics(recorder, store)

        analytics.onDreamDiscovered("dream-1")

        assertEquals(
            listOf(AnalyticsEvent.DREAM_DISCOVERED),
            recorder.events,
        )
    }

    @Test
    fun blankSessionIdIsRejected() {
        val store = InMemoryAnalyticsPreferencesStore(enabled = true)
        val analytics = BedtimeAnalytics(FakeAnalyticsTracker(), store)

        assertFailsWith<IllegalArgumentException> {
            analytics.onSleepStarted(" ")
        }
    }

    @Test
    fun negativeNightMemoryCountIsRejected() {
        val store = InMemoryAnalyticsPreferencesStore(enabled = true)
        val analytics = BedtimeAnalytics(FakeAnalyticsTracker(), store)

        assertFailsWith<IllegalArgumentException> {
            analytics.onNightMemoryViewed(-1)
        }
    }

    private fun sleepSession() = SleepSessionRecord(
        id = "session-1",
        startedAtEpochMillis = 0,
        endedAtEpochMillis = null,
        status = SleepSessionStatus.COMPLETED,
        source = SleepSessionSource.MANUAL,
        createdAtEpochMillis = 0,
        updatedAtEpochMillis = 0,
        schemaVersion = 2,
    )
}

private class InMemoryAnalyticsPreferencesStore(
    enabled: Boolean = false,
    installId: String? = null,
) : AnalyticsPreferencesStore {
    private var current: AnalyticsPreferences = AnalyticsPreferences(
        enabled = enabled,
        installId = installId,
    )

    override fun read(): AnalyticsPreferences = current

    override fun write(preferences: AnalyticsPreferences) {
        current = preferences
    }
}