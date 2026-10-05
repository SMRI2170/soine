package app.soine.companion

import kotlin.test.*

class CompanionRenderLifecycleCoordinatorTest {
    @Test fun hidingSceneSuspendsRendererWithoutLosingSemanticState() {
        val renderer = FakeCompanionRenderer()
        val coordinator = CompanionRenderLifecycleCoordinator(renderer)

        coordinator.submitSemanticState(
            intent = CompanionIntent.SLEEP,
            relationshipStage = CompanionRelationshipStage.CLOSE,
        )
        coordinator.sceneHidden()

        assertEquals(CompanionRendererStatus.SUSPENDED, renderer.status)
        assertEquals(CompanionIntent.SLEEP, coordinator.latestRequest.intent)
        assertEquals(CompanionRelationshipStage.CLOSE, coordinator.latestRequest.relationshipStage)
        assertEquals(CompanionSceneVisibility.HIDDEN, coordinator.latestRequest.visibility)
    }

    @Test fun semanticChangesWhileHiddenStaySuspendedAndAreRetained() {
        val renderer = FakeCompanionRenderer()
        val coordinator = CompanionRenderLifecycleCoordinator(renderer)

        coordinator.sceneHidden()
        coordinator.submitSemanticState(intent = CompanionIntent.BREATHE)

        assertEquals(CompanionRendererStatus.SUSPENDED, renderer.status)
        assertEquals(CompanionIntent.BREATHE, renderer.latestRequest?.intent)
        assertEquals(CompanionSceneVisibility.HIDDEN, renderer.latestRequest?.visibility)
    }

    @Test fun becomingVisibleResynchronizesLatestSemanticState() {
        val renderer = FakeCompanionRenderer()
        val coordinator = CompanionRenderLifecycleCoordinator(renderer)

        coordinator.sceneHidden()
        coordinator.submitSemanticState(
            intent = CompanionIntent.ROLL_OVER,
            relationshipStage = CompanionRelationshipStage.FAMILIAR,
        )
        coordinator.sceneVisible()

        assertEquals(CompanionRendererStatus.READY, renderer.status)
        assertEquals(CompanionIntent.ROLL_OVER, renderer.latestRequest?.intent)
        assertEquals(CompanionRelationshipStage.FAMILIAR, renderer.latestRequest?.relationshipStage)
        assertEquals(CompanionSceneVisibility.VISIBLE, renderer.latestRequest?.visibility)
    }

    @Test fun duplicateVisibilityDoesNotResubmitRendererWork() {
        val renderer = FakeCompanionRenderer()
        val coordinator = CompanionRenderLifecycleCoordinator(renderer)
        val initialCount = renderer.submittedRequests.size

        coordinator.sceneVisible()

        assertEquals(initialCount, renderer.submittedRequests.size)
    }

    @Test fun closeDelegatesRendererCleanup() {
        val renderer = FakeCompanionRenderer()
        val coordinator = CompanionRenderLifecycleCoordinator(renderer)

        coordinator.close()

        assertEquals(CompanionRendererStatus.IDLE, renderer.status)
    }
}
