package app.soine.accessibility

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Smoke test for the sleep start / wake CTA accessibility contract.
 *
 * The two CTAs (Bedtime "一緒に寝る" and Sleeping "起きる") drive the
 * core sleep lifecycle. The current accessibility policy requires:
 *
 *   - A `semantics { contentDescription = ... }` modifier on the CTA
 *     so TalkBack / VoiceOver can announce the action.
 *   - The string passed to `contentDescription` is the canonical label
 *     exposed by [AccessibilityPolicy].
 *
 * Compose UI instrumented tests are deferred (see
 * docs/ci-quality-policy.md). Until then, this test pins the contract
 * by:
 *
 *   1. Asserting the [AccessibilityPolicy] labels are present, non-blank,
 *      and stable. Renaming a label is a breaking accessibility change.
 *   2. Reading the `App.kt` source on the classpath and asserting the
 *      two `contentDescription =` sites reference the constants rather
 *      than literal strings. This catches a regression where a future
 *      edit inlines the label and bypasses the policy.
 *
 * The source check is intentionally textual: there is no Compose UI
 * runtime in commonTest. As soon as the policy lifts the
 * Compose-UI-instrumented-test deferral, this test can be replaced by
 * a true UI assertion.
 */
class SleepStartWakeCtaSemanticsTest {

    @Test
    fun sleepStartLabelIsStable() {
        assertEquals("睡眠を開始する", AccessibilityPolicy.SLEEP_START_CONTENT_DESCRIPTION)
    }

    @Test
    fun wakeLabelIsStable() {
        assertEquals("起床して朝の記録を見る", AccessibilityPolicy.WAKE_CONTENT_DESCRIPTION)
    }

    @Test
    fun labelsAreNonBlank() {
        assertTrue(AccessibilityPolicy.SLEEP_START_CONTENT_DESCRIPTION.isNotBlank())
        assertTrue(AccessibilityPolicy.WAKE_CONTENT_DESCRIPTION.isNotBlank())
    }

    @Test
    fun appScreenWiresSleepStartLabelThroughAccessibilityPolicy() {
        val bedtimeSource = loadBedtimeSource()
        assertTrue(
            bedtimeSource.contains("AccessibilityPolicy.SLEEP_START_CONTENT_DESCRIPTION"),
            "BedtimeScreen's sleep-start CTA must reference the AccessibilityPolicy label, " +
                "not an inline literal, so the policy is the single source of truth.",
        )
    }

    @Test
    fun appScreenWiresWakeLabelThroughAccessibilityPolicy() {
        val sleepingSource = loadSleepingSource()
        assertTrue(
            sleepingSource.contains("AccessibilityPolicy.WAKE_CONTENT_DESCRIPTION"),
            "SleepingScreen's wake CTA must reference the AccessibilityPolicy label, " +
                "not an inline literal, so the policy is the single source of truth.",
        )
    }

    @Test
    fun appScreenAppliesSemanticsBlockForSleepStartCta() {
        val bedtimeSource = loadBedtimeSource()
        // The semantics block must sit on the same composable that
        // owns the onStartSleep callback. We look at the second
        // occurrence of the constant — the first is the docstring
        // comment, the second is the actual call site.
        val firstIdx = bedtimeSource.indexOf("SLEEP_START_CONTENT_DESCRIPTION")
        check(firstIdx >= 0) { "Expected SLEEP_START_CONTENT_DESCRIPTION in BedtimeScreen.kt" }
        val secondIdx = bedtimeSource.indexOf("SLEEP_START_CONTENT_DESCRIPTION", firstIdx + 1)
        check(secondIdx >= 0) { "Expected a second occurrence of SLEEP_START_CONTENT_DESCRIPTION in BedtimeScreen.kt" }
        val start = (secondIdx - 200).coerceAtLeast(0)
        val end = (secondIdx + 400).coerceAtMost(bedtimeSource.length)
        val window = bedtimeSource.substring(start, end)
        assertTrue(
            window.contains("contentDescription = AccessibilityPolicy.SLEEP_START_CONTENT_DESCRIPTION"),
            "Sleep-start CTA must apply contentDescription to AccessibilityPolicy.SLEEP_START_CONTENT_DESCRIPTION. " +
                "Window dump:\n" + window,
        )
    }

