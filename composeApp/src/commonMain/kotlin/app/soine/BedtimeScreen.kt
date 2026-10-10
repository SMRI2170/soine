package app.soine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.soine.accessibility.AccessibilityPolicy
import app.soine.companion.CompanionIntent
import app.soine.companion.CompanionRenderRequest
import app.soine.companion.CompanionStaticFallback
import app.soine.companion.CompanionRelationshipStage
import app.soine.design.SoineColors
import app.soine.design.SoinePanel
import app.soine.design.SoinePrimaryButton
import app.soine.design.SoineQuietButton
import app.soine.design.SoineSectionHeader
import app.soine.design.SoineTokens
import app.soine.sleep.SleepState

/**
 * V1 bedtime screen — the signature moment of the app.
 *
 * The screen is a single vertical column that puts the companion
 * front and center. The hierarchy is:
 *
 *   1. Top navigation (small, quiet)
 *   2. Hero companion scene with a soft glow
 *   3. Headline + body
 *   4. Compact ambient sound / sleep timer status
 *   5. Primary CTA
 *
 * The screen honors [AccessibilityPolicy.MIN_TOUCH_TARGET_DP] for
 * every interactive control and exposes the primary CTA through
 * [AccessibilityPolicy.SLEEP_START_CONTENT_DESCRIPTION]. The
 * screen stays reachable in one tap to the primary action, and
 * in two taps to Dream Album / Settings.
 */
@Composable
fun BedtimeScreen(
    onStartSleep: () -> Unit,
    onOpenDreamAlbum: () -> Unit,
    onOpenSettings: () -> Unit,
    ambientSoundLabel: String,
    defaultTimerLabel: String,
    reduceMotion: Boolean,
    relationshipStage: CompanionRelationshipStage = CompanionRelationshipStage.NEW,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = SoineTokens.SpacingLg)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(SoineTokens.SpacingMd))
        TopNavigation(
            onOpenDreamAlbum = onOpenDreamAlbum,
            onOpenSettings = onOpenSettings,
        )
        Spacer(Modifier.height(SoineTokens.SpacingXl))

        HeroCompanionScene(
            relationshipStage = relationshipStage,
            reduceMotion = reduceMotion,
        )
        Spacer(Modifier.height(SoineTokens.SpacingLg))

        Text(
            text = "今日も一緒に眠ろう",
            style = MaterialTheme.typography.headlineSmall,
            color = SoineColors.cream,
        )
        Spacer(Modifier.height(SoineTokens.SpacingXs))
        Text(
            text = "音や計測を設定しなくても、そのまま始められます",
            style = MaterialTheme.typography.bodyMedium,
            color = SoineColors.hush,
        )
        Spacer(Modifier.height(SoineTokens.SpacingLg))

        CompactStatusRow(
            ambientSoundLabel = ambientSoundLabel,
            timerLabel = defaultTimerLabel,
        )
        Spacer(Modifier.height(SoineTokens.SpacingLg))

        SoinePrimaryButton(
            text = "一緒に寝る",
            onClick = onStartSleep,
            contentDescription = AccessibilityPolicy.SLEEP_START_CONTENT_DESCRIPTION,
        )
        Spacer(Modifier.height(SoineTokens.SpacingXl))
    }
}

@Composable
private fun TopNavigation(
    onOpenDreamAlbum: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "soine",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            color = SoineColors.cream,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(SoineTokens.SpacingXs)) {
            SoineQuietButton(
                text = "夢のアルバム",
                onClick = onOpenDreamAlbum,
                contentDescription = AccessibilityPolicy.BEDTIME_DREAM_ALBUM_CONTENT_DESCRIPTION,
            )
            SoineQuietButton(
                text = "設定",
                onClick = onOpenSettings,
                contentDescription = AccessibilityPolicy.BEDTIME_SETTINGS_CONTENT_DESCRIPTION,
            )
        }
    }
}

/**
 * The hero companion scene. The companion sits on a soft glow that
 * fades from cream at the center to midnight at the edges. The
 * scene's exact size and the relationship-stage variation are
 * pinned by the design system. The size grows in proportion to
 * the relationship stage: a new companion sits at the V1 default,
 * a deeper relationship stage makes the companion feel closer.
 */
