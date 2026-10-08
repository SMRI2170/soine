package app.soine.accessibility

fun interface AccessibilityPreferences {
    fun reduceMotionEnabled(): Boolean
}

object DefaultAccessibilityPreferences : AccessibilityPreferences {
    override fun reduceMotionEnabled(): Boolean = false
}

/**
 * Single source of truth for accessibility-related constants shared by
 * the Compose UI. Any new CTA that talks to the sleep lifecycle
 * (start / wake / abort / etc.) must expose its accessibility label
 * here so the smoke gate can verify the label is still wired into the
 * matching semantics block in `App.kt` (see
 * `SleepStartWakeCtaSemanticsTest`).
 */
object AccessibilityPolicy {
    const val MIN_TOUCH_TARGET_DP: Int = 48

    /**
     * Accessibility label for the bedtime "一緒に寝る" CTA that calls
     * `onStartSleep`. The button in `App.kt` `BedtimeScreen` must apply
     * this string via a `semantics { contentDescription = ... }` block
     * so TalkBack / VoiceOver can announce the action.
     */
    const val SLEEP_START_CONTENT_DESCRIPTION: String = "睡眠を開始する"

    /**
     * Accessibility label for the sleeping "起きる" CTA that calls
     * `onWake`. The button in `App.kt` `SleepingScreen` must apply this
     * string via a `semantics { contentDescription = ... }` block so
     * TalkBack / VoiceOver can announce the action.
     */
    const val WAKE_CONTENT_DESCRIPTION: String = "起床して朝の記録を見る"
}
