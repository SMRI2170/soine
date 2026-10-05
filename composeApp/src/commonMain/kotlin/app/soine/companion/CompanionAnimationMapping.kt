package app.soine.companion

data class CompanionAnimationClip(
    val id: String,
    val loop: Boolean,
) {
    init {
        require(id.isNotBlank()) { "Animation clip id must not be blank." }
    }
}

data class CompanionAnimationSelection(
    val requestedIntent: CompanionIntent,
    val clip: CompanionAnimationClip?,
) {
    val usesStaticFallback: Boolean
        get() = clip == null
}

/**
 * Maps semantic companion intents to stable asset clip ids.
 *
 * Each intent has an ordered fallback list. Platform renderers only need to
 * report which clip ids exist in the loaded model; missing clips never break
 * the bedtime flow.
 */
object CompanionAnimationMapping {
    private val catalog = mapOf(
        "idle" to CompanionAnimationClip("idle", loop = true),
        "notice_user" to CompanionAnimationClip("notice_user", loop = false),
        "look_at_user" to CompanionAnimationClip("look_at_user", loop = false),
        "move_closer" to CompanionAnimationClip("move_closer", loop = false),
        "settle" to CompanionAnimationClip("settle", loop = false),
        "curl_up" to CompanionAnimationClip("curl_up", loop = false),
        "sleep" to CompanionAnimationClip("sleep", loop = true),
        "breathe" to CompanionAnimationClip("breathe", loop = true),
        "ear_twitch" to CompanionAnimationClip("ear_twitch", loop = false),
        "roll_over" to CompanionAnimationClip("roll_over", loop = false),
        "yawn" to CompanionAnimationClip("yawn", loop = false),
        "brief_wake" to CompanionAnimationClip("brief_wake", loop = false),
        "wake" to CompanionAnimationClip("wake", loop = false),
        "stretch" to CompanionAnimationClip("stretch", loop = false),
    )

    private val candidates = mapOf(
        CompanionIntent.IDLE to listOf("idle"),
        CompanionIntent.NOTICE_USER to listOf("notice_user", "look_at_user", "idle"),
        CompanionIntent.LOOK_AT_USER to listOf("look_at_user", "idle"),
        CompanionIntent.MOVE_CLOSER to listOf("move_closer", "settle", "idle"),
        CompanionIntent.SETTLE to listOf("settle", "curl_up", "idle"),
        CompanionIntent.CURL_UP to listOf("curl_up", "settle", "idle"),
        CompanionIntent.SLEEP to listOf("sleep", "breathe", "idle"),
        CompanionIntent.BREATHE to listOf("breathe", "sleep", "idle"),
        CompanionIntent.EAR_TWITCH to listOf("ear_twitch", "breathe", "sleep", "idle"),
        CompanionIntent.ROLL_OVER to listOf("roll_over", "sleep", "idle"),
        CompanionIntent.YAWN to listOf("yawn", "settle", "idle"),
        CompanionIntent.BRIEF_WAKE to listOf("brief_wake", "sleep", "idle"),
        CompanionIntent.WAKE to listOf("wake", "idle"),
        CompanionIntent.STRETCH to listOf("stretch", "wake", "idle"),
    )

    val requiredClipIds: Set<String>
        get() = catalog.keys

    fun preferredClip(intent: CompanionIntent): CompanionAnimationClip =
        catalog.getValue(candidates.getValue(intent).first())

    fun resolve(
        intent: CompanionIntent,
        availableClipIds: Set<String>,
    ): CompanionAnimationSelection {
        val clip = candidates
            .getValue(intent)
            .firstNotNullOfOrNull { id ->
                if (id in availableClipIds) catalog.getValue(id) else null
            }
        return CompanionAnimationSelection(intent, clip)
    }
}
