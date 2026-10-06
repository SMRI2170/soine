package app.soine.dream

data class DreamEligibilityContext(
    val familiarity: Int,
    val season: DreamSeason? = null,
) {
    init {
        require(familiarity >= 0) { "Familiarity must not be negative." }
    }
}

interface DreamDefinitionRepository {
    fun all(): List<DreamDefinition>
    fun find(id: String): DreamDefinition?
    fun eligible(context: DreamEligibilityContext): List<DreamDefinition>
}

/**
 * Repository for authored dream content bundled with the app.
 *
 * IDs are stable content identities. Updating copy/art for an existing dream
 * keeps the ID and increments contentVersion.
 */
class InMemoryDreamDefinitionRepository(
    definitions: List<DreamDefinition>,
) : DreamDefinitionRepository {
    private val definitionsById: Map<String, DreamDefinition>

    init {
        require(definitions.map { it.id }.distinct().size == definitions.size) {
            "Dream definition ids must be unique."
        }
        definitionsById = definitions
            .sortedBy { it.id }
            .associateBy { it.id }
    }

    override fun all(): List<DreamDefinition> =
        definitionsById.values.toList()

    override fun find(id: String): DreamDefinition? =
        definitionsById[id]

    override fun eligible(
        context: DreamEligibilityContext,
    ): List<DreamDefinition> =
        definitionsById.values.filter { definition ->
            definition.minimumFamiliarity <= context.familiarity &&
                definition.isSeasonEligible(context.season)
        }
}
