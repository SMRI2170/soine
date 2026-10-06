package app.soine.dream

import app.soine.night.RarityBand
import app.soine.relationship.FamiliarityStage
import kotlin.test.*

class InitialDreamCatalogTest {
    @Test fun catalogContainsExactlyThirtyUniqueAuthoredDreams() {
        val dreams = InitialDreamCatalog.definitions

        assertEquals(30, dreams.size)
        assertEquals(30, dreams.map { it.id }.distinct().size)
        assertEquals(30, dreams.map { it.title }.distinct().size)
        assertEquals(30, dreams.map { it.shortLine }.distinct().size)
    }

    @Test fun everyDreamHasArtKeyAndCurrentContentVersion() {
        InitialDreamCatalog.definitions.forEach { dream ->
            assertFalse(dream.artKey.isNullOrBlank(), dream.id)
            assertEquals(DreamDefinition.CURRENT_CONTENT_VERSION, dream.contentVersion)
        }

        assertEquals(
            30,
            InitialDreamCatalog.definitions.mapNotNull { it.artKey }.distinct().size,
        )
    }

    @Test fun catalogContainsCommonUncommonAndRareDreams() {
        val rarities = InitialDreamCatalog.definitions.map { it.rarity }.toSet()

        assertEquals(RarityBand.entries.toSet(), rarities)
        assertTrue(
            InitialDreamCatalog.definitions.count { it.rarity == RarityBand.RARE } >= 3
        )
    }

    @Test fun catalogProvidesContentAcrossEveryRelationshipStage() {
        FamiliarityStage.entries.forEach { stage ->
            assertTrue(
                InitialDreamCatalog.definitions.any {
                    it.minimumFamiliarity == stage.persistedValue
                },
                "Missing authored dreams for " + stage,
            )
        }
    }

    @Test fun eachSeasonHasAtLeastTwoSeasonalDreams() {
        DreamSeason.entries.forEach { season ->
            assertTrue(
                InitialDreamCatalog.definitions.count {
                    season in it.eligibleSeasons
                } >= 2,
                "Missing seasonal coverage for " + season,
            )
        }
    }

    @Test fun allYearDreamsRemainMajorityOfInitialCatalog() {
        val allYear = InitialDreamCatalog.definitions.count {
            it.eligibleSeasons.isEmpty()
        }

        assertTrue(allYear >= 20)
    }

    @Test fun repositoryAppliesFamiliarityAndSeasonGatesToInitialCatalog() {
        val newWinter = InitialDreamCatalog.repository.eligible(
            DreamEligibilityContext(
                familiarity = FamiliarityStage.NEW.persistedValue,
                season = DreamSeason.WINTER,
            )
        )
        val closeWinter = InitialDreamCatalog.repository.eligible(
            DreamEligibilityContext(
                familiarity = FamiliarityStage.CLOSE.persistedValue,
                season = DreamSeason.WINTER,
            )
        )

        assertTrue(newWinter.all {
            it.minimumFamiliarity <= FamiliarityStage.NEW.persistedValue &&
                it.isSeasonEligible(DreamSeason.WINTER)
        })
        assertTrue(closeWinter.size > newWinter.size)
        assertTrue(closeWinter.any { it.id == "morning-before-morning" })
        assertFalse(newWinter.any { it.id == "morning-before-morning" })
    }

    @Test fun unknownSeasonDoesNotExposeSeasonalDreams() {
        val eligible = InitialDreamCatalog.repository.eligible(
            DreamEligibilityContext(
                familiarity = FamiliarityStage.CLOSE.persistedValue,
                season = null,
            )
        )

        assertTrue(eligible.isNotEmpty())
        assertTrue(eligible.all { it.eligibleSeasons.isEmpty() })
    }

    @Test fun contentAvoidsMedicalProductivityAndGuiltLanguage() {
        val forbidden = listOf(
            "睡眠不足",
            "寝不足",
            "不健康",
            "病気",
            "治療",
            "診断",
            "薬",
            "生産性",
            "効率",
            "頑張らないと",
            "眠らないと",
            "連続記録",
            "ストリーク",
        )

        InitialDreamCatalog.definitions.forEach { dream ->
            val text = dream.title + dream.shortLine
            forbidden.forEach { phrase ->
                assertFalse(
                    text.contains(phrase),
                    "Forbidden phrase in " + dream.id + ": " + phrase,
                )
            }
        }
    }

    @Test fun closeStageDreamsAreNotOverrepresented() {
        val closeOnly = InitialDreamCatalog.definitions.count {
            it.minimumFamiliarity == FamiliarityStage.CLOSE.persistedValue
        }

        assertTrue(closeOnly <= 3)
    }
}
