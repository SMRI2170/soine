package app.soine.companion

import app.soine.relationship.FamiliarityStage
import kotlin.test.*

class CompanionSleepingDistanceTest {
    @Test fun laterRelationshipStagesMoveMonotonicallyCloser() {
        val placements = CompanionRelationshipStage.entries.map {
            CompanionSleepingDistancePolicy.forStage(it)
        }

        assertEquals(
            listOf(
                CompanionSleepingDistance.FAR,
                CompanionSleepingDistance.NEAR,
                CompanionSleepingDistance.CLOSER,
                CompanionSleepingDistance.BESIDE,
            ),
            placements.map { it.distance },
        )
        assertTrue(
            placements.zipWithNext().all { (a, b) ->
                b.bedOffsetFraction < a.bedOffsetFraction
            }
        )
    }

    @Test fun familiarityMapsOneToOneIntoRendererStage() {
        assertEquals(
            CompanionRelationshipStage.entries,
            FamiliarityStage.entries.map { it.toCompanionRelationshipStage() },
        )
    }

    @Test fun reduceMotionKeepsFinalDistanceButRemovesTransition() {
        val animated = CompanionSleepingDistancePolicy.forStage(
            CompanionRelationshipStage.CLOSE,
            reduceMotion = false,
        )
        val reduced = CompanionSleepingDistancePolicy.forStage(
            CompanionRelationshipStage.CLOSE,
            reduceMotion = true,
        )

        assertEquals(animated.distance, reduced.distance)
        assertEquals(animated.bedOffsetFraction, reduced.bedOffsetFraction)
        assertTrue(animated.transitionDurationMillis > 0)
        assertEquals(0, reduced.transitionDurationMillis)
    }

    @Test fun normalizedOffsetsStayCameraIndependent() {
        CompanionRelationshipStage.entries.forEach { stage ->
            val placement = CompanionSleepingDistancePolicy.forStage(stage)
            assertTrue(placement.bedOffsetFraction in 0f..1f)
        }
    }
}
