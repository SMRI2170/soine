package app.soine.sound

/**
 * Aggregated, presentation-friendly view of a single
 * session's derived sound events.
 *
 * #178 requires the user-facing copy to be quiet and
 * never make a medical claim. The aggregation collapses
 * many events into one short summary line; the chrome
 * does not list categories, does not show confidence
 * percentages, and does not name the type labels
 * verbatim. The user sees one line of copy that
 * describes the *pattern* without a diagnosis.
 *
 * The data class intentionally does not carry raw
 * `confidence` values or `modelVersion` — those live in
 * the underlying [SoundEvent] for analytics / debugging,
 * but the morning screen never displays them.
 */
data class SoundEventSummary(
    /**
     * The total number of derived events aggregated into
     * this summary. A `0` value means no events were
     * detected; the morning screen treats this as the
     * "empty" path and does not surface the panel.
     */
    val totalCount: Int,
    /**
     * Count of [SoundEventType.VOCALIZATION] events.
     * Exposed for the presentation layer only — never
     * displayed directly in the morning screen.
     */
    val vocalizationCount: Int,
    /**
     * Count of [SoundEventType.LOUD_SOUND] events.
     * Exposed for the presentation layer only.
     */
    val loudSoundCount: Int,
    /**
     * Count of [SoundEventType.SNORE_LIKE] events.
     * Exposed for the presentation layer only.
     */
    val snoreLikeCount: Int,
    /**
     * Count of [SoundEventType.COUGH_LIKE] events.
     * Exposed for the presentation layer only.
     */
    val coughLikeCount: Int,
    /**
     * The wall-clock time of the latest event. The
     * presentation layer uses this only when the user
     * opens the privacy-and-data screen; the morning
     * screen does not display it.
     */
    val latestOccurredAtEpochMillis: Long?,
) {
    /**
     * True when the session had at least one derived
     * event. The morning screen uses this to gate the
     * reveal panel.
     */
    val hasEvents: Boolean get() = totalCount > 0
}

/**
 * Aggregates a list of [StoredSoundEvent] for a single
 * session into a [SoundEventSummary]. Events within a
 * short time window of the same type are merged so the
 * presentation layer does not see a category list — the
 * user reads one short summary line.
 *
 * The aggregation is intentionally lossy. The morning
 * screen does not need to know exactly how many times
 * the user coughed; it needs to know "there were
 * events, here's a calm one-line summary". The full
 * event log lives behind the privacy-and-data screen.
 */
fun List<StoredSoundEvent>.summarize(): SoundEventSummary {
    if (isEmpty()) {
        return SoundEventSummary(
            totalCount = 0,
            vocalizationCount = 0,
            loudSoundCount = 0,
            snoreLikeCount = 0,
            coughLikeCount = 0,
            latestOccurredAtEpochMillis = null,
        )
    }
    val vocalizationCount = count { it.event.type == SoundEventType.VOCALIZATION }
    val loudSoundCount = count { it.event.type == SoundEventType.LOUD_SOUND }
    val snoreLikeCount = count { it.event.type == SoundEventType.SNORE_LIKE }
    val coughLikeCount = count { it.event.type == SoundEventType.COUGH_LIKE }
    val latest = maxOfOrNull { it.event.occurredAtEpochMillis }
    return SoundEventSummary(
        totalCount = size,
        vocalizationCount = vocalizationCount,
        loudSoundCount = loudSoundCount,
        snoreLikeCount = snoreLikeCount,
        coughLikeCount = coughLikeCount,
        latestOccurredAtEpochMillis = latest,
    )
}

/**
 * Returns the user-facing summary line for a
 * [SoundEventSummary]. The line is intentionally short
 * and uses the "*のような*" pattern so the copy never
 * makes a medical claim.
 *
 * The copy does not include:
 *   - a raw count of events (the user does not need a
 *     number to feel the moment)
 *   - a confidence percentage (the morning screen never
 *     surfaces detector confidence)
 *   - a category label verbatim (the copy says
 *     "寝言のような音" rather than "VOCALIZATION")
 *
 * The function is pure so the morning screen can call
 * it from a Composable without side effects.
 */
fun SoundEventSummary.userFacingLine(): String? {
    if (!hasEvents) return null
    // Pick the dominant signal: the type with the most
    // events. When two or more types tie, the order in
    // the copy is SNORE_LIKE → VOCALIZATION → COUGH_LIKE
    // → LOUD_SOUND (the calmest / most-reassuring first).
    val dominantType = sequenceOf(
        SoundEventType.SNORE_LIKE to snoreLikeCount,
        SoundEventType.VOCALIZATION to vocalizationCount,
        SoundEventType.COUGH_LIKE to coughLikeCount,
        SoundEventType.LOUD_SOUND to loudSoundCount,
    )
        .filter { it.second > 0 }
        .maxByOrNull { it.second }
        ?.first
        ?: return null
    val phrase = when (dominantType) {
        SoundEventType.SNORE_LIKE -> "寝言のような音や、大きめの音が聞こえたかも"
        SoundEventType.VOCALIZATION -> "寝言のような小さな音がときどき聞こえたかも"
        SoundEventType.COUGH_LIKE -> "せきのような音が聞こえたかも"
        SoundEventType.LOUD_SOUND -> "大きめの音が聞こえたかも"
        SoundEventType.OTHER -> "何かの音が聞こえたかも"
    }
    return phrase
}
