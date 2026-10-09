package app.soine.accessibility

import app.soine.companion.CompanionIntent
import app.soine.companion.CompanionRenderRequest
import app.soine.companion.CompanionStaticFallback
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Smoke test for the reduce-motion coverage of the static companion
 * fallback.
 *
 * #195 requires:
 *
 *   - reduce-motion で情報が失われない
 *   - the relationship-distance transitions become zero-duration when
 *     reduce motion is enabled (already covered by
 *     [AccessibilityBaselineTest])
 *   - the static companion artwork remains a renderer-independent
 *     fallback, including when motion should be minimized
 *
 * The acceptance criteria extend the contract to every intent: when
 * reduce motion is enabled, every reachable companion intent must
 * produce a static fallback whose placement transition is zero
 * duration. A future intent that forgets the reduce-motion branch
 * would not be caught by the single-intent test, so the test walks
 * the full enum.
 */
class ReduceMotionCoverageTest {

    @Test
    fun everyIntentProducesZeroDurationPlacementWhenReduceMotionEnabled() {
        for (intent in CompanionIntent.entries) {
            val presentation = CompanionStaticFallback.forRequest(
                CompanionRenderRequest(
                    intent = intent,
                    reduceMotion = true,
                ),
            )
            assertEquals(
                0,
                presentation.sleepingPlacement.transitionDurationMillis,
                "intent $intent must use zero-duration placement under reduce motion",
            )
        }
    }

    @Test
    fun everyIntentProducesContentDescriptionWhenReduceMotionEnabled() {
        for (intent in CompanionIntent.entries) {
            val presentation = CompanionStaticFallback.forRequest(
                CompanionRenderRequest(
                    intent = intent,
                    reduceMotion = true,
                ),
            )
            assertTrue(
                presentation.artwork.contentDescription.isNotBlank(),
                "intent $intent must expose a content description so screen readers can announce it",
            )
            assertTrue(
                presentation.artwork.artKey.isNotBlank(),
                "intent $intent must expose an artKey so the static fallback can be replaced deterministically",
            )
        }
    }

    @Test
    fun reduceMotionStateIsHashable() {
        // The static fallback is also expected to be a pure function
        // of its inputs so the UI cache can key on (intent, reduceMotion).
        // This test guards against that contract being silently
        // changed.
        val request = CompanionRenderRequest(
            intent = CompanionIntent.SLEEP,
            reduceMotion = true,
        )
        val first = CompanionStaticFallback.forRequest(request)
        val second = CompanionStaticFallback.forRequest(request)

        assertEquals(first, second)
    }
}
