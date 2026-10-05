package app.soine.companion

import kotlin.test.*

class CompanionRendererTest {
    @Test fun semanticRequestContainsNoPlatformRendererTypes() {
        val renderer = FakeCompanionRenderer()
        val request = CompanionRenderRequest(
            intent = CompanionIntent.SETTLE,
            relationshipStage = CompanionRelationshipStage.FAMILIAR,
            visibility = CompanionSceneVisibility.VISIBLE,
            reaction = CompanionReactionTrigger("tap-head"),
        )

        renderer.submit(request)

        assertEquals(request, renderer.latestRequest)
        assertEquals(CompanionRendererStatus.READY, renderer.status)
    }

    @Test fun hiddenSceneSuspendsWithoutLosingSemanticState() {
        val renderer = FakeCompanionRenderer()
        val request = CompanionRenderRequest(
            intent = CompanionIntent.SLEEP,
            relationshipStage = CompanionRelationshipStage.CLOSE,
            visibility = CompanionSceneVisibility.HIDDEN,
        )

        renderer.submit(request)

        assertEquals(CompanionRendererStatus.SUSPENDED, renderer.status)
        assertEquals(CompanionIntent.SLEEP, renderer.latestRequest?.intent)
        assertEquals(CompanionRelationshipStage.CLOSE, renderer.latestRequest?.relationshipStage)
    }

    @Test fun statusCanBeObservedAndFailureDoesNotDiscardRequest() {
        val renderer = FakeCompanionRenderer()
        val statuses = mutableListOf<CompanionRendererStatus>()
        val subscription = renderer.observeStatus { statuses += it }

        renderer.submit(CompanionRenderRequest(intent = CompanionIntent.BREATHE))
        renderer.fail("asset-load")
        subscription.close()

        assertEquals(
            listOf(
                CompanionRendererStatus.IDLE,
                CompanionRendererStatus.READY,
                CompanionRendererStatus.FAILED("asset-load"),
            ),
            statuses,
        )
        assertEquals(CompanionIntent.BREATHE, renderer.latestRequest?.intent)
    }

    @Test fun reactionIdMustBeNonBlank() {
        assertFailsWith<IllegalArgumentException> {
            CompanionReactionTrigger(" ")
        }
    }
}
