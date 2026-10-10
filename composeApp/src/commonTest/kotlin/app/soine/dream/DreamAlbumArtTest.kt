package app.soine.dream

import app.soine.design.SoineColors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Pins the V1 Dream Album visual identity contract.
 *
 * The album is a quiet collection, not a row of identical "夢"
 * cards. Each [DreamDefinition.artKey] is mapped deterministically
 * to a [DreamMotif] and a [DreamPalette] so a list of dreams
 * reads as a constellation. The mapping must be:
 *
 *  - deterministic: the same artKey always maps to the same
 *    (motif, palette) tuple
 *  - diverse: across the authored catalog, multiple motifs and
 *    multiple palettes are used so a list of dreams has visual
 *    variety
 *  - safe on null / unknown: a missing artKey falls back to a
 *    neutral identity that does not break the layout
 */
class DreamAlbumArtTest {

    @Test
    fun visualIdentityForSameArtKeyIsStable() {
        val first = visualIdentityFor("dream_moon_window")
        val second = visualIdentityFor("dream_moon_window")
        assertEquals(first, second)
    }

    @Test
    fun visualIdentityForNullArtKeyFallsBackToNeutralOrb() {
        val identity = visualIdentityFor(null)
        assertEquals(DreamMotif.ORB, identity.motif)
        assertSame(DreamPalette.Neutral, identity.palette)
    }

    @Test
    fun visualIdentityForBlankArtKeyFallsBackToNeutralOrb() {
        val identity = visualIdentityFor("   ")
        assertEquals(DreamMotif.ORB, identity.motif)
        assertSame(DreamPalette.Neutral, identity.palette)
    }

    @Test
    fun differentArtKeysGetDifferentVisualIdentities() {
        // A handful of dreams with distinct English keys; the
        // deterministic hash should give each a unique (motif,
        // palette) pair. (Two different keys may share one
        // element by coincidence; the test allows that, but a
        // majority of pairs must differ.)
        val identities = listOf(
            "dream_cloud_nap",
            "dream_moon_window",
            "dream_star_pillow",
            "dream_quiet_library",
            "dream_tiny_train",
            "dream_lantern_path",
            "dream_paper_boat",
            "dream_soft_hill",
            "dream_warm_bakery",
            "dream_glass_bubbles",
        ).map { visualIdentityFor(it) }

        val distinctPairs = identities.toSet()
        // At least 8 of 10 dreams must have a unique (motif,
        // palette) pair. The catalog has 8 motifs and 6
        // palettes, so 48 combinations are available; we expect
        // strong visual variety, not 10 / 10 in practice but
        // close to it.
        assertTrue(
            distinctPairs.size >= 8,
            "Expected at least 8 distinct visual identities across 10 art keys, got ${distinctPairs.size}",
        )
    }

    @Test
    fun initialCatalogUsesMoreThanOneMotifAndOnePalette() {
        val catalog = InitialDreamCatalog.definitions.mapNotNull { it.artKey }
        val motifs = catalog.map { motifFor(it) }.toSet()
        val palettes = catalog.map { paletteFor(it) }.toSet()
        // Across 30 dreams we expect at least half the motif
        // and palette sets to be exercised. The exact number
        // depends on the hash distribution; this is a "do not
        // regress to a single visual" guard.
        assertTrue(
            motifs.size >= 4,
            "Initial catalog should use at least 4 different motifs, got ${motifs.size}",
        )
        assertTrue(
            palettes.size >= 4,
            "Initial catalog should use at least 4 different palettes, got ${palettes.size}",
        )
    }

    @Test
    fun palettesAreComposedFromSoineColorsOnly() {
        // The visual identity must not introduce a new color
        // outside the Soine palette. Every palette gradient and
        // accent must come from one of the Soine tokens.
        val soineTokens = setOf(
            SoineColors.cream,
            SoineColors.midnight,
            SoineColors.dusk,
            SoineColors.twilight,
            SoineColors.dim,
            SoineColors.glow,
            SoineColors.ember,
            SoineColors.hush,
            SoineColors.sunrise,
            SoineColors.soft,
        )
        for (palette in DREAM_PALETTES) {
            assertTrue(
                palette.start in soineTokens,
                "Palette start ${palette.start} is not part of the Soine palette",
            )
            assertTrue(
                palette.end in soineTokens,
                "Palette end ${palette.end} is not part of the Soine palette",
            )
            assertTrue(
                palette.accent in soineTokens,
                "Palette accent ${palette.accent} is not part of the Soine palette",
            )
        }
    }

    @Test
    fun motifForAndPaletteForAreDeterministic() {
        val motifA = motifFor("dream_constellation_map")
        val motifB = motifFor("dream_constellation_map")
        assertEquals(motifA, motifB)
        val paletteA = paletteFor("dream_constellation_map")
        val paletteB = paletteFor("dream_constellation_map")
        assertEquals(paletteA, paletteB)
    }

    @Test
    fun neutralPaletteIsDistinctFromAuthoredPalettes() {
        // The fallback palette must not collide with any of the
        // authored palettes so a missing art key does not get
        // the same visual as a real dream.
        for (palette in DREAM_PALETTES) {
            assertNotEquals(DreamPalette.Neutral, palette)
        }
    }
}
