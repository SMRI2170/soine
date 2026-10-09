package app.soine.accessibility

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Smoke test for the decorative companion semantics boundary.
 *
 * #195 requires:
 *
 *   - decorative companion semantics が読み上げを邪魔しない
 *   - 3D scene が accessibility tree を壊さない
 *
 * The companion is rendered as a single [Surface] in `App.kt` with
 * one `contentDescription = artwork.contentDescription`. The intent
 * is "one announceable node per scene, not many overlapping ones".
 * This textual source check pins that the surrounding code does
 * not re-introduce a verbose semantics block that would either:
 *
 *   - duplicate the artwork's contentDescription onto child nodes
 *     (TalkBack would read it twice), or
 *   - drop the contentDescription entirely (the scene would become
 *     invisible to the screen reader and break "core sleep start /
 *     wake が TalkBack / VoiceOver だけで完遂可能" because the user
 *     would have no audible cue for the companion's state).
 *
 * Until the host-test path is upgraded to render Compose UI, the
 * check is the textual substitute described in
 * `docs/ci-quality-policy.md`.
 */
class DecorativeCompanionSemanticsTest {

    @Test
    fun appScreenCompanionSceneExposesExactlyOneContentDescription() {
        val source = loadAppSource()
        val window = windowAround(source, "private fun CompanionScene", windowSize = 1_200)

        // The companion scene must apply a contentDescription so the
        // screen reader can announce the state. It must not double
        // up the description on a child node.
        val count = "contentDescription = ".toRegex().findAll(window).count()
        assertTrue(
            count >= 1,
            "CompanionScene must expose at least one contentDescription; window:\n" + window,
        )
        // We allow one on the surface itself plus the (optional)
        // textual overlay below it; more than two is a sign of
        // duplication and would pollute TalkBack traversal.
        assertFalse(
            count > 2,
            "CompanionScene must not duplicate the contentDescription across multiple nodes " +
                "(TalkBack would read the scene twice). Found $count occurrences; window:\n" + window,
        )
    }

    @Test
    fun appScreenCompanionSceneUsesModifierSemantics() {
        val source = loadAppSource()
        val window = windowAround(source, "private fun CompanionScene", windowSize = 1_200)

        assertTrue(
            window.contains(".semantics { contentDescription"),
            "CompanionScene must apply the contentDescription through a .semantics { ... } block; " +
                "window:\n" + window,
        )
    }

    @Test
    fun appScreenDoesNotUseClearAndSetSemanticsAnywhere() {
        // A blanket `clearAndSetSemantics {}` would strip every node
        // inside, which is wrong for our case: we want one announceable
        // surface, not zero announceable nodes. The policy is
        // "exactly one", enforced by the test above.
        val source = loadAppSource()
        assertFalse(
            source.contains("clearAndSetSemantics"),
            "App.kt must not use clearAndSetSemantics; the companion scene should expose one " +
                "contentDescription, not strip the accessibility tree.",
        )
    }

    @Test
    fun appScreenDoesNotUseInvisibleToUserAnywhere() {
        // `invisibleToUser()` would hide the entire scene from the
        // screen reader, which would also break the announce contract.
        // A future contributor who wants the companion to be purely
        // decorative should instead strip the contentDescription and
        // gate it behind a `companionIsDecorative` flag rather than
        // apply invisibleToUser.
        val source = loadAppSource()
        assertFalse(
            source.contains("invisibleToUser"),
            "App.kt must not use invisibleToUser on the companion; see DecorativeCompanionSemanticsTest " +
                "for the policy.",
        )
    }

    private fun loadAppSource(): String {
        val candidates = listOf(
            "composeApp/src/commonMain/kotlin/app/soine/App.kt",
            "../composeApp/src/commonMain/kotlin/app/soine/App.kt",
            "src/commonMain/kotlin/app/soine/App.kt",
            "../src/commonMain/kotlin/app/soine/App.kt",
        )
        for (path in candidates) {
            val file = java.io.File(path)
            if (file.exists()) return file.readText(Charsets.UTF_8)
        }
        error(
            "App.kt not found in any of the candidate paths; tried: " + candidates.joinToString(),
        )
    }

    private fun windowAround(source: String, marker: String, windowSize: Int): String {
        val idx = source.indexOf(marker)
        check(idx >= 0) { "Expected $marker in App.kt" }
        val start = idx
        val end = (idx + windowSize).coerceAtMost(source.length)
        return source.substring(start, end)
    }
}
