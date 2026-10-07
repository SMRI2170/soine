package app.soine.sound

import kotlin.math.PI
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PrototypeSoundFrameClassifierTest {
    @Test
    fun silenceProducesNoEvent() {
        assertNull(
            PrototypeSoundFrameClassifier.classify(
                samples = ShortArray(SAMPLE_RATE),
                sampleRateHz = SAMPLE_RATE,
                occurredAtEpochMillis = 1_000L,
            ),
        )
    }

    @Test
    fun dcBiasProducesNoEvent() {
        assertNull(
            PrototypeSoundFrameClassifier.classify(
                samples = ShortArray(SAMPLE_RATE) { 1_500 },
                sampleRateHz = SAMPLE_RATE,
                occurredAtEpochMillis = 1_500L,
            ),
        )
    }

    @Test
    fun lowFrequencyPeriodicFrameProducesSnoreLikeEvent() {
        val result = PrototypeSoundFrameClassifier.classify(
            samples = sineWave(frequencyHz = 100.0, amplitude = 0.20),
            sampleRateHz = SAMPLE_RATE,
            occurredAtEpochMillis = 2_000L,
        )

        assertEquals(SoundEventType.SNORE_LIKE, result?.type)
        assertEquals(SoundEventSource.ON_DEVICE_MICROPHONE, result?.source)
        assertEquals(PrototypeSoundFrameClassifier.MODEL_VERSION, result?.modelVersion)
        assertTrue(requireNotNull(result).confidence in 0.0..1.0)
    }

    @Test
    fun higherFrequencyActiveFrameProducesVocalizationEvent() {
        val result = PrototypeSoundFrameClassifier.classify(
            samples = sineWave(frequencyHz = 700.0, amplitude = 0.20),
            sampleRateHz = SAMPLE_RATE,
            occurredAtEpochMillis = 3_000L,
        )

        assertEquals(SoundEventType.VOCALIZATION, result?.type)
    }

    @Test
    fun highAmplitudeTransientProducesLoudSoundEvent() {
        val frame = ShortArray(SAMPLE_RATE)
        repeat(1_000) { index ->
            frame[index] = if (index % 2 == 0) 30_000 else -30_000
        }

        val result = PrototypeSoundFrameClassifier.classify(
            samples = frame,
            sampleRateHz = SAMPLE_RATE,
            occurredAtEpochMillis = 4_000L,
        )

        assertEquals(SoundEventType.LOUD_SOUND, result?.type)
    }

    private fun sineWave(
        frequencyHz: Double,
        amplitude: Double,
    ): ShortArray = ShortArray(SAMPLE_RATE) { index ->
        val normalized = amplitude * sin(2.0 * PI * frequencyHz * index / SAMPLE_RATE)
        (normalized * Short.MAX_VALUE).toInt().toShort()
    }

    private companion object {
        const val SAMPLE_RATE = 8_000
    }
}
