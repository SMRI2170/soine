package app.soine.night

import app.soine.companion.CompanionAnimationMapping
import app.soine.relationship.RelationshipState
import app.soine.sleep.SleepSessionRecord
import app.soine.sleep.SleepSessionSource
import app.soine.sleep.SleepSessionStatus
import kotlin.test.*

class InitialNightEventCatalogTest {
    @Test fun catalogContainsAtLeastTwentyUniqueVariations() {
        val definitions = InitialNightEventCatalog.definitions
        assertTrue(definitions.size >= 20)
        assertEquals(definitions.size, definitions.map { it.id }.distinct().size)
    }

    @Test fun everyDefinitionHasCompletePresentationMetadata() {
        InitialNightEventCatalog.definitions.forEach { definition ->
            assertTrue(definition.weight > 0)
            assertTrue(definition.morningLine.isNotBlank())
            assertTrue(definition.cooldownNights >= 0)
            assertTrue(definition.version > 0)
            assertTrue(
                CompanionAnimationMapping.preferredClip(definition.animationIntent).id.isNotBlank(),
            )
        }
    }

    @Test fun catalogCoversCoreNightEventTypes() {
        val types = InitialNightEventCatalog.definitions.map { it.type }.toSet()
        assertTrue(NightEventType.TURN_OVER in types)
        assertTrue(NightEventType.EAR_TWITCH in types)
        assertTrue(NightEventType.MOVE_CLOSER in types)
        assertTrue(NightEventType.CURL_UP in types)
        assertTrue(NightEventType.BRIEF_WAKE in types)
        assertTrue(NightEventType.FUNNY_POSE in types)
        assertTrue(NightEventType.DREAM in types)
        assertTrue(NightEventType.SOUND_REACTION in types)
    }

    @Test fun soundReactionContentRequiresAnObservedSoundSignal() {
        val soundDefinitions = InitialNightEventCatalog.definitions
            .filter { it.type == NightEventType.SOUND_REACTION }
        assertTrue(soundDefinitions.isNotEmpty())
        assertTrue(soundDefinitions.all { it.eligibility.requiresSoundSignal })

        val engine = WeightedNightEventEngine(soundDefinitions.map { it.toCandidate() })
        assertTrue(engine.generate(input(soundReactionCount = null)).isEmpty())
        assertTrue(engine.generate(input(soundReactionCount = 0)).isEmpty())
        assertTrue(engine.generate(input(soundReactionCount = 1)).isNotEmpty())
    }

    @Test fun generatedEventCanResolveBackToMorningContent() {
        val definition = InitialNightEventCatalog.find("curl-up-round")!!
        val event = NightEvent(
            id = "night-1:${definition.id}",
            type = definition.type,
            occurredAtEpochMillis = 2_000,
            rarity = definition.rarity,
            payloadVersion = definition.version,
        )

        assertEquals(definition, InitialNightEventCatalog.definitionFor(event))
    }

    @Test fun perEventCooldownSuppressesRecentRepeat() {
        val definition = InitialNightEventCatalog.find("funny-pose-twist")!!
        val candidate = definition.toCandidate()
        val history = NightEventHistory(
            recentNights = listOf(
                listOf(
                    NightEvent(
                        id = "previous:${definition.id}",
                        type = definition.type,
                        occurredAtEpochMillis = 2_000,
                        rarity = definition.rarity,
                    ),
                ),
            ),
        )

        assertNull(NightEventHistoryPolicy.primaryWeight(candidate, history))
    }

    private fun input(soundReactionCount: Int?) = NightEventEngineInput(
        session = SleepSessionRecord(
            id = "night-test",
            startedAtEpochMillis = 1_000,
            endedAtEpochMillis = 10_000,
            status = SleepSessionStatus.COMPLETED,
            source = SleepSessionSource.MANUAL,
            createdAtEpochMillis = 1_000,
            updatedAtEpochMillis = 10_000,
        ),
        relationship = RelationshipState(familiarity = 3),
        signals = NightEventSignals(soundReactionCount = soundReactionCount),
        maxEvents = 1,
    )
}
