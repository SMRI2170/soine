package app.soine.analytics

/**
 * Product analytics vocabulary owned by commonMain.
 *
 * Events intentionally carry no free-form payload so sensitive sleep content,
 * Health samples, raw audio, user text, and exact sleep timelines cannot be
 * attached accidentally.
 */
enum class AnalyticsEvent(
    val eventName: String,
) {
    BEDTIME_SCREEN_VIEWED("bedtime_screen_viewed"),
    SLEEP_SESSION_STARTED("sleep_session_started"),
    SESSION_RECOVERED("session_recovered"),
    SLEEP_SESSION_COMPLETED("sleep_session_completed"),
    MORNING_SUMMARY_VIEWED("morning_summary_viewed"),
    NIGHT_MEMORY_OPENED("night_memory_opened"),
    DREAM_DISCOVERED("dream_discovered"),
    ONBOARDING_STARTED("onboarding_started"),
    ONBOARDING_STEP_VIEWED("onboarding_step_viewed"),
    ONBOARDING_COMPLETED("onboarding_completed"),
    ONBOARDING_SKIPPED("onboarding_skipped"),
    ONBOARDING_REPLAYED("onboarding_replayed"),
}

fun interface AnalyticsTracker {
    fun track(event: AnalyticsEvent)
}

object NoOpAnalyticsTracker : AnalyticsTracker {
    override fun track(event: AnalyticsEvent) = Unit
}
