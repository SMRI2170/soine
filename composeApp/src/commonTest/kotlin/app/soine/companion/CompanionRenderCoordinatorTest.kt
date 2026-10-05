package app.soine.companion

import kotlin.test.*

class CompanionRenderCoordinatorTest {
    @Test fun hidingScenePreservesSemanticIntentAndSuspendsRenderer() {
        val renderer = FakeCompanionRenderer()
        val coordinator = CompanionRenderCoordinator(renderer)

        coordinator.setSemanticState(CompanionIntent.SLEEP)
        coordinator.setVisible(false)

        assertEquals(CompanionIntent.SLEEP, coordinator.currentRequest.intent)
        assertEquals(CompanionSceneVisibility.HIDDEN, coordinator.currentRequest.visibility)
        assertEquals(CompanionRendererStatus.SUSPENDED, renderer.status)
    }

    @Test fun semanticUpdatesWhileHiddenRemainHidden() {
        val renderer = FakeCompanionRenderer()
        val coordinator = CompanionRenderCoordinator(renderer)
        coordinator.setVisible(false)

        coordinator.setSemanticState(
            intent = CompanionIntent.ROLL_OVER,
            relationshipStage = CompanionRelationshipStage.FAMILIAR,
        )

        assertEquals(CompanionIntent.ROLL_OVER, renderer.latestRequest?.intent)
        assertEquals(CompanionRelationshipStage.FAMILIAR, renderer.latestRequest?.relationshipStage)
        assertEquals(CompanionSceneVisibility.HIDDEN, renderer.latestRequest?.visibility)
    }

    @Test fun becomingVisibleResubmitsLatestSemanticState() {
        val renderer = FakeCompanionRenderer()
        val coordinator = CompanionRenderCoordinator(renderer)
        coordinator.setSemanticState(CompanionIntent.BREATHE)
        coordinator.setVisible(false)
        coordinator.setSemanticState(CompanionIntent.EAR_TWITCH)

        coordinator.setVisible(true)

        assertEquals(CompanionIntent.EAR_TWITCH, renderer.latestRequest?.intent)
        assertEquals(CompanionSceneVisibility.VISIBLE, renderer.latestRequest?.visibility)
        assertEquals(CompanionRendererStatus.READY, renderer.status)
    }

    @Test fun duplicateVisibilityDoesNotCreateExtraRendererWork() {
        val renderer = FakeCompanionRenderer()
        val coordinator = CompanionRenderCoordinator(renderer)
        coordinator.setSemanticState(CompanionIntent.IDLE)
        val before = renderer.submittedRequests.size

        coordinator.setVisible(true)

        assertEquals(before, renderer.submittedRequests.size)
    }

    @Test fun visibilitySourceCanDriveCoordinatorWithoutSleepOrAudioDependencies() {
        val renderer = FakeCompanionRenderer()
        val coordinator = CompanionRenderCoordinator(renderer)
        val source = FakeCompanionSceneVisibilitySource()
        val subscription = source.observe { coordinator.setVisible(it) }

        coordinator.setSemanticState(CompanionIntent.SLEEP)
        source.setVisible(false)
        source.setVisible(true)
        subscription.close()

        assertEquals(CompanionIntent.SLEEP, renderer.latestRequest?.intent)
        assertEquals(CompanionSceneVisibility.VISIBLE, renderer.latestRequest?.visibility)
    }
}