@Composable
private fun HeroCompanionScene(
    relationshipStage: CompanionRelationshipStage,
    reduceMotion: Boolean,
) {
    val sceneSize = heroSceneSize(relationshipStage)
    Box(
        modifier = Modifier.size(sceneSize),
        contentAlignment = Alignment.Center,
    ) {
        // Soft glow behind the companion. The brush fades from
        // cream (at the center) to midnight (at the edges) so the
        // scene reads as the warm anchor of the screen.
        Surface(
            modifier = Modifier
                .size(sceneSize)
                .alpha(0.7f),
            shape = CircleShape,
            color = Color.Transparent,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .semantics { contentDescription = "" },
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = CircleShape,
                    color = Color.Transparent,
                ) {
                    androidx.compose.foundation.Canvas(
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        val center = androidx.compose.ui.geometry.Offset(
                            x = size.width / 2f,
                            y = size.height / 2f,
                        )
                        val radius = size.minDimension / 2f
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    SoineColors.cream.copy(alpha = 0.20f),
                                    SoineColors.cream.copy(alpha = 0.0f),
                                ),
                                center = center,
                                radius = radius,
                            ),
                            radius = radius,
                            center = center,
                        )
                    }
                }
            }
        }
        CompanionSceneContent(
            state = SleepState.READY,
            reduceMotion = reduceMotion,
        )
    }
}

/**
 * Pinned size for the hero companion. The relationship stage
 * adjusts the visual size so a closer relationship feels closer
 * to the camera.
 */
private fun heroSceneSize(relationshipStage: CompanionRelationshipStage) = when (relationshipStage) {
    CompanionRelationshipStage.NEW -> 220.dp
    CompanionRelationshipStage.WARMING_UP -> 230.dp
    CompanionRelationshipStage.FAMILIAR -> 250.dp
    CompanionRelationshipStage.CLOSE -> 280.dp
}

/**
 * Compact status row — ambient sound on the left, sleep timer on
 * the right, in a single horizontal [SoinePanel]. The card is
 * intentionally short so the user can take the status in at a
 * glance and reach the primary CTA without scrolling on a 6.1"
 * phone at the default text scale.
 */
@Composable
private fun CompactStatusRow(
    ambientSoundLabel: String,
    timerLabel: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(SoineTokens.SpacingSm),
    ) {
        SoinePanel(
            modifier = Modifier.weight(1f),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(SoineTokens.SpacingXxs)) {
                SoineSectionHeader("環境音")
                Text(
                    text = ambientSoundLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SoineColors.cream,
                )
            }
        }
        SoinePanel(
            modifier = Modifier.weight(1f),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(SoineTokens.SpacingXxs)) {
                SoineSectionHeader("スリープタイマー")
                Text(
                    text = timerLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SoineColors.cream,
                )
            }
        }
    }
}

/**
 * Companion scene content. Pulled out of [BedtimeScreen] so the
 * layout can swap it for a 3D scene when the renderer EPIC lands
 * without touching the bedtime screen itself.
 */
@Composable
internal fun CompanionSceneContent(
    state: SleepState,
    reduceMotion: Boolean,
    intentOverride: CompanionIntent? = null,
) {
    val intent = intentOverride ?: when (state) {
        SleepState.SLEEPING -> CompanionIntent.SLEEP
        SleepState.FINISHED -> CompanionIntent.WAKE
        SleepState.READY -> CompanionIntent.IDLE
    }
    val presentation = CompanionStaticFallback.forRequest(
        CompanionRenderRequest(
            intent = intent,
            reduceMotion = reduceMotion,
        ),
    )
    val artwork = presentation.artwork
    Surface(
        modifier = Modifier
            .size(width = 190.dp, height = 150.dp)
            .semantics { contentDescription = artwork.contentDescription },
        shape = androidx.compose.foundation.shape.RoundedCornerShape(64.dp),
        color = SoineColors.dusk,
        tonalElevation = SoineTokens.ElevationToneMid,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                artwork.glyph,
                style = MaterialTheme.typography.displaySmall,
                color = SoineColors.cream,
            )
        }
    }
}
