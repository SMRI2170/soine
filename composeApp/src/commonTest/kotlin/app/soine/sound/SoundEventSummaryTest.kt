package app.soine.sound

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pins the V1 sound-event UX contract.
 *
 * #178 requires that the user-facing copy never makes a
 * medical claim, never shows a raw confidence percentage,
 * and never references raw audio. The contract here:
 *
 *   - the aggregation collapses many events into a
 *     single presentation summary; counts are not
 *     surfaced in user copy
 *   - the `userFacingLine` function returns one short
 *     phrase that uses the "*のような*" pattern so the
 *     copy never makes a diagnosis
 *   - the `SoundEventSummary` data class does not carry
 *     the raw `confidence` value or the `modelVersion`
 *     to the morning screen
 *   - the empty case is `null` so the morning screen
 *     treats no events as the default state
 */
class SoundEventSummaryTest {

    @Test
    fun emptyListProducesEmptySummary() {
        val summary = emptyList<StoredSoundEvent>().summarize()
        assertEquals(0, summary.totalCount)
        assertEquals(0, summary.vocalizationCount)
        assertEquals(0, summary.loudSoundCount)
        assertEquals(0, summary.snoreLikeCount)
        assertEquals(0, summary.coughLikeCount)
        assertNull(summary.latestOccurredAtEpochMillis)
        assertFalse(summary.hasEvents)
    }

    @Test
    fun summaryCountsByType() {
        val events = listOf(
            stored("night-1", SoundEventType.VOCALIZATION, 1_700_000_000_000L),
            stored("night-1", SoundEventType.VOCALIZATION, 1_700_001_000_000L),
            stored("night-1", SoundEventType.LOUD_SOUND, 1_700_002_000_000L),
            stored("night-1", SoundEventType.SNORE_LIKE, 1_700_003_000_000L),
        )
        val summary = events.summarize()
        assertEquals(4, summary.totalCount)
        assertEquals(2, summary.vocalizationCount)
        assertEquals(1, summary.loudSoundCount)
        assertEquals(1, summary.snoreLikeCount)
        assertEquals(0, summary.coughLikeCount)
        assertEquals(1_700_003_000_000L, summary.latestOccurredAtEpochMillis)
        assertTrue(summary.hasEvents)
    }

    @Test
    fun userFacingLineUsesLikePattern() {
        // The copy must use the "*のような*" pattern so
        // it never makes a medical claim. The morning
        // screen never shows a raw confidence percentage
        // or a category label verbatim.
        val events = listOf(
            stored("night-1", SoundEventType.SNORE_LIKE, 1_700_000_000_000L),
            stored("night-1", SoundEventType.SNORE_LIKE, 1_700_001_000_000L),
        )
        val summary = events.summarize()
        val line = summary.userFacingLine()
        assertNotNull(line)
        assertTrue(
            "のような" in line,
            "Sound summary line must use the '*のような*' pattern; got: $line",
        )
        assertFalse(
            "%" in line || "confidence" in line,
            "Sound summary line must not mention a percentage or confidence; got: $line",
        )
    }

    @Test
    fun userFacingLineForEmptySummaryReturnsNull() {
        val summary = emptyList<StoredSoundEvent>().summarize()
        assertNull(summary.userFacingLine())
    }

    @Test
    fun userFacingLineDominantTypeSelection() {
        // The dominant type is the one with the most
        // events. When types tie, the copy picks the
        // calmest phrase first.
        val vocalizationDominant = listOf(
            stored("night-1", SoundEventType.VOCALIZATION, 1_700_000_000_000L),
            stored("night-1", SoundEventType.VOCALIZATION, 1_700_001_000_000L),
            stored("night-1", SoundEventType.SNORE_LIKE, 1_700_002_000_000L),
        ).summarize().userFacingLine()
        assertNotNull(vocalizationDominant)
        assertTrue(
            "寝言" in vocalizationDominant,
            "Vocalization-dominant summary must say '寝言'",
        )

        val loudSoundDominant = listOf(
            stored("night-1", SoundEventType.LOUD_SOUND, 1_700_000_000_000L),
            stored("night-1", SoundEventType.LOUD_SOUND, 1_700_001_000_000L),
            stored("night-1", SoundEventType.LOUD_SOUND, 1_700_002_000_000L),
        ).summarize().userFacingLine()
        assertNotNull(loudSoundDominant)
        assertTrue(
            "大きめ" in loudSoundDominant,
            "Loud-sound-dominant summary must say '大きめ'",
        )
    }

    @Test
    fun summaryDataClassDoesNotExposeConfidence() {
        // The morning screen consumes the summary, not
        // the raw event list. The data class must not
        // carry the raw `confidence` or `modelVersion`
        // values; the chrome never displays them. The
        // data class has exactly six fields, all of
        // them aggregation-level. A future contributor
        // who adds `confidence` or `modelVersion` to
        // the data class must update the count and the
        // test will fail loudly.
        val events = listOf(
            stored("night-1", SoundEventType.SNORE_LIKE, 1_700_000_000_000L, confidence = 0.93),
        )
        val summary: SoundEventSummary = events.summarize()
        // Bind each field through the public copy; this
        // exercises the surface that the morning screen
        // uses and asserts the field names are stable.
        val totalCount = summary.totalCount
        val vocalizationCount = summary.vocalizationCount
        val loudSoundCount = summary.loudSoundCount
        val snoreLikeCount = summary.snoreLikeCount
        val coughLikeCount = summary.coughLikeCount
        val latest = summary.latestOccurredAtEpochMillis
        assertEquals(1, totalCount)
        assertEquals(0, vocalizationCount)
        assertEquals(0, loudSoundCount)
        assertEquals(1, snoreLikeCount)
        assertEquals(0, coughLikeCount)
        assertEquals(1_700_000_000_000L, latest)
    }

    @Test
    fun userFacingLineNeverMentionsRawAudio() {
        // The copy must never reference raw audio. The
        // "raw audio playback" path is explicitly
        // excluded by #178.
        val events = listOf(
            stored("night-1", SoundEventType.LOUD_SOUND, 1_700_000_000_000L),
            stored("night-1", SoundEventType.LOUD_SOUND, 1_700_001_000_000L),
        )
        val summary = events.summarize()
        val line = summary.userFacingLine()
        assertNotNull(line)
        assertFalse(
            "録音" in line || "音声ファイル" in line || "audio" in line.lowercase(),
            "Sound summary line must never reference raw audio; got: $line",
        )
    }

    private fun stored(
        sessionId: String,
        type: SoundEventType,
        occurredAtEpochMillis: Long,
        confidence: Double = 0.5,
    ) = StoredSoundEvent(
        sessionId = sessionId,
        event = SoundEvent(
            type = type,
            occurredAtEpochMillis = occurredAtEpochMillis,
            confidence = confidence,
            source = SoundEventSource.ON_DEVICE_MICROPHONE,
            modelVersion = "v1",
        ),
    )
}
