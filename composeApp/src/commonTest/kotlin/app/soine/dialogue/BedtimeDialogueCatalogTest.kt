package app.soine.dialogue

import app.soine.relationship.FamiliarityStage
import app.soine.relationship.RelationshipState
import kotlin.test.*

class BedtimeDialogueCatalogTest {
    @Test fun catalogContainsAtLeastThirtyUniqueLines() {
        assertTrue(BedtimeDialogueCatalog.all.size >= 30)
        assertEquals(
            BedtimeDialogueCatalog.all.size,
            BedtimeDialogueCatalog.all.map { it.id }.distinct().size,
        )
    }

    @Test fun eachMomentHasAtLeastTenLines() {
        BedtimeDialogueMoment.entries.forEach { moment ->
            assertTrue(
                BedtimeDialogueCatalog.all.count { it.moment == moment } >= 10,
                "Not enough lines for " + moment,
            )
        }
    }

    @Test fun lightsOutLinesStayShort() {
        val lightsOut = BedtimeDialogueCatalog.all.filter {
            it.moment == BedtimeDialogueMoment.LIGHTS_OUT
        }

        assertTrue(lightsOut.all { it.text.length <= 12 })
    }

    @Test fun contentAvoidsExcitedMedicalAndProductivityLanguage() {
        val forbidden = listOf(
            "！",
            "睡眠不足",
            "不健康",
            "病気",
            "治療",
            "診断",
            "生産性",
            "効率",
            "頑張",
            "眠らないと",
        )

        BedtimeDialogueCatalog.all.forEach { line ->
            forbidden.forEach { phrase ->
                assertFalse(
                    line.text.contains(phrase),
                    "Forbidden phrase in " + line.id + ": " + phrase,
                )
            }
        }
    }

    @Test fun newRelationshipDoesNotReceiveLaterStageLines() {
        val eligible = BedtimeDialogueCatalog.eligible(
            stage = FamiliarityStage.NEW,
            moment = BedtimeDialogueMoment.SETTLING,
        )

        assertTrue(eligible.isNotEmpty())
        assertTrue(eligible.all { it.minimumFamiliarity == FamiliarityStage.NEW })
    }

    @Test fun closeRelationshipCanUseCloseSpecificLines() {
        val eligible = BedtimeDialogueCatalog.eligible(
            stage = FamiliarityStage.CLOSE,
            moment = BedtimeDialogueMoment.ARRIVAL,
        )

        assertTrue(eligible.any { it.minimumFamiliarity == FamiliarityStage.CLOSE })
    }

    @Test fun selectorUsesRequestedBedtimeMoment() {
        val selected = CompanionDialogueSelector.select(
            CompanionDialogueContext(
                phase = CompanionDialoguePhase.BEDTIME,
                relationship = RelationshipState(),
                bedtimeMoment = BedtimeDialogueMoment.LIGHTS_OUT,
                selectionMode = CompanionDialogueSelectionMode.DeterministicTest(7),
            )
        )

        assertTrue(selected.id.startsWith("bed-lights-"))
        assertTrue(selected.text.length <= 12)
    }

    @Test fun laterRelationshipPrefersRelationshipSpecificBedtimeLine() {
        val selected = CompanionDialogueSelector.select(
            CompanionDialogueContext(
                phase = CompanionDialoguePhase.BEDTIME,
                relationship = RelationshipState(
                    totalCompletedSleepMillis = 120L * 3_600_000L,
                    completedSessions = 31,
                    familiarity = FamiliarityStage.CLOSE.persistedValue,
                ),
                bedtimeMoment = BedtimeDialogueMoment.SETTLING,
            )
        )

        assertEquals(CompanionDialogueSource.RELATIONSHIP, selected.source)
        assertTrue(selected.id.startsWith("bed-settle-"))
    }

    @Test fun recentBedtimeLineRotatesToAnotherEligibleLine() {
        val first = CompanionDialogueSelector.select(
            CompanionDialogueContext(
                phase = CompanionDialoguePhase.BEDTIME,
                relationship = RelationshipState(),
                bedtimeMoment = BedtimeDialogueMoment.ARRIVAL,
            )
        )
        val second = CompanionDialogueSelector.select(
            CompanionDialogueContext(
                phase = CompanionDialoguePhase.BEDTIME,
                relationship = RelationshipState(),
                bedtimeMoment = BedtimeDialogueMoment.ARRIVAL,
                recentDialogueIdsNewestFirst = listOf(first.id),
            )
        )

        assertNotEquals(first.id, second.id)
    }
}
