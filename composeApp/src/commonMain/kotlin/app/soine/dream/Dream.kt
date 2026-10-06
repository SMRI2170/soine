package app.soine.dream

import app.soine.night.RarityBand

enum class DreamSeason {
    SPRING,
    SUMMER,
    AUTUMN,
    WINTER,
}

data class DreamDefinition(
    val id: String,
    val title: String,
    val shortLine: String,
    val rarity: RarityBand,
    val minimumFamiliarity: Int = 0,
    /**
     * Empty means all-year. Seasonal dreams list one or more eligible seasons.
     */
    val eligibleSeasons: Set<DreamSeason> = emptySet(),
    val artKey: String? = null,
    val contentVersion: Int = CURRENT_CONTENT_VERSION,
) {
    init {
        require(id.isNotBlank()) { "Dream id must not be blank." }
        require(title.isNotBlank()) { "Dream title must not be blank." }
        require(shortLine.isNotBlank()) { "Dream short line must not be blank." }
        require(minimumFamiliarity >= 0) {
            "Minimum familiarity must not be negative."
        }
        require(artKey == null || artKey.isNotBlank()) {
            "Dream art key must be null or non-blank."
        }
        require(contentVersion > 0) { "Dream content version must be positive." }
    }

    fun isSeasonEligible(season: DreamSeason?): Boolean =
        eligibleSeasons.isEmpty() || (season != null && season in eligibleSeasons)

    companion object {
        const val CURRENT_CONTENT_VERSION: Int = 1
    }
}

data class DreamDiscovery(
    val dreamId: String,
    val sessionId: String,
    val discoveredAtEpochMillis: Long,
) {
    init {
        require(dreamId.isNotBlank()) { "Dream discovery id must not be blank." }
        require(sessionId.isNotBlank()) { "Dream discovery session id must not be blank." }
        require(discoveredAtEpochMillis >= 0) {
            "Dream discovery timestamp must not be negative."
        }
    }
}
