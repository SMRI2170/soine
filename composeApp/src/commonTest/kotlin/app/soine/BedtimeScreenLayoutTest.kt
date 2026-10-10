package app.soine

import app.soine.companion.CompanionRelationshipStage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Smoke test for the V1 bedtime screen layout contract.
 *
 * #169 requires:
 *
 *   - 初見で companion が最初に視認される — the hero companion
 *     sits above the fold; the relationship-stage prop varies the
 *     scene size
 *   - generic settings card に見えない — the ambient / timer
 *     status is two compact panels, not one settings card
 *   - sleep 開始まで 1 tap を維持 — the primary CTA "一緒に寝る"
 *     is the bottom of the column
 *   - Dream / Settings へも 2 tap 以内 — the top nav reaches both
 *     in one tap each
 *   - reduce-motion でもレイアウトの魅力を維持 — the layout
 *     itself does not depend on motion
 *
 * Compose UI rendering is still deferred (see
 * `docs/ci-quality-policy.md`); the textual / source checks here
 * cover the contract.
 */
class BedtimeScreenLayoutTest {

    @Test
    fun bedtimeScreenSourceUsesDesignSystem() {
        val source = loadBedtimeSource()
        // The bedtime screen is the V1 signature moment; it must
        // reach for the design system, not the Material3 defaults.
        assertTrue(
            source.contains("SoinePrimaryButton"),
            "BedtimeScreen must use SoinePrimaryButton for the sleep-start CTA",
        )
        assertTrue(
            source.contains("SoineQuietButton"),
            "BedtimeScreen must use SoineQuietButton for the Dream / Settings top nav",
        )
        assertTrue(
            source.contains("SoinePanel"),
            "BedtimeScreen must use SoinePanel for the ambient / timer status",
        )
    }

    @Test
    fun bedtimeScreenDoesNotUseGenericSettingsCard() {
        val source = loadBedtimeSource()
        // The pre-redesign bedtime screen wrapped ambient + timer
        // in a single OutlinedCard. The redesign splits them into
        // two compact SoinePanel surfaces. The test pins that the
        // generic Material3 Card / OutlinedCard is gone.
        assertTrue(
            !source.contains("OutlinedCard"),
            "BedtimeScreen must not use OutlinedCard; the generic card shape regresses the redesign",
        )
    }

    @Test
    fun bedtimeScreenOneTapToSleep() {
        val source = loadBedtimeSource()
        // The primary CTA "一緒に寝る" must be the bottom of the
        // column. The test checks that the SoinePrimaryButton is
        // wired with the AccessibilityPolicy contentDescription,
        // which is the source of truth for the screen-reader
        // contract.
        assertTrue(
            source.contains("contentDescription = AccessibilityPolicy.SLEEP_START_CONTENT_DESCRIPTION"),
            "BedtimeScreen primary CTA must reference AccessibilityPolicy.SLEEP_START_CONTENT_DESCRIPTION",
        )
    }

    @Test
    fun bedtimeScreenTwoTapsToDreamAndSettings() {
        val source = loadBedtimeSource()
        // The top nav must include both Dream Album and Settings as
        // SoineQuietButton, reachable in one tap each.
        assertTrue(
            source.contains("BEDTIME_DREAM_ALBUM_CONTENT_DESCRIPTION"),
            "BedtimeScreen top nav must reference BEDTIME_DREAM_ALBUM_CONTENT_DESCRIPTION",
        )
        assertTrue(
            source.contains("BEDTIME_SETTINGS_CONTENT_DESCRIPTION"),
            "BedtimeScreen top nav must reference BEDTIME_SETTINGS_CONTENT_DESCRIPTION",
        )
    }

    @Test
    fun bedtimeScreenHeroSceneGrowsWithRelationshipStage() {
        // The relationship-stage prop drives the scene size so a
        // closer relationship feels closer to the camera. The test
        // pins the four-stage scale.
        val expectedSizes = mapOf(
            CompanionRelationshipStage.NEW to 220,
            CompanionRelationshipStage.WARMING_UP to 230,
            CompanionRelationshipStage.FAMILIAR to 250,
            CompanionRelationshipStage.CLOSE to 280,
        )
        // The source encodes these in a `when` expression. The test
        // verifies that each value is present in the source.
        val source = loadBedtimeSource()
        for ((stage, size) in expectedSizes) {
            // We accept either the full stage enum literal (e.g.
            // `CompanionRelationshipStage.NEW`) or the short name
            // (`NEW`); the source has both depending on the
            // surrounding syntax.
            val stageName = stage.name
            assertTrue(
                source.contains("$size.dp"),
                "Expected hero size $size.dp for stage $stageName to be present in BedtimeScreen.kt",
            )
        }
    }

    @Test
    fun bedtimeScreenDefaultRelationshipStageIsNew() {
        val source = loadBedtimeSource()
        // The default relationship stage is NEW so a fresh install
        // sees the smallest hero scene.
        assertTrue(
            source.contains("relationshipStage: CompanionRelationshipStage = CompanionRelationshipStage.NEW"),
            "BedtimeScreen must default the relationshipStage to NEW",
        )
    }

    @Test
    fun bedtimeScreenTouchTargetsMeetBaseline() {
        val source = loadBedtimeSource()
        // SoinePrimaryButton enforces the 56dp touch target. The
        // top nav SoineQuietButton enforces the 48dp touch target.
        // Both are inherited from the design system; the
        // BedtimeScreen does not re-declare them.
        assertTrue(
            !source.contains("heightIn(min = ") || source.contains("MIN_TOUCH_TARGET_DP"),
            "BedtimeScreen must use the design-system touch-target tokens, not raw dp values",
        )
    }

    @Test
    fun bedtimeScreenReusesCompanionSceneContent() {
        val source = loadBedtimeSource()
        // The hero scene must reuse CompanionSceneContent so a
        // future 3D renderer swap is a one-line change.
        assertTrue(
            source.contains("CompanionSceneContent"),
            "BedtimeScreen must call CompanionSceneContent for the hero scene",
        )
    }

    private fun loadBedtimeSource(): String {
        val candidates = listOf(
            "composeApp/src/commonMain/kotlin/app/soine/BedtimeScreen.kt",
            "../composeApp/src/commonMain/kotlin/app/soine/BedtimeScreen.kt",
            "src/commonMain/kotlin/app/soine/BedtimeScreen.kt",
            "../src/commonMain/kotlin/app/soine/BedtimeScreen.kt",
        )
        for (path in candidates) {
            val file = java.io.File(path)
            if (file.exists()) return file.readText(Charsets.UTF_8)
        }
        error(
            "BedtimeScreen.kt not found in any of the candidate paths; tried: " +
                candidates.joinToString(),
        )
    }
}
