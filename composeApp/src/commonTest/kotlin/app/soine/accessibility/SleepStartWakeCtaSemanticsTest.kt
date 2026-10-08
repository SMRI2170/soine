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
        val source = loadAppSource()
        assertTrue(
            source.contains("AccessibilityPolicy.SLEEP_START_CONTENT_DESCRIPTION"),
            "BedtimeScreen's sleep-start CTA must reference the AccessibilityPolicy label, " +
                "not an inline literal, so the policy is the single source of truth.",
        )
    }

    @Test
    fun appScreenWiresWakeLabelThroughAccessibilityPolicy() {
        val source = loadAppSource()
        assertTrue(
            source.contains("AccessibilityPolicy.WAKE_CONTENT_DESCRIPTION"),
            "SleepingScreen's wake CTA must reference the AccessibilityPolicy label, " +
                "not an inline literal, so the policy is the single source of truth.",
        )
    }

    @Test
    fun appScreenAppliesSemanticsBlockForSleepStartCta() {
        val source = loadAppSource()
        // The semantics block must sit on the same composable that owns
        // the onClick = onStartSleep callback. We check the surrounding
        // window so the two facts stay correlated.
        val window = windowAround(source, "onClick = onStartSleep")
        assertTrue(
            window.contains("contentDescription = AccessibilityPolicy.SLEEP_START_CONTENT_DESCRIPTION"),
            "Sleep-start Button must wrap onStartSleep with a semantics block that sets " +
                "contentDescription to AccessibilityPolicy.SLEEP_START_CONTENT_DESCRIPTION. " +
                "Window dump:\n" + window,
        )
    }

    @Test
    fun appScreenAppliesSemanticsBlockForWakeCta() {
        val source = loadAppSource()
        val window = windowAround(source, "onClick = onWake")
        assertTrue(
            window.contains("contentDescription = AccessibilityPolicy.WAKE_CONTENT_DESCRIPTION"),
            "Wake Button must wrap onWake with a semantics block that sets " +
                "contentDescription to AccessibilityPolicy.WAKE_CONTENT_DESCRIPTION. " +
                "Window dump:\n" + window,
        )
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

    /**
     * Returns a 600-char window around the first occurrence of [marker].
     * The window is wide enough to cover a `Button(...)` block with the
     * modifier chain and the `onClick = ...` site.
     */
    private fun windowAround(source: String, marker: String): String {
        val idx = source.indexOf(marker)
        check(idx >= 0) { "Expected $marker in App.kt" }
        val start = (idx - 200).coerceAtLeast(0)
        val end = (idx + 400).coerceAtMost(source.length)
        return source.substring(start, end)
    }
}
