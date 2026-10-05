package app.soine.sleep

import kotlin.test.*

class SleepSummaryTest {
    @Test fun completedRecordProducesStableSummary() {
        val record = completed(start = 1_000, end = 7_201_000, source = SleepSessionSource.RECOVERED)
        val summary = assertNotNull(record.summary())
        assertEquals(7_200_000, summary.durationMillis)
        assertEquals(1_000, summary.bedtimeEpochMillis)
        assertEquals(7_201_000, summary.wakeTimeEpochMillis)
        assertEquals(SleepSessionSource.RECOVERED, summary.source)
        assertNull(summary.estimates)
    }

    @Test fun formatsHoursAndMinutes() =
        assertEquals("7時間32分", completed(0, (7 * 60 + 32) * 60_000L).summary()!!.displayDuration())

    @Test fun zeroDurationIsValid() =
        assertEquals("0分", completed(5_000, 5_000).summary()!!.displayDuration())

    @Test fun longDurationIsNotArtificiallyClamped() =
        assertEquals(36 * 60 * 60_000L, completed(0, 36 * 60 * 60_000L).summary()!!.durationMillis)

    @Test fun invalidTimestampReturnsNoSummary() =
        assertNull(completed(5_000, 4_999).summary())

    @Test fun activeOrMissingEndReturnsNoSummary() {
        assertNull(record(SleepSessionStatus.SLEEPING, 1_000, null).summary())
        assertNull(record(SleepSessionStatus.COMPLETED, 1_000, null).summary())
    }

    @Test fun durationIsTimezoneIndependentAndHandlesDayBoundary() {
        // Epoch arithmetic remains correct across midnight and later timezone changes.
        assertEquals(20 * 60_000L, completed(86_390_000L, 87_590_000L).summary()!!.durationMillis)
    }

    private fun completed(start: Long, end: Long, source: SleepSessionSource = SleepSessionSource.MANUAL) =
        record(SleepSessionStatus.COMPLETED, start, end, source)

    private fun record(status: SleepSessionStatus, start: Long, end: Long?, source: SleepSessionSource = SleepSessionSource.MANUAL) =
        SleepSessionRecord(
            id = "night",
            startedAtEpochMillis = start,
            endedAtEpochMillis = end,
            status = status,
            source = source,
            createdAtEpochMillis = start,
            updatedAtEpochMillis = end ?: start,
        )
}
