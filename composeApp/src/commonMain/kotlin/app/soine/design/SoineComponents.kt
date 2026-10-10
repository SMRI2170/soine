package app.soine.design

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import app.soine.accessibility.AccessibilityPolicy

/**
 * Primary CTA. One per screen at most.
 *
 * The button is filled with the cream-derived [SoineColors.glow]
 * accent, sits on the midnight background, and uses a 56dp height
 * so it stays a comfortable thumb-press at large text scales. The
 * corner radius matches [SoineTokens.RadiusMd] so the button feels
 * soft without becoming a pill.
 *
 * Every [SoinePrimaryButton] must expose its action through
 * `contentDescription` so a future screen reader test can verify
 * the contract without rendering the composable.
 */
@Composable
fun SoinePrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = text,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(SoineTokens.RadiusMd),
        colors = ButtonDefaults.buttonColors(
            containerColor = SoineColors.glow,
            contentColor = SoineColors.midnight,
            disabledContainerColor = SoineColors.dim,
            disabledContentColor = SoineColors.soft,
        ),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = SoineTokens.PrimaryCtaHeight)
            .semantics { this.contentDescription = contentDescription },
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Quiet action — text-only with a subtle background. Used for
 * secondary paths like "設定" or "もどる" that must remain
 * discoverable but should not compete with the primary CTA.
 */
@Composable
fun SoineQuietButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = text,
    enabled: Boolean = true,
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(SoineTokens.RadiusSm),
        modifier = modifier
            .heightIn(min = SoineTokens.QuietCtaHeight)
            .semantics { this.contentDescription = contentDescription },
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = SoineColors.hush,
        )
    }
}

/**
 * Surface for grouping related content. The panel uses a tonal
 * elevation (a slightly lighter surface) instead of a border so the
 * hierarchy reads at a glance under low light.
 *
 * Replaces `Card(modifier = ..., shape = RoundedCornerShape(...))`
 * for non-interactive grouping. Interactive surfaces should still
 * use [androidx.compose.material3.Card] or a Clickable wrapper
 * because this panel is not focusable.
 */
@Composable
fun SoinePanel(
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(SoineTokens.RadiusLg),
        color = SoineColors.dusk,
        tonalElevation = SoineTokens.ElevationToneMid,
        contentColor = SoineColors.dusk,
    ) {
        Box(
            modifier = Modifier.padding(SoineTokens.SpacingLg),
            contentAlignment = contentAlignment,
        ) {
            content()
        }
    }
}

/**
 * Section header — short label above a panel or list section.
 * Uses [SoineColors.hush] so it does not compete with the body
 * content below it.
 */
@Composable
fun SoineSectionHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = SoineColors.hush,
        modifier = modifier,
    )
}

/**
 * Inline row of two quiet buttons. Used for the "もどる / 次へ"
 * pair in the onboarding flow.
 */
@Composable
fun SoineQuietButtonRow(
    primaryText: String,
    primaryContentDescription: String,
    onPrimaryClick: () -> Unit,
    secondaryText: String? = null,
    secondaryContentDescription: String? = null,
    onSecondaryClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(SoineTokens.SpacingSm),
    ) {
        if (secondaryText != null && onSecondaryClick != null) {
            SoineQuietButton(
                text = secondaryText,
                onClick = onSecondaryClick,
                contentDescription = secondaryContentDescription ?: secondaryText,
                modifier = Modifier.weight(1f),
            )
        } else {
            Spacer(modifier = Modifier.width(0.dp))
        }
        Button(
            onClick = onPrimaryClick,
            enabled = true,
            shape = RoundedCornerShape(SoineTokens.RadiusMd),
            colors = ButtonDefaults.buttonColors(
                containerColor = SoineColors.glow,
                contentColor = SoineColors.midnight,
            ),
            modifier = Modifier
                .weight(1f)
                .heightIn(min = SoineTokens.PrimaryCtaHeight)
                .semantics { this.contentDescription = primaryContentDescription },
        ) {
            Text(
                text = primaryText,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

/**
 * Visual divider on the dusk background. A hairline of the
 * [SoineColors.divider] color.
 */
@Composable
fun SoineDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(SoineTokens.Divider),
    ) {
        Surface(
            color = SoineColors.divider,
            contentColor = Color.Transparent,
            modifier = Modifier.fillMaxWidth(),
        ) {}
    }
}
