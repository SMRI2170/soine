package app.soine.companion

import kotlin.coroutines.startCoroutine
import kotlin.test.*

class BedtimeSignatureControllerTest {
    @Test fun signatureFollowsProductSequence() {
        assertEquals(
            listOf(
                CompanionIntent.LOOK_AT_USER,
                CompanionIntent.MOVE_CLOSER,
                CompanionIntent.CURL_UP,
                CompanionIntent.SLEEP,
                CompanionIntent.BREATHE,
            ),
            BedtimeSignatureSequence.steps.map { it.intent },
        )
    }

    @Test fun controllerSubmitsEachSemanticStepInOrder() {
        val renderer = FakeCompanionRenderer()
        val controller = BedtimeSignatureController(renderer)

        controller.start(CompanionRelationshipStage.FAMILIAR)
        while (controller.state?.completed != true) controller.advance()

        assertEquals(
            BedtimeSignatureSequence.steps.map { it.intent },
            renderer.submittedRequests.map { it.intent },
        )
        assertEquals(
            List(BedtimeSignatureSequence.steps.size) { CompanionRelationshipStage.FAMILIAR },
            renderer.submittedRequests.map { it.relationshipStage },
        )
    }

    @Test fun uiBecomesQuietOnlyAtStableBreathingState() {
        val controller = BedtimeSignatureController(FakeCompanionRenderer())
        val quietStates = mutableListOf<Boolean>()

        quietStates += controller.start().quietUi
        while (controller.state?.completed != true) quietStates += controller.advance().quietUi

        assertEquals(listOf(false, false, false, false, true), quietStates)
        assertEquals(CompanionIntent.BREATHE, controller.state?.step?.intent)
    }

    @Test fun rendererFailureNeverEscapesIntoSleepFlow() {
        val renderer = ThrowingRenderer()
        val controller = BedtimeSignatureController(renderer)

        val first = controller.start()
        val second = controller.advance()

        assertTrue(first.rendererFailed)
        assertTrue(second.rendererFailed)
        assertEquals(CompanionIntent.MOVE_CLOSER, second.step.intent)
    }

    @Test fun runnerWaitsForEachTimedStepAndEndsInBreathingState() {
        val renderer = FakeCompanionRenderer()
        val waits = mutableListOf<Long>()
        val states = mutableListOf<BedtimeSignatureState>()
        val runner = BedtimeSignatureRunner(
            controller = BedtimeSignatureController(renderer),
            wait = { millis -> waits += millis },
        )

        val result = runSuspend {
            runner.run(CompanionRelationshipStage.CLOSE) { states += it }
        }

        assertEquals(listOf(600L, 900L, 900L, 500L), waits)
        assertEquals(BedtimeSignatureSequence.steps.size, states.size)
        assertEquals(CompanionIntent.BREATHE, result.step.intent)
        assertTrue(result.quietUi)
        assertTrue(result.completed)
    }

    @Test fun runnerCompletesEvenWhenRendererThrowsAtEveryStep() {
        val runner = BedtimeSignatureRunner(
            controller = BedtimeSignatureController(ThrowingRenderer()),
            wait = {},
        )

        val result = runSuspend { runner.run() }

        assertTrue(result.completed)
        assertTrue(result.rendererFailed)
        assertEquals(CompanionIntent.BREATHE, result.step.intent)
    }

    @Test fun advancingCompletedSequenceIsIdempotent() {
        val renderer = FakeCompanionRenderer()
        val controller = BedtimeSignatureController(renderer)
        controller.start()
        while (controller.state?.completed != true) controller.advance()
        val count = renderer.submittedRequests.size

        val again = controller.advance()

        assertTrue(again.completed)
        assertEquals(count, renderer.submittedRequests.size)
    }

    private fun <T> runSuspend(block: suspend () -> T): T {
        var outcome: Result<T>? = null
        block.startCoroutine(
            object : kotlin.coroutines.Continuation<T> {
                override val context: kotlin.coroutines.CoroutineContext =
                    kotlin.coroutines.EmptyCoroutineContext

                override fun resumeWith(result: Result<T>) {
                    outcome = result
                }
            }
        )
        return checkNotNull(outcome) {
            "Test coroutine suspended unexpectedly; this helper only supports synchronous suspend code."
        }.getOrThrow()
    }

    private class ThrowingRenderer : CompanionRenderer {
        override val status: CompanionRendererStatus = CompanionRendererStatus.FAILED("test")

        override fun observeStatus(observer: CompanionRendererStatusObserver): AutoCloseable =
            AutoCloseable {}

        override fun submit(request: CompanionRenderRequest) {
            error("renderer unavailable")
        }

        override fun close() = Unit
    }
}
