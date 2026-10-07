package app.soine.health

import app.soine.sleep.SleepSessionRecord
import app.soine.sleep.SleepSessionSource
import app.soine.sleep.SleepSessionStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HealthSleepMergePolicyTest {

    @Test
    fun manualSummaryRemainsAuthoritative() {
        val record = completed(start = 1_000, end = 11_000)

        val enriched = record.summaryWithHealth(
            listOf(
                signal(
                    start = 2_000,
                    end = 9_000,
                    type = SleepSignalType.ASLEEP,
                ),
            ),
        )!!

        assertEquals(10_000, enriched.manual.durationMillis)
        assertEquals(1_000, enriched.manual.bedtimeEpochMillis)
        assertEquals(11_000, enriched.manual.wakeTimeEpochMillis)
        assertEquals(SleepSessionSource.MANUAL, enriched.manual.source)
        assertEquals("一緒に寝た時間", enriched.manualLabel)
    }

    @Test
    fun signalIntervalsAreClampedToManualSession() {
        val estimate = completed(10_000, 20_000)
            .summaryWithHealth(
                listOf(
                    signal(
                        start = 5_000,
                        end = 25_000,
                        type = SleepSignalType.ASLEEP,
                    ),
                ),
            )!!
            .healthEstimates
            .single()

        assertEquals(10_000, estimate.estimatedSleepStartEpochMillis)
        assertEquals(20_000, estimate.estimatedWakeEpochMillis)
        assertEquals(10_000, estimate.asleepMillis)
    }

    @Test
    fun awakeWinsWhenSignalsConflict() {
        val estimate = completed(0, 10_000)
            .summaryWithHealth(
                listOf(
                    signal(0, 10_000, SleepSignalType.ASLEEP),
                    signal(4_000, 6_000, SleepSignalType.AWAKE),
                ),
            )!!
            .healthEstimates
            .single()

        assertEquals(8_000, estimate.asleepMillis)
        assertEquals(2_000, estimate.awakeMillis)
        assertEquals(0L, estimate.estimatedSleepStartEpochMillis)
        assertEquals(10_000, estimate.estimatedWakeEpochMillis)
    }

    @Test
    fun inBedDoesNotCountAsAsleepAndDetailedSleepWinsOverIt() {
        val estimate = completed(0, 10_000)
            .summaryWithHealth(
                listOf(
                    signal(0, 10_000, SleepSignalType.IN_BED),
                    signal(2_000, 8_000, SleepSignalType.ASLEEP),
                ),
            )!!
            .healthEstimates
            .single()

        assertEquals(6_000, estimate.asleepMillis)
        assertEquals(0L, estimate.awakeMillis)
        assertEquals(2_000, estimate.estimatedSleepStartEpochMillis)
        assertEquals(8_000, estimate.estimatedWakeEpochMillis)
    }

    @Test
    fun providersAreKeptSeparateInsteadOfSilentlyBlended() {
        val enriched = completed(0, 10_000)
            .summaryWithHealth(
                listOf(
                    signal(
                        1_000,
                        7_000,
                        SleepSignalType.ASLEEP,
                        provider = SleepSignalProvider.HEALTH_CONNECT,
                    ),
                    signal(
                        2_000,
                        8_000,
                        SleepSignalType.ASLEEP,
                        provider = SleepSignalProvider.HEALTH_KIT,
                    ),
                ),
            )!!

        assertEquals(2, enriched.healthEstimates.size)
        assertEquals(
            listOf(
                SleepSignalProvider.HEALTH_CONNECT,
                SleepSignalProvider.HEALTH_KIT,
            ),
            enriched.healthEstimates.map(HealthSleepEstimate::provider),
        )
        assertEquals("推定睡眠（Health Connect）", enriched.healthEstimates[0].displayLabel)
        assertEquals("推定睡眠（Apple Health）", enriched.healthEstimates[1].displayLabel)
    }

    @Test
    fun confidenceUsesConservativeMinimumOnlyWhenComplete() {
        val record = completed(0, 10_000)

        val complete = record.summaryWithHealth(
            listOf(
                signal(1_000, 4_000, SleepSignalType.ASLEEP, confidence = 0.9),
                signal(4_000, 8_000, SleepSignalType.ASLEEP, confidence = 0.7),
            ),
        )!!.healthEstimates.single()
        assertEquals(0.7, complete.confidence)

        val incomplete = record.summaryWithHealth(
            listOf(
                signal(1_000, 4_000, SleepSignalType.ASLEEP, confidence = 0.9),
                signal(4_000, 8_000, SleepSignalType.ASLEEP, confidence = null),
            ),
        )!!.healthEstimates.single()
        assertNull(incomplete.confidence)
    }

    @Test
    fun noOverlappingAsleepSignalProducesNoHealthEstimate() {
        val enriched = completed(10_000, 20_000)
            .summaryWithHealth(
                listOf(
                    signal(0, 5_000, SleepSignalType.ASLEEP),
                    signal(12_000, 15_000, SleepSignalType.IN_BED),
                ),
            )!!

        assertTrue(enriched.healthEstimates.isEmpty())
    }

    @Test
    fun incompleteManualSessionCannotBeEnriched() {
        val active = SleepSessionRecord(
            id = "night",
            startedAtEpochMillis = 1_000,
            endedAtEpochMillis = null,
            status = SleepSessionStatus.SLEEPING,
            source = SleepSessionSource.MANUAL,
            createdAtEpochMillis = 1_000,
            updatedAtEpochMillis = 1_000,
        )

        assertNull(active.summaryWithHealth(emptyList()))
    }

    private fun completed(start: Long, end: Long) = SleepSessionRecord(
        id = "night",
        startedAtEpochMillis = start,
        endedAtEpochMillis = end,
        status = SleepSessionStatus.COMPLETED,
        source = SleepSessionSource.MANUAL,
        createdAtEpochMillis = start,
        updatedAtEpochMillis = end,
    )

    private fun signal(
        start: Long,
        end: Long,
        type: SleepSignalType,
        provider: SleepSignalProvider = SleepSignalProvider.HEALTH_CONNECT,
        confidence: Double? = null,
    ) = SleepSignal(
        startEpochMillis = start,
        endEpochMillis = end,
        type = type,
        source = SleepSignalSource(provider),
        confidence = confidence,
    )
}
