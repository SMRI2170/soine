package app.soine.companion

/**
 * A failure-injecting [CompanionRenderer] for tests.
 *
 * #185 requires that the V1 graceful-degradation contract
 * be pinned through failure-injection tests. The fake lets
 * a test author specify the failure pattern up front:
 *
 *   - [failNTimes] — fail the next N `submit` calls with the
 *     given code, then succeed. Use to verify the
 *     automatic-retry path.
 *   - [alwaysFail] — every `submit` returns FAILED. Use to
 *     verify the static-fallback path.
 *   - [recoverAfter] — fail until the Nth submit, then
 *     succeed. Use to verify the recovery contract.
 *
 * The fake also records every failure event through the
 * telemetry hook so a test can assert that the right
 * `CompanionRendererFailureEvent` was emitted.
 */
class FailureInjectingCompanionRenderer : CompanionRenderer {
    private val observers = mutableSetOf<CompanionRendererStatusObserver>()
    private val requests = mutableListOf<CompanionRenderRequest>()
    private val failureEvents = mutableListOf<CompanionRendererFailureEvent>()

    override var status: CompanionRendererStatus = CompanionRendererStatus.IDLE
        private set

    private var failRemaining: Int = 0
    private var failCode: String? = null
    private var isProgrammedToAlwaysFail: Boolean = false

    val submittedRequests: List<CompanionRenderRequest>
        get() = requests.toList()

    val recordedFailureEvents: List<CompanionRendererFailureEvent>
        get() = failureEvents.toList()

    fun failNTimes(count: Int, code: String? = null) {
        isProgrammedToAlwaysFail = false
        failRemaining = count
        failCode = code
        transition(CompanionRendererStatus.FAILED(code))
    }

    fun alwaysFail(code: String? = null) {
        isProgrammedToAlwaysFail = true
        failCode = code
        transition(CompanionRendererStatus.FAILED(code))
    }

    fun recover() {
        isProgrammedToAlwaysFail = false
        failRemaining = 0
        failCode = null
        transition(CompanionRendererStatus.READY)
    }

    override fun observeStatus(observer: CompanionRendererStatusObserver): AutoCloseable {
        observers += observer
        observer.onStatusChanged(status)
        return AutoCloseable { observers -= observer }
    }

    override fun close() {
        observers.clear()
    }

    override fun submit(request: CompanionRenderRequest) {
        val shouldFail = isProgrammedToAlwaysFail || failRemaining > 0
        if (shouldFail) {
            val code = failCode
            failureEvents += CompanionRendererFailureEvent(
                code = code,
                consecutiveFailures = (failureEvents.size + 1),
            )
            if (!isProgrammedToAlwaysFail) {
                failRemaining -= 1
            }
            transition(CompanionRendererStatus.FAILED(code))
            return
        }
        requests += request
        transition(
            if (request.visibility == CompanionSceneVisibility.HIDDEN) {
                CompanionRendererStatus.SUSPENDED
            } else {
                CompanionRendererStatus.READY
            },
        )
    }

    private fun transition(next: CompanionRendererStatus) {
        status = next
        observers.toList().forEach { it.onStatusChanged(next) }
    }
}
