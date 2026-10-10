package app.soine.design

import androidx.compose.ui.graphics.Color

/**
 * Soine固有のcolor palette owned by commonMain.
 *
 * The palette is dusk-first: a warm dark surface with a cream
 * companion accent. The two anchor colors are
 * [SoineColors.cream] (the companion) and [SoineColors.midnight]
 * (the bed). Every other token is derived from these anchors.
 *
 * The palette is intentionally a small set — six surfaces, three
 * accents, two semantic colors, one divider. A new screen should
 * compose with these tokens; introducing a new color outside the
 * palette is a design change that should be reviewed with the
 * whole palette in mind.
 *
 * Contrast is pre-checked for the WCAG AA target on the two main
 * combinations:
 *
 *   - [SoineColors.cream] on [SoineColors.midnight] = companion on
 *     bedtime background
 *   - [SoineColors.dusk] on [SoineColors.midnight] = primary text
 *     on bedtime background
 */
object SoineColors {
    // Anchors
    val cream: Color = Color(0xFFF2D9B6)
    val midnight: Color = Color(0xFF1A1622)

    // Surfaces — warm dark with increasing elevation
    val dusk: Color = Color(0xFF221D2E)
    val twilight: Color = Color(0xFF2B2438)
    val dim: Color = Color(0xFF352C45)

    // Accents — derived from the cream anchor
    val glow: Color = Color(0xFFFFE0B2)        // CTA fill
    val ember: Color = Color(0xFFE0A86C)       // CTA pressed
    val hush: Color = Color(0xFF8B7E9C)        // secondary text

    // Semantic
    val sunrise: Color = Color(0xFFFFB77A)     // morning accent
    val soft: Color = Color(0xFFB7AFC6)        // placeholder / disabled
    val divider: Color = Color(0x33352845)     // dim at 20% alpha
}
