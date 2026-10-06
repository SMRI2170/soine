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
    initialReduceMotion: Boolean = false,
) : AutoCloseable {
    private var visible: Boolean = true
    private var current = request(
        intent = CompanionIntent.IDLE,
        relationshipStage = initialStage,
        reaction = null,
        reduceMotion = initialReduceMotion,
    )

    val currentRequest: CompanionRenderRequest
        get() = current

    fun setSemanticState(
        intent: CompanionIntent,
        relationshipStage: CompanionRelationshipStage = current.relationshipStage,
        reaction: CompanionReactionTrigger? = null,
        reduceMotion: Boolean = current.reduceMotion,
    ) {
        current = request(
            intent = intent,
            relationshipStage = relationshipStage,
            reaction = reaction,
            reduceMotion = reduceMotion,
        )
        renderer.submit(current)
    }

    fun setReduceMotion(enabled: Boolean) {
        if (current.reduceMotion == enabled) return
        current = request(
            intent = current.intent,
            relationshipStage = current.relationshipStage,
            reaction = current.reaction,
            reduceMotion = enabled,
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

    private fun request(
        intent: CompanionIntent,
        relationshipStage: CompanionRelationshipStage,
        reaction: CompanionReactionTrigger?,
        reduceMotion: Boolean,
    ) = CompanionRenderRequest(
        intent = intent,
        relationshipStage = relationshipStage,
        visibility = visibility(),
        reaction = reaction,
        reduceMotion = reduceMotion,
        sleepingPlacement = CompanionSleepingDistancePolicy.forStage(
            relationshipStage,
            reduceMotion,
        ),
    )

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
