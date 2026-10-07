package app.soine.sound

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Lightweight heuristic used only by the Android #55 feasibility spike.
 *
 * It intentionally makes no medical claim. The classifier extracts amplitude,
 * zero-crossing rate and a coarse low-frequency periodicity score from an
 * in-memory PCM frame and immediately returns a derived [SoundEvent] or null.
 * It never owns or persists raw audio.
 */
object PrototypeSoundFrameClassifier {
    const val MODEL_VERSION: String = "android-heuristic-v1"

    fun classify(
        samples: ShortArray,
        sampleCount: Int = samples.size,
        sampleRateHz: Int,
        occurredAtEpochMillis: Long,
    ): SoundEvent? {
        require(sampleCount in 1..samples.size) { "sampleCount must fit the PCM buffer." }
        require(sampleRateHz > 0) { "sampleRateHz must be positive." }
        require(occurredAtEpochMillis >= 0L) { "Event timestamp must not be negative." }
        if (sampleCount < MIN_FRAME_SAMPLES) return null

        var sumSquares = 0.0
        var peak = 0.0
        var zeroCrossings = 0
        var previous = samples[0].toDouble() / PCM_SCALE

        for (index in 0 until sampleCount) {
            val normalized = samples[index].toDouble() / PCM_SCALE
            sumSquares += normalized * normalized
            peak = maxOf(peak, abs(normalized))
            if (index > 0 && (normalized >= 0.0) != (previous >= 0.0)) {
                zeroCrossings += 1
            }
            previous = normalized
        }

        val rms = sqrt(sumSquares / sampleCount)
        if (rms < MIN_ACTIVE_RMS) return null

        val zeroCrossingRate =
            zeroCrossings.toDouble() / (sampleCount - 1).coerceAtLeast(1)
        val periodicity = if (rms >= SNORE_MIN_RMS) {
            lowFrequencyPeriodicity(
                samples = samples,
                sampleCount = sampleCount,
                sampleRateHz = sampleRateHz,
            )
        } else {
            0.0
        }

        val (type, confidence) = when {
            peak >= LOUD_MIN_PEAK && rms >= LOUD_MIN_RMS ->
                SoundEventType.LOUD_SOUND to
                    (0.55 + 0.45 * maxOf(peak, (rms / 0.35))).coerceIn(0.0, 1.0)

            rms >= SNORE_MIN_RMS &&
                zeroCrossingRate <= SNORE_MAX_ZERO_CROSSING_RATE &&
                periodicity >= SNORE_MIN_PERIODICITY ->
                SoundEventType.SNORE_LIKE to
                    (0.45 + 0.55 * periodicity).coerceIn(0.0, 1.0)

            rms >= VOCALIZATION_MIN_RMS &&
                zeroCrossingRate in VOCALIZATION_ZERO_CROSSING_RANGE ->
                SoundEventType.VOCALIZATION to
                    (0.50 + (rms / 0.40) * 0.35).coerceIn(0.0, 0.90)

            else -> return null
        }

        return SoundEvent(
            type = type,
            occurredAtEpochMillis = occurredAtEpochMillis,
            confidence = confidence,
            source = SoundEventSource.ON_DEVICE_MICROPHONE,
            modelVersion = MODEL_VERSION,
        )
    }

    private fun lowFrequencyPeriodicity(
        samples: ShortArray,
        sampleCount: Int,
        sampleRateHz: Int,
    ): Double {
        val minLag = maxOf(1, sampleRateHz / MAX_PERIODIC_FREQUENCY_HZ)
        val maxLag = minOf(
            sampleCount / 2,
            maxOf(minLag, sampleRateHz / MIN_PERIODIC_FREQUENCY_HZ),
        )
        if (maxLag <= minLag) return 0.0

        var best = 0.0
        var lag = minLag
        while (lag <= maxLag) {
            var numerator = 0.0
            var leftEnergy = 0.0
            var rightEnergy = 0.0

            for (index in lag until sampleCount) {
                val left = samples[index].toDouble() / PCM_SCALE
                val right = samples[index - lag].toDouble() / PCM_SCALE
                numerator += left * right
                leftEnergy += left * left
                rightEnergy += right * right
            }

            if (leftEnergy > 0.0 && rightEnergy > 0.0) {
                best = maxOf(
                    best,
                    numerator / sqrt(leftEnergy * rightEnergy),
                )
            }
            lag += AUTOCORRELATION_LAG_STEP
        }
        return best.coerceIn(0.0, 1.0)
    }

    private const val PCM_SCALE = 32_768.0
    private const val MIN_FRAME_SAMPLES = 128
    private const val MIN_ACTIVE_RMS = 0.02

    private const val LOUD_MIN_PEAK = 0.88
    private const val LOUD_MIN_RMS = 0.06

    private const val SNORE_MIN_RMS = 0.035
    private const val SNORE_MAX_ZERO_CROSSING_RATE = 0.08
    private const val SNORE_MIN_PERIODICITY = 0.55

    private const val VOCALIZATION_MIN_RMS = 0.03
    private val VOCALIZATION_ZERO_CROSSING_RANGE = 0.08..0.45

    private const val MIN_PERIODIC_FREQUENCY_HZ = 50
    private const val MAX_PERIODIC_FREQUENCY_HZ = 300
    private const val AUTOCORRELATION_LAG_STEP = 4
}
