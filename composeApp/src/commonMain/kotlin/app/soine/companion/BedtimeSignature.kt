package app.soine.companion

data class BedtimeSignatureStep(
    val intent: CompanionIntent,
    val minimumDurationMillis: Long,
    val quietUi: Boolean = false,
) {
    init {
        require(minimumDurationMillis >= 0) { "Step duration must not be negative." }
    }
}

object BedtimeSignatureSequence {
    val steps: List<BedtimeSignatureStep> = listOf(
        BedtimeSignatureStep(CompanionIntent.LOOK_AT_USER, 600),
        BedtimeSignatureStep(CompanionIntent.MOVE_CLOSER, 900),
        BedtimeSignatureStep(CompanionIntent.CURL_UP, 900),
        BedtimeSignatureStep(CompanionIntent.SLEEP, 500),
        BedtimeSignatureStep(CompanionIntent.BREATHE, 0, quietUi = true),
    )
}

data class BedtimeSignatureState(
    val stepIndex: Int,
    val step: BedtimeSignatureStep,
    val relationshipStage: CompanionRelationshipStage,
    val rendererFailed: Boolean = false,
    val completed: Boolean = false,
) {
    val quietUi: Boolean get() = step.quietUi
}

/**
 * Drives the semantic bedtime signature independently from sleep-session state.
 *
 * Renderer failures are deliberately swallowed and represented as state so a
 * failed animation can never prevent the already-started sleep session.
 */
class BedtimeSignatureController(
    private val renderer: CompanionRenderer,
) {
    var state: BedtimeSignatureState? = null
        private set

    fun start(
        relationshipStage: CompanionRelationshipStage = CompanionRelationshipStage.NEW,
    ): BedtimeSignatureState {
        val first = BedtimeSignatureState(
            stepIndex = 0,
            step = BedtimeSignatureSequence.steps.first(),
            relationshipStage = relationshipStage,
        )
        state = submitSafely(first)
        return state!!
    }

    fun advance(): BedtimeSignatureState {
        val current = state ?: return start()
        if (current.completed) return current

        val nextIndex = current.stepIndex + 1
        if (nextIndex >= BedtimeSignatureSequence.steps.size) {
            return current.copy(completed = true).also { state = it }
        }

        val next = current.copy(
            stepIndex = nextIndex,
            step = BedtimeSignatureSequence.steps[nextIndex],
            rendererFailed = current.rendererFailed,
            completed = nextIndex == BedtimeSignatureSequence.steps.lastIndex,
        )
        state = submitSafely(next)
        return state!!
    }

    private fun submitSafely(next: BedtimeSignatureState): BedtimeSignatureState {
        val result = runCatching {
            renderer.submit(
                CompanionRenderRequest(
                    intent = next.step.intent,
                    relationshipStage = next.relationshipStage,
                    visibility = CompanionSceneVisibility.VISIBLE,
                ),
            )
        }
        return next.copy(rendererFailed = next.rendererFailed || result.isFailure)
    }
}


class BedtimeSignatureRunner(
    private val controller: BedtimeSignatureController,
    private val wait: suspend (Long) -> Unit,
) {
    suspend fun run(
        relationshipStage: CompanionRelationshipStage = CompanionRelationshipStage.NEW,
        onState: (BedtimeSignatureState) -> Unit = {},
    ): BedtimeSignatureState {
        var current = controller.start(relationshipStage)
        onState(current)

        while (!current.completed) {
            if (current.step.minimumDurationMillis > 0L) {
                wait(current.step.minimumDurationMillis)
            }
            current = controller.advance()
            onState(current)
        }
        return current
    }
}
