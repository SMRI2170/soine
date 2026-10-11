package app.soine.onboarding

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Source-text regression for the OnboardingController Compose observation
 * contract. The contract:
 *
 *   - `currentStep` and `lastResult` must be backed by `mutableStateOf(...)`
 *     so the screen recomposes when the controller mutates them. A plain
 *     Kotlin `var` would not trigger a recomposition and the onboarding
 *     flow would not advance even though the click handler fires.
 *
 * The composition semantics are not asserted at runtime here because the
 * project's testing strategy deliberately defers Compose instrumented tests;
 * see `docs/testing-strategy.md`. Source inspection is precise enough to
 * catch a regression where someone reverts either field to a plain `var`.
 *
 * This test was added after a production incident where the bug above
 * surfaced on the AVD "ramen_test" Pixel 6 emulator: a fresh install showed
 * page 1/3 but tap-to-advance was a no-op because the controller wrote a
 * plain `var` and Compose never recomposed. The fix moved both fields to a
 * `mutableStateOf(...)` backed property with a `private set` setter so the
 * mutation surface stays unchanged from the screen's point of view.
 *
 * The test mirrors the project's `SleepingScreenLayoutTest` pattern:
 * a textual assertion pinned against the source file. A future PR that
 * removes the backing `mutableStateOf` (plain Kotlin `var`) must update this
 * test alongside the change because the structural contract is no longer
 * satisfied.
 */
class OnboardingControllerComposeStateContractTest {

    @Test
    fun currentStepIsBackedByMutableStateOf() {
        val source = loadOnboardingControllerSource()
        // Pin: `currentStep` is exposed through a private backing field that
        // is created via `mutableStateOf(0)`. The screen reads `currentStep`
        // through the public getter, which delegates to `_currentStep.value`.
        assertTrue(
            source.contains("private val _currentStep = mutableStateOf(0)"),
            "OnboardingController must back currentStep with a `mutableStateOf(0)` private field. " +
                "A plain Kotlin `var` would not trigger a Compose recomposition and the onboarding " +
                "flow would not advance. See OnboardingControllerComposeStateContractTest kdoc.",
        )
    }

    @Test
    fun lastResultIsBackedByMutableStateOf() {
        val source = loadOnboardingControllerSource()
        // Pin: `lastResult` is also backed by a `mutableStateOf` so the
        // LaunchedEffect in OnboardingScreen that navigates away on
        // COMPLETED / SKIPPED actually fires.
        assertTrue(
            source.contains("private val _lastResult = mutableStateOf(Result.PENDING)"),
            "OnboardingController must back lastResult with a `mutableStateOf(Result.PENDING)` private " +
                "field. A plain Kotlin `var` would not trigger the LaunchedEffect that navigates away " +
                "from the onboarding screen.",
        )
    }

    @Test
    fun noPlainKotlinVarLingersForCurrentStep() {
        val source = loadOnboardingControllerSource()
        // The original regression source was
        //     `var currentStep: Int = 0\n        private set`
        // which is what shipped on the emulator during the production bug.
        // A future refactor that drops the `mutableStateOf` backing will
        // re-introduce that exact shape; pin its absence.
        val pattern = Regex(
            """var\s+currentStep\s*:\s*Int\s*=""",
            RegexOption.MULTILINE,
        )
        val offendingMatch = pattern.find(source)?.value
        assertTrue(
            offendingMatch == null,
            "OnboardingController must not declare `var currentStep: Int = ...`; " +
                "the field must be backed by `mutableStateOf(0)` so Compose recomposes. " +
                "Found offending declaration: `$offendingMatch`",
        )
    }

    @Test
    fun noPlainKotlinVarLingersForLastResult() {
        val source = loadOnboardingControllerSource()
        val pattern = Regex(
            """var\s+lastResult\s*:\s*Result\s*=""",
            RegexOption.MULTILINE,
        )
        val offendingMatch = pattern.find(source)?.value
        assertTrue(
            offendingMatch == null,
            "OnboardingController must not declare `var lastResult: Result = ...`; " +
                "the field must be backed by `mutableStateOf(Result.PENDING)`. " +
                "Found offending declaration: `$offendingMatch`",
        )
    }

    @Test
    fun composeRuntimeImportsArePresent() {
        val source = loadOnboardingControllerSource()
        // The backing `mutableStateOf` requires three imports:
        //   androidx.compose.runtime.getValue
        //   androidx.compose.runtime.mutableStateOf
        //   androidx.compose.runtime.setValue
        // Without them the file will not compile and a silent downgrade to
        // plain `var` is the most likely rebuild error.
        assertTrue(
            source.contains("import androidx.compose.runtime.getValue"),
            "OnboardingController must import androidx.compose.runtime.getValue for the property delegate.",
        )
        assertTrue(
            source.contains("import androidx.compose.runtime.mutableStateOf"),
            "OnboardingController must import androidx.compose.runtime.mutableStateOf.",
        )
        assertTrue(
            source.contains("import androidx.compose.runtime.setValue"),
            "OnboardingController must import androidx.compose.runtime.setValue for the property delegate.",
        )
    }

    private fun loadOnboardingControllerSource(): String {
        val candidates = listOf(
            "composeApp/src/commonMain/kotlin/app/soine/onboarding/OnboardingController.kt",
            "../composeApp/src/commonMain/kotlin/app/soine/onboarding/OnboardingController.kt",
            "src/commonMain/kotlin/app/soine/onboarding/OnboardingController.kt",
            "../src/commonMain/kotlin/app/soine/onboarding/OnboardingController.kt",
        )
        for (path in candidates) {
            val file = File(path)
            if (file.exists()) return file.readText(Charsets.UTF_8)
        }
        error(
            "OnboardingController.kt not found in any of the candidate paths; tried: " +
                candidates.joinToString(),
        )
    }
}