    @Test
    fun appScreenAppliesSemanticsBlockForWakeCta() {
        val sleepingSource = loadSleepingSource()
        // The wake CTA's contentDescription uses the AccessibilityPolicy
        // constant. The constant may appear in a docstring and at
        // the call site (BedtimeScreen.kt) or only at the call site
        // (SleepingScreen.kt). We check the actual call site by
        // looking for the `contentDescription = ...` pattern.
        val firstIdx = sleepingSource.indexOf("WAKE_CONTENT_DESCRIPTION")
        check(firstIdx >= 0) { "Expected WAKE_CONTENT_DESCRIPTION in SleepingScreen.kt" }
        // Walk forward through the file. If the first occurrence is
        // already on a `contentDescription = ...` line, use it.
        // Otherwise look for the second occurrence (call site).
        val searchStart = if (looksLikeContentDescription(sleepingSource, firstIdx)) {
            firstIdx
        } else {
            val next = sleepingSource.indexOf("WAKE_CONTENT_DESCRIPTION", firstIdx + 1)
            check(next >= 0) {
                "Expected a call site for WAKE_CONTENT_DESCRIPTION in SleepingScreen.kt"
            }
            next
        }
        val start = (searchStart - 200).coerceAtLeast(0)
        val end = (searchStart + 400).coerceAtMost(sleepingSource.length)
        val window = sleepingSource.substring(start, end)
        assertTrue(
            window.contains("contentDescription = AccessibilityPolicy.WAKE_CONTENT_DESCRIPTION"),
            "Wake Button must wrap onWake with a semantics block that sets " +
                "contentDescription to AccessibilityPolicy.WAKE_CONTENT_DESCRIPTION. " +
                "Window dump:\n" + window,
        )
    }

    private fun looksLikeContentDescription(source: String, idx: Int): Boolean {
        // Walk back 200 chars and look for a `contentDescription =`
        // pattern on the same logical line.
        val lookbackStart = (idx - 200).coerceAtLeast(0)
        val window = source.substring(lookbackStart, idx)
        return window.contains("contentDescription =")
    }

    private fun loadAppSource(): String {
        // The Gradle test task's working directory is the project root
        // (the directory that contains `composeApp/` and `androidApp/`).
        // We try a small set of well-known relative paths so the smoke
        // gate works whether Gradle is invoked from the repo root, from
        // composeApp/, or from CI's checkout path.
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
            "App.kt not found in any of the candidate paths; cannot run the CTA " +
                "semantics smoke gate. Tried: " + candidates.joinToString(),
        )
    }

    private fun loadBedtimeSource(): String {
        // #169 moved the bedtime screen into its own file. The
        // wake CTA stays in App.kt; the sleep-start CTA moves to
        // BedtimeScreen.kt.
        val candidates = listOf(
            "composeApp/src/commonMain/kotlin/app/soine/BedtimeScreen.kt",
            "../composeApp/src/commonMain/kotlin/app/soine/BedtimeScreen.kt",
            "src/commonMain/kotlin/app/soine/BedtimeScreen.kt",
            "../src/commonMain/kotlin/app/soine/BedtimeScreen.kt",
        )
        for (path in candidates) {
            val file = java.io.File(path)
            if (file.exists()) return file.readText(Charsets.UTF_8)
        }
        error(
            "BedtimeScreen.kt not found in any of the candidate paths; cannot run the CTA " +
                "semantics smoke gate. Tried: " + candidates.joinToString(),
        )
    }

    private fun loadSleepingSource(): String {
        // #170 moved the sleeping screen into its own file. The
        // wake CTA moved with it; the test loads the file directly
        // instead of going through App.kt.
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
            "SleepingScreen.kt not found in any of the candidate paths; cannot run the CTA " +
                "semantics smoke gate. Tried: " + candidates.joinToString(),
        )
    }

    /**
     * Returns a 600-char window around the first occurrence of [marker].
     * The window is wide enough to cover a `Button(...)` block with the
     * modifier chain and the `onClick = ...` site.
     */
    private fun windowAround(source: String, marker: String, windowSize: Int = 600): String {
        val idx = source.indexOf(marker)
        check(idx >= 0) { "Expected $marker in App.kt" }
        val start = (idx - 200).coerceAtLeast(0)
        val end = (idx + windowSize).coerceAtMost(source.length)
        return source.substring(start, end)
    }
}
