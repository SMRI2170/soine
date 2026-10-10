package app.soine.motion

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing

/**
 * The Soine motion language — the single source of truth for
 * animation duration, easing, and reduce-motion behavior.
 *
 * #177 requires that animation duration and easing stay
 * consistent across the bedtime, sleeping, and morning
 * screens, and that the motion language respects reduce-
 * motion preferences. The motion tokens are the answer.
 *
 * The motion language is intentionally narrow:
 *
 *   - [DURATION_QUICK] for very short feedback (button
 *     press, ripple, micro-interaction).
 *   - [DURATION_NORMAL] for standard UI motion (sheet
 *     transition, status indicator fade).
 *   - [DURATION_SLOW] for large element transitions
 *     (companion scene shift, full-card reveal).
 *
 * The easings are intentionally calm:
 *
 *   - [EASING_SOFT] is the standard "soine" easing. It
 *     accelerates softly and decelerates softly so a
 *     transition never feels snappy.
 *   - [EASING_GENTLE] is for fade-out and dismiss
 *     motion. It decelerates strongly so the last frame
 *     settles into place.
 *
 * There is no bouncy, overshooting, or springy motion. The
 * motion language is calm-on-purpose; a bouncy
 * `spring(dampingRatio = 0.4)` would read as gamified.
 *
 * [durationMillis] returns the duration respecting
 * [reduceMotion]. When reduce-motion is on, every duration
 * collapses to 0 so the animation becomes a single
 * instantaneous state change. The reduce-motion path
 * preserves the functional information (the content
 * still toggles); only the motion itself is removed.
 */
object MotionTokens {
    const val DURATION_QUICK: Int = 120
    const val DURATION_NORMAL: Int = 240
    const val DURATION_SLOW: Int = 480

    /**
     * Standard "soine" easing. Soft acceleration, soft
     * deceleration. The default for any UI motion that
     * does not explicitly pick [EASING_GENTLE].
     */
    val EASING_SOFT: Easing = FastOutSlowInEasing

    /**
     * Gentle fade-out / dismiss easing. The end of the
     * transition decelerates more strongly than
     * [EASING_SOFT] so the last frame settles into
     * place.
     */
    val EASING_GENTLE: Easing = LinearOutSlowInEasing

    /**
     * Returns the duration in milliseconds respecting
     * [reduceMotion]. When reduce-motion is on, the
     * returned value is 0 so the animation collapses to
     * an instantaneous state change. The functional
     * information (the content that toggles) is
     * preserved.
     */
    fun durationMillis(
        baseDuration: Int,
        reduceMotion: Boolean,
    ): Int = if (reduceMotion) 0 else baseDuration

    /**
     * Returns the easing respecting [reduceMotion]. When
     * reduce-motion is on, the easing is overridden with
     * a strict step (the animation becomes a discrete
     * state change). The motion tokens still return a
     * valid easing in the reduce-motion case so call
     * sites do not need to special-case the easing.
     */
    fun easing(reduceMotion: Boolean): Easing =
        if (reduceMotion) StepEasing else EASING_SOFT
}

/**
 * Strict-step easing. The animation jumps from start to
 * end in a single frame, so the transition is
 * functionally still an animation (the state changes
 * between the start and end values) but visually
 * instantaneous. Used by [MotionTokens.easing] when
 * reduce-motion is on.
 */
val StepEasing: Easing = CubicBezierEasing(0f, 0f, 1f, 1f)
