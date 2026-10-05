package app.soine.companion

/**
 * Deterministic renderer used by previews and shared UI tests.
 */
class FakeCompanionRenderer : CompanionRenderer {
    private val observers = mutableSetOf<CompanionRendererStatusObserver>()
    private val requests = mutableListOf<CompanionRenderRequest>()

    override var status: CompanionRendererStatus = CompanionRendererStatus.IDLE
        private set

    val submittedRequests: List<CompanionRenderRequest>
        get() = requests.toList()

    val latestRequest: CompanionRenderRequest?
        get() = requests.lastOrNull()

    override fun observeStatus(observer: CompanionRendererStatusObserver): AutoCloseable {
        observers += observer
        observer.onStatusChanged(status)
        return AutoCloseable { observers -= observer }
    }

    override fun submit(request: CompanionRenderRequest) {
        requests += request
        transition(
            if (request.visibility == CompanionSceneVisibility.HIDDEN) {
                CompanionRendererStatus.SUSPENDED
            } else {
                CompanionRendererStatus.READY
            },
        )
    }

    fun fail(code: String? = null) {
        transition(CompanionRendererStatus.FAILED(code))
    }

    override fun close() {
        transition(CompanionRendererStatus.IDLE)
        observers.clear()
    }

    private fun transition(next: CompanionRendererStatus) {
        status = next
        observers.toList().forEach { it.onStatusChanged(next) }
    }
}
