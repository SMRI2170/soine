package app.soine.sleep

import app.soine.time.JapanLocalTimeZone
import app.soine.time.LocalTimeZones
import app.soine.time.UtcTimeZone
import app.soine.time.format.DisplayFormatters
import app.soine.time.format.JapaneseDisplayFormatter
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * Smoke test for the cross-binding interaction that #196 calls out:
 *
 *   - the user is in the middle of a sleep session
 *   - the device timezone changes (manual travel, DST, etc.)
 *   - the active session must keep its `startedAtEpochMillis` and
 *     `endedAtEpochMillis` byte-for-byte stable
 *   - the persisted summary must keep its `durationMillis` stable
 *   - the displayed string (e.g. the next-morning "morning summary"
 *     wall-clock label) is allowed to change because the renderer
 *     reads `LocalTimeZones.current` at display time
 *
 * The test swaps the [LocalTimeZones] and [DisplayFormatters]
 * bindings while a session is "in progress" and asserts the absolute
 * timestamps are untouched.
 */
class ActiveSessionTimezoneChangeTest {

    private val previousTimeZone = LocalTimeZones.current
    private val previousFormatter = DisplayFormatters.current

    @AfterTest
    fun restoreBindings() {
        LocalTimeZones.current = previousTimeZone
        DisplayFormatters.current = previousFormatter
    }

    @Test
    fun activeSessionTimestampsSurviveTimezoneSwap() {
        val start = 1_700_000_000_000L
        val session = SleepSession().start(now = start)

        // Before travel: JST.
        LocalTimeZones.current = JapanLocalTimeZone
        val beforeStart = session.startedAtEpochMillis
        val beforeDisplay = DisplayFormatters.current.formatNightEventTime(start, LocalTimeZones.current)

        // After travel: UTC. The session object is untouched because
        // it does not read any timezone-aware state.
        LocalTimeZones.current = UtcTimeZone
        val afterStart = session.startedAtEpochMillis
        val afterDisplay = DisplayFormatters.current.formatNightEventTime(start, LocalTimeZones.current)

        assertEquals(beforeStart, afterStart)
        assertNotEquals(beforeDisplay, afterDisplay)
    }

    @Test
    fun completedSummaryDurationSurvivesTimezoneSwap() {
        val start = 1_700_000_000_000L
        val end = start + 8 * 60 * 60 * 1_000L
        val session = SleepSession().start(now = start).finish(now = end)

        LocalTimeZones.current = JapanLocalTimeZone
        val beforeDuration = session.durationMillis()

        LocalTimeZones.current = UtcTimeZone
        val afterDuration = session.durationMillis()

        assertEquals(beforeDuration, afterDuration)
        assertEquals(8 * 60 * 60 * 1_000L, afterDuration)
    }

    @Test
    fun formatterSwapIsolatedFromSessionData() {
        val start = 1_700_000_000_000L
        val session = SleepSession().start(now = start).finish(now = start + 3_600_000L)

        // Swap the formatter binding while a session is "in flight"
        // and assert the session's persisted duration is unaffected.
        val beforeDuration = session.durationMillis()
        DisplayFormatters.current = JapaneseDisplayFormatter
        val afterDuration = session.durationMillis()

        assertEquals(beforeDuration, afterDuration)
    }

    @Test
    fun sessionRecordRoundTripPreservesTimestamps() {
        val startedAt = 1_700_000_000_000L
        val endedAt = startedAt + 6 * 60 * 60 * 1_000L
        val created = 1_700_000_000_000L
        val updated = endedAt
        val record = SleepSessionRecord(
            id = "night-1",
            startedAtEpochMillis = startedAt,
            endedAtEpochMillis = endedAt,
            status = SleepSessionStatus.COMPLETED,
            source = SleepSessionSource.MANUAL,
            createdAtEpochMillis = created,
            updatedAtEpochMillis = updated,
        )

        LocalTimeZones.current = JapanLocalTimeZone
        val before = record.copy()
        LocalTimeZones.current = UtcTimeZone
        val after = record.copy()

        // The copy is structurally equal under any timezone: the
        // record only carries absolute epoch millis.
        assertEquals(before, after)
    }
}
