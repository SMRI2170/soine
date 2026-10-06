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
 * claims. Unknown/unsupported context always falls back to authored generic
 * dialogue.
 */
object CompanionDialogueSelector {
    fun select(context: CompanionDialogueContext): CompanionDialogue {
        val candidates = buildList {
            addAll(dreamCandidates(context))
            addAll(nightEventCandidates(context))
            addAll(routineCandidates(context))
            addAll(relationshipCandidates(context))
            if (context.phase == CompanionDialoguePhase.BEDTIME) {
                addAll(bedtimeCatalogCandidates(context))
            } else {
                addAll(genericCandidates(context.phase))
            }
        }

        val eligible = candidates.filter {
            !onCooldown(it, context.recentDialogueIdsNewestFirst)
        }
        val pool = if (eligible.isNotEmpty()) {
            eligible
        } else if (context.phase == CompanionDialoguePhase.BEDTIME) {
            bedtimeCatalogCandidates(context)
        } else {
            genericCandidates(context.phase)
        }

        val topPriority = pool.maxOf { it.priority }
        return choose(
            candidates = pool.filter { it.priority == topPriority },
            mode = context.selectionMode,
        )
    }

    private fun dreamCandidates(
        context: CompanionDialogueContext,
    ): List<CompanionDialogue> {
        if (context.phase != CompanionDialoguePhase.MORNING) return emptyList()
        val dream = context.discoveredDream ?: return emptyList()
        if (dream.id.isBlank() || dream.shortLine.isBlank()) return emptyList()

        val dreamRecentlyShown = context.recentDialogueIdsNewestFirst
            .take(DREAM_GROUP_COOLDOWN_SELECTIONS)
            .any {
                it == "dream-" + dream.id ||
                    it.startsWith("morning-dream-")
            }
        if (dreamRecentlyShown) return emptyList()

        return buildList {
            add(
                CompanionDialogue(
                    id = "dream-" + dream.id,
                    text = dream.shortLine,
                    source = CompanionDialogueSource.DREAM,
                    priority = 500,
                    cooldownSelections = DREAM_GROUP_COOLDOWN_SELECTIONS,
                )
            )
            MorningDialogueCatalog.dreamDefinitions().forEach { definition ->
                add(
                    CompanionDialogue(
                        id = definition.id,
                        text = definition.text,
                        source = CompanionDialogueSource.DREAM,
                        priority = 500,
                        cooldownSelections = DREAM_GROUP_COOLDOWN_SELECTIONS,
                    )
                )
            }
        }
    }

    private fun nightEventCandidates(
        context: CompanionDialogueContext,
    ): List<CompanionDialogue> {
        if (context.phase != CompanionDialoguePhase.MORNING) return emptyList()
        val event = context.nightEvents.maxWithOrNull(
            compareBy<NightEvent> { rarityRank(it.rarity) }
                .thenBy { eventRank(it.type) }
                .thenBy { it.occurredAtEpochMillis }
        ) ?: return emptyList()

        return MorningDialogueCatalog.forEvent(event.type).map { definition ->
            CompanionDialogue(
                id = definition.id,
                text = definition.text,
                source = CompanionDialogueSource.NIGHT_EVENT,
                priority = 400 + rarityRank(event.rarity),
                cooldownSelections = 3,
            )
        }
    }

    private fun routineCandidates(
        context: CompanionDialogueContext,
    ): List<CompanionDialogue> {
        val profile = context.routineProfile ?: return emptyList()
        val routineContext = context.routineContext ?: return emptyList()
        val expectedPhase = when (context.phase) {
            CompanionDialoguePhase.BEDTIME -> RoutineDialoguePhase.BEDTIME
            CompanionDialoguePhase.MORNING -> RoutineDialoguePhase.MORNING
        }
        if (routineContext.phase != expectedPhase) return emptyList()

        // RoutineDialogueSelector remains the evidence gate. Repetition is
        // handled once at this outer selector so authored variants can rotate.
        val supported = RoutineDialogueSelector.select(
            profile = profile,
            context = routineContext,
            recentDialogueIds = emptySet(),
        )
        if (supported.source == RoutineDialogueSource.GENERIC) return emptyList()

        val exact = CompanionDialogue(
            id = supported.id,
            text = supported.text,
            source = CompanionDialogueSource.ROUTINE,
            priority = 305,
            cooldownSelections = 3,
        )
        if (context.phase != CompanionDialoguePhase.MORNING) return listOf(exact)

        val variants = MorningDialogueCatalog
            .forRoutineTrigger(supported.id)
            .map { definition ->
                CompanionDialogue(
                    id = definition.id,
                    text = definition.text,
                    source = CompanionDialogueSource.ROUTINE,
                    priority = 300,
                    cooldownSelections = 3,
                )
            }
        return listOf(exact) + variants
    }

    private fun relationshipCandidates(
        context: CompanionDialogueContext,
    ): List<CompanionDialogue> {
        if (context.phase == CompanionDialoguePhase.BEDTIME) return emptyList()

        return MorningDialogueCatalog
            .forRelationship(context.relationship.familiarityStage)
            .map { definition ->
                CompanionDialogue(
                    id = definition.id,
                    text = definition.text,
                    source = CompanionDialogueSource.RELATIONSHIP,
                    priority = 200 + definition.minimumFamiliarity.persistedValue * 10,
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
        CompanionDialoguePhase.MORNING ->
            MorningDialogueCatalog.genericDefinitions().map { definition ->
                generic(definition.id, definition.text)
            }
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

    private const val DREAM_GROUP_COOLDOWN_SELECTIONS = 8
}
