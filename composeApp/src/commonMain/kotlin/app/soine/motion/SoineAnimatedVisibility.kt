package app.soine.motion

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Animated visibility with the Soine motion language.
 *
 * Wraps the Material 3 [AnimatedVisibility] with the
 * V1 motion tokens so the bedtime / sleeping / morning
 * screens share the same fade timing and easing. The
 * wrapper accepts a [reduceMotion] flag; when it is
 * `true`, every duration collapses to 0 so the
 * transition becomes a single instantaneous state
 * change. The functional information (the content
 * that toggles) is preserved.
 *
 * The default enter / exit is a 240 ms fade. For a
 * vertical-collapse transition (e.g. the secondary
 * controls on the sleeping screen), call sites can
 * override with [expandVertically] / [shrinkVertically]
 * and the wrapper will apply the same duration /
 * easing to those transitions.
 */
@Composable
fun SoineAnimatedVisibility(
    visible: Boolean,
    modifier: Modifier = Modifier,
    reduceMotion: Boolean = false,
    enter: EnterTransition = fadeIn(
        animationSpec = tween(
            durationMillis = MotionTokens.durationMillis(
                MotionTokens.DURATION_NORMAL,
                reduceMotion,
            ),
            easing = MotionTokens.easing(reduceMotion),
        ),
    ),
    exit: ExitTransition = fadeOut(
        animationSpec = tween(
            durationMillis = MotionTokens.durationMillis(
                MotionTokens.DURATION_NORMAL,
                reduceMotion,
            ),
            easing = MotionTokens.easing(reduceMotion),
        ),
    ),
    label: String = "SoineAnimatedVisibility",
    content: @Composable AnimatedVisibilityScope.() -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = enter,
        exit = exit,
        label = label,
        content = content,
    )
}

/**
 * Vertical-collapsing animated visibility with the
 * Soine motion language. Use for the secondary controls
 * on the sleeping screen and similar surfaces that
 * reveal and dismiss in a vertical direction.
 */
@Composable
fun SoineAnimatedVisibilityVertical(
    visible: Boolean,
    modifier: Modifier = Modifier,
    reduceMotion: Boolean = false,
    label: String = "SoineAnimatedVisibilityVertical",
    content: @Composable AnimatedVisibilityScope.() -> Unit,
) {
    val duration = MotionTokens.durationMillis(
        MotionTokens.DURATION_NORMAL,
        reduceMotion,
    )
    val easing = MotionTokens.easing(reduceMotion)
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = expandVertically(
            animationSpec = tween(durationMillis = duration, easing = easing),
        ) + fadeIn(
            animationSpec = tween(durationMillis = duration, easing = easing),
        ),
        exit = shrinkVertically(
            animationSpec = tween(durationMillis = duration, easing = easing),
        ) + fadeOut(
            animationSpec = tween(durationMillis = duration, easing = easing),
        ),
        label = label,
        content = content,
    )
}
