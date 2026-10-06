package app.soine.dream

import app.soine.night.RarityBand
import app.soine.relationship.RelationshipState
import app.soine.sleep.SleepSessionRecord
import app.soine.sleep.SleepSessionStatus
import kotlin.test.*

class DreamDiscoveryEngineTest {
    @Test fun activeSessionCannotBeEvaluated() {
        val session = session(
            id = "active",
            start = 1_000,
            end = null,
            status = SleepSessionStatus.SLEEPING,
        )

        assertFailsWith<IllegalArgumentException> {
            DreamDiscoveryInput(
                session = session,
                relationship = RelationshipState(),
            )
        }
    }

    @Test fun completedSessionGetsAtMostOneDeterministicDecision() {
        val repository = InMemoryDreamDefinitionRepository(
            listOf(dream("cloud"), dream("moon"))
        )
        val engine = DeterministicDreamDiscoveryEngine(
            repository = repository,
            discoveryChancePercent = 100,
        )
        val input = input(session("night-1", 1_000, 10_000))

        val first = assertIs<DreamDiscoveryOutcome.Evaluated>(engine.evaluate(input))
        val second = assertIs<DreamDiscoveryOutcome.Evaluated>(engine.evaluate(input))

        assertNotNull(first.discovery)
        assertEquals(first.decision, second.decision)
        assertEquals(first.discovery, second.discovery)
        assertEquals(first.dream, second.dream)
    }

    @Test fun priorNoDreamDecisionPreventsReroll() {
        val repository = InMemoryDreamDefinitionRepository(listOf(dream("cloud")))
        val engine = DeterministicDreamDiscoveryEngine(
            repository = repository,
            discoveryChancePercent = 100,
        )
        val session = session("night-fixed", 1_000, 10_000)
        val previous = DreamDiscoveryDecision(
            sessionId = session.id,
            algorithmVersion = 1,
            dreamId = null,
            evaluatedAtEpochMillis = 10_000,
        )

        val result = engine.evaluate(
            input(session).copy(priorDecisions = listOf(previous))
        )

        val existing = assertIs<DreamDiscoveryOutcome.Existing>(result)
        assertEquals(previous, existing.decision)
        assertNull(existing.dream)
    }

    @Test fun extremeDurationDoesNotChangeDiscoveryRollOrSelectedDream() {
        val repository = InMemoryDreamDefinitionRepository(
            listOf(
                dream("cloud", RarityBand.COMMON),
                dream("moon", RarityBand.UNCOMMON),
                dream("star", RarityBand.RARE),
            )
        )
        val engine = DeterministicDreamDiscoveryEngine(
            repository = repository,
            discoveryChancePercent = 100,
        )
        val short = session(
            id = "same-session-identity",
            start = 1_000,
            end = 1_001,
        )
        val extreme = session(
            id = "same-session-identity",
            start = 1_000,
            end = 1_000 + 30L * 24L * 60L * 60L * 1_000L,
        )

        val shortResult = assertIs<DreamDiscoveryOutcome.Evaluated>(
            engine.evaluate(input(short))
        )
        val extremeResult = assertIs<DreamDiscoveryOutcome.Evaluated>(
            engine.evaluate(input(extreme))
        )

        assertEquals(shortResult.decision.dreamId, extremeResult.decision.dreamId)
        assertEquals(shortResult.dream?.id, extremeResult.dream?.id)
    }

    @Test fun zeroChanceNeverDiscoversEvenAfterVeryLongSession() {
        val repository = InMemoryDreamDefinitionRepository(listOf(dream("cloud")))
        val engine = DeterministicDreamDiscoveryEngine(
            repository = repository,
            discoveryChancePercent = 0,
        )
        val veryLong = session(
            id = "long-night",
            start = 1_000,
            end = 1_000 + 365L * 24L * 60L * 60L * 1_000L,
        )

        val result = assertIs<DreamDiscoveryOutcome.Evaluated>(
            engine.evaluate(input(veryLong))
        )

        assertNull(result.discovery)
        assertNull(result.dream)
        assertNull(result.decision.dreamId)
    }

    @Test fun alreadyDiscoveredDreamIsExcludedFromFutureSelection() {
        val discovered = dream("already")
        val newDream = dream("new")
        val repository = InMemoryDreamDefinitionRepository(
            listOf(discovered, newDream)
        )
        val engine = DeterministicDreamDiscoveryEngine(
            repository = repository,
            discoveryChancePercent = 100,
        )

        val result = assertIs<DreamDiscoveryOutcome.Evaluated>(
            engine.evaluate(
                input(session("night-2", 2_000, 12_000)).copy(
                    existingDiscoveries = listOf(
                        DreamDiscovery(
                            dreamId = discovered.id,
                            sessionId = "old-night",
                            discoveredAtEpochMillis = 1_000,
                        )
                    )
                )
            )
        )

        assertEquals(newDream.id, result.dream?.id)
    }

