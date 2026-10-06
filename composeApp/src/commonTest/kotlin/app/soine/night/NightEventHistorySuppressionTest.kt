package app.soine.night

import app.soine.relationship.RelationshipState
import app.soine.sleep.SleepSessionRecord
import app.soine.sleep.SleepSessionSource
import app.soine.sleep.SleepSessionStatus
import kotlin.test.*

class NightEventHistorySuppressionTest {
    @Test fun previousNightRepeatGetsReducedWeight() {
        val candidate = candidate("turn", NightEventType.TURN_OVER, 50)
        val history = NightEventHistory(
            recentNights = listOf(listOf(event("previous:turn", NightEventType.TURN_OVER))),
        )
        assertEquals(10, NightEventHistoryPolicy.primaryWeight(candidate, history))
    }

    @Test fun rareCandidateIsExcludedDuringThreeNightCooldown() {
        val candidate = candidate("dream", NightEventType.DREAM, 20, RarityBand.RARE)
        val history = NightEventHistory(
            recentNights = listOf(
                listOf(event("n-1:turn", NightEventType.TURN_OVER)),
                listOf(event("n-2:dream", NightEventType.DREAM, RarityBand.RARE)),
            ),
        )
        assertNull(NightEventHistoryPolicy.primaryWeight(candidate, history))
    }

    @Test fun rareCandidateReturnsAfterCooldownWindow() {
        val candidate = candidate("dream", NightEventType.DREAM, 20, RarityBand.RARE)
        val history = NightEventHistory(
            recentNights = listOf(
                listOf(event("n-1:turn", NightEventType.TURN_OVER)),
                listOf(event("n-2:ear", NightEventType.EAR_TWITCH)),
                listOf(event("n-3:curl", NightEventType.CURL_UP)),
                listOf(event("n-4:dream", NightEventType.DREAM, RarityBand.RARE)),
            ),
        )
        assertEquals(20, NightEventHistoryPolicy.primaryWeight(candidate, history))
    }

    @Test fun poolShortageFallsBackInsteadOfReturningEmptyNight() {
        val rare = candidate("dream", NightEventType.DREAM, 1, RarityBand.RARE)
        val history = NightEventHistory(
            recentNights = listOf(
                listOf(event("previous:dream", NightEventType.DREAM, RarityBand.RARE)),
            ),
        )
        val generated = WeightedNightEventEngine(listOf(rare))
            .generate(input(history = history, maxEvents = 1))
        assertEquals(1, generated.size)
        assertEquals(NightEventType.DREAM, generated.single().type)
    }

    @Test fun persistedV1MetadataKeepsPreSuppressionBehavior() {
        val rare = candidate("dream", NightEventType.DREAM, 1, RarityBand.RARE)
        val session = completedSession()
        val history = NightEventHistory(
            recentNights = listOf(
                listOf(event("previous:dream", NightEventType.DREAM, RarityBand.RARE)),
            ),
        )
        val v1 = NightEventGenerationMetadata.forSession(session, algorithmVersion = 1)

        val generated = WeightedNightEventEngine(listOf(rare)).generate(
            input(session, history, 1, v1),
        )

        assertEquals(1, generated.size)
        assertEquals(NightEventType.DREAM, generated.single().type)
    }

    @Test fun recentEventsStillActsAsOneNightHistory() {
        val candidate = candidate("turn", NightEventType.TURN_OVER, 50)
        val history = NightEventHistory(
            recentEvents = listOf(event("previous:turn", NightEventType.TURN_OVER)),
        )
        assertEquals(10, NightEventHistoryPolicy.primaryWeight(candidate, history))
    }

    private fun candidate(
        id: String,
        type: NightEventType,
        weight: Int,
        rarity: RarityBand = RarityBand.COMMON,
    ) = WeightedNightEventCandidate(id, type, weight, rarity)

    private fun event(
        id: String,
        type: NightEventType,
        rarity: RarityBand = RarityBand.COMMON,
    ) = NightEvent(id, type, 500, rarity)

    private fun input(
        session: SleepSessionRecord = completedSession(),
        history: NightEventHistory,
        maxEvents: Int,
        metadata: NightEventGenerationMetadata? = null,
    ) = NightEventEngineInput(
        session = session,
        relationship = RelationshipState(familiarity = 3),
        history = history,
        generationMetadata = metadata,
        maxEvents = maxEvents,
    )

    private fun completedSession() = SleepSessionRecord(
        id = "night-current",
        startedAtEpochMillis = 1_000,
        endedAtEpochMillis = 28_801_000,
        status = SleepSessionStatus.COMPLETED,
        source = SleepSessionSource.MANUAL,
        createdAtEpochMillis = 1_000,
        updatedAtEpochMillis = 28_801_000,
    )
}
