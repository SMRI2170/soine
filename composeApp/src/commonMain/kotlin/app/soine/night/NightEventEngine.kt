package app.soine.night

import app.soine.relationship.RelationshipState
import app.soine.sleep.SleepSessionRecord
import app.soine.sleep.SleepSessionStatus

data class NightEventHistory(
    val recentEvents: List<NightEvent> = emptyList(),
)

data class NightEventSignals(
    val soundReactionCount: Int? = null,
    val briefWakeCount: Int? = null,
) {
    init {
        require(soundReactionCount == null || soundReactionCount >= 0) {
            "Sound reaction count must not be negative."
        }
        require(briefWakeCount == null || briefWakeCount >= 0) {
            "Brief wake count must not be negative."
        }
    }
}

data class NightEventEngineInput(
    val session: SleepSessionRecord,
    val relationship: RelationshipState,
    val history: NightEventHistory = NightEventHistory(),
    val signals: NightEventSignals = NightEventSignals(),
    val maxEvents: Int = DEFAULT_MAX_EVENTS,
) {
    init {
        require(session.status == SleepSessionStatus.COMPLETED) {
            "Night events can only be generated for a completed session."
        }
        require(session.endedAtEpochMillis != null) {
            "Completed session must have an end timestamp."
        }
        require(maxEvents in 0..MAX_EVENTS_LIMIT) {
            "maxEvents must be between 0 and $MAX_EVENTS_LIMIT."
        }
    }

    companion object {
        const val DEFAULT_MAX_EVENTS: Int = 3
        const val MAX_EVENTS_LIMIT: Int = 5
    }
}

/**
 * Pure, renderer-independent night-event generation boundary.
 *
 * Implementations must be deterministic: identical input and [rulesVersion]
 * produce the same ordered event list. Randomness therefore has to be derived
 * from persisted/session-stable input rather than wall-clock time.
 */
interface NightEventEngine {
    val rulesVersion: Int

    fun generate(input: NightEventEngineInput): List<NightEvent>
}
