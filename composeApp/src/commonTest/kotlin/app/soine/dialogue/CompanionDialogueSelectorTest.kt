package app.soine.dialogue

import app.soine.dream.DreamDefinition
import app.soine.night.NightEvent
import app.soine.night.NightEventType
import app.soine.night.RarityBand
import app.soine.relationship.*
import kotlin.test.*

class CompanionDialogueSelectorTest {
    @Test fun morningPriorityIsDreamThenNightThenRoutineThenRelationshipThenGeneric() {
        val base = context(
            relationship = relationship(FamiliarityStage.CLOSE),
            routineProfile = highConfidenceRoutine(),
            routineContext = RoutineDialogueContext(
                phase = RoutineDialoguePhase.MORNING,
                currentMinuteOfDay = 8 * 60,
            ),
            nightEvents = listOf(
                NightEvent("n1", NightEventType.FUNNY_POSE, 1_000, RarityBand.RARE)
            ),
            discoveredDream = DreamDefinition(
                id = "moon",
                title = "月の夢",
                shortLine = "月の上を歩いていたみたい",
                rarity = RarityBand.RARE,
            ),
        )

        assertEquals(
            CompanionDialogueSource.DREAM,
            CompanionDialogueSelector.select(base).source,
        )
        assertEquals(
            CompanionDialogueSource.NIGHT_EVENT,
            CompanionDialogueSelector.select(
                base.copy(recentDialogueIdsNewestFirst = listOf("dream-moon"))
            ).source,
        )
    }

    @Test fun routineBeatsRelationshipWhenSupported() {
        val selected = CompanionDialogueSelector.select(
            context(
                phase = CompanionDialoguePhase.BEDTIME,
                relationship = relationship(FamiliarityStage.CLOSE),
                routineProfile = highConfidenceRoutine(),
                routineContext = RoutineDialogueContext(
                    phase = RoutineDialoguePhase.BEDTIME,
                    currentMinuteOfDay = 22 * 60,
                ),
            )
        )

        assertEquals(CompanionDialogueSource.ROUTINE, selected.source)
        assertEquals("routine-bedtime-earlier", selected.id)
    }

    @Test fun weakRoutineFallsBackToRelationshipWithoutPretendingToKnowRoutine() {
        val weak = RoutineProfile(
            observationCount = 5,
            typicalBedtime = RoutineTimeRange(
                startMinuteOfDay = 1380,
                endMinuteOfDay = 1430,
                centerMinuteOfDay = 1400,
                sampleCount = 5,
                confidence = RoutineConfidence.MEDIUM,
            ),
            typicalWakeTime = null,
            frequentAmbientSound = null,
            weekdayWeekendTendency = null,
        )

        val selected = CompanionDialogueSelector.select(
            context(
                phase = CompanionDialoguePhase.BEDTIME,
                relationship = relationship(FamiliarityStage.WARMING_UP),
                routineProfile = weak,
                routineContext = RoutineDialogueContext(
                    phase = RoutineDialoguePhase.BEDTIME,
                    currentMinuteOfDay = 22 * 60,
                ),
            )
        )

        assertEquals(CompanionDialogueSource.RELATIONSHIP, selected.source)
        assertFalse(selected.text.contains("いつもより"))
    }

    @Test fun newRelationshipWithNoEvidenceGetsGenericFallback() {
        val selected = CompanionDialogueSelector.select(
            context(relationship = relationship(FamiliarityStage.NEW))
        )

        assertEquals(CompanionDialogueSource.GENERIC, selected.source)
    }

    @Test fun cooldownSkipsRecentRelationshipLine() {
        val selected = CompanionDialogueSelector.select(
            context(
                relationship = relationship(FamiliarityStage.CLOSE),
                recent = listOf("relationship-close"),
            )
        )

        assertEquals(CompanionDialogueSource.GENERIC, selected.source)
        assertNotEquals("relationship-close", selected.id)
    }

    @Test fun recentDreamFallsThroughToNightEvent() {
        val selected = CompanionDialogueSelector.select(
            context(
                relationship = relationship(FamiliarityStage.FAMILIAR),
                nightEvents = listOf(
                    NightEvent("event", NightEventType.MOVE_CLOSER, 1_000, RarityBand.UNCOMMON)
                ),
                discoveredDream = DreamDefinition(
                    id = "cloud",
                    title = "雲の夢",
                    shortLine = "雲の上で眠っていたみたい",
                    rarity = RarityBand.UNCOMMON,
                ),
                recent = listOf("dream-cloud"),
            )
        )

        assertEquals(CompanionDialogueSource.NIGHT_EVENT, selected.source)
    }

