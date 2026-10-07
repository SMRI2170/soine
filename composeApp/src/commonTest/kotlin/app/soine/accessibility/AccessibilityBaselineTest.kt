package app.soine.accessibility

import app.soine.companion.CompanionIntent
import app.soine.companion.CompanionRenderRequest
import app.soine.companion.CompanionStaticFallback
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AccessibilityBaselineTest {
    @Test
    fun defaultPreferenceDoesNotForceReducedMotion() {
        assertFalse(DefaultAccessibilityPreferences.reduceMotionEnabled())
    }

    @Test
    fun minimumTouchTargetMeetsBaseline() {
        assertTrue(AccessibilityPolicy.MIN_TOUCH_TARGET_DP >= 48)
    }

    @Test
    fun reducedMotionStaticFallbackHasNoPlacementTransitionAndIsDescribed() {
        val presentation = CompanionStaticFallback.forRequest(
            CompanionRenderRequest(
                intent = CompanionIntent.SLEEP,
                reduceMotion = true,
            )
        )

        assertEquals(0, presentation.sleepingPlacement.transitionDurationMillis)
        assertTrue(presentation.artwork.contentDescription.isNotBlank())
        assertTrue(presentation.artwork.artKey.isNotBlank())
    }
}
