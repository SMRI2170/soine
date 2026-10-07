package app.soine.time

import app.soine.formatJapaneseDiscoveryDate
import app.soine.formatNightEventTime
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class LocalTimeZoneTest {

    private val previousBinding = LocalTimeZones.current

    @AfterTest
    fun restoreBinding() {
        LocalTimeZones.current = previousBinding
    }

    @Test
    fun japanTimeZoneReturnsFixedUtcPlusNineOffset() {
        val epochMillis = 1_700_000_000_000L

        assertEquals(9 * 60 * 60 * 1_000, JapanLocalTimeZone.utcOffsetMillisAt(epochMillis))
    }

    @Test
    fun utcTimeZoneReturnsZeroOffset() {
        val epochMillis = 1_700_000_000_000L

        assertEquals(0, UtcTimeZone.utcOffsetMillisAt(epochMillis))
    }

    @Test
    fun displayFollowsCurrentBinding() {
        val epochMillis = 1_700_000_000_000L

        LocalTimeZones.current = JapanLocalTimeZone
        val jstLabel = formatNightEventTime(epochMillis)

        LocalTimeZones.current = UtcTimeZone
        val utcLabel = formatNightEventTime(epochMillis)

        assertEquals(jstLabel, formatNightEventTime(epochMillis, JapanLocalTimeZone))
        assertEquals(utcLabel, formatNightEventTime(epochMillis, UtcTimeZone))
    }

    @Test
    fun midnightCrossingDisplaysDifferentTimesForTheSameSession() {
        // A session that starts at 23:50 local (JST) and ends at 00:30 local
        // should still render 23:50 / 00:30 in the user's local wall clock.
        // The absolute timestamps are stored as epoch millis; the renderer
        // applies the current timezone offset to produce the labels.
        LocalTimeZones.current = JapanLocalTimeZone
        val startLocal = 1_700_000_000_000L // arbitrary epoch; check label formatting
        val endLocal = startLocal + 40 * 60 * 1_000L

        val startLabel = formatNightEventTime(startLocal)
        val endLabel = formatNightEventTime(endLocal)

        assertEquals("00:00", startLabel)
        assertEquals("00:40", endLabel)
    }

    @Test
    fun travelChangesDisplayEvenWhenAbsoluteTimestampsAreUnchanged() {
        val epochMillis = 1_700_000_000_000L

        LocalTimeZones.current = JapanLocalTimeZone
        val jstLabel = formatNightEventTime(epochMillis)

        LocalTimeZones.current = UtcTimeZone
        val utcLabel = formatNightEventTime(epochMillis)

        // The two labels must differ; the absolute timestamp was never
        // modified by the timezone change.
        assertEquals(false, jstLabel == utcLabel)
    }

    @Test
    fun discoveryDateFollowsCurrentBinding() {
        val epochMillis = 1_700_000_000_000L

        LocalTimeZones.current = JapanLocalTimeZone
        val jstDate = formatJapaneseDiscoveryDate(epochMillis)

        LocalTimeZones.current = UtcTimeZone
        val utcDate = formatJapaneseDiscoveryDate(epochMillis)

        assertEquals(jstDate, formatJapaneseDiscoveryDate(epochMillis, JapanLocalTimeZone))
        assertEquals(utcDate, formatJapaneseDiscoveryDate(epochMillis, UtcTimeZone))
    }
}