package app.soine.dream

import app.soine.night.RarityBand
import kotlin.test.*

class DreamDefinitionRepositoryTest {
    @Test fun allYearDreamIsEligibleWithoutSeason() {
        val dream = dream(id = "cloud")
        val repository = InMemoryDreamDefinitionRepository(listOf(dream))

        assertEquals(
            listOf(dream),
            repository.eligible(DreamEligibilityContext(familiarity = 0)),
        )
    }

    @Test fun seasonalDreamRequiresMatchingSeason() {
        val summer = dream(
            id = "summer-sea",
            seasons = setOf(DreamSeason.SUMMER),
        )
        val repository = InMemoryDreamDefinitionRepository(listOf(summer))

        assertTrue(
            repository.eligible(
                DreamEligibilityContext(familiarity = 0, season = DreamSeason.WINTER)
            ).isEmpty()
        )
        assertTrue(
            repository.eligible(
                DreamEligibilityContext(familiarity = 0, season = null)
            ).isEmpty()
        )
        assertEquals(
            listOf(summer),
            repository.eligible(
                DreamEligibilityContext(familiarity = 0, season = DreamSeason.SUMMER)
            ),
        )
    }

    @Test fun familiarityGatePreventsEarlyDiscovery() {
        val closeDream = dream(id = "close", familiarity = 3)
        val repository = InMemoryDreamDefinitionRepository(listOf(closeDream))

        assertTrue(
            repository.eligible(DreamEligibilityContext(familiarity = 2)).isEmpty()
        )
        assertEquals(
            listOf(closeDream),
            repository.eligible(DreamEligibilityContext(familiarity = 3)),
        )
    }

    @Test fun repositoryOrderingIsStableById() {
        val repository = InMemoryDreamDefinitionRepository(
            listOf(
                dream("zeta"),
                dream("alpha"),
                dream("middle"),
            )
        )

        assertEquals(
            listOf("alpha", "middle", "zeta"),
            repository.all().map { it.id },
        )
    }

    @Test fun duplicateIdsAreRejected() {
        assertFailsWith<IllegalArgumentException> {
            InMemoryDreamDefinitionRepository(
                listOf(
                    dream("same"),
                    dream("same", version = 2),
                )
            )
        }
    }

    @Test fun findReturnsCurrentDefinitionIncludingContentVersionAndArtKey() {
        val expected = dream(
            id = "moon",
            artKey = "dream_moon",
            version = 3,
        )
        val repository = InMemoryDreamDefinitionRepository(listOf(expected))

        val actual = assertNotNull(repository.find("moon"))

        assertEquals(3, actual.contentVersion)
        assertEquals("dream_moon", actual.artKey)
        assertNull(repository.find("missing"))
    }

    @Test fun definitionValidationRejectsInvalidContent() {
        assertFailsWith<IllegalArgumentException> {
            dream(id = " ")
        }
        assertFailsWith<IllegalArgumentException> {
            dream(id = "bad-familiarity", familiarity = -1)
        }
        assertFailsWith<IllegalArgumentException> {
            dream(id = "bad-version", version = 0)
        }
        assertFailsWith<IllegalArgumentException> {
            dream(id = "bad-art", artKey = " ")
        }
    }

    @Test fun discoveryValidationRejectsMissingIdentity() {
        assertFailsWith<IllegalArgumentException> {
            DreamDiscovery("", "night-1", 1_000)
        }
        assertFailsWith<IllegalArgumentException> {
            DreamDiscovery("cloud", "", 1_000)
        }
        assertFailsWith<IllegalArgumentException> {
            DreamDiscovery("cloud", "night-1", -1)
        }
    }

    private fun dream(
        id: String,
        familiarity: Int = 0,
        seasons: Set<DreamSeason> = emptySet(),
        artKey: String? = null,
        version: Int = 1,
    ) = DreamDefinition(
        id = id,
        title = "title-" + id,
        shortLine = "line-" + id,
        rarity = RarityBand.COMMON,
        minimumFamiliarity = familiarity,
        eligibleSeasons = seasons,
        artKey = artKey,
        contentVersion = version,
    )
}
