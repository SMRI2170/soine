package app.soine.health

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlatformSleepNormalizerTest {

    @Test
    fun stageRecordsBecomeOrderedSignalsWithSourceAttribution() {
        val signals = normalizePlatformSleepRecord(
            record = PlatformSleepRecord(
                startEpochMillis = 1_000,
                endEpochMillis = 5_000,
                sourceId = "com.example.watch",
                stages = listOf(
                    PlatformSleepStage(3_000, 4_000, SleepSignalType.AWAKE),
                    PlatformSleepStage(1_000, 3_000, SleepSignalType.ASLEEP),
                ),
            ),
            provider = SleepSignalProvider.HEALTH_CONNECT,
        )

        assertEquals(
            listOf(SleepSignalType.ASLEEP, SleepSignalType.AWAKE),
            signals.map(SleepSignal::type),
        )
        assertEquals("com.example.watch", signals.first().source.sourceId)
        assertEquals(
            SleepSignalProvider.HEALTH_CONNECT,
            signals.first().source.provider,
        )
    }

    @Test
    fun sessionWithoutStagesFallsBackToAsleepInterval() {
        val signals = normalizePlatformSleepRecord(
            record = PlatformSleepRecord(
                startEpochMillis = 10_000,
                endEpochMillis = 20_000,
            ),
            provider = SleepSignalProvider.HEALTH_CONNECT,
        )

        assertEquals(1, signals.size)
        assertEquals(SleepSignalType.ASLEEP, signals.single().type)
        assertEquals(10_000, signals.single().startEpochMillis)
        assertEquals(20_000, signals.single().endEpochMillis)
    }

    @Test
    fun invalidStageIsDroppedWithoutInvalidatingSession() {
        val signals = normalizePlatformSleepRecord(
            record = PlatformSleepRecord(
                startEpochMillis = 10_000,
                endEpochMillis = 20_000,
                stages = listOf(
                    PlatformSleepStage(12_000, 12_000, SleepSignalType.UNKNOWN),
                ),
            ),
            provider = SleepSignalProvider.HEALTH_CONNECT,
        )

        assertEquals(1, signals.size)
        assertEquals(SleepSignalType.ASLEEP, signals.single().type)
    }

    @Test
    fun healthKitInBedFixtureKeepsProviderAndSource() {
        val signals = normalizePlatformSleepRecord(
            record = PlatformSleepRecord(
                startEpochMillis = 30_000,
                endEpochMillis = 40_000,
                sourceId = "com.apple.health",
                sourceName = "Health",
                stages = listOf(
                    PlatformSleepStage(
                        startEpochMillis = 30_000,
                        endEpochMillis = 40_000,
                        type = SleepSignalType.IN_BED,
                    ),
                ),
            ),
            provider = SleepSignalProvider.HEALTH_KIT,
        )

        assertEquals(SleepSignalType.IN_BED, signals.single().type)
        assertEquals(SleepSignalProvider.HEALTH_KIT, signals.single().source.provider)
        assertEquals("com.apple.health", signals.single().source.sourceId)
        assertEquals("Health", signals.single().source.sourceName)
    }

    @Test
    fun invalidSessionProducesNoSignal() {
        val signals = normalizePlatformSleepRecord(
            record = PlatformSleepRecord(
                startEpochMillis = 20_000,
                endEpochMillis = 20_000,
            ),
            provider = SleepSignalProvider.HEALTH_CONNECT,
        )

        assertTrue(signals.isEmpty())
    }
}
