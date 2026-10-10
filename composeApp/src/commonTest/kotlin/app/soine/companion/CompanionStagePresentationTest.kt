package app.soine.companion

import app.soine.design.SoineColors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pins the V1 relationship-stage visual presentation contract.
 *
 * #175 requires that progression be felt through distance,
 * posture, reaction, and the morning greeting rather than
 * through an XP / level number. The
 * [CompanionStagePresentationPolicy] is the single source of
 * truth for the bedtime / sleeping / morning visuals.
 *
 * The contract here is:
 *
 *  - every stage returns a presentation, and the values are
 *    deterministic
 *  - the bedtime hero size grows monotonically with closeness
 *  - the sleeping bed offset shrinks monotonically with
 *    closeness
 *  - the morning glow alpha grows monotonically with closeness
 *  - the NEW stage is intentionally the quietest (no
 *    greeting suffix, no reveal copy) so a fresh install does
 *    not feel overwhelmed
 *  - the presentation never introduces a new color outside
 *    the Soine palette
 *  - the policy does not embed XP / level / session-count
 *    numbers anywhere
 */
class CompanionStagePresentationTest {

    @Test
    fun policyReturnsPresentationForEveryStage() {
        for (stage in CompanionRelationshipStage.values()) {
            val presentation = CompanionStagePresentationPolicy.forStage(stage)
            // The data class is not null; the contract is that
            // every stage has a presentation. The presence
            // check guards against a future "default to NEW"
            // fallback that would erase a stage.
            assertNotNull(presentation)
        }
    }

    @Test
    fun bedtimeHeroSizeGrowsMonotonicallyWithCloseness() {
        val new = CompanionStagePresentationPolicy.forStage(CompanionRelationshipStage.NEW)
        val warming = CompanionStagePresentationPolicy.forStage(CompanionRelationshipStage.WARMING_UP)
        val familiar = CompanionStagePresentationPolicy.forStage(CompanionRelationshipStage.FAMILIAR)
        val close = CompanionStagePresentationPolicy.forStage(CompanionRelationshipStage.CLOSE)
        assertTrue(new.bedtimeHeroSizeDp < warming.bedtimeHeroSizeDp)
        assertTrue(warming.bedtimeHeroSizeDp < familiar.bedtimeHeroSizeDp)
        assertTrue(familiar.bedtimeHeroSizeDp < close.bedtimeHeroSizeDp)
    }

    @Test
    fun sleepingBedOffsetShrinksMonotonicallyWithCloseness() {
        val new = CompanionStagePresentationPolicy.forStage(CompanionRelationshipStage.NEW)
        val warming = CompanionStagePresentationPolicy.forStage(CompanionRelationshipStage.WARMING_UP)
        val familiar = CompanionStagePresentationPolicy.forStage(CompanionRelationshipStage.FAMILIAR)
        val close = CompanionStagePresentationPolicy.forStage(CompanionRelationshipStage.CLOSE)
        assertTrue(new.sleepingBedOffsetFraction > warming.sleepingBedOffsetFraction)
        assertTrue(warming.sleepingBedOffsetFraction > familiar.sleepingBedOffsetFraction)
        assertTrue(familiar.sleepingBedOffsetFraction > close.sleepingBedOffsetFraction)
        // The bed offset must always stay within the
        // normalized 0..1 range that
        // CompanionSleepingPlacement requires.
        for (stage in CompanionRelationshipStage.values()) {
            val presentation = CompanionStagePresentationPolicy.forStage(stage)
            assertTrue(presentation.sleepingBedOffsetFraction in 0f..1f)
        }
    }

    @Test
    fun morningGlowAlphaGrowsMonotonicallyWithCloseness() {
        val new = CompanionStagePresentationPolicy.forStage(CompanionRelationshipStage.NEW)
        val warming = CompanionStagePresentationPolicy.forStage(CompanionRelationshipStage.WARMING_UP)
        val familiar = CompanionStagePresentationPolicy.forStage(CompanionRelationshipStage.FAMILIAR)
        val close = CompanionStagePresentationPolicy.forStage(CompanionRelationshipStage.CLOSE)
        assertTrue(new.morningGlowAlpha <= warming.morningGlowAlpha)
        assertTrue(warming.morningGlowAlpha <= familiar.morningGlowAlpha)
        assertTrue(familiar.morningGlowAlpha <= close.morningGlowAlpha)
    }

    @Test
    fun newStageIsTheQuietest() {
        val presentation = CompanionStagePresentationPolicy.forStage(CompanionRelationshipStage.NEW)
        // NEW must not have a greeting suffix — the bedtime
        // and morning greetings keep the default.
        assertNull(presentation.bedtimeGreetingSuffix)
        assertNull(presentation.morningGreetingSuffix)
        // NEW must not have a "stage advanced" reveal — a
        // fresh install has nothing to advance from.
        assertNull(presentation.stageAdvancedReveal)
    }

    @Test
    fun familiarAndCloseStagesCarryRevealCopy() {
        val familiar = CompanionStagePresentationPolicy.forStage(CompanionRelationshipStage.FAMILIAR)
        val close = CompanionStagePresentationPolicy.forStage(CompanionRelationshipStage.CLOSE)
        // FAMILIAR and CLOSE are the only stages that surface
        // a "stage advanced" notice when the relationship
        // crosses. WARMING_UP is intentionally without a
        // reveal so the first promotion does not feel like
        // a celebration.
        assertNotNull(familiar.stageAdvancedReveal)
        assertNotNull(close.stageAdvancedReveal)
    }

    @Test
    fun presentationColorsAreComposedFromSoinePalette() {
        val soineTokens = setOf(
            SoineColors.cream,
            SoineColors.midnight,
            SoineColors.dusk,
            SoineColors.twilight,
            SoineColors.dim,
            SoineColors.glow,
            SoineColors.ember,
            SoineColors.hush,
            SoineColors.sunrise,
            SoineColors.soft,
        )
        for (stage in CompanionRelationshipStage.values()) {
            val presentation = CompanionStagePresentationPolicy.forStage(stage)
            assertTrue(
                presentation.morningGlowColor in soineTokens,
                "Morning glow color for $stage is not part of the Soine palette",
            )
        }
    }

    @Test
    fun policyIsVersioned() {
        // The policy exposes a version constant so a future
        // polish slice can rebalance the curves without
        // invalidating the existing screenshots.
        assertEquals(1, CompanionStagePresentationPolicy.CURRENT_VERSION)
    }

    @Test
    fun presentationDoesNotEmbedXpLevelOrSessionCount() {
        // The presentation strings must not embed a number
        // that could read as XP / level / session-count. The
        // chrome stays quiet; the user feels the change.
        for (stage in CompanionRelationshipStage.values()) {
            val presentation = CompanionStagePresentationPolicy.forStage(stage)
            listOfNotNull(
                presentation.bedtimeGreetingSuffix,
                presentation.morningGreetingSuffix,
                presentation.stageAdvancedReveal,
            ).forEach { copy ->
                assertTrue(
                    !copy.contains(Regex("[0-9]")),
                    "Stage $stage presentation copy must not embed a number; got: $copy",
                )
            }
        }
    }
}
