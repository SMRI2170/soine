package app.soine.dialogue

import app.soine.dream.DreamDefinition
import app.soine.night.NightEvent
import app.soine.night.NightEventType
import app.soine.night.RarityBand
import app.soine.relationship.*

enum class CompanionDialoguePhase {
    BEDTIME,
    MORNING,
}

enum class CompanionDialogueSource {
    DREAM,
    NIGHT_EVENT,
    ROUTINE,
    RELATIONSHIP,
    GENERIC,
}

data class CompanionDialogue(
    val id: String,
    val text: String,
    val source: CompanionDialogueSource,
    val priority: Int,
    val cooldownSelections: Int = 1,
) {
    init {
        require(id.isNotBlank())
        require(text.isNotBlank())
        require(priority >= 0)
        require(cooldownSelections >= 0)
    }
}

sealed interface CompanionDialogueSelectionMode {
    data object Default : CompanionDialogueSelectionMode
    data class DeterministicTest(val seed: Long) : CompanionDialogueSelectionMode
}

data class CompanionDialogueContext(
    val phase: CompanionDialoguePhase,
    val relationship: RelationshipState,
    val routineProfile: RoutineProfile? = null,
    val routineContext: RoutineDialogueContext? = null,
    val nightEvents: List<NightEvent> = emptyList(),
    val discoveredDream: DreamDefinition? = null,
    val bedtimeMoment: BedtimeDialogueMoment = BedtimeDialogueMoment.SETTLING,
    val recentDialogueIdsNewestFirst: List<String> = emptyList(),
    val selectionMode: CompanionDialogueSelectionMode = CompanionDialogueSelectionMode.Default,
)

/**
 * Shared dialogue prioritizer.
 *
 * Only authored, non-medical lines are emitted. Inputs may influence which
 * line is selected, but are never interpolated into health or diagnostic
 * claims. Unknown/unsupported context always falls back to generic dialogue.
 */
object CompanionDialogueSelector {
    fun select(context: CompanionDialogueContext): CompanionDialogue {
        val candidates = buildList {
            dreamCandidate(context)?.let(::add)
            nightEventCandidate(context)?.let(::add)
            routineCandidate(context)?.let(::add)
            relationshipCandidate(context)?.let(::add)
            if (context.phase == CompanionDialoguePhase.BEDTIME) {
                addAll(bedtimeCatalogCandidates(context))
            } else {
                addAll(genericCandidates(context.phase))
            }
        }

        val eligible = candidates.filter { !onCooldown(it, context.recentDialogueIdsNewestFirst) }
        val pool = if (eligible.isNotEmpty()) {
            eligible
        } else if (context.phase == CompanionDialoguePhase.BEDTIME) {
            bedtimeCatalogCandidates(context)
        } else {
            genericCandidates(context.phase)
        }

        val topPriority = pool.maxOf { it.priority }
        val top = pool.filter { it.priority == topPriority }
        return choose(top, context.selectionMode)
    }

    private fun dreamCandidate(context: CompanionDialogueContext): CompanionDialogue? {
        if (context.phase != CompanionDialoguePhase.MORNING) return null
        val dream = context.discoveredDream ?: return null
        if (dream.id.isBlank() || dream.shortLine.isBlank()) return null
        return CompanionDialogue(
            id = "dream-" + dream.id,
            text = dream.shortLine,
            source = CompanionDialogueSource.DREAM,
            priority = 500,
            cooldownSelections = 8,
        )
    }

    private fun nightEventCandidate(context: CompanionDialogueContext): CompanionDialogue? {
        if (context.phase != CompanionDialoguePhase.MORNING) return null
        val event = context.nightEvents.maxWithOrNull(
            compareBy<NightEvent> { rarityRank(it.rarity) }
                .thenBy { eventRank(it.type) }
                .thenBy { it.occurredAtEpochMillis }
        ) ?: return null

        val line = when (event.type) {
            NightEventType.TURN_OVER -> "夜中に、ころんと寝返りしてたよ"
            NightEventType.EAR_TWITCH -> "寝ながら耳がぴくっとしてたみたい"
            NightEventType.MOVE_CLOSER -> "夜のあいだに、少し近くで眠ってたよ"
            NightEventType.CURL_UP -> "いつの間にか、まるくなって眠ってたよ"
            NightEventType.BRIEF_WAKE -> "夜中に一度だけ、そっと目を開けてたみたい"
            NightEventType.FUNNY_POSE -> "朝見たら、ちょっと不思議な寝相だったよ"
            NightEventType.DREAM -> "なんだか夢を見ていたみたい"
            NightEventType.SOUND_REACTION -> "夜の音に、少しだけ反応してたみたい"
        }
        return CompanionDialogue(
            id = "night-event-" + event.type.name.lowercase(),
            text = line,
            source = CompanionDialogueSource.NIGHT_EVENT,
            priority = 400 + rarityRank(event.rarity),
            cooldownSelections = 3,
        )
    }

