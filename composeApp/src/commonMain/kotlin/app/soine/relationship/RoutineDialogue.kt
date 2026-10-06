package app.soine.relationship

enum class RoutineDialoguePhase {
    BEDTIME,
    MORNING,
}

enum class RoutineDialogueSource {
    GENERIC,
    BEDTIME_TIMING,
    WAKE_TIMING,
    AMBIENT_SOUND,
    WEEKDAY_WEEKEND,
}

data class RoutineDialogue(
    val id: String,
    val text: String,
    val source: RoutineDialogueSource,
    val confidence: RoutineConfidence? = null,
) {
    init {
        require(id.isNotBlank()) { "Dialogue id must not be blank." }
        require(text.isNotBlank()) { "Dialogue text must not be blank." }
        if (source == RoutineDialogueSource.GENERIC) {
            require(confidence == null) { "Generic dialogue must not claim routine confidence." }
        }
    }
}

data class RoutineDialogueContext(
    val phase: RoutineDialoguePhase,
    val currentMinuteOfDay: Int? = null,
    val activeAmbientSoundId: String? = null,
    val ambientSoundLabels: Map<String, String> = emptyMap(),
    val dayType: RoutineDayType? = null,
) {
    init {
        require(currentMinuteOfDay == null || currentMinuteOfDay in 0..1439) {
            "Current minute must be null or in 0..1439."
        }
    }
}

/**
 * Selects one cautious routine-aware line or a generic fallback.
 *
 * Timing claims require HIGH confidence. Sound suggestions can use MEDIUM
 * confidence because RoutineProfileGenerator already suppresses weak/tied
 * preferences. Recent IDs are avoided whenever another eligible line exists.
 */
object RoutineDialogueSelector {
    private const val TIMING_DIFFERENCE_MINUTES = 45

    private val bedtimeFallbacks = listOf(
        RoutineDialogue(
            id = "generic-bedtime-1",
            text = "今日も一緒に眠ろう",
            source = RoutineDialogueSource.GENERIC,
        ),
        RoutineDialogue(
            id = "generic-bedtime-2",
            text = "そろそろ、ゆっくりしよう",
            source = RoutineDialogueSource.GENERIC,
        ),
        RoutineDialogue(
            id = "generic-bedtime-3",
            text = "ここで待ってたよ",
            source = RoutineDialogueSource.GENERIC,
        ),
    )

    private val morningFallbacks = listOf(
        RoutineDialogue(
            id = "generic-morning-1",
            text = "おはよう",
            source = RoutineDialogueSource.GENERIC,
        ),
        RoutineDialogue(
            id = "generic-morning-2",
            text = "今日も一緒に起きられたね",
            source = RoutineDialogueSource.GENERIC,
        ),
        RoutineDialogue(
            id = "generic-morning-3",
            text = "朝になったね",
            source = RoutineDialogueSource.GENERIC,
        ),
    )

    fun select(
        profile: RoutineProfile?,
        context: RoutineDialogueContext,
        recentDialogueIds: Set<String> = emptySet(),
    ): RoutineDialogue {
        val routineCandidates = profile
            ?.let { candidates(it, context) }
            .orEmpty()

        firstNotRecent(routineCandidates, recentDialogueIds)?.let { return it }

        val fallbacks = when (context.phase) {
            RoutineDialoguePhase.BEDTIME -> bedtimeFallbacks
            RoutineDialoguePhase.MORNING -> morningFallbacks
        }
        return firstNotRecent(fallbacks, recentDialogueIds) ?: fallbacks.first()
    }

    private fun candidates(
        profile: RoutineProfile,
        context: RoutineDialogueContext,
    ): List<RoutineDialogue> = buildList {
        when (context.phase) {
            RoutineDialoguePhase.BEDTIME -> {
                bedtimeTiming(profile, context)?.let(::add)
                soundSuggestion(profile, context)?.let(::add)
                weekendBedtime(profile, context)?.let(::add)
            }
            RoutineDialoguePhase.MORNING -> {
                wakeTiming(profile, context)?.let(::add)
                weekendMorning(profile, context)?.let(::add)
            }
        }
    }

