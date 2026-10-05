package app.soine.companion

class FakeCompanionSceneVisibilitySource(
    initialVisible: Boolean = true,
) : CompanionSceneVisibilitySource {
    private val observers = mutableSetOf<CompanionSceneVisibilityObserver>()
    private var visible = initialVisible

    override fun observe(observer: CompanionSceneVisibilityObserver): AutoCloseable {
        observers += observer
        observer.onVisibilityChanged(visible)
        return AutoCloseable { observers -= observer }
    }

    fun setVisible(value: Boolean) {
        if (visible == value) return
        visible = value
        observers.toList().forEach { it.onVisibilityChanged(value) }
    }

    override fun close() {
        observers.clear()
    }
}
