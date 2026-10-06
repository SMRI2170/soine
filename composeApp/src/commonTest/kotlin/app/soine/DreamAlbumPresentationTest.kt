package app.soine

import app.soine.dream.DreamDefinition
import app.soine.dream.DreamDiscovery
import app.soine.night.RarityBand
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DreamAlbumPresentationTest {
    @Test
    fun discoveredDreamsAreShownFirstAndUndiscoveredCopyStaysHidden() {
        val common = dream("common", "雲の夢", RarityBand.COMMON)
        val rare = dream("rare", "星の夢", RarityBand.RARE)

        val entries = buildDreamAlbumEntries(
            definitions = listOf(common, rare),
            discoveries = listOf(
                DreamDiscovery(
                    dreamId = rare.id,
                    sessionId = "night-1",
                    discoveredAtEpochMillis = 1_791_300_600_000L,
                ),
            ),
        )

        assertEquals("rare", entries.first().id)
        assertTrue(entries.first().discovered)
        assertEquals("星の夢", entries.first().title)
        assertEquals("めずらしい夢", entries.first().rarityLabel)
        assertEquals("2026年10月7日", entries.first().discoveredDateLabel)

        val hidden = entries.last()
        assertFalse(hidden.discovered)
        assertEquals("？？？", hidden.title)
        assertEquals("まだ見つけていない夢", hidden.shortLine)
        assertNull(hidden.rarityLabel)
        assertNull(hidden.discoveredDateLabel)
    }

    @Test
    fun duplicateDiscoveryKeepsTheEarliestDiscoveryDate() {
        val definition = dream("moon", "月の夢", RarityBand.UNCOMMON)

        val entry = buildDreamAlbumEntries(
            definitions = listOf(definition),
            discoveries = listOf(
                DreamDiscovery("moon", "night-2", 1_791_387_000_000L),
                DreamDiscovery("moon", "night-1", 1_791_300_600_000L),
            ),
        ).single()

        assertEquals(1_791_300_600_000L, entry.discoveredAtEpochMillis)
        assertEquals("2026年10月7日", entry.discoveredDateLabel)
    }

    @Test
    fun japaneseDiscoveryDateUsesJapanCalendarDayAcrossUtcBoundary() {
        assertEquals(
            "2026年10月7日",
            formatJapaneseDiscoveryDate(1_791_300_600_000L),
        )
    }

    private fun dream(
        id: String,
        title: String,
        rarity: RarityBand,
    ) = DreamDefinition(
        id = id,
        title = title,
        shortLine = title + "を見たみたい",
        rarity = rarity,
        artKey = "dream_" + id,
    )
}
