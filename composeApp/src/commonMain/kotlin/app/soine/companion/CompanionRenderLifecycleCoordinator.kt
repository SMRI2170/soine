package app.soine.companion

/**
 * Keeps semantic companion state independent from renderer lifetime.
 *
 * Platform lifecycle code reports whether the scene is visible. Hidden scenes
 * are submitted as HIDDEN so a platform renderer can stop its render loop.
 * The latest semantic state is retained and re-submitted when visibility
 * returns.
 */
class CompanionRenderLifecycleCoordinator(
    private val renderer: CompanionRenderer,
    initialRequest: CompanionRenderRequest = CompanionRenderRequest(),
) : AutoCloseable {
    var latestRequest: CompanionRenderRequest = initialRequest
        private set

    val visibility: CompanionSceneVisibility
        get() = latestRequest.visibility

    init {
        renderer.submit(initialRequest)
    }

    fun submitSemanticState(
        intent: CompanionIntent = latestRequest.intent,
        relationshipStage: CompanionRelationshipStage = latestRequest.relationshipStage,
        reaction: CompanionReactionTrigger? = latestRequest.reaction,
    ) {
        latestRequest = latestRequest.copy(
            intent = intent,
            relationshipStage = relationshipStage,
            reaction = reaction,
        )
        renderer.submit(latestRequest)
    }

    fun setVisibility(visibility: CompanionSceneVisibility) {
        if (visibility == latestRequest.visibility) return
        latestRequest = latestRequest.copy(visibility = visibility)
        renderer.submit(latestRequest)
    }

    fun sceneHidden() = setVisibility(CompanionSceneVisibility.HIDDEN)

    fun sceneVisible() = setVisibility(CompanionSceneVisibility.VISIBLE)

    override fun close() {
        renderer.close()
    }
}
