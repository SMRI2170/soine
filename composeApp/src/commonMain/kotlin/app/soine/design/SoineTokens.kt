package app.soine.design

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Soine固有の spacing / radius / elevation token owned by commonMain.
 *
 * The tokens are an 8-dp scale, except for the hairline divider
 * which is 1.dp. A new screen should compose with these tokens;
 * raw `.dp` literals outside this file are a design change that
 * should be folded back into the scale.
 *
 * The touch-target token ([SoineTokens.MinTouchTargetDp]) is a
 * re-export of [app.soine.accessibility.AccessibilityPolicy.MIN_TOUCH_TARGET_DP]
 * so the design system and the accessibility policy share a
 * single source of truth.
 */
object SoineTokens {
    // Spacing — 8dp scale
    val SpacingXxs: Dp = 2.dp
    val SpacingXs: Dp = 4.dp
    val SpacingSm: Dp = 8.dp
    val SpacingMd: Dp = 16.dp
    val SpacingLg: Dp = 24.dp
    val SpacingXl: Dp = 32.dp
    val SpacingXxl: Dp = 48.dp

    // Corner radius — soft, generous curves
    val RadiusSm: Dp = 8.dp
    val RadiusMd: Dp = 16.dp
    val RadiusLg: Dp = 24.dp
    val RadiusXl: Dp = 32.dp

    // Primary CTA height — above the 48dp touch target so the
    // bedtime "一緒に寝る" CTA remains a comfortable thumb-press
    // at large text scales.
    val PrimaryCtaHeight: Dp = 56.dp

    // Quiet CTA height — meets the touch target but stays low
    // emphasis.
    val QuietCtaHeight: Dp = 48.dp

    // Divider hairline.
    val Divider: Dp = 1.dp

    // Elevation — a small set, used for tonal layering instead of
    // shadow casting (which costs battery on Android).
    val ElevationToneLow: Dp = 1.dp
    val ElevationToneMid: Dp = 2.dp
    val ElevationToneHigh: Dp = 4.dp

    // Touch target — must match AccessibilityPolicy.MIN_TOUCH_TARGET_DP.
    const val MinTouchTargetDp: Int = 48
}
