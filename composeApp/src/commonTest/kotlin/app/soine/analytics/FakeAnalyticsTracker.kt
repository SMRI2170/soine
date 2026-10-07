package app.soine.analytics

internal class FakeAnalyticsTracker : AnalyticsTracker {
    private val _events = mutableListOf<AnalyticsEvent>()

    val events: List<AnalyticsEvent>
        get() = _events.toList()

    override fun track(event: AnalyticsEvent) {
        _events += event
    }
}
