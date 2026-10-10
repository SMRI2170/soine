package app.soine.perf

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Pins the V1 performance-budget contract in code.
 *
 * #184 requires that the V1 ship the binary / asset /
 * dependency budgets in source so a CI step can verify
 * the current state. The contract here:
 *
 *   - the `PerfBudget.Binary` hard ceilings match the
 *     ceilings in [docs/performance-budget.md][perf-budget-doc]
 *     and the values in
 *     [scripts/perf-budget-check.sh][perf-budget-script]
 *   - the `PerfBudget.Dependencies` ceilings are
 *     monotonic (target <= max) and the targets are
 *     positive
 *   - a future contributor who lowers a hard ceiling
 *     (e.g. raises the APK size to 50 MB) breaks the
 *     test
 *   - the relative-regression threshold is pinned at 25 %
 *
 * [perf-budget-doc]: ../../../../docs/performance-budget.md
 * [perf-budget-script]: ../../../../scripts/perf-budget-check.sh
 */
class PerfBudgetContractTest {

    @Test
    fun androidApkHardCeilingIsPinned() {
        // 18 MiB. A future contributor who raises the
        // ceiling to a value that hurts the Play upload
        // budget breaks the test.
        assertEquals(18L * 1024L * 1024L, PerfBudget.Binary.MAX_ANDROID_APK_BYTES)
        assertTrue(
            PerfBudget.Binary.TARGET_ANDROID_APK_BYTES < PerfBudget.Binary.MAX_ANDROID_APK_BYTES,
        )
    }

    @Test
    fun androidAabHardCeilingIsPinned() {
        assertEquals(18L * 1024L * 1024L, PerfBudget.Binary.MAX_ANDROID_AAB_BYTES)
        assertTrue(
            PerfBudget.Binary.TARGET_ANDROID_AAB_BYTES < PerfBudget.Binary.MAX_ANDROID_AAB_BYTES,
        )
    }

    @Test
    fun iosIpaHardCeilingIsPinned() {
        assertEquals(35L * 1024L * 1024L, PerfBudget.Binary.MAX_IOS_IPA_BYTES)
        assertTrue(
            PerfBudget.Binary.TARGET_IOS_IPA_BYTES < PerfBudget.Binary.MAX_IOS_IPA_BYTES,
        )
    }

    @Test
    fun kmpFrameworkHardCeilingIsPinned() {
        assertEquals(12L * 1024L * 1024L, PerfBudget.Binary.MAX_KMP_FRAMEWORK_BYTES)
        assertTrue(
            PerfBudget.Binary.TARGET_KMP_FRAMEWORK_BYTES < PerfBudget.Binary.MAX_KMP_FRAMEWORK_BYTES,
        )
    }

    @Test
    fun companionGlbHardCeilingIsPinned() {
        // 200 KB. The current PoC is 30,616 B; the
        // production asset can grow but must stay under
        // the 200 KB ceiling.
        assertEquals(200L * 1024L, PerfBudget.Binary.MAX_COMPANION_GLB_BYTES)
        assertTrue(
            PerfBudget.Binary.TARGET_COMPANION_GLB_BYTES < PerfBudget.Binary.MAX_COMPANION_GLB_BYTES,
        )
    }

    @Test
    fun ambientAudioHardCeilingIsPinned() {
        assertEquals(96L * 1024L, PerfBudget.Binary.MAX_AMBIENT_AUDIO_BYTES)
        assertEquals(24L * 1024L, PerfBudget.Binary.TARGET_AMBIENT_AUDIO_BYTES)
        // Total ambient audio budget.
        assertEquals(288L * 1024L, PerfBudget.Binary.MAX_TOTAL_AMBIENT_AUDIO_BYTES)
        assertEquals(60L * 1024L, PerfBudget.Binary.TARGET_TOTAL_AMBIENT_AUDIO_BYTES)
    }

    @Test
    fun backgroundTextureAndFontHardCeilingsArePinned() {
        assertEquals(128L * 1024L, PerfBudget.Binary.MAX_BACKGROUND_TEXTURE_BYTES)
        assertEquals(32L * 1024L, PerfBudget.Binary.TARGET_BACKGROUND_TEXTURE_BYTES)
        assertEquals(96L * 1024L, PerfBudget.Binary.MAX_FONT_SUBSET_BYTES)
        assertEquals(48L * 1024L, PerfBudget.Binary.TARGET_FONT_SUBSET_BYTES)
    }

    @Test
    fun dependencyHardCeilingsArePinned() {
        assertEquals(8, PerfBudget.Dependencies.TARGET_COMPOSE_APP_DIRECT_DEPS)
        assertEquals(12, PerfBudget.Dependencies.MAX_COMPOSE_APP_DIRECT_DEPS)
        assertEquals(4, PerfBudget.Dependencies.TARGET_ANDROID_APP_DIRECT_DEPS)
        assertEquals(6, PerfBudget.Dependencies.MAX_ANDROID_APP_DIRECT_DEPS)
        assertEquals(12, PerfBudget.Dependencies.TARGET_TOTAL_DIRECT_DEPS)
        assertEquals(18, PerfBudget.Dependencies.MAX_TOTAL_DIRECT_DEPS)
    }

    @Test
    fun dependencyTargetsAreBelowMax() {
        // The target is the engineering alert level; the
        // max is the hard ceiling. A future contributor
        // who flips them breaks the contract.
        assertTrue(
            PerfBudget.Dependencies.TARGET_COMPOSE_APP_DIRECT_DEPS < PerfBudget.Dependencies.MAX_COMPOSE_APP_DIRECT_DEPS,
        )
        assertTrue(
            PerfBudget.Dependencies.TARGET_ANDROID_APP_DIRECT_DEPS < PerfBudget.Dependencies.MAX_ANDROID_APP_DIRECT_DEPS,
        )
        assertTrue(
            PerfBudget.Dependencies.TARGET_TOTAL_DIRECT_DEPS < PerfBudget.Dependencies.MAX_TOTAL_DIRECT_DEPS,
        )
    }

    @Test
    fun relativeRegressionThresholdIsPinned() {
        // 25 %. A future polish slice can change this
        // value through a code change; the test
        // guarantees the constant exists and is in a
        // safe range.
        val threshold = PerfBudget.RELATIVE_REGRESSION_THRESHOLD_PERCENT
        assertTrue(threshold in 1..100, "Threshold $threshold must be in 1..100")
        assertEquals(25, threshold)
    }
}
