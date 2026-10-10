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

    /**
     * Accessibility label for the onboarding "スキップ" CTA. The
     * button in `OnboardingScreen.kt` must apply this string via a
     * `semantics { contentDescription = ... }` block so TalkBack /
     * VoiceOver can announce the action.
     */
    const val ONBOARDING_SKIP_CONTENT_DESCRIPTION: String = "オンボーディングをスキップする"

    /**
     * Accessibility label for the onboarding "もどる" CTA.
     */
    const val ONBOARDING_BACK_CONTENT_DESCRIPTION: String = "前のステップに戻る"

    /**
     * Accessibility label for the onboarding "次へ" / "はじめる" CTA.
     */
    const val ONBOARDING_NEXT_CONTENT_DESCRIPTION: String = "次のステップへ進む"

    /**
     * Accessibility label for the "オンボーディングをもう一度見る"
     * text button in Settings. The SettingsScreen text button must
     * apply this string via a `semantics { contentDescription = ... }`
     * block so TalkBack / VoiceOver can announce the action.
     */
    const val SETTINGS_REPLAY_ONBOARDING_CONTENT_DESCRIPTION: String = "オンボーディングをもう一度見る"

    /**
     * Accessibility label for the bedtime "夢のアルバム" quiet
     * button. The BedtimeScreen top navigation must apply this
     * string via a `semantics { contentDescription = ... }` block.
     */
    const val BEDTIME_DREAM_ALBUM_CONTENT_DESCRIPTION: String = "夢のアルバムを開く"

    /**
     * Accessibility label for the bedtime "設定" quiet button.
     */
    const val BEDTIME_SETTINGS_CONTENT_DESCRIPTION: String = "設定を開く"

    /**
     * Accessibility label for the sleeping scene tap-to-reveal
     * interaction. Tapping the scene brings the secondary controls
     * back. The label is exposed both as a contentDescription and
     * an onClickLabel so TalkBack / VoiceOver announce the action.
     */
    const val SLEEPING_SCENE_REVEAL_CONTENT_DESCRIPTION: String = "画面をタップして操作を表示"

    /**
     * Accessibility label for the sleeping "一時停止 / 再生" quiet
     * button.
     */
    const val SLEEPING_AUDIO_TOGGLE_CONTENT_DESCRIPTION: String = "環境音の再生と一時停止を切り替え"

    /**
     * Accessibility label for the sleeping "タイマー" quiet button.
     */
    const val SLEEPING_TIMER_CONTENT_DESCRIPTION: String = "スリープタイマーを変更"

    /**
     * Accessibility label for the sleeping "タイマーを解除" quiet
     * button.
     */
    const val SLEEPING_TIMER_CANCEL_CONTENT_DESCRIPTION: String = "スリープタイマーを解除"

    /**
     * Accessibility label for the sleeping "設定" quiet button.
     */
    const val SLEEPING_SETTINGS_CONTENT_DESCRIPTION: String = "設定を開く"
}
