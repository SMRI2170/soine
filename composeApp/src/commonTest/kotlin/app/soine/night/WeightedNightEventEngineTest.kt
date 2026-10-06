package app.soine.night

import app.soine.relationship.RelationshipState
import app.soine.sleep.SleepSessionRecord
import app.soine.sleep.SleepSessionSource
import app.soine.sleep.SleepSessionStatus
import kotlin.test.*

class WeightedNightEventEngineTest {
    @Test fun weightedTicketUsesCandidateWeightRanges() {
        val weights = listOf(2, 3, 5)

        assertEquals(0, WeightedNightSelection.chooseIndex(weights, 0))
        assertEquals(0, WeightedNightSelection.chooseIndex(weights, 1))
        assertEquals(1, WeightedNightSelection.chooseIndex(weights, 2))
        assertEquals(1, WeightedNightSelection.chooseIndex(weights, 4))
        assertEquals(2, WeightedNightSelection.chooseIndex(weights, 5))
        assertEquals(2, WeightedNightSelection.chooseIndex(weights, 9))
    }

    @Test fun identicalInputProducesIdenticalEvents() {
        val engine = engine()
        val input = input(familiarity = 3, maxEvents = 3)
        assertEquals(engine.generate(input), engine.generate(input))
    }

    @Test fun relationshipGatingExcludesLockedCandidates() {
        val events = engine().generate(input(familiarity = 0, maxEvents = 5))
        assertTrue(events.none { it.type == NightEventType.DREAM })
        assertTrue(events.none { it.type == NightEventType.FUNNY_POSE })
    }

    @Test fun selectionNeverExceedsNightLimitAndDoesNotDuplicateCandidates() {
        val events = engine().generate(input(familiarity = 3, maxEvents = 3))
        assertEquals(3, events.size)
        assertEquals(events.size, events.map { it.id }.distinct().size)
    }

    @Test fun candidateRarityAndPayloadVersionArePreserved() {
        val candidate = WeightedNightEventCandidate(
            id = "rare-dream",
            type = NightEventType.DREAM,
            weight = 1,
            rarity = RarityBand.RARE,
            payloadVersion = 2,
        )
        val event = WeightedNightEventEngine(listOf(candidate))
            .generate(input(familiarity = 0, maxEvents = 1))
            .single()

        assertEquals(RarityBand.RARE, event.rarity)
        assertEquals(2, event.payloadVersion)
    }

    @Test fun eventTimesStayInsideCompletedSessionAndAreChronological() {
        val input = input(familiarity = 3, maxEvents = 5)
        val events = engine().generate(input)
        val start = input.session.startedAtEpochMillis
        val end = input.session.endedAtEpochMillis!!

        assertTrue(events.all { it.occurredAtEpochMillis in start..end })
        assertEquals(events.sortedBy { it.occurredAtEpochMillis }, events)
    }

    @Test fun invalidCandidateDefinitionsAreRejected() {
        assertFailsWith<IllegalArgumentException> {
            WeightedNightEventCandidate("bad", NightEventType.TURN_OVER, weight = 0)
        }
        assertFailsWith<IllegalArgumentException> {
            WeightedNightEventEngine(
                listOf(
                    WeightedNightEventCandidate("same", NightEventType.TURN_OVER, 1),
                    WeightedNightEventCandidate("same", NightEventType.EAR_TWITCH, 1),
                ),
            )
        }
    }

    private fun engine() = WeightedNightEventEngine(
        listOf(
            WeightedNightEventCandidate("turn", NightEventType.TURN_OVER, 50),
            WeightedNightEventCandidate("ear", NightEventType.EAR_TWITCH, 30),
            WeightedNightEventCandidate(
                "funny",
                NightEventType.FUNNY_POSE,
                15,
                rarity = RarityBand.UNCOMMON,
                minimumFamiliarity = 1,
            ),
            WeightedNightEventCandidate(
                "dream",
                NightEventType.DREAM,
                5,
                rarity = RarityBand.RARE,
                minimumFamiliarity = 2,
            ),
        ),
    )

    private fun input(familiarity: Int, maxEvents: Int) = NightEventEngineInput(
        session = SleepSessionRecord(
            id = "night-1",
            startedAtEpochMillis = 1_000,
            endedAtEpochMillis = 28_801_000,
            status = SleepSessionStatus.COMPLETED,
            source = SleepSessionSource.MANUAL,
            createdAtEpochMillis = 1_000,
            updatedAtEpochMillis = 28_801_000,
        ),
        relationship = RelationshipState(familiarity = familiarity),
        maxEvents = maxEvents,
    )
}
