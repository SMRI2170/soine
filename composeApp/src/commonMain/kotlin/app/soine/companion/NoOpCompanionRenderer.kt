package app.soine.companion

/**
 * Safe default renderer used until a platform 3D implementation is injected.
 *
 * It accepts semantic requests without owning session/audio lifetime and keeps
 * the shared sleep flow functional when no renderer is available.
 */
object NoOpCompanionRenderer : CompanionRenderer {
    override val status: CompanionRendererStatus = CompanionRendererStatus.READY

    override fun observeStatus(observer: CompanionRendererStatusObserver): AutoCloseable {
        observer.onStatusChanged(status)
        return AutoCloseable {}
    }

    override fun submit(request: CompanionRenderRequest) = Unit

    override fun close() = Unit
}
