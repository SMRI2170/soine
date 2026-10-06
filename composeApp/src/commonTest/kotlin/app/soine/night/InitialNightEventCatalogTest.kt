package app.soine.night

import app.soine.relationship.RelationshipState
import app.soine.sleep.SleepSessionRecord
import app.soine.sleep.SleepSessionSource
import app.soine.sleep.SleepSessionStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InitialNightEventCatalogTest {
    @Test
    fun catalogContainsTwentyUniqueAuthoredDefinitions() {
        val definitions = InitialNightEventCatalog.definitions

        assertEquals(20, definitions.size)
        assertEquals(20, definitions.map { it.id }.distinct().size)
        assertEquals(20, definitions.map { it.morningLine }.distinct().size)
        assertTrue(definitions.all { it.contentVersion == NightEventDefinition.CURRENT_CONTENT_VERSION })
    }

    @Test
    fun everyNightEventTypeHasInitialContent() {
        val covered = InitialNightEventCatalog.definitions.map { it.type }.toSet()

        assertEquals(NightEventType.entries.toSet(), covered)
    }

    @Test
    fun authoredDefinitionsCarryEligibilityRarityAnimationCooldownAndVersion() {
        val definitions = InitialNightEventCatalog.definitions

        assertTrue(definitions.all { it.weight > 0 })
        assertTrue(definitions.all { it.minimumFamiliarity >= 0 })
        assertTrue(definitions.all { it.cooldownNights >= 0 })
        assertTrue(definitions.all { it.payloadVersion > 0 })
        assertTrue(definitions.all { it.morningLine.isNotBlank() })
        assertTrue(definitions.any { it.rarity == RarityBand.RARE && it.cooldownNights >= 3 })
        assertTrue(definitions.any { it.minimumFamiliarity > 0 })
    }

    @Test
    fun candidateConversionPreservesGenerationMetadata() {
        InitialNightEventCatalog.definitions.forEach { definition ->
            val candidate = definition.toCandidate()

            assertEquals(definition.id, candidate.id)
            assertEquals(definition.type, candidate.type)
            assertEquals(definition.weight, candidate.weight)
            assertEquals(definition.rarity, candidate.rarity)
            assertEquals(definition.minimumFamiliarity, candidate.minimumFamiliarity)
            assertEquals(definition.cooldownNights, candidate.cooldownNights)
            assertEquals(definition.payloadVersion, candidate.payloadVersion)
        }
    }

    @Test
    fun generatedEventsResolveBackToAuthoredContent() {
        val events = InitialNightEventCatalog.engine.generate(
            NightEventEngineInput(
                session = completedSession(),
                relationship = RelationshipState(familiarity = 3),
                maxEvents = 3,
            )
        )

        assertTrue(events.isNotEmpty())
        events.forEach { event ->
            val definition = InitialNightEventCatalog.definitionFor(event)
            assertNotNull(definition)
            assertEquals(event.type, definition.type)
            assertEquals(event.rarity, definition.rarity)
        }
    }

    @Test
    fun unknownEventDoesNotFabricatePresentationContent() {
        val unknown = NightEvent(
            id = "night-1:not-in-catalog",
            type = NightEventType.TURN_OVER,
            occurredAtEpochMillis = 2_000L,
        )

        assertNull(InitialNightEventCatalog.definitionFor(unknown))
    }

    private fun completedSession() = SleepSessionRecord(
        id = "catalog-night",
        startedAtEpochMillis = 1_000L,
        endedAtEpochMillis = 28_801_000L,
        status = SleepSessionStatus.COMPLETED,
        source = SleepSessionSource.MANUAL,
        createdAtEpochMillis = 1_000L,
        updatedAtEpochMillis = 28_801_000L,
    )
}
