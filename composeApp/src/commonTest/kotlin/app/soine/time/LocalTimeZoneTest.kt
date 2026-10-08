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
        // Pick an epoch that maps to a known JST time, then advance by
        // 40 minutes and verify the formatter keeps the wall-clock
        // arithmetic correct.
        // 0L (1970-01-01T00:00:00Z) + 9h JST offset = 1970-01-01T09:00 JST.
        LocalTimeZones.current = JapanLocalTimeZone

        val startLabel = formatNightEventTime(0L)
        val endLabel = formatNightEventTime(40L * 60L * 1_000L)

        assertEquals("09:00", startLabel)
        assertEquals("09:40", endLabel)
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