package app.soine.night

enum class NightEventType {
    TURN_OVER,
    EAR_TWITCH,
    MOVE_CLOSER,
    CURL_UP,
    BRIEF_WAKE,
    FUNNY_POSE,
    DREAM,
    SOUND_REACTION,
}

enum class RarityBand { COMMON, UNCOMMON, RARE }

data class NightEvent(
    val id: String,
    val type: NightEventType,
    val occurredAtEpochMillis: Long,
    val rarity: RarityBand = RarityBand.COMMON,
    val payloadVersion: Int = 1,
)
