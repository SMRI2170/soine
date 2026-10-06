package app.soine.night

import app.soine.relationship.RelationshipState
import app.soine.sleep.SleepSessionRecord
import app.soine.sleep.SleepSessionSource
import app.soine.sleep.SleepSessionStatus
import kotlin.test.*

class NightEventEngineTest {
    @Test fun completedSessionCanBuildEngineInput() {
        val input = NightEventEngineInput(
            session = completedSession(),
            relationship = RelationshipState(),
        )

        assertEquals(3, input.maxEvents)
        assertTrue(input.history.recentEvents.isEmpty())
        assertNull(input.signals.soundReactionCount)
    }

    @Test fun activeSessionIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            NightEventEngineInput(
                session = completedSession().copy(
                    status = SleepSessionStatus.SLEEPING,
                    endedAtEpochMillis = null,
                ),
                relationship = RelationshipState(),
            )
        }
    }

    @Test fun completedSessionWithoutEndIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            NightEventEngineInput(
                session = completedSession().copy(endedAtEpochMillis = null),
                relationship = RelationshipState(),
            )
        }
    }

    @Test fun eventLimitIsBounded() {
        assertFailsWith<IllegalArgumentException> {
            NightEventEngineInput(
                session = completedSession(),
                relationship = RelationshipState(),
                maxEvents = NightEventEngineInput.MAX_EVENTS_LIMIT + 1,
            )
        }
    }

    @Test fun optionalSignalsRejectNegativeCounts() {
        assertFailsWith<IllegalArgumentException> {
            NightEventSignals(soundReactionCount = -1)
        }
        assertFailsWith<IllegalArgumentException> {
            NightEventSignals(briefWakeCount = -1)
        }
    }

    @Test fun deterministicContractCanBeImplementedWithoutRendererTypes() {
        val event = NightEvent(
            id = "night-1:turn-over",
            type = NightEventType.TURN_OVER,
            occurredAtEpochMillis = 2_000,
        )
        val engine = object : NightEventEngine {
            override val rulesVersion: Int = 1
            override fun generate(input: NightEventEngineInput): List<NightEvent> =
                listOf(event).take(input.maxEvents)
        }
        val input = NightEventEngineInput(completedSession(), RelationshipState())

        assertEquals(engine.generate(input), engine.generate(input))
        assertEquals(1, engine.rulesVersion)
    }

    private fun completedSession() = SleepSessionRecord(
        id = "night-1",
        startedAtEpochMillis = 1_000,
        endedAtEpochMillis = 5_000,
        status = SleepSessionStatus.COMPLETED,
        source = SleepSessionSource.MANUAL,
        createdAtEpochMillis = 1_000,
        updatedAtEpochMillis = 5_000,
    )
}
