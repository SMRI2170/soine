package app.soine.sleep

import kotlin.test.Test
import kotlin.test.assertEquals

class SleepSessionTest {
    @Test
    fun startAndFinishCalculatesDuration() {
        val finished = SleepSession().start(1_000).finish(6_000)
        assertEquals(SleepState.FINISHED, finished.state)
        assertEquals(5_000, finished.durationMillis())
    }
}
