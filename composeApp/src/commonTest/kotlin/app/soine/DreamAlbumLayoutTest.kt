package app.soine

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Smoke test for the V1 Dream Album layout contract.
 *
 * #172 requires:
 *
 *   - 一覧だけで複数の夢が視覚的に区別できる — every cell
 *     uses the per-dream [app.soine.dream.DreamAlbumArtTile]
 *     (gradient + motif) instead of a single "夢" glyph
 *   - 「夢」という同じglyphの反復を廃止 — no `Text("夢", ...)`
 *     placeholder for the discovered state
 *   - 未発見状態にも世界観がある — the undiscovered state
 *     uses the same art tile in silhouette form, not a "？"
 *     placeholder
 *   - login bonus / loot-box 的な pressure を出さない — no
 *     progress bar, no streak counter, no "login" copy
 *   - detail は dialog ではなく immersive sheet / page — the
 *     detail surface is a `ModalBottomSheet`
 *
 * Compose UI rendering is still deferred (see
 * `docs/ci-quality-policy.md`); the textual / source checks
 * here cover the contract.
 */
class DreamAlbumLayoutTest {

    @Test
    fun dreamAlbumScreenUsesDesignSystem() {
        val source = loadDreamAlbumSource()
        assertTrue(
            source.contains("SoineQuietButton"),
            "DreamAlbumScreen must use SoineQuietButton for the back action",
        )
        assertTrue(
            source.contains("SoinePanel"),
            "DreamAlbumScreen must use SoinePanel for the empty state and detail sections",
        )
        assertTrue(
            source.contains("SoineSectionHeader"),
            "DreamAlbumScreen must use SoineSectionHeader for sub-section labels",
        )
    }

    @Test
    fun dreamAlbumScreenRendersArtTile() {
        val source = loadDreamAlbumSource()
        assertTrue(
            source.contains("DreamAlbumArtTile"),
            "DreamAlbumScreen must render DreamAlbumArtTile (per-dream visual identity)",
        )
    }

    @Test
    fun dreamAlbumScreenUsesGridLayout() {
        val source = loadDreamAlbumSource()
        assertTrue(
            source.contains("LazyVerticalGrid"),
            "DreamAlbumScreen must use LazyVerticalGrid so the album reads as a constellation / scrapbook, not a flat list",
        )
        assertTrue(
            source.contains("GridCells.Fixed(2)"),
            "DreamAlbumScreen must use a 2-column grid so the art tile and title fit a single screen at default text scale",
        )
    }

    @Test
    fun dreamAlbumScreenDetailIsImmersiveSheetNotDialog() {
        val source = loadDreamAlbumSource()
        assertTrue(
            source.contains("ModalBottomSheet"),
            "DreamAlbumScreen must surface the dream detail as a ModalBottomSheet (immersive sheet / page), not an AlertDialog",
        )
        assertTrue(
            !source.contains("AlertDialog"),
            "DreamAlbumScreen must not use AlertDialog for the dream detail; the sheet is the immersive page",
        )
    }

    @Test
    fun dreamAlbumScreenDoesNotRepeatTheSameDreamGlyph() {
        val source = loadDreamAlbumSource()
        // The previous V0 rendered every discovered dream with
        // a `Text("夢")` glyph. The redesign pins the contract
        // by forbidding that exact string in the visual layer.
        // (The Japanese 文字 "夢" is still allowed in copy like
        // the screen title "夢のアルバム"; the test searches for
        // the explicit glyph-as-placeholder pattern.)
        assertTrue(
            !source.contains("Text(\n                        if (entry.discovered) \"夢\""),
            "DreamAlbumScreen must not render the '夢' glyph as a placeholder for the discovered cell",
        )
    }

    @Test
    fun dreamAlbumScreenUndiscoveredStateUsesArtTileSilhouette() {
        val source = loadDreamAlbumScreenSource()
        // The undiscovered state must flow through the art tile
        // with `discovered = false` so the silhouette is the
        // same shape, just dimmer. The test guards the call
        // site.
        assertTrue(
            source.contains("discovered = entry.discovered") || source.contains("discovered = discovered"),
            "DreamAlbumCell must pass `discovered = entry.discovered` into DreamAlbumArtTile so the silhouette is preserved",
        )
    }

    @Test
    fun dreamAlbumScreenMergesDescendantSemantics() {
        val source = loadDreamAlbumSource()
        assertTrue(
            source.contains("semantics(mergeDescendants = true)"),
            "DreamAlbumScreen cells must merge their child semantics so TalkBack reads one concise description per dream, not glyph + title + line + date",
        )
    }

    @Test
    fun dreamAlbumScreenHasNoProgressBarOrStreakCounter() {
        val source = loadDreamAlbumSource()
        // Loot-box pressure guard: no LinearProgressIndicator /
        // progress bar / "streak" copy / "login" copy. The
        // header is the only count and reads as memory, not
        // as a goal. We strip the file's comment lines so a
        // mention in a doc comment does not trip the guard.
        val nonCommentSource = source
            .lineSequence()
            .filter { line ->
                val trimmed = line.trimStart()
                !(trimmed.startsWith("*") || trimmed.startsWith("//"))
            }
            .joinToString("\n")
        assertTrue(
            !nonCommentSource.contains("LinearProgressIndicator"),
            "DreamAlbumScreen must not use LinearProgressIndicator; the album is a memory surface, not a progress bar",
        )
        assertTrue(
            !nonCommentSource.contains("progressBar") && !nonCommentSource.contains("ProgressBar"),
            "DreamAlbumScreen must not import a progress bar component",
        )
        assertTrue(
            !nonCommentSource.contains("Streak") && !nonCommentSource.contains("streak"),
            "DreamAlbumScreen must not introduce streak language in code or strings",
        )
        assertTrue(
            !nonCommentSource.contains("Login") && !nonCommentSource.contains("ログイン"),
            "DreamAlbumScreen must not introduce login / daily-bonus language in code or strings",
        )
    }

    @Test
    fun dreamAlbumScreenUsesSoftEmptyStateCopy() {
        val source = loadDreamAlbumSource()
        // The empty state copy must explicitly say "急いで集め
        // なくても大丈夫です" so the screen does not pressure
        // the user to collect. This is the loot-box-pressure
        // guard at the copy layer.
        assertTrue(
            source.contains("急いで集めなくても大丈夫"),
            "DreamAlbumScreen empty state must explicitly say the user does not need to rush collecting",
        )
    }

    @Test
    fun dreamAlbumEntryCarriesArtKey() {
        val source = loadDreamAlbumSource()
        // The data layer must carry the artKey so the cell can
        // resolve a visual identity. Without this field the
        // grid would fall back to the neutral palette for every
        // dream.
        assertTrue(
            source.contains("artKey: String?"),
            "DreamAlbumEntry must carry an artKey field so the cell can resolve a per-dream visual identity",
        )
    }

    private fun loadDreamAlbumSource(): String {
        return loadFile("composeApp/src/commonMain/kotlin/app/soine/DreamAlbumScreen.kt")
    }

    private fun loadDreamAlbumScreenSource(): String = loadDreamAlbumSource()

    private fun loadFile(relativePath: String): String {
        val candidates = listOf(
            relativePath,
            "../$relativePath",
            relativePath.removePrefix("composeApp/"),
            "../${relativePath.removePrefix("composeApp/")}",
        )
        for (path in candidates) {
            val file = java.io.File(path)
            if (file.exists()) return file.readText(Charsets.UTF_8)
        }
        error("Source file not found in any of the candidate paths: $relativePath")
    }
}
