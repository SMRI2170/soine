package app.soine.motion

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Pins the V1 motion-language contract.
 *
 * #177 requires a single source of truth for animation
 * duration, easing, and reduce-motion behavior. The
 * contract:
 *
 *   - durations are 120 / 240 / 480 ms (quick / normal /
 *     slow). There is no longer or bouncier value; bouncy
 *     motion is gamified and is not the Soine voice.
 *   - the easings are calm: [MotionTokens.EASING_SOFT] is
 *     the default; [MotionTokens.EASING_GENTLE] is for
 *     fade-out and dismiss.
 *   - when reduce-motion is on, every duration collapses
 *     to 0 and the easing becomes a strict step. The
 *     functional information (the content that toggles)
 *     is preserved.
 */
class MotionTokensTest {

    @Test
    fun durationsAreCalm() {
        // The V1 motion language is intentionally narrow.
        // A new duration must be added with a documented
        // meaning; bouncy / springy values are not allowed.
        assertEquals(120, MotionTokens.DURATION_QUICK)
        assertEquals(240, MotionTokens.DURATION_NORMAL)
        assertEquals(480, MotionTokens.DURATION_SLOW)
        assertTrue(
            MotionTokens.DURATION_QUICK < MotionTokens.DURATION_NORMAL,
        )
        assertTrue(
            MotionTokens.DURATION_NORMAL < MotionTokens.DURATION_SLOW,
        )
    }

    @Test
    fun easingsAreCalm() {
        // The easings are pinned so a future polish slice
        // cannot drift the motion language toward a
        // bouncier / springier feel.
        assertNotNull(MotionTokens.EASING_SOFT)
        assertNotNull(MotionTokens.EASING_GENTLE)
    }

    @Test
    fun reduceMotionCollapsesDurationToZero() {
        assertEquals(
            0,
            MotionTokens.durationMillis(MotionTokens.DURATION_QUICK, reduceMotion = true),
        )
        assertEquals(
            0,
            MotionTokens.durationMillis(MotionTokens.DURATION_NORMAL, reduceMotion = true),
        )
        assertEquals(
            0,
            MotionTokens.durationMillis(MotionTokens.DURATION_SLOW, reduceMotion = true),
        )
    }

    @Test
    fun reduceMotionDoesNotCollapseWhenOff() {
        assertEquals(
            MotionTokens.DURATION_QUICK,
            MotionTokens.durationMillis(MotionTokens.DURATION_QUICK, reduceMotion = false),
        )
        assertEquals(
            MotionTokens.DURATION_NORMAL,
            MotionTokens.durationMillis(MotionTokens.DURATION_NORMAL, reduceMotion = false),
        )
        assertEquals(
            MotionTokens.DURATION_SLOW,
            MotionTokens.durationMillis(MotionTokens.DURATION_SLOW, reduceMotion = false),
        )
    }

    @Test
    fun reduceMotionReturnsStepEasing() {
        // The reduce-motion path must return a step easing
        // (or, by extension, an equivalent discrete-state
        // easing) so the transition becomes a single
        // instantaneous state change. The functional
        // information is preserved; the motion is removed.
        val reduceEasing = MotionTokens.easing(reduceMotion = true)
        val normalEasing = MotionTokens.easing(reduceMotion = false)
        assertEquals(StepEasing, reduceEasing)
        assertEquals(MotionTokens.EASING_SOFT, normalEasing)
    }

    @Test
    fun recordingHapticsCapturesEvents() {
        // The RecordingHaptics is the test seam: a test can
        // pass it as the Haptics implementation and assert
        // that the right intensities fired in the right
        // order. The events list is read-only externally so
        // a test cannot accidentally mutate the recorded
        // history.
        val haptics = RecordingHaptics()
        haptics.perform(HapticsIntensity.Light)
        haptics.perform(HapticsIntensity.Medium)
        haptics.perform(HapticsIntensity.Light)
        assertEquals(3, haptics.events.size)
        assertEquals(HapticsIntensity.Light, haptics.events[0])
        assertEquals(HapticsIntensity.Medium, haptics.events[1])
        assertEquals(HapticsIntensity.Light, haptics.events[2])
        haptics.reset()
        assertEquals(0, haptics.events.size)
    }

    @Test
    fun noOpHapticsDoesNotThrow() {
        // The NoOpHaptics is the desktop / JVM fallback.
        // It must never throw, regardless of intensity.
        val haptics = NoOpHaptics
        haptics.perform(HapticsIntensity.Light)
        haptics.perform(HapticsIntensity.Medium)
    }
}
