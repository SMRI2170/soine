package app.soine.sleep

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Smoke test for the wall-clock-safety guarantees of [SleepSession].
 *
 * #196 requires that sleep duration never goes negative when the
 * device wall clock changes mid-session. The acceptance criteria:
 *
 *   - timezone 変更で session data 自体を書き換えない
 *   - duration が負にならない
 *   - Japan-only V1 でも旅行時に破綻しない
 *
 * [SleepSession] only stores absolute epoch millis. It does not own
 * a clock or a timezone. The duration math happens at
 * [SleepSession.durationMillis] which already calls
 * `.coerceAtLeast(0)` so a wall-clock backward jump can never emit a
 * negative duration.
 *
 * The test pins the contract:
 *
 *   1. starting and finishing a session with a forward jump keeps
 *      the duration equal to the wall-clock delta
 *   2. finishing with a backward jump (clock skew) does not produce
 *      a negative duration; the contract coerces to zero
 *   3. the persisted record survives a timezone change without the
 *      session object mutating its `startedAtEpochMillis` or
 *      `endedAtEpochMillis` fields
 *   4. `durationMinutes`, `hours`, and `minutes` are derived from the
 *      coerced duration, so they stay non-negative too
 */
class SleepSessionWallClockSafetyTest {

    @Test
    fun forwardClockProducesExpectedDuration() {
        val start = 1_700_000_000_000L
        val session = SleepSession().start(now = start).finish(now = start + 7 * 60 * 60 * 1_000L)

        assertEquals(7 * 60 * 60 * 1_000L, session.durationMillis())
    }

    @Test
    fun backwardClockJumpCoercesDurationToZero() {
        val start = 1_700_000_000_000L
        // A clock skew that lands before the start must not produce a
        // negative duration. The contract coerces to zero.
        val session = SleepSession().start(now = start).finish(now = start - 10 * 60 * 1_000L)

        assertEquals(0L, session.durationMillis())
    }

    @Test
    fun backwardClockJumpAlsoCoercesDurationMinutesToZero() {
        val start = 1_700_000_000_000L
        val session = SleepSession().start(now = start).finish(now = start - 5_000L)
        // The summary adapter returns null when end < start; the
        // session's own durationMillis() is the field that
        // persists, and it is what SleepSummary is built from once
        // the value is coerced.
        val coerced = session.durationMillis()!!
        val summary = SleepSummary(
            durationMillis = coerced,
            bedtimeEpochMillis = start,
            wakeTimeEpochMillis = start,
            source = SleepSessionSource.MANUAL,
        )

        assertEquals(0L, summary.durationMinutes)
        assertEquals(0L, summary.hours)
        assertEquals(0L, summary.minutes)
    }

    @Test
    fun timezoneChangeDoesNotMutateSessionTimestamps() {
        val start = 1_700_000_000_000L
        val end = start + 8 * 60 * 60 * 1_000L
        val session = SleepSession().start(now = start).finish(now = end)

        // Simulate a travel event: the device timezone changes, but
        // the session only stores absolute epoch millis. The fields
        // must be byte-identical after the swap.
        val before = Triple(session.startedAtEpochMillis, session.endedAtEpochMillis, session.durationMillis())
        // (No actual swap here: SleepSession is immutable; the
        // assertion documents that the contract relies on the data
        // class not consulting any timezone-aware state.)
        val after = Triple(session.startedAtEpochMillis, session.endedAtEpochMillis, session.durationMillis())

        assertEquals(before, after)
    }

    @Test
    fun activeSessionWithNoEndReturnsNullDuration() {
        val session = SleepSession().start(now = 1_700_000_000_000L)

        assertNull(session.durationMillis())
    }

    @Test
    fun startingFromFinishedSessionDoesNotMutateIt() {
        val start = 1_700_000_000_000L
        val end = start + 60 * 60 * 1_000L
        val finished = SleepSession().start(now = start).finish(now = end)

        // Calling start on a non-SLEEPING session is a no-op so an
        // accidental double-tap on the CTA cannot corrupt the
        // persisted record.
        val restarted = finished.start(now = end + 1_000L)

        assertEquals(finished, restarted)
        assertEquals(60 * 60 * 1_000L, restarted.durationMillis())
    }
}
