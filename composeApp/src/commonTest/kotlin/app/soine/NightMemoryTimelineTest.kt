package app.soine

import app.soine.night.InitialNightEventCatalog
import app.soine.night.NightEvent
import app.soine.night.NightEventEngineInput
import app.soine.night.NightEventType
import app.soine.night.RarityBand
import app.soine.relationship.RelationshipState
import app.soine.sleep.SleepSessionRecord
import app.soine.sleep.SleepSessionSource
import app.soine.sleep.SleepSessionStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NightMemoryTimelineTest {
    @Test
    fun timelineSortsByOccurredAtAndCapsAtThreeEntries() {
        val events = listOf(
            event("night:curl-small", NightEventType.CURL_UP, 20_000L),
            event("night:turn-soft", NightEventType.TURN_OVER, 10_000L),
            event("night:ear-one-twitch", NightEventType.EAR_TWITCH, 15_000L),
            event("night:wake-peek", NightEventType.BRIEF_WAKE, 25_000L),
        )

        val entries = buildNightMemoryEntries(events)

        assertEquals(3, entries.size)
        assertEquals(
            listOf("night:turn-soft", "night:ear-one-twitch", "night:curl-small"),
            entries.map { it.eventId },
        )
    }

    @Test
    fun emptyEventsProduceNoTimelineEntries() {
        assertTrue(buildNightMemoryEntries(emptyList()).isEmpty())
    }

    @Test
    fun unknownContentIsHiddenInsteadOfFabricatingCopy() {
        val entries = buildNightMemoryEntries(
            listOf(event("night:unknown", NightEventType.TURN_OVER, 10_000L))
        )

        assertTrue(entries.isEmpty())
    }

    @Test
    fun entryUsesExactAuthoredLineAndAnimationArtHook() {
        val entry = buildNightMemoryEntries(
            listOf(event("night:turn-soft", NightEventType.TURN_OVER, 10_000L))
        ).single()
        val definition = InitialNightEventCatalog.find("turn-soft")!!

        assertEquals(definition.morningLine, entry.line)
        assertEquals("night_event_turn", entry.artKey)
        assertTrue(entry.glyph.isNotBlank())
    }

    @Test
    fun japaneseClockLabelUsesJst() {
        assertEquals("09:00", formatNightEventTime(0L))
        assertEquals("00:30", formatNightEventTime(55_800_000L))
    }

    @Test
    fun sameCompletedSessionProducesSamePresentedMemoryOrder() {
        val session = completedSession()
        val input = NightEventEngineInput(
            session = session,
            relationship = RelationshipState(familiarity = 3),
            maxEvents = 3,
        )

        val first = buildNightMemoryEntries(InitialNightEventCatalog.engine.generate(input))
        val reopened = buildNightMemoryEntries(InitialNightEventCatalog.engine.generate(input))

        assertEquals(first, reopened)
        assertTrue(first.size <= 3)
    }

    private fun event(
        id: String,
        type: NightEventType,
        at: Long,
    ) = NightEvent(
        id = id,
        type = type,
        occurredAtEpochMillis = at,
        rarity = RarityBand.COMMON,
    )

    private fun completedSession() = SleepSessionRecord(
        id = "timeline-night",
        startedAtEpochMillis = 1_000L,
        endedAtEpochMillis = 28_801_000L,
        status = SleepSessionStatus.COMPLETED,
        source = SleepSessionSource.MANUAL,
        createdAtEpochMillis = 1_000L,
        updatedAtEpochMillis = 28_801_000L,
    )
}
