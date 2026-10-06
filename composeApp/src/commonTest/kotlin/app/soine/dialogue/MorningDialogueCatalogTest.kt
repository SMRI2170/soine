package app.soine.dialogue

import app.soine.night.NightEventType
import app.soine.relationship.FamiliarityStage
import kotlin.test.*

class MorningDialogueCatalogTest {
    @Test fun catalogContainsAtLeastFiftyUniqueLines() {
        assertTrue(MorningDialogueCatalog.all.size >= 50)
        assertEquals(
            MorningDialogueCatalog.all.size,
            MorningDialogueCatalog.all.map { it.id }.distinct().size,
        )
        assertEquals(
            MorningDialogueCatalog.all.size,
            MorningDialogueCatalog.all.map { it.text }.distinct().size,
        )
    }

    @Test fun catalogCoversAllRequiredContentKinds() {
        val counts = MorningDialogueCatalog.all.groupingBy { it.kind }.eachCount()

        assertTrue((counts[MorningDialogueKind.GENERIC] ?: 0) >= 15)
        assertTrue((counts[MorningDialogueKind.EVENT] ?: 0) >= 16)
        assertTrue((counts[MorningDialogueKind.DREAM] ?: 0) >= 6)
        assertTrue((counts[MorningDialogueKind.ROUTINE] ?: 0) >= 6)
        assertTrue((counts[MorningDialogueKind.RELATIONSHIP] ?: 0) >= 7)
    }

    @Test fun everyNightEventHasAtLeastTwoMorningVariants() {
        NightEventType.entries.forEach { type ->
            assertTrue(
                MorningDialogueCatalog.forEvent(type).size >= 2,
                "Not enough morning dialogue variants for " + type,
            )
        }
    }

    @Test fun relationshipLinesAreStageGated() {
        assertTrue(
            MorningDialogueCatalog.forRelationship(FamiliarityStage.NEW).isEmpty()
        )
        assertTrue(
            MorningDialogueCatalog
                .forRelationship(FamiliarityStage.WARMING_UP)
                .all { it.minimumFamiliarity == FamiliarityStage.WARMING_UP }
        )
        assertTrue(
            MorningDialogueCatalog
                .forRelationship(FamiliarityStage.CLOSE)
                .any { it.minimumFamiliarity == FamiliarityStage.CLOSE }
        )
    }

    @Test fun routineVariantsOnlyExistForSupportedTriggerIds() {
        val supported = setOf(
            "routine-wake-earlier",
            "routine-wake-later",
            "routine-weekend-morning",
        )
        val routine = MorningDialogueCatalog.all.filter {
            it.kind == MorningDialogueKind.ROUTINE
        }

        assertTrue(routine.all { it.routineTriggerId in supported })
        supported.forEach { trigger ->
            assertTrue(MorningDialogueCatalog.forRoutineTrigger(trigger).size >= 2)
        }
    }

    @Test fun copyNeverBlamesShortSleepOrMakesMedicalJudgement() {
        val forbidden = listOf(
            "睡眠不足",
            "寝不足",
            "短すぎ",
            "もっと寝",
            "眠らないと",
            "不健康",
            "健康に悪",
            "病気",
            "治療",
            "診断",
            "薬",
            "頑張らないと",
            "だめ",
        )

        MorningDialogueCatalog.all.forEach { line ->
            forbidden.forEach { phrase ->
                assertFalse(
                    line.text.contains(phrase),
                    "Forbidden phrase in " + line.id + ": " + phrase,
                )
            }
        }
    }

    @Test fun eventDialogueRotatesWhenPreviousVariantIsRecent() {
        val event = app.soine.night.NightEvent(
            id = "night:event",
            type = NightEventType.TURN_OVER,
            occurredAtEpochMillis = 1_000,
        )
        val relationship = app.soine.relationship.RelationshipState()

        val first = CompanionDialogueSelector.select(
            CompanionDialogueContext(
                phase = CompanionDialoguePhase.MORNING,
                relationship = relationship,
                nightEvents = listOf(event),
            )
        )
        val second = CompanionDialogueSelector.select(
            CompanionDialogueContext(
                phase = CompanionDialoguePhase.MORNING,
                relationship = relationship,
                nightEvents = listOf(event),
                recentDialogueIdsNewestFirst = listOf(first.id),
            )
        )

        assertEquals(CompanionDialogueSource.NIGHT_EVENT, first.source)
        assertEquals(CompanionDialogueSource.NIGHT_EVENT, second.source)
        assertNotEquals(first.id, second.id)
        assertNotEquals(first.text, second.text)
    }

    @Test fun genericMorningDialogueRotatesWithoutImmediateRepeat() {
        val relationship = app.soine.relationship.RelationshipState()
        val first = CompanionDialogueSelector.select(
            CompanionDialogueContext(
                phase = CompanionDialoguePhase.MORNING,
                relationship = relationship,
            )
        )
        val second = CompanionDialogueSelector.select(
            CompanionDialogueContext(
                phase = CompanionDialoguePhase.MORNING,
                relationship = relationship,
                recentDialogueIdsNewestFirst = listOf(first.id),
            )
        )

        assertEquals(CompanionDialogueSource.GENERIC, first.source)
        assertEquals(CompanionDialogueSource.GENERIC, second.source)
        assertNotEquals(first.id, second.id)
    }
}
