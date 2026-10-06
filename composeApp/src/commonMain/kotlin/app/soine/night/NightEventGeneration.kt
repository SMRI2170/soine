package app.soine.night

import app.soine.sleep.SleepSessionRecord
import app.soine.sleep.SleepSessionStatus

/**
 * Stable identity for one night's generated story.
 *
 * Persist this metadata with generated events when persistence is introduced.
 * Re-opening an existing night must reuse its stored algorithmVersion even
 * after the app ships a newer generator.
 */
data class NightEventGenerationMetadata(
    val sessionId: String,
    val seed: Long,
    val algorithmVersion: Int,
) {
    init {
        require(sessionId.isNotBlank()) { "Session id must not be blank." }
        require(algorithmVersion > 0) { "Algorithm version must be positive." }
    }

    companion object {
        const val CURRENT_ALGORITHM_VERSION: Int = 1

        fun forSession(
            session: SleepSessionRecord,
            algorithmVersion: Int = CURRENT_ALGORITHM_VERSION,
        ): NightEventGenerationMetadata {
            require(session.status == SleepSessionStatus.COMPLETED) {
                "Night-event seed requires a completed session."
            }
            val endedAt = requireNotNull(session.endedAtEpochMillis) {
                "Completed session must have an end timestamp."
            }
            return NightEventGenerationMetadata(
                sessionId = session.id,
                seed = StableNightSeed.hash(
                    session.id,
                    session.startedAtEpochMillis,
                    endedAt,
                ),
                algorithmVersion = algorithmVersion,
            )
        }

        /**
         * Compatibility rule: once a night has metadata, retain it forever.
         * A newer current algorithm is used only for sessions without metadata.
         */
        fun reuseOrCreate(
            session: SleepSessionRecord,
            persisted: NightEventGenerationMetadata?,
            currentAlgorithmVersion: Int = CURRENT_ALGORITHM_VERSION,
        ): NightEventGenerationMetadata {
            require(persisted == null || persisted.sessionId == session.id) {
                "Persisted generation metadata belongs to another session."
            }
            return persisted ?: forSession(session, currentAlgorithmVersion)
        }
    }
}

/**
 * Version-independent seed derivation. Keep this function unchanged; changes
 * to event selection belong behind algorithmVersion instead.
 */
internal object StableNightSeed {
    private const val FNV_OFFSET_BASIS: Long = -3750763034362895579L
    private const val FNV_PRIME: Long = 1099511628211L

    fun hash(sessionId: String, startedAt: Long, endedAt: Long): Long {
        var hash = FNV_OFFSET_BASIS

        fun mixByte(value: Int) {
            hash = hash xor (value and 0xff).toLong()
            hash *= FNV_PRIME
        }

        sessionId.encodeToByteArray().forEach { mixByte(it.toInt()) }
        mixByte(0xff)
        mixLong(startedAt, ::mixByte)
        mixByte(0xfe)
        mixLong(endedAt, ::mixByte)
        return hash
    }

    private fun mixLong(value: Long, mixByte: (Int) -> Unit) {
        repeat(8) { index ->
            mixByte((value ushr (index * 8)).toInt())
        }
    }
}

/**
 * Small explicit PRNG whose behavior is controlled by our algorithm version,
 * rather than Kotlin's Random implementation.
 */
class NightEventRandom(seed: Long) {
    private var state: Long = if (seed != 0L) seed else GOLDEN_GAMMA

    fun nextLong(): Long {
        var x = state
        x = x xor (x shl 13)
        x = x xor (x ushr 7)
        x = x xor (x shl 17)
        state = x
        return x
    }

    fun nextInt(bound: Int): Int {
        require(bound > 0) { "Bound must be positive." }
        val nonNegative = nextLong() ushr 1
        return (nonNegative % bound.toLong()).toInt()
    }

    companion object {
        private const val GOLDEN_GAMMA: Long = -7046029254386353131L
    }
}