    @Test fun rareNightEventWinsAmongNightEvents() {
        val selected = CompanionDialogueSelector.select(
            context(
                relationship = relationship(FamiliarityStage.NEW),
                nightEvents = listOf(
                    NightEvent("common", NightEventType.TURN_OVER, 2_000, RarityBand.COMMON),
                    NightEvent("rare", NightEventType.FUNNY_POSE, 1_000, RarityBand.RARE),
                ),
            )
        )

        assertEquals("night-event-funny_pose", selected.id)
    }

    @Test fun deterministicTestModeReturnsSameLineForSameSeed() {
        val a = CompanionDialogueSelector.select(
            context(
                relationship = relationship(FamiliarityStage.NEW),
                mode = CompanionDialogueSelectionMode.DeterministicTest(42),
            )
        )
        val b = CompanionDialogueSelector.select(
            context(
                relationship = relationship(FamiliarityStage.NEW),
                mode = CompanionDialogueSelectionMode.DeterministicTest(42),
            )
        )

        assertEquals(a, b)
        assertEquals(CompanionDialogueSource.GENERIC, a.source)
    }

    @Test fun authoredOutputsDoNotContainHealthJudgementLanguage() {
        val contexts = listOf(
            context(relationship = relationship(FamiliarityStage.NEW)),
            context(relationship = relationship(FamiliarityStage.WARMING_UP)),
            context(relationship = relationship(FamiliarityStage.FAMILIAR)),
            context(relationship = relationship(FamiliarityStage.CLOSE)),
            context(
                relationship = relationship(FamiliarityStage.CLOSE),
                nightEvents = NightEventType.entries.mapIndexed { index, type ->
                    NightEvent("e" + index, type, index.toLong(), RarityBand.COMMON)
                },
            ),
        )

        val forbidden = listOf("睡眠不足", "不健康", "病気", "治療", "診断", "眠らないと")

        contexts.forEach { input ->
            val line = CompanionDialogueSelector.select(input).text
            forbidden.forEach { phrase ->
                assertFalse(line.contains(phrase), "Unexpected health judgement: " + line)
            }
        }
    }

    private fun context(
        phase: CompanionDialoguePhase = CompanionDialoguePhase.MORNING,
        relationship: RelationshipState,
        routineProfile: RoutineProfile? = null,
        routineContext: RoutineDialogueContext? = null,
        nightEvents: List<NightEvent> = emptyList(),
        discoveredDream: DreamDefinition? = null,
        recent: List<String> = emptyList(),
        mode: CompanionDialogueSelectionMode = CompanionDialogueSelectionMode.Default,
    ) = CompanionDialogueContext(
        phase = phase,
        relationship = relationship,
        routineProfile = routineProfile,
        routineContext = routineContext,
        nightEvents = nightEvents,
        discoveredDream = discoveredDream,
        recentDialogueIdsNewestFirst = recent,
        selectionMode = mode,
    )

    private fun relationship(stage: FamiliarityStage) = RelationshipState(
        totalCompletedSleepMillis = when (stage) {
            FamiliarityStage.NEW -> 0
            FamiliarityStage.WARMING_UP -> 8L * 3_600_000L
            FamiliarityStage.FAMILIAR -> 30L * 3_600_000L
            FamiliarityStage.CLOSE -> 120L * 3_600_000L
        },
        completedSessions = when (stage) {
            FamiliarityStage.NEW -> 0
            FamiliarityStage.WARMING_UP -> 1
            FamiliarityStage.FAMILIAR -> 8
            FamiliarityStage.CLOSE -> 31
        },
        familiarity = stage.persistedValue,
    )

    private fun highConfidenceRoutine() = RoutineProfile(
        observationCount = 12,
        typicalBedtime = RoutineTimeRange(
            startMinuteOfDay = 23 * 60,
            endMinuteOfDay = 0,
            centerMinuteOfDay = 23 * 60 + 30,
            sampleCount = 12,
            confidence = RoutineConfidence.HIGH,
        ),
        typicalWakeTime = RoutineTimeRange(
            startMinuteOfDay = 6 * 60 + 30,
            endMinuteOfDay = 7 * 60 + 30,
            centerMinuteOfDay = 7 * 60,
            sampleCount = 12,
            confidence = RoutineConfidence.HIGH,
        ),
        frequentAmbientSound = null,
        weekdayWeekendTendency = null,
    )
}
