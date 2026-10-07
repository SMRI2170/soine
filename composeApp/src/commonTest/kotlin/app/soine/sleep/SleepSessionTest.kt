package app.soine.sleep

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SleepSessionTest {
    @Test
    fun startAndFinishCalculatesDuration() {
        val finished = SleepSession().start(1_000).finish(6_000)
        assertEquals(SleepState.FINISHED, finished.state)
        assertEquals(5_000, finished.durationMillis())
    }

    @Test
    fun wallClockBackwardJumpKeepsDurationNonNegative() {
        // If the system clock jumps backwards mid-session (DST reset,
        // NTP correction, manual adjustment) the absolute timestamps may
        // record an end value smaller than the start value. Duration
        // must clamp at zero rather than report a negative duration.
        val finished = SleepSession().start(10_000).finish(5_000)

        assertEquals(0L, finished.durationMillis())
    }

    @Test
    fun unfinishedSessionHasNoDuration() {
        val sleeping = SleepSession().start(1_000)

        assertEquals(SleepState.SLEEPING, sleeping.state)
        assertNull(sleeping.durationMillis())
    }
}
