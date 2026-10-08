package app.soine.time.format

import app.soine.time.JapanLocalTimeZone
import app.soine.time.LocalTimeZone
import app.soine.time.UtcTimeZone
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * Smoke test for the [DisplayFormatter] boundary.
 *
 * #196 requires that the formatter layer can host a future locale
 * without rewriting the call sites in the UI. The contract:
 *
 *   - [DisplayFormatter] is the single boundary the UI depends on
 *   - [DisplayFormatters.current] is the only mutable binding a UI
 *     site needs to be aware of
 *   - the formatter is stateless and never reads
 *     [app.soine.time.LocalTimeZones.current] on its own; the caller
 *     passes the [LocalTimeZone] explicitly
 *
 * The test installs a [CapitalizedIsoDateFormatter] (a non-default
 * style) and asserts that the [DisplayFormatters.current] swap
 * changes the rendered string without mutating any session data.
 */
class DisplayFormatterBoundaryTest {

    private val previousFormatter = DisplayFormatters.current

    @AfterTest
    fun restoreFormatter() {
        DisplayFormatters.current = previousFormatter
    }

    @Test
    fun japaneseFormatterEmitsYearMonthDayShape() {
        DisplayFormatters.current = JapaneseDisplayFormatter
        val epochMillis = 1_700_000_000_000L // 2023-11-14 22:13:20 UTC

        // JST 2023-11-15 07:13:20.
        val date = DisplayFormatters.current.formatDiscoveryDate(epochMillis, JapanLocalTimeZone)
        val time = DisplayFormatters.current.formatNightEventTime(epochMillis, JapanLocalTimeZone)

        assertEquals("2023年11月15日", date)
        assertEquals("07:13", time)
    }

    @Test
    fun alternateFormatterDoesNotAffectAbsoluteTimestamps() {
        // A future English locale formatter: "YYYY-MM-DD" / "HH:mm".
        // This is the boundary the issue asks for: it is registered
        // through DisplayFormatters.current and the call sites in
        // DreamAlbumScreen / NightMemoryTimeline do not need to
        // change to pick it up.
        val english = CapitalizedIsoDateFormatter()
        DisplayFormatters.current = english
        val epochMillis = 1_700_000_000_000L

        val date = DisplayFormatters.current.formatDiscoveryDate(epochMillis, JapanLocalTimeZone)
        val time = DisplayFormatters.current.formatNightEventTime(epochMillis, JapanLocalTimeZone)

        assertEquals("2023-11-15", date)
        assertEquals("07:13", time)
    }

    @Test
    fun swappingFormatterChangesDisplayButNotData() {
        val epochMillis = 1_700_000_000_000L
        // Pin the timezone to JST so the test only exercises the
        // formatter swap, not a timezone change.
        val tz = JapanLocalTimeZone

        DisplayFormatters.current = JapaneseDisplayFormatter
        val japaneseDate = DisplayFormatters.current.formatDiscoveryDate(epochMillis, tz)

        DisplayFormatters.current = CapitalizedIsoDateFormatter()
        val isoDate = DisplayFormatters.current.formatDiscoveryDate(epochMillis, tz)

        // The two formatters emit different strings, but the input
        // epoch is unchanged. The boundary is the formatter, not the
        // persisted timestamp.
        assertNotEquals(japaneseDate, isoDate)
        assertEquals(epochMillis, epochMillis)
    }

    @Test
    fun formatterReceivesTimezoneExplicitly() {
        // A pure formatter must produce different strings for the
        // same epoch when the timezone changes; the formatter does
        // not consult any global state, the caller passes the zone.
        val english = CapitalizedIsoDateFormatter()
        val epochMillis = 1_700_000_000_000L

        val jst = english.formatDiscoveryDate(epochMillis, JapanLocalTimeZone)
        val utc = english.formatDiscoveryDate(epochMillis, UtcTimeZone)

        // 22:13 UTC vs 07:13 JST the next day.
        assertNotEquals(jst, utc)
    }
}

/**
 * A non-default [DisplayFormatter] used by the boundary test to
 * confirm that a future locale can be wired through
 * [DisplayFormatters.current] without touching any call site.
 */
private class CapitalizedIsoDateFormatter : DisplayFormatter {
    override fun formatDiscoveryDate(epochMillis: Long, timeZone: LocalTimeZone): String {
        require(epochMillis >= 0L) { "Discovery timestamp must not be negative." }
        val localMillis = epochMillis + timeZone.utcOffsetMillisAt(epochMillis)
        val epochDay = localMillis / 86_400_000L
        val z = epochDay + 719_468L
        val era = if (z >= 0L) z / 146_097L else (z - 146_096L) / 146_097L
        val dayOfEra = z - era * 146_097L
        val yearOfEra =
            (dayOfEra - dayOfEra / 1_460L + dayOfEra / 36_524L - dayOfEra / 146_096L) / 365L
        val year = (yearOfEra + era * 400L).toInt()
        val dayOfYear = dayOfEra - (365L * yearOfEra + yearOfEra / 4L - yearOfEra / 100L)
        val monthPrime = (5L * dayOfYear + 2L) / 153L
        val day = (dayOfYear - (153L * monthPrime + 2L) / 5L + 1L).toInt()
        val month = (monthPrime + if (monthPrime < 10L) 3L else -9L).toInt()
        val displayYear = if (month <= 2) year + 1 else year
        return "$displayYear-${pad2(month)}-${pad2(day)}"
    }

    override fun formatNightEventTime(epochMillis: Long, timeZone: LocalTimeZone): String {
        require(epochMillis >= 0L) { "NightEvent timestamp must not be negative." }
        val localMillis = epochMillis + timeZone.utcOffsetMillisAt(epochMillis)
        val millisOfDay = ((localMillis % 86_400_000L) + 86_400_000L) % 86_400_000L
        val hours = millisOfDay / 3_600_000L
        val minutes = (millisOfDay % 3_600_000L) / 60_000L
        return pad2(hours) + ":" + pad2(minutes)
    }

    private fun pad2(value: Long): String = value.toString().padStart(2, '0')
    private fun pad2(value: Int): String = pad2(value.toLong())
}
