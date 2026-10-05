package app.soine.dream

import app.soine.night.RarityBand

data class DreamDefinition(
    val id: String,
    val title: String,
    val shortLine: String,
    val rarity: RarityBand,
    val minimumFamiliarity: Int = 0,
    val artKey: String? = null,
)

data class DreamDiscovery(
    val dreamId: String,
    val sessionId: String,
    val discoveredAtEpochMillis: Long,
)