    @Test fun allEligibleDreamsDiscoveredProducesNoDuplicate() {
        val only = dream("only")
        val repository = InMemoryDreamDefinitionRepository(listOf(only))
        val engine = DeterministicDreamDiscoveryEngine(
            repository = repository,
            discoveryChancePercent = 100,
        )

        val result = assertIs<DreamDiscoveryOutcome.Evaluated>(
            engine.evaluate(
                input(session("night-3", 3_000, 13_000)).copy(
                    existingDiscoveries = listOf(
                        DreamDiscovery(
                            dreamId = only.id,
                            sessionId = "old-night",
                            discoveredAtEpochMillis = 1_000,
                        )
                    )
                )
            )
        )

        assertNull(result.dream)
        assertNull(result.discovery)
    }

    @Test fun relationshipGateIsAppliedBeforeLottery() {
        val closeOnly = DreamDefinition(
            id = "close-only",
            title = "近くの夢",
            shortLine = "すぐそばで見た夢",
            rarity = RarityBand.COMMON,
            minimumFamiliarity = 3,
        )
        val repository = InMemoryDreamDefinitionRepository(listOf(closeOnly))
        val engine = DeterministicDreamDiscoveryEngine(
            repository = repository,
            discoveryChancePercent = 100,
        )

        val early = assertIs<DreamDiscoveryOutcome.Evaluated>(
            engine.evaluate(
                input(
                    session("early", 1_000, 2_000),
                    familiarity = 2,
                )
            )
        )
        val close = assertIs<DreamDiscoveryOutcome.Evaluated>(
            engine.evaluate(
                input(
                    session("close", 1_000, 2_000),
                    familiarity = 3,
                )
            )
        )

        assertNull(early.dream)
        assertEquals("close-only", close.dream?.id)
    }

    @Test fun seasonalGateIsAppliedBeforeLottery() {
        val winterOnly = DreamDefinition(
            id = "snow",
            title = "雪の夢",
            shortLine = "雪の上を歩いたみたい",
            rarity = RarityBand.COMMON,
            eligibleSeasons = setOf(DreamSeason.WINTER),
        )
        val repository = InMemoryDreamDefinitionRepository(listOf(winterOnly))
        val engine = DeterministicDreamDiscoveryEngine(
            repository = repository,
            discoveryChancePercent = 100,
        )

        val summer = assertIs<DreamDiscoveryOutcome.Evaluated>(
            engine.evaluate(
                input(session("summer", 1_000, 2_000)).copy(
                    season = DreamSeason.SUMMER,
                )
            )
        )
        val winter = assertIs<DreamDiscoveryOutcome.Evaluated>(
            engine.evaluate(
                input(session("winter", 1_000, 2_000)).copy(
                    season = DreamSeason.WINTER,
                )
            )
        )

        assertNull(summer.dream)
        assertEquals("snow", winter.dream?.id)
    }

    @Test fun discoveryTimestampUsesCompletedSessionEnd() {
        val repository = InMemoryDreamDefinitionRepository(listOf(dream("cloud")))
        val engine = DeterministicDreamDiscoveryEngine(
            repository = repository,
            discoveryChancePercent = 100,
        )

        val result = assertIs<DreamDiscoveryOutcome.Evaluated>(
            engine.evaluate(input(session("night-time", 1_000, 9_876)))
        )

        assertEquals(9_876, result.decision.evaluatedAtEpochMillis)
        assertEquals(9_876, result.discovery?.discoveredAtEpochMillis)
    }

    private fun input(
        session: SleepSessionRecord,
        familiarity: Int = 0,
    ) = DreamDiscoveryInput(
        session = session,
        relationship = RelationshipState(familiarity = familiarity),
    )

    private fun session(
        id: String,
        start: Long,
        end: Long?,
        status: SleepSessionStatus = SleepSessionStatus.COMPLETED,
    ) = SleepSessionRecord(
        id = id,
        startedAtEpochMillis = start,
        endedAtEpochMillis = end,
        status = status,
        createdAtEpochMillis = start,
        updatedAtEpochMillis = end ?: start,
    )

    private fun dream(
        id: String,
        rarity: RarityBand = RarityBand.COMMON,
    ) = DreamDefinition(
        id = id,
        title = "title-" + id,
        shortLine = "line-" + id,
        rarity = rarity,
    )
}
