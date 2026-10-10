package app.soine.design

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Soine固有の [MaterialTheme] wrapper. Use this in place of the
 * raw [MaterialTheme] so the bedtime / sleeping / morning surfaces
 * share one color scheme, one type scale, and one set of
 * component defaults.
 *
 * The theme is dusk-first by design. A future light-mode pass will
 * add a second scheme without changing call sites.
 */
@Composable
fun SoineTheme(
    content: @Composable () -> Unit,
) {
    val colorScheme = darkColorScheme(
        primary = SoineColors.glow,
        onPrimary = SoineColors.midnight,
        secondary = SoineColors.cream,
        onSecondary = SoineColors.midnight,
        tertiary = SoineColors.sunrise,
        onTertiary = SoineColors.midnight,
        background = SoineColors.midnight,
        onBackground = SoineColors.dusk,
        surface = SoineColors.dusk,
        onSurface = SoineColors.cream,
        surfaceVariant = SoineColors.twilight,
        onSurfaceVariant = SoineColors.hush,
        outline = SoineColors.divider,
        error = SoineColors.ember,
        onError = SoineColors.midnight,
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SoineTypography.default,
        content = content,
    )
}
