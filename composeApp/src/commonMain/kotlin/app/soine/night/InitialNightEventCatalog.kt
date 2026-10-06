package app.soine.night

import app.soine.companion.CompanionIntent

data class NightEventEligibility(
    val minimumFamiliarity: Int = 0,
    val requiresSoundSignal: Boolean = false,
) {
    init {
        require(minimumFamiliarity >= 0) { "Minimum familiarity must not be negative." }
    }
}

data class NightEventDefinition(
    val id: String,
    val type: NightEventType,
    val eligibility: NightEventEligibility = NightEventEligibility(),
    val weight: Int,
    val rarity: RarityBand,
    val morningLine: String,
    val animationIntent: CompanionIntent,
    val cooldownNights: Int,
    val version: Int = 1,
) {
    init {
        require(id.isNotBlank())
        require(':' !in id)
        require(weight > 0)
        require(morningLine.isNotBlank())
        require(cooldownNights >= 0)
        require(version > 0)
    }

    fun toCandidate(): WeightedNightEventCandidate =
        WeightedNightEventCandidate(
            id = id,
            type = type,
            weight = weight,
            rarity = rarity,
            minimumFamiliarity = eligibility.minimumFamiliarity,
            cooldownNights = cooldownNights,
            requiresSoundSignal = eligibility.requiresSoundSignal,
            payloadVersion = version,
        )
}

object InitialNightEventCatalog {
    val definitions: List<NightEventDefinition> = listOf(
        definition("turn-over-soft", NightEventType.TURN_OVER, 48, RarityBand.COMMON, 0, "夜中に、そっと寝返りしてたみたい。", CompanionIntent.ROLL_OVER, 1),
        definition("turn-over-double", NightEventType.TURN_OVER, 36, RarityBand.COMMON, 0, "ねむ、何度かころんと向きを変えてた。", CompanionIntent.ROLL_OVER, 1),
        definition("turn-over-near", NightEventType.TURN_OVER, 20, RarityBand.UNCOMMON, 1, "寝返りのあと、少しだけこっちを向いてた。", CompanionIntent.ROLL_OVER, 2),

        definition("ear-twitch-small", NightEventType.EAR_TWITCH, 44, RarityBand.COMMON, 0, "耳がぴくっと動く瞬間があったよ。", CompanionIntent.EAR_TWITCH, 1),
        definition("ear-twitch-listen", NightEventType.EAR_TWITCH, 28, RarityBand.COMMON, 0, "何か聞こえたのか、耳をちょっと動かしてた。", CompanionIntent.EAR_TWITCH, 1),
        definition("ear-twitch-dream", NightEventType.EAR_TWITCH, 14, RarityBand.UNCOMMON, 1, "夢の中でも何か聞いていたのかも。", CompanionIntent.EAR_TWITCH, 2),

        definition("move-closer-little", NightEventType.MOVE_CLOSER, 30, RarityBand.COMMON, 1, "気づいたら、少しだけ近くに来てた。", CompanionIntent.MOVE_CLOSER, 1),
        definition("move-closer-pillow", NightEventType.MOVE_CLOSER, 18, RarityBand.UNCOMMON, 2, "枕のほうへ、そっと寄ってきてた。", CompanionIntent.MOVE_CLOSER, 2),
        definition("move-closer-familiar", NightEventType.MOVE_CLOSER, 9, RarityBand.RARE, 3, "いつもより近いところで眠ってた。", CompanionIntent.MOVE_CLOSER, 4),

        definition("curl-up-round", NightEventType.CURL_UP, 42, RarityBand.COMMON, 0, "まんまるになって眠ってた。", CompanionIntent.CURL_UP, 1),
        definition("curl-up-small", NightEventType.CURL_UP, 32, RarityBand.COMMON, 0, "小さく丸まって、すやすやしてた。", CompanionIntent.CURL_UP, 1),
        definition("curl-up-cozy", NightEventType.CURL_UP, 17, RarityBand.UNCOMMON, 1, "落ち着いたみたいで、ぎゅっと丸くなってた。", CompanionIntent.CURL_UP, 2),

        definition("brief-wake-peek", NightEventType.BRIEF_WAKE, 24, RarityBand.COMMON, 0, "一度だけ、少し目を開けてたみたい。", CompanionIntent.BRIEF_WAKE, 1),
        definition("brief-wake-look", NightEventType.BRIEF_WAKE, 13, RarityBand.UNCOMMON, 2, "夜中にちょっと起きて、こちらを見てた。", CompanionIntent.LOOK_AT_USER, 2),

        definition("funny-pose-paws", NightEventType.FUNNY_POSE, 16, RarityBand.UNCOMMON, 1, "朝方、手足がちょっと変な向きになってた。", CompanionIntent.STRETCH, 2),
        definition("funny-pose-twist", NightEventType.FUNNY_POSE, 10, RarityBand.UNCOMMON, 2, "寝相がくずれて、ちょっと面白い格好になってた。", CompanionIntent.ROLL_OVER, 3),

        definition("dream-soft", NightEventType.DREAM, 11, RarityBand.UNCOMMON, 1, "やさしい夢を見ていたような顔だった。", CompanionIntent.SLEEP, 2),
        definition("dream-stars", NightEventType.DREAM, 5, RarityBand.RARE, 2, "星の夢でも見てたのかな。", CompanionIntent.BREATHE, 4),

        definition("sound-reaction-twitch", NightEventType.SOUND_REACTION, 22, RarityBand.COMMON, 0, "音に反応して、耳が少し動いたみたい。", CompanionIntent.EAR_TWITCH, 1, requiresSoundSignal = true),
        definition("sound-reaction-settle", NightEventType.SOUND_REACTION, 12, RarityBand.UNCOMMON, 1, "環境音が流れると、少し落ち着いたみたい。", CompanionIntent.SETTLE, 2, requiresSoundSignal = true),
    )

    val candidates: List<WeightedNightEventCandidate> =
        definitions.map(NightEventDefinition::toCandidate)

    private val byId = definitions.associateBy { it.id }

    fun find(id: String): NightEventDefinition? = byId[id]

    fun definitionFor(event: NightEvent): NightEventDefinition? =
        find(eventCandidateId(event))

    private fun definition(
        id: String,
        type: NightEventType,
        weight: Int,
        rarity: RarityBand,
        minimumFamiliarity: Int,
        morningLine: String,
        animationIntent: CompanionIntent,
        cooldownNights: Int,
        requiresSoundSignal: Boolean = false,
    ) = NightEventDefinition(
        id = id,
        type = type,
        eligibility = NightEventEligibility(
            minimumFamiliarity = minimumFamiliarity,
            requiresSoundSignal = requiresSoundSignal,
        ),
        weight = weight,
        rarity = rarity,
        morningLine = morningLine,
        animationIntent = animationIntent,
        cooldownNights = cooldownNights,
    )
}
