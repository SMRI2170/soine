package app.soine.companion

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Pins the V1 production-asset pipeline contract.
 *
 * #173 requires the V1 to ship the static 2D fallback as
 * the primary path; the production 3D asset is owned by
 * a future polish slice. The contract here:
 *
 *   - the V1 default is `STATIC_FALLBACK`
 *   - the acceptance criteria are pinned so a future
 *     artist session has a clear bar
 *   - the static fallback is **not** a degraded path; it
 *     is the V1 voice
 *   - the required clip ids match the
 *     `CompanionAnimationMapping` set
 */
class CompanionAssetPipelineTest {

    @Test
    fun v1DefaultIsStaticFallback() {
        // The V1 default must be STATIC_FALLBACK. The
        // production 3D asset is owned by a future polish
        // slice. A future artist session flips the status
        // to PRODUCTION_READY; the call sites do not
        // change.
        assertEquals(
            CompanionAssetPipelineStatus.STATIC_FALLBACK,
            CompanionAssetPipeline.current,
        )
    }

    @Test
    fun requiredClipIdsCoverEveryCompanionIntent() {
        // The production asset must include a clip for
        // every semantic intent. Missing clips fall back
        // to the static path through
        // CompanionRendererFallbackPolicy.
        for (intent in CompanionIntent.values()) {
            val clip = CompanionAnimationMapping.preferredClip(intent)
            assertNotNull(clip)
            assertTrue(
                clip.id.isNotBlank(),
                "CompanionAnimationMapping.preferredClip($intent) must return a non-blank clip id",
            )
        }
    }

    @Test
    fun requiredClipIdsMatchAnimationMapping() {
        // The contract requires that the production
        // asset's required clip ids are exactly the set
        // that CompanionAnimationMapping owns. A future
        // contributor who adds a clip without updating
        // the contract breaks the test.
        assertEquals(
            CompanionAnimationMapping.requiredClipIds,
            CompanionAssetAcceptance.REQUIRED_CLIP_IDS,
        )
    }

    @Test
    fun acceptanceCriteriaArePinned() {
        // The asset budgets are conservative ceilings.
        // A future contributor who lowers the bar (e.g.
        // raises MAX_TRIANGLES to a value that hurts
        // mid-range Android) breaks the test.
        assertEquals(8_000, CompanionAssetAcceptance.MAX_TRIANGLES)
        assertEquals(48, CompanionAssetAcceptance.MAX_BONES)
        assertEquals(1_500_000L, CompanionAssetAcceptance.MAX_GLB_BYTES)
    }

    @Test
    fun staticFallbackCoversEveryIntent() {
        // The static 2D fallback must cover every
        // semantic intent so the V1 voice is complete.
        // A future contributor who adds a new intent
        // without updating the fallback breaks the test
        // (the `when` is non-exhaustive).
        for (intent in CompanionIntent.values()) {
            val artwork = CompanionStaticFallback.forIntent(intent)
            assertTrue(
                artwork.glyph.isNotBlank(),
                "CompanionStaticFallback must cover intent $intent",
            )
            assertTrue(
                artwork.contentDescription.isNotBlank(),
                "CompanionStaticFallback must expose a contentDescription for $intent",
            )
        }
    }
}
