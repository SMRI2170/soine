package app.soine.night

enum class NightEventAnimationIntent {
    TURN,
    EAR_TWITCH,
    MOVE_CLOSER,
    CURL_UP,
    BRIEF_WAKE,
    FUNNY_POSE,
    DREAM,
    SOUND_REACTION,
}

data class NightEventDefinition(
    val id: String,
    val type: NightEventType,
    val weight: Int,
    val rarity: RarityBand,
    val minimumFamiliarity: Int,
    val morningLine: String,
    val animationIntent: NightEventAnimationIntent,
    val cooldownNights: Int,
    val payloadVersion: Int = 1,
    val contentVersion: Int = CURRENT_CONTENT_VERSION,
) {
    init {
        require(id.isNotBlank()) { "NightEvent content id must not be blank." }
        require(':' !in id) { "NightEvent content id must not contain ':'." }
        require(weight > 0) { "NightEvent content weight must be positive." }
        require(minimumFamiliarity >= 0) { "Minimum familiarity must not be negative." }
        require(morningLine.isNotBlank()) { "Morning line must not be blank." }
        require(cooldownNights >= 0) { "Cooldown nights must not be negative." }
        require(payloadVersion > 0) { "Payload version must be positive." }
        require(contentVersion > 0) { "Content version must be positive." }
    }

    fun toCandidate(): WeightedNightEventCandidate =
        WeightedNightEventCandidate(
            id = id,
            type = type,
            weight = weight,
            rarity = rarity,
            minimumFamiliarity = minimumFamiliarity,
            cooldownNights = cooldownNights,
            payloadVersion = payloadVersion,
        )

    companion object {
        const val CURRENT_CONTENT_VERSION: Int = 1
    }
}

