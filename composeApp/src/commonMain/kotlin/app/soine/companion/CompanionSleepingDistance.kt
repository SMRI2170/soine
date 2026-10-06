package app.soine.companion

import app.soine.relationship.FamiliarityStage

enum class CompanionSleepingDistance {
    FAR,
    NEAR,
    CLOSER,
    BESIDE,
}

/**
 * Renderer-neutral bed placement.
 *
 * [bedOffsetFraction] is normalized in bed space: 0f is directly beside the
 * user and 1f is the far edge of the companion's usable bed area. Platform
 * renderers map this value into their own world/screen coordinates while
 * keeping the fixed camera independent from relationship rules.
 */
data class CompanionSleepingPlacement(
    val distance: CompanionSleepingDistance,
    val bedOffsetFraction: Float,
    val transitionDurationMillis: Int,
) {
    init {
        require(bedOffsetFraction in 0f..1f) {
            "Bed offset must be normalized to 0..1."
        }
        require(transitionDurationMillis >= 0) {
            "Transition duration must not be negative."
        }
    }
}

object CompanionSleepingDistancePolicy {
    private const val DEFAULT_TRANSITION_MILLIS = 700

    fun forStage(
        stage: CompanionRelationshipStage,
        reduceMotion: Boolean = false,
    ): CompanionSleepingPlacement {
        val duration = if (reduceMotion) 0 else DEFAULT_TRANSITION_MILLIS
        return when (stage) {
            CompanionRelationshipStage.NEW -> CompanionSleepingPlacement(
                distance = CompanionSleepingDistance.FAR,
                bedOffsetFraction = 0.78f,
                transitionDurationMillis = duration,
            )
            CompanionRelationshipStage.WARMING_UP -> CompanionSleepingPlacement(
                distance = CompanionSleepingDistance.NEAR,
                bedOffsetFraction = 0.60f,
                transitionDurationMillis = duration,
            )
            CompanionRelationshipStage.FAMILIAR -> CompanionSleepingPlacement(
                distance = CompanionSleepingDistance.CLOSER,
                bedOffsetFraction = 0.38f,
                transitionDurationMillis = duration,
            )
            CompanionRelationshipStage.CLOSE -> CompanionSleepingPlacement(
                distance = CompanionSleepingDistance.BESIDE,
                bedOffsetFraction = 0.18f,
                transitionDurationMillis = duration,
            )
        }
    }
}

fun FamiliarityStage.toCompanionRelationshipStage(): CompanionRelationshipStage =
    when (this) {
        FamiliarityStage.NEW -> CompanionRelationshipStage.NEW
        FamiliarityStage.WARMING_UP -> CompanionRelationshipStage.WARMING_UP
        FamiliarityStage.FAMILIAR -> CompanionRelationshipStage.FAMILIAR
        FamiliarityStage.CLOSE -> CompanionRelationshipStage.CLOSE
    }
