package app.soine

import app.soine.accessibility.AccessibilityPolicy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Smoke test for the V1 sleeping screen layout contract.
 *
 * #170 requires:
 *
 *   - quietUi 時に不要な文字が常時表示されない — the secondary
 *     controls auto-hide and the top status row is the only
 *     chrome left on screen
 *   - 片手で audio / timer / wake へ到達可能 — the audio
 *     toggle, the timer button, and the wake CTA are all
 *     reachable with one hand
 *   - screen-off 前提でも session state を壊さない — the wake
 *     CTA is always anchored, so a screen-off / wake-by-button
 *     does not lose the session
 *   - 3D failure 時も同じ visual hierarchy を維持 — the scene
 *     uses CompanionSceneContent which is renderer-independent
 *
 * Compose UI rendering is still deferred (see
 * `docs/ci-quality-policy.md`); the textual / source checks here
 * cover the contract.
 */
class SleepingScreenLayoutTest {

    @Test
    fun sleepingScreenUsesDesignSystem() {
        val source = loadSleepingSource()
        assertTrue(
            source.contains("SoinePrimaryButton"),
            "SleepingScreen must use SoinePrimaryButton for the wake CTA",
        )
        assertTrue(
            source.contains("SoineQuietButton"),
            "SleepingScreen must use SoineQuietButton for the audio / timer / settings secondary actions",
        )
    }

    @Test
    fun sleepingScreenReusesCompanionSceneContent() {
        val source = loadSleepingSource()
        // The hero companion must reuse the shared content so a
        // future 3D renderer swap is a one-line change.
        assertTrue(
            source.contains("CompanionSceneContent"),
            "SleepingScreen must call CompanionSceneContent for the hero scene",
        )
    }

    @Test
    fun sleepingScreenWakeCtaIsAnchored() {
        val source = loadSleepingSource()
        // The wake CTA stays reachable in one tap regardless of
        // the quiet / revealed state. The test pins that the
        // SoinePrimaryButton is wired to AccessibilityPolicy.WAKE_CONTENT_DESCRIPTION
        // so the screen-reader contract is preserved.
        assertTrue(
            source.contains("AccessibilityPolicy.WAKE_CONTENT_DESCRIPTION"),
            "SleepingScreen must wire the wake CTA to AccessibilityPolicy.WAKE_CONTENT_DESCRIPTION",
        )
    }

    @Test
    fun sleepingScreenAutoHideConstantIsStable() {
        val source = loadSleepingSource()
        // The auto-hide duration is a public constant. The test
        // pins the value so a future tweak does not silently
        // shorten or lengthen the bedside scene's quiet window.
        assertTrue(
            source.contains("const val AUTO_HIDE_AFTER_MILLIS: Long = 6_000L"),
            "SleepingScreen must keep AUTO_HIDE_AFTER_MILLIS at 6 seconds",
        )
    }

    @Test
    fun sleepingScreenTapToRevealIsExposed() {
        val source = loadSleepingSource()
        // The scene's tap-to-reveal interaction must be exposed
        // through AccessibilityPolicy so TalkBack / VoiceOver
        // announce the action.
        assertTrue(
            source.contains("AccessibilityPolicy.SLEEPING_SCENE_REVEAL_CONTENT_DESCRIPTION"),
            "SleepingScreen must reference SLEEPING_SCENE_REVEAL_CONTENT_DESCRIPTION",
        )
    }

    @Test
    fun sleepingScreenSecondaryActionsExposeContentDescriptions() {
        val source = loadSleepingSource()
        // The audio / timer / settings quiet buttons must each
        // carry an AccessibilityPolicy content description.
        for (constant in listOf(
            "SLEEPING_AUDIO_TOGGLE_CONTENT_DESCRIPTION",
            "SLEEPING_TIMER_CONTENT_DESCRIPTION",
            "SLEEPING_TIMER_CANCEL_CONTENT_DESCRIPTION",
            "SLEEPING_SETTINGS_CONTENT_DESCRIPTION",
        )) {
            assertTrue(
                source.contains(constant),
                "SleepingScreen must reference $constant",
            )
        }
    }

    @Test
    fun sleepingScreenDefaultAutoHideIsSixSeconds() {
        // The default argument pins the bedside scene's quiet
        // window at 6 seconds so a future refactor cannot silently
        // change the user experience.
        assertEquals(6_000L, AUTO_HIDE_AFTER_MILLIS)
    }

    @Test
    fun sleepingScreenDefaultQuietUiIsFalse() {
        // The first render must show the controls so the user
        // can see the secondary actions; the auto-hide kicks in
        // after the first reveal.
        val source = loadSleepingSource()
        assertTrue(
            source.contains("var controlsVisible by remember { mutableStateOf(!quietUi) }"),
            "SleepingScreen must default controlsVisible to !quietUi",
        )
    }

    private fun loadSleepingSource(): String {
        val candidates = listOf(
            "composeApp/src/commonMain/kotlin/app/soine/SleepingScreen.kt",
            "../composeApp/src/commonMain/kotlin/app/soine/SleepingScreen.kt",
            "src/commonMain/kotlin/app/soine/SleepingScreen.kt",
            "../src/commonMain/kotlin/app/soine/SleepingScreen.kt",
        )
        for (path in candidates) {
            val file = java.io.File(path)
            if (file.exists()) return file.readText(Charsets.UTF_8)
        }
        error(
            "SleepingScreen.kt not found in any of the candidate paths; tried: " +
                candidates.joinToString(),
        )
    }
}
