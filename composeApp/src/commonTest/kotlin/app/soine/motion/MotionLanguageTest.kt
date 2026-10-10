package app.soine.motion

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Source-audit smoke test for the V1 motion language.
 *
 * #177 requires the motion language to be consistent
 * across the bedtime, sleeping, and morning screens,
 * and haptics to fire only at meaningful moments. The
 * textual / source checks here pin the contract.
 *
 * Compose UI rendering is still deferred (see
 * `docs/ci-quality-policy.md`); the textual source
 * checks here cover the items that can be pinned by
 * automation.
 */
class MotionLanguageTest {

    @Test
    fun sleepingScreenUsesSoineAnimatedVisibility() {
        val source = loadScreen("SleepingScreen.kt")
        assertTrue(
            source.contains("SoineAnimatedVisibility"),
            "SleepingScreen must use SoineAnimatedVisibility so the fade timing / easing stay consistent with the motion tokens",
        )
        assertTrue(
            source.contains("SoineAnimatedVisibilityVertical"),
            "SleepingScreen must use SoineAnimatedVisibilityVertical for the secondary controls reveal / dismiss",
        )
        // The bare Material AnimatedVisibility is forbidden.
        // We strip the import line and any direct call site.
        // Use a regex-style boundary: the bare call must be
        // preceded by whitespace and not be a substring of
        // "SoineAnimatedVisibility" / "SoineAnimatedVisibilityVertical".
        val importLine = "import androidx.compose.animation.AnimatedVisibility"
        assertTrue(
            !source.contains(importLine),
            "SleepingScreen must not import the bare Material AnimatedVisibility; the Soine wrapper owns the motion language",
        )
        // Replace all "SoineAnimatedVisibility" / "SoineAnimatedVisibilityVertical"
        // with placeholders, then check the residue for a bare "AnimatedVisibility(".
        val residue = source
            .replace("SoineAnimatedVisibilityVertical", "")
            .replace("SoineAnimatedVisibility", "")
        assertTrue(
            !residue.contains("AnimatedVisibility("),
            "SleepingScreen must not call the bare Material AnimatedVisibility directly",
        )
    }

    @Test
    fun designSystemButtonsHavePressFeedback() {
        val source = loadSource(
            "composeApp/src/commonMain/kotlin/app/soine/design/SoineComponents.kt",
        )
        assertTrue(
            source.contains("animateFloatAsState"),
            "SoineComponents must animate the press scale through animateFloatAsState so the press feedback is a true state-driven animation",
        )
        assertTrue(
            source.contains("collectIsPressedAsState"),
            "SoineComponents must drive the press feedback from the InteractionSource so the press scale tracks the actual press state",
        )
        assertTrue(
            source.contains("DURATION_QUICK"),
            "SoineComponents must source the press feedback duration from MotionTokens.DURATION_QUICK so the press feedback stays consistent with the motion language",
        )
    }

    @Test
    fun soineAppFiresHapticOnSleepStart() {
        val source = loadScreen("SoineApp.kt")
        val startIdx = source.indexOf("onStartSleep")
        assertTrue(startIdx >= 0)
        // The haptic must fire inside the onStartSleep
        // callback, after the previous-relationship-stage
        // capture (which sits at the start of the
        // callback) and before / after the start of the
        // new session. The exact ordering is the team's
        // choice; the test only guards the presence.
        val startSlice = source.substring(
            startIdx,
            (startIdx + 4000).coerceAtMost(source.length),
        )
        assertTrue(
            startSlice.contains("haptics.perform(HapticsIntensity.Light)"),
            "SoineApp must fire a light haptic inside the onStartSleep callback so sleep-start is a meaningful moment",
        )
    }

    @Test
    fun soineAppFiresHapticOnWake() {
        val source = loadScreen("SoineApp.kt")
        val wakeIdx = source.indexOf("onWake = {")
        assertTrue(wakeIdx >= 0)
        val wakeSlice = source.substring(
            wakeIdx,
            (wakeIdx + 4000).coerceAtMost(source.length),
        )
        assertTrue(
            wakeSlice.contains("haptics.perform(HapticsIntensity.Light)"),
            "SoineApp must fire a light haptic on wake so the morning transition is a meaningful moment",
        )
    }

    @Test
    fun soineAppFiresMediumHapticOnDreamDiscovery() {
        val source = loadScreen("SoineApp.kt")
        // There are two assignments to dreamDiscoveries:
        // the initial state (line ~191) and the post-wake
        // re-evaluation (line ~510). The haptic lives next
        // to the second one, so we anchor on the wake
        // handler.
        val wakeIdx = source.indexOf("onWake = {")
        assertTrue(wakeIdx >= 0)
        val wakeSlice = source.substring(
            wakeIdx,
            (wakeIdx + 6000).coerceAtMost(source.length),
        )
        assertTrue(
            wakeSlice.contains("haptics.perform(HapticsIntensity.Medium)"),
            "SoineApp must fire a medium haptic on a new dream discovery so the user feels the '見つけた' moment",
        )
    }

    @Test
    fun motionTokensAreNotBouncy() {
        // The motion language is intentionally calm. A
        // bouncy / springy value would regress the V1
        // voice. The contract here is that the
        // MotionTokens file does not import a spring
        // animation spec. Comments that mention "spring"
        // as a thing we explicitly avoid are excluded
        // from the check.
        val source = loadSource(
            "composeApp/src/commonMain/kotlin/app/soine/motion/MotionTokens.kt",
        )
        val nonCommentSource = source
            .lineSequence()
            .filter { line ->
                val trimmed = line.trimStart()
                !(trimmed.startsWith("*") || trimmed.startsWith("//"))
            }
            .joinToString("\n")
        assertTrue(
            !nonCommentSource.contains("Spring") && !nonCommentSource.contains("spring"),
            "MotionTokens must not introduce a spring / bouncy motion; the V1 voice is calm and slow",
        )
    }

    private fun loadScreen(filename: String): String = loadFile(
        "composeApp/src/commonMain/kotlin/app/soine/$filename",
    )

    private fun loadSource(relativePath: String): String = loadFile(relativePath)

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
