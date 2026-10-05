package app.soine.sleep

import kotlin.test.Test
import kotlin.test.assertEquals

class SleepSummaryTest {
    @Test
    fun formatsHoursAndMinutes() {
        val summary = SleepSummary((7 * 60 + 32) * 60_000L)
        assertEquals("7時間32分", summary.displayDuration())
    }

    @Test
    fun formatsMinutesOnly() {
        assertEquals("42分", SleepSummary(42 * 60_000L).displayDuration())
    }
}
