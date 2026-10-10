package app.soine.motion

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

/**
 * Android implementation of [Haptics] backed by
 * [HapticFeedbackConstants]. The view's
 * [View.performHapticFeedback] is the platform entry
 * point so the haptic respects the device's "touch
 * feedback" system setting.
 *
 * The two intensities map to the standard Android
 * constants:
 *
 *   - [HapticsIntensity.Light] uses
 *     [HapticFeedbackConstants.CONTEXT_CLICK] — a soft
 *     confirmation tick. The user's device's haptic
 *     engine produces a short, low-amplitude click.
 *   - [HapticsIntensity.Medium] uses
 *     [HapticFeedbackConstants.LONG_PRESS] — a longer
 *     confirmation pulse. Used for the
 *     dream-discovery and stage-advance moments.
 *
 * The `FLAG_IGNORE_GLOBAL_SETTING` is intentionally
 * NOT set; the system setting (Settings → Sounds →
 * Touch feedback) gates every haptic. A user who has
 * disabled touch feedback does not feel a vibration
 * even on a meaningful moment. This is the right
 * tradeoff: a user who has explicitly disabled haptics
 * has a stronger signal than the app's notion of
 * "meaningful moment".
 */
class AndroidHaptics(
    private val view: View,
) : Haptics {
    override fun perform(intensity: HapticsIntensity) {
        val constant = when (intensity) {
            HapticsIntensity.Light -> HapticFeedbackConstants.CONTEXT_CLICK
            HapticsIntensity.Medium -> HapticFeedbackConstants.LONG_PRESS
        }
        view.performHapticFeedback(constant)
    }
}

@Composable
actual fun rememberHaptics(): Haptics {
    val view = LocalView.current
    return remember(view) { AndroidHaptics(view) }
}
