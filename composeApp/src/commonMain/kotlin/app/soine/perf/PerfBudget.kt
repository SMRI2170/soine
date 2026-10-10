package app.soine.perf

/**
 * The V1 performance-budget contract.
 *
 * #184 requires that the V1 ship the binary / asset /
 * runtime / memory / FPS budgets in code so a CI step
 * can verify the current state and detect a relative
 * regression before the next release. The values are
 * mirrored from [`docs/performance-budget.md`][perf-budget-doc]
 * so the source of truth and the docs stay in sync.
 *
 * The contract is intentionally narrow. A new asset
 * class adds a new field; a new dependency ceiling adds
 * a new field. A future contributor who lowers a target
 * (e.g. raises MAX_ASSET_BYTES to a value that hurts
 * mid-range Android) breaks the test.
 *
 * [perf-budget-doc]: ../../../docs/performance-budget.md
 */
object PerfBudget {
    /**
     * Binary / asset budget.
     */
    object Binary {
        const val MAX_ANDROID_APK_BYTES: Long = 18L * 1024L * 1024L
        const val TARGET_ANDROID_APK_BYTES: Long = 12L * 1024L * 1024L
        const val MAX_ANDROID_AAB_BYTES: Long = 18L * 1024L * 1024L
        const val TARGET_ANDROID_AAB_BYTES: Long = 12L * 1024L * 1024L
        const val MAX_IOS_IPA_BYTES: Long = 35L * 1024L * 1024L
        const val TARGET_IOS_IPA_BYTES: Long = 25L * 1024L * 1024L
        const val MAX_KMP_FRAMEWORK_BYTES: Long = 12L * 1024L * 1024L
        const val TARGET_KMP_FRAMEWORK_BYTES: Long = 8L * 1024L * 1024L

        // Per-asset hard ceilings.
        const val MAX_COMPANION_GLB_BYTES: Long = 200L * 1024L
        const val TARGET_COMPANION_GLB_BYTES: Long = 60L * 1024L
        const val MAX_AMBIENT_AUDIO_BYTES: Long = 96L * 1024L
        const val TARGET_AMBIENT_AUDIO_BYTES: Long = 24L * 1024L
        const val MAX_BACKGROUND_TEXTURE_BYTES: Long = 128L * 1024L
        const val TARGET_BACKGROUND_TEXTURE_BYTES: Long = 32L * 1024L
        const val MAX_FONT_SUBSET_BYTES: Long = 96L * 1024L
        const val TARGET_FONT_SUBSET_BYTES: Long = 48L * 1024L

        // Total ambient audio budget (rain + waves + white-noise).
        const val MAX_TOTAL_AMBIENT_AUDIO_BYTES: Long = 288L * 1024L
        const val TARGET_TOTAL_AMBIENT_AUDIO_BYTES: Long = 60L * 1024L
    }

    /**
     * Cold-start dependency cap.
     */
    object Dependencies {
        const val TARGET_COMPOSE_APP_DIRECT_DEPS: Int = 8
        const val MAX_COMPOSE_APP_DIRECT_DEPS: Int = 12
        const val TARGET_ANDROID_APP_DIRECT_DEPS: Int = 4
        const val MAX_ANDROID_APP_DIRECT_DEPS: Int = 6
        const val TARGET_TOTAL_DIRECT_DEPS: Int = 12
        const val MAX_TOTAL_DIRECT_DEPS: Int = 18
    }

    /**
     * The relative-regression threshold. A tracked asset
     * that grows by more than this percentage versus the
     * previous release fails the CI step.
     */
    const val RELATIVE_REGRESSION_THRESHOLD_PERCENT: Int = 25
}