object InitialNightEventCatalog {
    val definitions: List<NightEventDefinition> = listOf(
        definition(
            id = "turn-soft",
            type = NightEventType.TURN_OVER,
            weight = 55,
            line = "夜中に、ころんと向きを変えてたみたい",
            animation = NightEventAnimationIntent.TURN,
        ),
        definition(
            id = "turn-back",
            type = NightEventType.TURN_OVER,
            weight = 45,
            line = "いつの間にか、反対を向いて眠ってたよ",
            animation = NightEventAnimationIntent.TURN,
        ),
        definition(
            id = "turn-tiny",
            type = NightEventType.TURN_OVER,
            weight = 35,
            line = "ほんの少しだけ寝返りしてたみたい",
            animation = NightEventAnimationIntent.TURN,
        ),
        definition(
            id = "ear-one-twitch",
            type = NightEventType.EAR_TWITCH,
            weight = 40,
            line = "夜中に耳がぴくっと動いてたよ",
            animation = NightEventAnimationIntent.EAR_TWITCH,
        ),
        definition(
            id = "ear-two-twitches",
            type = NightEventType.EAR_TWITCH,
            weight = 28,
            line = "眠りながら、耳が小さく二度動いてたみたい",
            animation = NightEventAnimationIntent.EAR_TWITCH,
        ),
        definition(
            id = "closer-small-step",
            type = NightEventType.MOVE_CLOSER,
            weight = 32,
            line = "夜のあいだに、少しだけ近くへ来てたよ",
            animation = NightEventAnimationIntent.MOVE_CLOSER,
            minimumFamiliarity = 1,
            rarity = RarityBand.UNCOMMON,
            cooldownNights = 1,
        ),
        definition(
            id = "closer-pillow-side",
            type = NightEventType.MOVE_CLOSER,
            weight = 24,
            line = "気づいたら、枕の近くまで寄ってたみたい",
            animation = NightEventAnimationIntent.MOVE_CLOSER,
            minimumFamiliarity = 1,
            rarity = RarityBand.UNCOMMON,
            cooldownNights = 1,
        ),
        definition(
            id = "closer-right-beside",
            type = NightEventType.MOVE_CLOSER,
            weight = 10,
            line = "朝には、すぐそばで眠ってたよ",
            animation = NightEventAnimationIntent.MOVE_CLOSER,
            minimumFamiliarity = 2,
            rarity = RarityBand.RARE,
            cooldownNights = 3,
        ),
        definition(
            id = "curl-small",
            type = NightEventType.CURL_UP,
            weight = 42,
            line = "いつの間にか、小さく丸まってたみたい",
            animation = NightEventAnimationIntent.CURL_UP,
        ),
        definition(
            id = "curl-tight",
            type = NightEventType.CURL_UP,
            weight = 30,
            line = "夜中は、きゅっと丸くなって眠ってたよ",
            animation = NightEventAnimationIntent.CURL_UP,
        ),
        definition(
            id = "curl-loose",
            type = NightEventType.CURL_UP,
            weight = 26,
            line = "ゆるく丸くなって、そのまま眠ってたみたい",
            animation = NightEventAnimationIntent.CURL_UP,
        ),
        definition(
            id = "wake-peek",
            type = NightEventType.BRIEF_WAKE,
            weight = 22,
            line = "夜中に少しだけ目を開けて、また眠ったみたい",
            animation = NightEventAnimationIntent.BRIEF_WAKE,
            cooldownNights = 1,
        ),
        definition(
            id = "wake-look-around",
            type = NightEventType.BRIEF_WAKE,
            weight = 16,
            line = "一度だけ、そっとあたりを見てたみたい",
            animation = NightEventAnimationIntent.BRIEF_WAKE,
            cooldownNights = 1,
        ),
        definition(
            id = "pose-paws-up",
            type = NightEventType.FUNNY_POSE,
            weight = 14,
            line = "朝見たら、手を上げたまま眠ってたよ",
            animation = NightEventAnimationIntent.FUNNY_POSE,
            minimumFamiliarity = 1,
            rarity = RarityBand.UNCOMMON,
            cooldownNights = 2,
        ),
        definition(
            id = "pose-sideways",
            type = NightEventType.FUNNY_POSE,
            weight = 12,
            line = "ちょっと不思議な向きで眠ってたみたい",
            animation = NightEventAnimationIntent.FUNNY_POSE,
            minimumFamiliarity = 1,
            rarity = RarityBand.UNCOMMON,
            cooldownNights = 2,
        ),
        definition(
            id = "dream-soft-smile",
            type = NightEventType.DREAM,
            weight = 8,
            line = "眠りながら、少しだけ楽しそうな顔をしてたみたい",
            animation = NightEventAnimationIntent.DREAM,
            minimumFamiliarity = 2,
            rarity = RarityBand.RARE,
            cooldownNights = 3,
        ),
        definition(
            id = "dream-little-move",
            type = NightEventType.DREAM,
            weight = 14,
            line = "夢の中で歩いてたのか、少しだけ動いてたよ",
            animation = NightEventAnimationIntent.DREAM,
            minimumFamiliarity = 1,
            rarity = RarityBand.UNCOMMON,
            cooldownNights = 2,
        ),
        definition(
            id = "sound-rain",
            type = NightEventType.SOUND_REACTION,
            weight = 28,
            line = "夜の音に合わせて、少しだけ耳が動いてたみたい",
            animation = NightEventAnimationIntent.SOUND_REACTION,
        ),
        definition(
            id = "sound-small-pause",
            type = NightEventType.SOUND_REACTION,
            weight = 22,
            line = "音が変わったとき、一瞬だけ動きを止めてたよ",
            animation = NightEventAnimationIntent.SOUND_REACTION,
        ),
        definition(
            id = "sound-settle-again",
            type = NightEventType.SOUND_REACTION,
            weight = 18,
            line = "夜の音を聞いて、また静かに丸まってたみたい",
            animation = NightEventAnimationIntent.SOUND_REACTION,
            minimumFamiliarity = 1,
        ),
    )

    val candidates: List<WeightedNightEventCandidate> =
        definitions.map(NightEventDefinition::toCandidate)

    val engine: WeightedNightEventEngine by lazy {
        WeightedNightEventEngine(candidates)
    }

    init {
        require(definitions.size >= 20) {
            "Initial NightEvent catalog must contain at least 20 definitions."
        }
        require(definitions.map { it.id }.distinct().size == definitions.size) {
            "Initial NightEvent ids must be unique."
        }
    }

    fun find(id: String): NightEventDefinition? =
        definitions.firstOrNull { it.id == id }

    fun definitionFor(event: NightEvent): NightEventDefinition? =
        find(eventCandidateId(event))

    private fun definition(
        id: String,
        type: NightEventType,
        weight: Int,
        line: String,
        animation: NightEventAnimationIntent,
        rarity: RarityBand = RarityBand.COMMON,
        minimumFamiliarity: Int = 0,
        cooldownNights: Int = 0,
        payloadVersion: Int = 1,
    ) = NightEventDefinition(
        id = id,
        type = type,
        weight = weight,
        rarity = rarity,
        minimumFamiliarity = minimumFamiliarity,
        morningLine = line,
        animationIntent = animation,
        cooldownNights = cooldownNights,
        payloadVersion = payloadVersion,
    )
}
