package app.soine.companion

import androidx.compose.ui.graphics.Color
import app.soine.design.SoineColors

/**
 * Single source of truth for the relationship-stage visual
 * presentation.
 *
 * #175 requires that progression be felt through distance,
 * posture, reaction, and the morning greeting rather than
 * through an XP / level number. Each
 * [CompanionRelationshipStage] maps to a deterministic
 * presentation that the bedtime, sleeping, and morning
 * screens consume so a screenshot at one stage is visually
 * distinct from a screenshot at another stage.
 *
 * The presentation never introduces a new color outside the
 * Soine palette. The values are renderer-neutral; the
 * future 3D renderer reads the same struct and maps the
 * values into world / screen coordinates.
 *
 * The presentation does not display the stage value, the
 * session count, or the cumulative hours anywhere in the
 * chrome. The user feels the change; the chrome stays
 * quiet.
 */
data class CompanionStagePresentation(
    /**
     * The bedtime hero scene size. A closer relationship
     * stage makes the companion sit closer to the camera.
     */
    val bedtimeHeroSizeDp: Int,
    /**
     * The bedtime hero glow alpha. A closer relationship
     * warms the scene; a fresh relationship keeps the
     * glow restrained.
     */
    val bedtimeGlowAlpha: Float,
    /**
     * The bedtime sub-label suffix. The headline stays
     * "今日も一緒に眠ろう" for every stage; the suffix
     * adds a one-phrase variation so the bedtime greeting
     * itself is stage-aware without exposing a number.
     */
    val bedtimeGreetingSuffix: String?,
    /**
     * The sleeping bed offset (0f = directly beside the
     * user, 1f = the far edge). The companion drifts
     * closer as the relationship deepens.
     */
    val sleepingBedOffsetFraction: Float,
    /**
     * The sleeping accent alpha. The static scene's
     * overlay accent uses this alpha so a closer
     * relationship warms the bedside scene without
     * changing the camera.
     */
    val sleepingAccentAlpha: Float,
    /**
     * The morning glow color. A closer relationship
     * shifts the morning glow from a quiet cream to a
     * warmer sunrise accent; the change is subtle on
     * purpose.
     */
    val morningGlowColor: Color,
    /**
     * The morning glow alpha. Pairs with the color.
     */
    val morningGlowAlpha: Float,
    /**
     * The morning greeting suffix appended after the
     * default greeting. NEW keeps the default; CLOSE
     * adds a one-phrase variation that signals "we've
     * been together a while" without saying a number.
     */
    val morningGreetingSuffix: String?,
    /**
     * The "stage advanced" reveal copy, shown in the
     * morning screen once when the stage has crossed
     * since the last wake. A quiet one-line notice,
     * not a celebration.
     */
    val stageAdvancedReveal: String?,
)

/**
 * Policy for the relationship-stage visual presentation.
 *
 * The values are pinned and versioned with
 * [CompanionStagePresentationPolicy.CURRENT_VERSION] so a
 * future polish slice can change the curves without
 * invalidating the existing screenshots.
 */
object CompanionStagePresentationPolicy {
    const val CURRENT_VERSION: Int = 1

    fun forStage(stage: CompanionRelationshipStage): CompanionStagePresentation =
        when (stage) {
            CompanionRelationshipStage.NEW -> CompanionStagePresentation(
                bedtimeHeroSizeDp = 220,
                bedtimeGlowAlpha = 0.18f,
                bedtimeGreetingSuffix = null,
                sleepingBedOffsetFraction = 0.78f,
                sleepingAccentAlpha = 0.10f,
                morningGlowColor = SoineColors.cream,
                morningGlowAlpha = 0.18f,
                morningGreetingSuffix = null,
                stageAdvancedReveal = null,
            )
            CompanionRelationshipStage.WARMING_UP -> CompanionStagePresentation(
                bedtimeHeroSizeDp = 230,
                bedtimeGlowAlpha = 0.22f,
                bedtimeGreetingSuffix = "、少しわかってきたね",
                sleepingBedOffsetFraction = 0.60f,
                sleepingAccentAlpha = 0.14f,
                morningGlowColor = SoineColors.cream,
                morningGlowAlpha = 0.20f,
                morningGreetingSuffix = "、少しわかってきたね",
                stageAdvancedReveal = null,
            )
            CompanionRelationshipStage.FAMILIAR -> CompanionStagePresentation(
                bedtimeHeroSizeDp = 250,
                bedtimeGlowAlpha = 0.26f,
                bedtimeGreetingSuffix = "、この時間、なんだか落ち着くね",
                sleepingBedOffsetFraction = 0.38f,
                sleepingAccentAlpha = 0.18f,
                morningGlowColor = SoineColors.sunrise,
                morningGlowAlpha = 0.20f,
                morningGreetingSuffix = "、この時間、なんだか落ち着くね",
                stageAdvancedReveal = "少し、近づけた気がする。",
            )
            CompanionRelationshipStage.CLOSE -> CompanionStagePresentation(
                bedtimeHeroSizeDp = 280,
                bedtimeGlowAlpha = 0.30f,
                bedtimeGreetingSuffix = "、今日はもっとそばにいるね",
                sleepingBedOffsetFraction = 0.18f,
                sleepingAccentAlpha = 0.22f,
                morningGlowColor = SoineColors.sunrise,
                morningGlowAlpha = 0.22f,
                morningGreetingSuffix = "、ずっといたかった",
                stageAdvancedReveal = "もっと近くに来たよ。",
            )
        }
}
