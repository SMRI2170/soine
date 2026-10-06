package app.soine.night

data class WeightedNightEventCandidate(
    val id: String,
    val type: NightEventType,
    val weight: Int,
    val rarity: RarityBand = RarityBand.COMMON,
    val minimumFamiliarity: Int = 0,
    val cooldownNights: Int = if (rarity == RarityBand.RARE) 3 else 0,
    val payloadVersion: Int = 1,
) {
    init {
        require(id.isNotBlank()) { "Candidate id must not be blank." }
        require(':' !in id) { "Candidate id must not contain ':'." }
        require(weight > 0) { "Candidate weight must be positive." }
        require(minimumFamiliarity >= 0) { "Minimum familiarity must not be negative." }
        require(cooldownNights >= 0) { "Cooldown nights must not be negative." }
        require(payloadVersion > 0) { "Payload version must be positive." }
    }
}

class WeightedNightEventEngine(
    private val candidates: List<WeightedNightEventCandidate>,
    override val rulesVersion: Int = NightEventGenerationMetadata.CURRENT_ALGORITHM_VERSION,
) : NightEventEngine {
    init {
        require(rulesVersion in 1..NightEventGenerationMetadata.CURRENT_ALGORITHM_VERSION) {
            "Unsupported rules version: $rulesVersion"
        }
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

        val eligible = candidates.filter { it.minimumFamiliarity <= input.relationship.familiarity }
        if (eligible.isEmpty()) return emptyList()

        val metadata = NightEventGenerationMetadata.reuseOrCreate(
            session = input.session,
            persisted = input.generationMetadata,
            currentAlgorithmVersion = rulesVersion,
        )
        require(metadata.algorithmVersion in 1..rulesVersion) {
            "Unsupported persisted algorithm version: ${metadata.algorithmVersion}"
        }

        val random = NightEventRandom(metadata.seed)
        return when (metadata.algorithmVersion) {
            1 -> select(
                pool = eligible.map { EffectiveCandidate(it, it.weight) }.toMutableList(),
                count = minOf(input.maxEvents, eligible.size),
                random = random,
                input = input,
            )
            2 -> generateWithHistory(input, eligible, random)
            else -> error("Unsupported algorithm version: ${metadata.algorithmVersion}")
        }.sortedBy { it.occurredAtEpochMillis }
    }

    private fun generateWithHistory(
        input: NightEventEngineInput,
        eligible: List<WeightedNightEventCandidate>,
        random: NightEventRandom,
    ): List<NightEvent> {
        val target = minOf(input.maxEvents, eligible.size)
        val primary = eligible.mapNotNull { candidate ->
            NightEventHistoryPolicy.primaryWeight(candidate, input.history)
                ?.let { EffectiveCandidate(candidate, it) }
        }.toMutableList()

        val selected = select(primary, target, random, input).toMutableList()
        if (selected.size >= target) return selected

        val selectedCandidateIds = selected.mapTo(mutableSetOf(), ::eventCandidateId)
        val fallback = eligible
            .filterNot { it.id in selectedCandidateIds }
            .map { EffectiveCandidate(it, NightEventHistoryPolicy.fallbackWeight(it)) }
            .toMutableList()

        selected += select(
            pool = fallback,
            count = target - selected.size,
            random = random,
            input = input,
        )
        return selected
    }

    private fun select(
        pool: MutableList<EffectiveCandidate>,
        count: Int,
        random: NightEventRandom,
        input: NightEventEngineInput,
    ): List<NightEvent> {
        val end = requireNotNull(input.session.endedAtEpochMillis)
        return buildList {
            repeat(minOf(count, pool.size)) {
                val totalWeight = pool.sumOf { it.weight.toLong() }
                val ticket = (random.nextLong() ushr 1) % totalWeight
                val index = WeightedNightSelection.chooseIndex(
                    weights = pool.map { it.weight },
                    ticket = ticket,
                )
                val chosen = pool.removeAt(index).candidate
                add(
                    NightEvent(
                        id = "${input.session.id}:${chosen.id}",
                        type = chosen.type,
                        occurredAtEpochMillis = randomTimestamp(
                            random = random,
                            start = input.session.startedAtEpochMillis,
                            end = end,
                        ),
                        rarity = chosen.rarity,
                        payloadVersion = chosen.payloadVersion,
                    )
                )
            }
        }
    }

    private fun randomTimestamp(random: NightEventRandom, start: Long, end: Long): Long {
        if (start == end) return start
        val span = end - start
        val offset = (random.nextLong() ushr 1) % (span + 1)
        return start + offset
    }

    private data class EffectiveCandidate(
        val candidate: WeightedNightEventCandidate,
        val weight: Int,
    )
}

internal object NightEventHistoryPolicy {
    const val PREVIOUS_NIGHT_WEIGHT_DIVISOR: Int = 5
    const val RARE_COOLDOWN_NIGHTS: Int = 3
    private const val FALLBACK_WEIGHT_DIVISOR: Int = 10

    fun primaryWeight(candidate: WeightedNightEventCandidate, history: NightEventHistory): Int? {
        val nights = history.nightsNewestFirst()
        val key = candidate.id
        val onCooldown = candidate.cooldownNights > 0 &&
            nights.take(candidate.cooldownNights).flatten().any { eventCandidateId(it) == key }
        if (onCooldown) return null

        val repeatedLastNight = nights.firstOrNull().orEmpty().any { eventCandidateId(it) == key }
        return if (repeatedLastNight) maxOf(1, candidate.weight / PREVIOUS_NIGHT_WEIGHT_DIVISOR)
        else candidate.weight
    }

    fun fallbackWeight(candidate: WeightedNightEventCandidate): Int =
        maxOf(1, candidate.weight / FALLBACK_WEIGHT_DIVISOR)
}

internal fun eventCandidateId(event: NightEvent): String =
    event.id.substringAfter(':', event.id)

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
