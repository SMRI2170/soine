package app.soine.motion

import androidx.compose.runtime.Composable

/**
 * Platform-agnostic haptic feedback.
 *
 * #177 requires haptics only at meaningful moments
 * (sleep start, wake, dream discovery) — never on every
 * button press, never on every state change. The
 * [Haptics] interface is the boundary; the platform
 * impl lives in `androidMain` and `iosMain`.
 *
 * The two strengths are:
 *
 *   - [HapticsIntensity.Light] for confirmations (sleep
 *     start, wake, button press on the primary CTA).
 *     The Android implementation uses
 *     `HapticFeedbackConstants.CONTEXT_CLICK`; the iOS
 *     implementation uses `UIImpactFeedbackGenerator`
 *     with `.light`.
 *   - [HapticsIntensity.Medium] for discoveries (a new
 *     dream, a stage advance). The Android implementation
 *     uses `HapticFeedbackConstants.LONG_PRESS`; the iOS
 *     implementation uses `UIImpactFeedbackGenerator`
 *     with `.medium`.
 *
 * The interface is intentionally narrow. A new intensity
 * must come with a documented meaning; the call site must
 * pick the right intensity for the moment, not a
 * one-size-fits-all default.
 */
interface Haptics {
    fun perform(intensity: HapticsIntensity)
}

/**
 * The two haptic intensities the V1 ships. New
 * intensities (e.g. `Selection`) can be added; the
 * platform impls must extend to support them.
 */
enum class HapticsIntensity {
    Light,
    Medium,
}

/**
 * No-op haptics for tests and the desktop / JVM target.
 * The test impl never produces a vibration, so a
 * test that needs to assert "haptic fired" can use
 * a [RecordingHaptics] instead.
 */
object NoOpHaptics : Haptics {
    override fun perform(intensity: HapticsIntensity) {
        // Intentionally no-op.
    }
}

/**
 * Recording haptics for tests. Each [perform] call
 * appends the intensity to a list. Tests can assert
 * against the recorded list.
 */
class RecordingHaptics : Haptics {
    private val _events: MutableList<HapticsIntensity> = mutableListOf()
    val events: List<HapticsIntensity> get() = _events.toList()
    override fun perform(intensity: HapticsIntensity) {
        _events.add(intensity)
    }
    fun reset() {
        _events.clear()
    }
}

/**
 * Returns the platform-bound [Haptics] implementation.
 * The Android source set uses
 * [HapticFeedbackConstants][android.view.HapticFeedbackConstants];
 * the iOS source set uses
 * [UIImpactFeedbackGenerator][platform.UIKit.UIImpactFeedbackGenerator].
 * The desktop / JVM source set falls back to [NoOpHaptics].
 */
@Composable
expect fun rememberHaptics(): Haptics