    private fun bedtimeTiming(
        profile: RoutineProfile,
        context: RoutineDialogueContext,
    ): RoutineDialogue? {
        val current = context.currentMinuteOfDay ?: return null
        val typical = profile.typicalBedtime ?: return null
        if (typical.confidence != RoutineConfidence.HIGH) return null

        val difference = signedCircularDifference(
            from = typical.centerMinuteOfDay,
            to = current,
        )
        return when {
            difference <= -TIMING_DIFFERENCE_MINUTES -> RoutineDialogue(
                id = "routine-bedtime-earlier",
                text = "今日はいつもより早いね",
                source = RoutineDialogueSource.BEDTIME_TIMING,
                confidence = typical.confidence,
            )
            difference >= TIMING_DIFFERENCE_MINUTES -> RoutineDialogue(
                id = "routine-bedtime-later",
                text = "今日はいつもより少し遅めだね",
                source = RoutineDialogueSource.BEDTIME_TIMING,
                confidence = typical.confidence,
            )
            else -> null
        }
    }

    private fun wakeTiming(
        profile: RoutineProfile,
        context: RoutineDialogueContext,
    ): RoutineDialogue? {
        val current = context.currentMinuteOfDay ?: return null
        val typical = profile.typicalWakeTime ?: return null
        if (typical.confidence != RoutineConfidence.HIGH) return null

        val difference = signedCircularDifference(
            from = typical.centerMinuteOfDay,
            to = current,
        )
        return when {
            difference <= -TIMING_DIFFERENCE_MINUTES -> RoutineDialogue(
                id = "routine-wake-earlier",
                text = "今日は少し早起きだね",
                source = RoutineDialogueSource.WAKE_TIMING,
                confidence = typical.confidence,
            )
            difference >= TIMING_DIFFERENCE_MINUTES -> RoutineDialogue(
                id = "routine-wake-later",
                text = "今日はゆっくりの朝だね",
                source = RoutineDialogueSource.WAKE_TIMING,
                confidence = typical.confidence,
            )
            else -> null
        }
    }

    private fun soundSuggestion(
        profile: RoutineProfile,
        context: RoutineDialogueContext,
    ): RoutineDialogue? {
        val preference = profile.frequentAmbientSound ?: return null
        if (context.activeAmbientSoundId == preference.soundId) return null
        val label = context.ambientSoundLabels[preference.soundId] ?: return null
        if (label.isBlank()) return null

        return RoutineDialogue(
            id = "routine-sound-" + preference.soundId,
            text = label + "の音にする？",
            source = RoutineDialogueSource.AMBIENT_SOUND,
            confidence = preference.confidence,
        )
    }

    private fun weekendBedtime(
        profile: RoutineProfile,
        context: RoutineDialogueContext,
    ): RoutineDialogue? {
        if (context.dayType != RoutineDayType.WEEKEND) return null
        val tendency = profile.weekdayWeekendTendency ?: return null
        if (tendency.confidence != RoutineConfidence.HIGH) return null
        if (tendency.weekendBedtimeShiftMinutes < RoutineProfileGenerator.MEANINGFUL_DAY_SHIFT_MINUTES) {
            return null
        }

        return RoutineDialogue(
            id = "routine-weekend-bedtime",
            text = "休日は少しゆっくりめだね",
            source = RoutineDialogueSource.WEEKDAY_WEEKEND,
            confidence = tendency.confidence,
        )
    }

    private fun weekendMorning(
        profile: RoutineProfile,
        context: RoutineDialogueContext,
    ): RoutineDialogue? {
        if (context.dayType != RoutineDayType.WEEKEND) return null
        val tendency = profile.weekdayWeekendTendency ?: return null
        if (tendency.confidence != RoutineConfidence.HIGH) return null
        if (tendency.weekendWakeShiftMinutes < RoutineProfileGenerator.MEANINGFUL_DAY_SHIFT_MINUTES) {
            return null
        }

        return RoutineDialogue(
            id = "routine-weekend-morning",
            text = "休日の朝は、いつもよりゆっくりだね",
            source = RoutineDialogueSource.WEEKDAY_WEEKEND,
            confidence = tendency.confidence,
        )
    }

    private fun firstNotRecent(
        dialogues: List<RoutineDialogue>,
        recentDialogueIds: Set<String>,
    ): RoutineDialogue? =
        dialogues.firstOrNull { it.id !in recentDialogueIds }

    private fun signedCircularDifference(from: Int, to: Int): Int {
        var difference = normalizeMinute(to) - normalizeMinute(from)
        if (difference >= 720) difference -= 1440
        if (difference < -720) difference += 1440
        return difference
    }

    private fun normalizeMinute(value: Int): Int =
        ((value % 1440) + 1440) % 1440
}
