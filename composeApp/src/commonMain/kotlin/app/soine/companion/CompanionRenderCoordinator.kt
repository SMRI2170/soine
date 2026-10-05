package app.soine.companion

/**
 * Keeps the latest semantic companion state independent from whether the
 * renderer is currently visible.
 *
 * Platform lifecycle code only calls setVisible(). Audio and sleep-session
 * lifetimes are intentionally not dependencies of this coordinator.
 */
class CompanionRenderCoordinator(
    private val renderer: CompanionRenderer,
    initialStage: CompanionRelationshipStage = CompanionRelationshipStage.NEW,
) : AutoCloseable {
    private var visible: Boolean = true
    private var current = CompanionRenderRequest(
        intent = CompanionIntent.IDLE,
        relationshipStage = initialStage,
        visibility = CompanionSceneVisibility.VISIBLE,
    )

    val currentRequest: CompanionRenderRequest
        get() = current

    fun setSemanticState(
        intent: CompanionIntent,
        relationshipStage: CompanionRelationshipStage = current.relationshipStage,
        reaction: CompanionReactionTrigger? = null,
    ) {
        current = CompanionRenderRequest(
            intent = intent,
            relationshipStage = relationshipStage,
            visibility = visibility(),
            reaction = reaction,
        )
        renderer.submit(current)
    }

    fun setVisible(isVisible: Boolean) {
        if (visible == isVisible) return
        visible = isVisible
        current = current.copy(visibility = visibility())
        renderer.submit(current)
    }

    fun resync() {
        renderer.submit(current.copy(visibility = visibility()))
    }

    private fun visibility(): CompanionSceneVisibility =
        if (visible) CompanionSceneVisibility.VISIBLE else CompanionSceneVisibility.HIDDEN

    override fun close() {
        renderer.close()
    }
}

/**
 * Minimal platform lifecycle boundary. Android/iOS adapters can translate
 * Activity/UIViewController visibility into this callback without bringing
 * platform lifecycle types into commonMain.
 */
fun interface CompanionSceneVisibilityObserver {
    fun onVisibilityChanged(visible: Boolean)
}

interface CompanionSceneVisibilitySource : AutoCloseable {
    fun observe(observer: CompanionSceneVisibilityObserver): AutoCloseable
    override fun close()
}
