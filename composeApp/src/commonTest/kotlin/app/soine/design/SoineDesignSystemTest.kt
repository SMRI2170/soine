package app.soine.design

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Smoke test for the V1 design system foundation.
 *
 * #168 ships the first slice of the Soine visual design language:
 *
 *   - [SoineColors]: a small warm-dark palette with a cream
 *     companion accent and a midnight bed background
 *   - [SoineTypography]: a night-oriented type scale
 *   - [SoineTokens]: an 8dp spacing scale, a soft corner radius
 *     set, a primary / quiet CTA height, a tonal elevation set,
 *     and a touch-target re-export
 *   - [SoinePrimaryButton] / [SoineQuietButton] / [SoinePanel] /
 *     [SoineSectionHeader] / [SoineDivider]: the V1 component
 *     surface
 *   - [SoineTheme]: a [androidx.compose.material3.MaterialTheme]
 *     wrapper that pins the bedtime color scheme
 *
 * The contract the test pins:
 *
 *   - the palette anchor colors do not move silently — a
 *     `git diff` of the wrong file does not change them
 *   - the spacing / radius / CTA / touch-target tokens stay
 *     consistent with the accessibility baseline
 *   - the typography scale is non-empty and has the styles the
 *     bedtime / sleeping / morning screens use
 *
 * Compose UI rendering is still deferred (see
 * `docs/ci-quality-policy.md`); the textual / contract checks
 * here are the deliberate host-test substitute.
 */
class SoineDesignSystemTest {

    @Test
    fun paletteAnchorsAreStable() {
        // The cream / midnight anchors define the visual identity.
        // A refactor that moves them silently would change every
        // bedtime screen, so the test pins the values.
        val creamArgb: Int = soineColorArgb(SoineColors.cream)
        val midnightArgb: Int = soineColorArgb(SoineColors.midnight)
        assertEquals(expected = 0xFFF2D9B6.toInt(), actual = creamArgb)
        assertEquals(expected = 0xFF1A1622.toInt(), actual = midnightArgb)
    }

    @Test
    fun paletteSurfacesAreWarmerTowardCompanion() {
        // The warm-dark family (midnight < dusk < twilight < dim) is
        // ordered by elevation. The companion anchor (cream) sits
        // above midnight and is warmer / lighter.
        assertTrue(
            luminance(SoineColors.midnight) < luminance(SoineColors.dusk),
            "midnight must be darker than dusk",
        )
        assertTrue(
            luminance(SoineColors.dusk) < luminance(SoineColors.twilight),
            "dusk must be darker than twilight",
        )
        assertTrue(
            luminance(SoineColors.twilight) < luminance(SoineColors.dim),
            "twilight must be darker than dim",
        )
    }

    @Test
    fun companionAnchorSitsAboveMidnightSurface() {
        // The cream / midnight contrast is the primary brand pair;
        // the test pins the relationship so a future color tweak
        // cannot flip them.
        assertNotEquals(
            luminance(SoineColors.cream),
            luminance(SoineColors.midnight),
            "cream and midnight must be different luminance",
        )
        assertTrue(
            luminance(SoineColors.cream) > luminance(SoineColors.midnight),
            "cream must be lighter than midnight",
        )
    }

    @Test
    fun spacingScaleIsEightDp() {
        assertEquals(2, SoineTokens.SpacingXxs.value.toInt())
        assertEquals(4, SoineTokens.SpacingXs.value.toInt())
        assertEquals(8, SoineTokens.SpacingSm.value.toInt())
        assertEquals(16, SoineTokens.SpacingMd.value.toInt())
        assertEquals(24, SoineTokens.SpacingLg.value.toInt())
        assertEquals(32, SoineTokens.SpacingXl.value.toInt())
        assertEquals(48, SoineTokens.SpacingXxl.value.toInt())
    }

    @Test
    fun primaryCtaHeightMeetsTouchTarget() {
        assertTrue(
            SoineTokens.PrimaryCtaHeight >= SoineTokens.MinTouchTargetDp.dp,
            "primary CTA must meet the 48dp touch target",
        )
    }

    @Test
    fun quietCtaHeightMeetsTouchTarget() {
        assertTrue(
            SoineTokens.QuietCtaHeight >= SoineTokens.MinTouchTargetDp.dp,
            "quiet CTA must meet the 48dp touch target",
        )
    }

    @Test
    fun radiusScaleIsSoft() {
        // The radius scale is small (8-32dp) so the corners are
        // soft without becoming a pill.
        assertTrue(SoineTokens.RadiusSm < SoineTokens.RadiusMd)
        assertTrue(SoineTokens.RadiusMd < SoineTokens.RadiusLg)
        assertTrue(SoineTokens.RadiusLg < SoineTokens.RadiusXl)
        assertTrue(SoineTokens.RadiusXl.value <= 32f)
    }

    @Test
    fun typographyHasAllExpectedRoles() {
        val typography = SoineTypography.default
        // The bedtime / sleeping / morning screens rely on these
        // specific roles. A future refactor that drops one of them
        // would break the screen at compile time, but the test
        // pins the contract so a re-tying cannot silently swap a
        // larger style for a smaller one.
        assertNotEquals(typography.displaySmall, typography.bodySmall)
        assertNotEquals(typography.headlineSmall, typography.titleSmall)
        assertNotEquals(typography.titleLarge, typography.titleSmall)
        assertNotEquals(typography.bodyLarge, typography.bodySmall)
        assertNotEquals(typography.labelLarge, typography.labelSmall)
    }

    @Test
    fun dividerIsHairline() {
        assertEquals(1, SoineTokens.Divider.value.toInt())
    }

    @Test
    fun typographyDisplayStyleHasGenerousLineHeight() {
        // The bedtime headline reads at arm's length under low
        // light. The line height must be at least 1.15x the font
        // size, matching the typography doc.
        val display = SoineTypography.default.displaySmall
        val ratio = display.lineHeight.value / display.fontSize.value
        assertTrue(
            ratio >= 1.15f,
            "displaySmall must have a generous line height (got $ratio)",
        )
    }

    @Test
    fun touchTargetTokenMatchesAccessibilityPolicy() {
        assertEquals(48, SoineTokens.MinTouchTargetDp)
        assertEquals(SoineTokens.MinTouchTargetDp, app.soine.accessibility.AccessibilityPolicy.MIN_TOUCH_TARGET_DP)
    }
}

private fun soineColorArgb(color: androidx.compose.ui.graphics.Color): Int {
    val a = (color.alpha * 255).toInt() and 0xFF
    val r = (color.red * 255).toInt() and 0xFF
    val g = (color.green * 255).toInt() and 0xFF
    val b = (color.blue * 255).toInt() and 0xFF
    return (a shl 24) or (r shl 16) or (g shl 8) or b
}

private fun luminance(color: androidx.compose.ui.graphics.Color): Double {
    // Use the standard relative luminance formula so the test is
    // platform-independent.
    fun toLinear(channel: Float): Double {
        val c = channel.toDouble()
        return if (c <= 0.03928) c / 12.92 else Math.pow((c + 0.055) / 1.055, 2.4)
    }
    val r = toLinear(color.red)
    val g = toLinear(color.green)
    val b = toLinear(color.blue)
    return 0.2126 * r + 0.7152 * g + 0.0722 * b
}