    private fun routineCandidate(context: CompanionDialogueContext): CompanionDialogue? {
        val profile = context.routineProfile ?: return null
        val routineContext = context.routineContext ?: return null
        val expectedPhase = when (context.phase) {
            CompanionDialoguePhase.BEDTIME -> RoutineDialoguePhase.BEDTIME
            CompanionDialoguePhase.MORNING -> RoutineDialoguePhase.MORNING
        }
        if (routineContext.phase != expectedPhase) return null

        val selected = RoutineDialogueSelector.select(
            profile = profile,
            context = routineContext,
            recentDialogueIds = context.recentDialogueIdsNewestFirst.toSet(),
        )
        if (selected.source == RoutineDialogueSource.GENERIC) return null

        return CompanionDialogue(
            id = selected.id,
            text = selected.text,
            source = CompanionDialogueSource.ROUTINE,
            priority = 300,
            cooldownSelections = 3,
        )
    }

    private fun relationshipCandidate(
        context: CompanionDialogueContext,
    ): CompanionDialogue? {
        if (context.phase == CompanionDialoguePhase.BEDTIME) return null
        return when (context.relationship.familiarityStage) {
            FamiliarityStage.NEW -> null
            FamiliarityStage.WARMING_UP -> CompanionDialogue(
                id = "relationship-warming",
                text = "少しずつ、一緒の朝に慣れてきたね",
                source = CompanionDialogueSource.RELATIONSHIP,
                priority = 200,
                cooldownSelections = 4,
            )
            FamiliarityStage.FAMILIAR -> CompanionDialogue(
                id = "relationship-familiar",
                text = "一緒に起きる朝も、だいぶ増えたね",
                source = CompanionDialogueSource.RELATIONSHIP,
                priority = 210,
                cooldownSelections = 4,
            )
            FamiliarityStage.CLOSE -> CompanionDialogue(
                id = "relationship-close",
                text = "おはよう。今日もすぐそばにいるよ",
                source = CompanionDialogueSource.RELATIONSHIP,
                priority = 220,
                cooldownSelections = 4,
            )
        }
    }

    private fun bedtimeCatalogCandidates(
        context: CompanionDialogueContext,
    ): List<CompanionDialogue> =
        BedtimeDialogueCatalog.eligible(
            stage = context.relationship.familiarityStage,
            moment = context.bedtimeMoment,
        ).map { definition ->
            val relationshipSpecific =
                definition.minimumFamiliarity != FamiliarityStage.NEW
            CompanionDialogue(
                id = definition.id,
                text = definition.text,
                source = if (relationshipSpecific) {
                    CompanionDialogueSource.RELATIONSHIP
                } else {
                    CompanionDialogueSource.GENERIC
                },
                priority = if (relationshipSpecific) {
                    200 + definition.minimumFamiliarity.persistedValue * 10
                } else {
                    100
                },
                cooldownSelections = 4,
            )
        }

    private fun genericCandidates(
        phase: CompanionDialoguePhase,
    ): List<CompanionDialogue> = when (phase) {
        CompanionDialoguePhase.BEDTIME -> listOf(
            generic("generic-bedtime-a", "今日も一緒に眠ろう"),
            generic("generic-bedtime-b", "そろそろ、ゆっくりしよう"),
            generic("generic-bedtime-c", "おやすみの準備、できたよ"),
        )
        CompanionDialoguePhase.MORNING -> listOf(
            generic("generic-morning-a", "おはよう"),
            generic("generic-morning-b", "朝になったね"),
            generic("generic-morning-c", "今日も一緒に起きられたね"),
        )
    }

    private fun generic(id: String, text: String) = CompanionDialogue(
        id = id,
        text = text,
        source = CompanionDialogueSource.GENERIC,
        priority = 100,
        cooldownSelections = 1,
    )

    private fun onCooldown(
        candidate: CompanionDialogue,
        recentIdsNewestFirst: List<String>,
    ): Boolean {
        if (candidate.cooldownSelections == 0) return false
        val index = recentIdsNewestFirst.indexOf(candidate.id)
        return index >= 0 && index < candidate.cooldownSelections
    }

    private fun choose(
        candidates: List<CompanionDialogue>,
        mode: CompanionDialogueSelectionMode,
    ): CompanionDialogue {
        require(candidates.isNotEmpty())
        val sorted = candidates.sortedBy { it.id }
        return when (mode) {
            CompanionDialogueSelectionMode.Default -> sorted.first()
            is CompanionDialogueSelectionMode.DeterministicTest -> {
                val index = stableIndex(mode.seed, sorted.size)
                sorted[index]
            }
        }
    }

    private fun stableIndex(seed: Long, size: Int): Int {
        val mixed = seed xor (seed ushr 33) xor (seed shl 11)
        return ((mixed and Long.MAX_VALUE) % size.toLong()).toInt()
    }

    private fun rarityRank(rarity: RarityBand): Int = when (rarity) {
        RarityBand.COMMON -> 1
        RarityBand.UNCOMMON -> 2
        RarityBand.RARE -> 3
    }

    private fun eventRank(type: NightEventType): Int = when (type) {
        NightEventType.DREAM -> 8
        NightEventType.MOVE_CLOSER -> 7
        NightEventType.FUNNY_POSE -> 6
        NightEventType.BRIEF_WAKE -> 5
        NightEventType.SOUND_REACTION -> 4
        NightEventType.CURL_UP -> 3
        NightEventType.EAR_TWITCH -> 2
        NightEventType.TURN_OVER -> 1
    }
}
