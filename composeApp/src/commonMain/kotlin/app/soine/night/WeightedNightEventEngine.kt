package app.soine.night

data class WeightedNightEventCandidate(
    val id: String,
    val type: NightEventType,
    val weight: Int,
    val rarity: RarityBand = RarityBand.COMMON,
    val minimumFamiliarity: Int = 0,
    val payloadVersion: Int = 1,
) {
    init {
        require(id.isNotBlank()) { "Candidate id must not be blank." }
        require(weight > 0) { "Candidate weight must be positive." }
        require(minimumFamiliarity >= 0) { "Minimum familiarity must not be negative." }
        require(payloadVersion > 0) { "Payload version must be positive." }
    }
}

class WeightedNightEventEngine(
    private val candidates: List<WeightedNightEventCandidate>,
    override val rulesVersion: Int = NightEventGenerationMetadata.CURRENT_ALGORITHM_VERSION,
) : NightEventEngine {
    init {
        require(rulesVersion > 0) { "Rules version must be positive." }
        require(candidates.map { it.id }.distinct().size == candidates.size) {
            "Candidate ids must be unique."
        }
    }

    override fun generate(input: NightEventEngineInput): List<NightEvent> {
        if (input.maxEvents == 0) return emptyList()

        val end = requireNotNull(input.session.endedAtEpochMillis)
        require(end >= input.session.startedAtEpochMillis) {
            "Session end must not precede its start."
        }

        val eligible = candidates
            .filter { it.minimumFamiliarity <= input.relationship.familiarity }
            .toMutableList()
        if (eligible.isEmpty()) return emptyList()

        val metadata = NightEventGenerationMetadata.forSession(
            session = input.session,
            algorithmVersion = rulesVersion,
        )
        val random = NightEventRandom(metadata.seed)
        val selected = mutableListOf<NightEvent>()

        repeat(minOf(input.maxEvents, eligible.size)) {
            val totalWeight = eligible.sumOf { it.weight.toLong() }
            val ticket = (random.nextLong() ushr 1) % totalWeight
            val index = WeightedNightSelection.chooseIndex(
                weights = eligible.map { it.weight },
                ticket = ticket,
            )
            val candidate = eligible.removeAt(index)
            selected += NightEvent(
                id = "${input.session.id}:${candidate.id}",
                type = candidate.type,
                occurredAtEpochMillis = randomTimestamp(
                    random = random,
                    start = input.session.startedAtEpochMillis,
                    end = end,
                ),
                rarity = candidate.rarity,
                payloadVersion = candidate.payloadVersion,
            )
        }

        return selected.sortedBy { it.occurredAtEpochMillis }
    }

    private fun randomTimestamp(
        random: NightEventRandom,
        start: Long,
        end: Long,
    ): Long {
        if (start == end) return start
        val span = end - start
        val offset = (random.nextLong() ushr 1) % (span + 1)
        return start + offset
    }
}

internal object WeightedNightSelection {
    fun chooseIndex(weights: List<Int>, ticket: Long): Int {
        require(weights.isNotEmpty()) { "Weights must not be empty." }
        require(weights.all { it > 0 }) { "Weights must be positive." }
        val total = weights.sumOf { it.toLong() }
        require(ticket in 0 until total) { "Ticket must be within total weight." }

        var cursor = ticket
        weights.forEachIndexed { index, weight ->
            if (cursor < weight) return index
            cursor -= weight
        }
        error("Unreachable weighted selection state.")
    }
}
