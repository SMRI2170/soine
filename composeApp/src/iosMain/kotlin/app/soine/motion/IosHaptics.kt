package app.soine.motion

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle

/**
 * iOS implementation of [Haptics] backed by
 * [UIImpactFeedbackGenerator]. The generator is
 * created on the first call and cached for the
 * lifetime of the [IosHaptics] instance; the iOS
 * haptic engine keeps the generator warm so a tap-
 * to-haptic latency stays below the 100 ms threshold
 * users perceive as instant.
 *
 * The two intensities map to the standard iOS
 * feedback styles:
 *
 *   - [HapticsIntensity.Light] uses
 *     [UIImpactFeedbackStyle.Light] — a short, soft
 *     confirmation tick. Used for the sleep-start
 *     and wake moments.
 *   - [HapticsIntensity.Medium] uses
 *     [UIImpactFeedbackStyle.Medium] — a longer
 *     confirmation pulse. Used for the
 *     dream-discovery and stage-advance moments.
 *
 * The generator respects the device's haptic setting
 * (Settings → Sounds & Haptics → System Haptics).
 * A user who has disabled System Haptics does not
 * feel a vibration even on a meaningful moment. This
 * is the right tradeoff.
 */
class IosHaptics : Haptics {
    private val lightGenerator: UIImpactFeedbackGenerator =
        UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleLight)
    private val mediumGenerator: UIImpactFeedbackGenerator =
        UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleMedium)

    override fun perform(intensity: HapticsIntensity) {
        when (intensity) {
            HapticsIntensity.Light -> lightGenerator.impactOccurred()
            HapticsIntensity.Medium -> mediumGenerator.impactOccurred()
        }
    }
}

@Composable
actual fun rememberHaptics(): Haptics = remember { IosHaptics() }
