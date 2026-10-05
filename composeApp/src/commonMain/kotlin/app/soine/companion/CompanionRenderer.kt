package app.soine.companion

enum class CompanionRelationshipStage {
    NEW,
    WARMING_UP,
    FAMILIAR,
    CLOSE,
}

enum class CompanionSceneVisibility {
    VISIBLE,
    HIDDEN,
}

data class CompanionReactionTrigger(
    val id: String,
) {
    init {
        require(id.isNotBlank()) { "Reaction id must not be blank." }
    }
}

data class CompanionRenderRequest(
    val intent: CompanionIntent = CompanionIntent.IDLE,
    val relationshipStage: CompanionRelationshipStage = CompanionRelationshipStage.NEW,
    val visibility: CompanionSceneVisibility = CompanionSceneVisibility.VISIBLE,
    val reaction: CompanionReactionTrigger? = null,
)

sealed interface CompanionRendererStatus {
    data object IDLE : CompanionRendererStatus
    data object READY : CompanionRendererStatus
    data object SUSPENDED : CompanionRendererStatus
    data class FAILED(val code: String? = null) : CompanionRendererStatus
}

fun interface CompanionRendererStatusObserver {
    fun onStatusChanged(status: CompanionRendererStatus)
}

/**
 * Renderer-neutral boundary between shared UI/domain state and platform 3D.
 *
 * Implementations translate semantic intents into engine-specific assets and
 * animation clips. No SceneKit, Filament, OpenGL, Metal or model types cross
 * this boundary.
 */
interface CompanionRenderer : AutoCloseable {
    val status: CompanionRendererStatus

    fun observeStatus(observer: CompanionRendererStatusObserver): AutoCloseable
    fun submit(request: CompanionRenderRequest)

    override fun close()
}
