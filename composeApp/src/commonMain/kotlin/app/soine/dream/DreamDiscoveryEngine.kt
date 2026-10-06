package app.soine.dream

import app.soine.night.RarityBand
import app.soine.relationship.RelationshipState
import app.soine.sleep.SleepSessionRecord
import app.soine.sleep.SleepSessionStatus

data class DreamDiscoveryDecision(
    val sessionId: String,
    val algorithmVersion: Int,
    val dreamId: String?,
    val evaluatedAtEpochMillis: Long,
) {
    init {
        require(sessionId.isNotBlank()) { "Session id must not be blank." }
        require(algorithmVersion > 0) { "Algorithm version must be positive." }
        require(dreamId == null || dreamId.isNotBlank()) {
            "Dream id must be null or non-blank."
        }
        require(evaluatedAtEpochMillis >= 0) {
            "Evaluation timestamp must not be negative."
        }
    }
}

data class DreamDiscoveryInput(
    val session: SleepSessionRecord,
    val relationship: RelationshipState,
    val season: DreamSeason? = null,
    val existingDiscoveries: List<DreamDiscovery> = emptyList(),
    val priorDecisions: List<DreamDiscoveryDecision> = emptyList(),
) {
    init {
        require(session.status == SleepSessionStatus.COMPLETED) {
            "Dream discovery can only be evaluated for a completed session."
        }
        require(session.endedAtEpochMillis != null) {
            "Completed session must have an end timestamp."
        }
    }
}

sealed interface DreamDiscoveryOutcome {
    data class Existing(
        val decision: DreamDiscoveryDecision,
        val dream: DreamDefinition?,
    ) : DreamDiscoveryOutcome

    data class Evaluated(
        val decision: DreamDiscoveryDecision,
        val discovery: DreamDiscovery?,
        val dream: DreamDefinition?,
    ) : DreamDiscoveryOutcome
}

/**
 * Deterministic one-shot dream discovery for a completed sleep session.
 *
 * Discovery chance is fixed per completed session and never scales with sleep
 * duration. The stable roll is derived from session identity, not duration, so
 * an extreme-duration session receives no advantage.
 */
class DeterministicDreamDiscoveryEngine(
    private val repository: DreamDefinitionRepository,
    private val discoveryChancePercent: Int = DEFAULT_DISCOVERY_CHANCE_PERCENT,
    val algorithmVersion: Int = CURRENT_ALGORITHM_VERSION,
) {
    init {
        require(discoveryChancePercent in 0..100) {
            "Discovery chance must be between 0 and 100."
        }
        require(algorithmVersion > 0) { "Algorithm version must be positive." }
    }

    fun evaluate(input: DreamDiscoveryInput): DreamDiscoveryOutcome {
        input.priorDecisions.firstOrNull { it.sessionId == input.session.id }?.let { existing ->
            val dream = existing.dreamId?.let(repository::find)
            return DreamDiscoveryOutcome.Existing(existing, dream)
        }

        val endedAt = requireNotNull(input.session.endedAtEpochMillis)
        val discoveredIds = input.existingDiscoveries.mapTo(mutableSetOf()) { it.dreamId }
        val eligible = repository.eligible(
            DreamEligibilityContext(
                familiarity = input.relationship.familiarity,
                season = input.season,
            )
        ).filterNot { it.id in discoveredIds }

        val random = DreamDiscoveryRandom(
            DreamDiscoverySeed.hash(input.session.id, algorithmVersion)
        )

        val selected = when {
            eligible.isEmpty() -> null
            random.nextInt(100) >= discoveryChancePercent -> null
            else -> selectWeighted(eligible, random)
        }

        val decision = DreamDiscoveryDecision(
            sessionId = input.session.id,
            algorithmVersion = algorithmVersion,
            dreamId = selected?.id,
            evaluatedAtEpochMillis = endedAt,
        )
        val discovery = selected?.let {
            DreamDiscovery(
                dreamId = it.id,
                sessionId = input.session.id,
                discoveredAtEpochMillis = endedAt,
            )
        }
        return DreamDiscoveryOutcome.Evaluated(
            decision = decision,
            discovery = discovery,
            dream = selected,
        )
    }

    private fun selectWeighted(
        definitions: List<DreamDefinition>,
        random: DreamDiscoveryRandom,
    ): DreamDefinition {
        val weights = definitions.map { rarityWeight(it.rarity) }
        val total = weights.sum()
        var ticket = random.nextInt(total)
        weights.forEachIndexed { index, weight ->
            if (ticket < weight) return definitions[index]
            ticket -= weight
        }
        error("Unreachable dream selection state.")
    }

    private fun rarityWeight(rarity: RarityBand): Int = when (rarity) {
        RarityBand.COMMON -> 70
        RarityBand.UNCOMMON -> 25
        RarityBand.RARE -> 5
    }

    companion object {
        const val DEFAULT_DISCOVERY_CHANCE_PERCENT: Int = 30
        const val CURRENT_ALGORITHM_VERSION: Int = 1
    }
}

internal object DreamDiscoverySeed {
    private const val FNV_OFFSET_BASIS: Long = -3750763034362895579L
    private const val FNV_PRIME: Long = 1099511628211L

    fun hash(sessionId: String, algorithmVersion: Int): Long {
        var hash = FNV_OFFSET_BASIS

        fun mixByte(value: Int) {
            hash = hash xor (value and 0xff).toLong()
            hash *= FNV_PRIME
        }

        sessionId.encodeToByteArray().forEach { mixByte(it.toInt()) }
        mixByte(0xff)
        repeat(4) { index ->
            mixByte(algorithmVersion ushr (index * 8))
        }
        return hash
    }
}

internal class DreamDiscoveryRandom(seed: Long) {
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
        return ((nextLong() ushr 1) % bound.toLong()).toInt()
    }

    companion object {
        private const val GOLDEN_GAMMA: Long = -7046029254386353131L
    }
}
