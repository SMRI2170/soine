package app.soine

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import app.soine.companion.CompanionRelationshipStage
import app.soine.companion.CompanionStagePresentationPolicy
import app.soine.design.SoineColors
import app.soine.design.SoinePrimaryButton
import app.soine.design.SoineQuietButton
import app.soine.design.SoineSectionHeader
import app.soine.design.SoineTokens
import app.soine.sleep.SleepSession
import app.soine.sleep.SleepState
import kotlinx.coroutines.delay

/**
 * V1 sleeping screen — the bedside scene.
 *
 * The screen is intentionally quiet. Three rules drive the layout:
 *
 *   1. The companion is the hero. The wake CTA is always reachable.
 *   2. The secondary controls (audio toggle, timer, status) auto-hide
 *      once the BREATHE intent lands. A single tap on the scene
 *      brings them back.
 *   3. The screen respects reduce-motion. The auto-hide fade is the
 *      only motion; the layout itself stays still.
 *
 * The screen shares [CompanionSceneContent] with the bedtime and
 * morning screens so a future 3D renderer can replace the static
 * fallback in one place.
 */
@Composable
fun SleepingScreen(
    session: SleepSession,
    onWake: () -> Unit,
    onOpenSettings: () -> Unit,
    ambientSoundLabel: String,
    audioPlaying: Boolean,
    remainingTimerLabel: String?,
    onToggleAudio: () -> Unit,
    onSetTimer: (Int) -> Unit,
    onCancelTimer: () -> Unit,
    reduceMotion: Boolean,
    companionIntent: CompanionIntent?,
    quietUi: Boolean,
    relationshipStage: CompanionRelationshipStage = CompanionRelationshipStage.NEW,
    autoHideAfterMillis: Long = AUTO_HIDE_AFTER_MILLIS,
) {
    var timerDialog by remember { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(!quietUi) }
    val sceneRevealInteractionLabel = AccessibilityPolicy.SLEEPING_SCENE_REVEAL_CONTENT_DESCRIPTION

    // After a quiet period, fade the secondary controls back off.
    // The wake CTA stays anchored at the bottom of the column so
    // the user always has the path to morning. The fade is the
    // only motion; the layout itself is static.
    LaunchedEffect(controlsVisible, autoHideAfterMillis, reduceMotion) {
        if (reduceMotion || autoHideAfterMillis <= 0L) {
            return@LaunchedEffect
        }
        if (!controlsVisible) return@LaunchedEffect
        delay(autoHideAfterMillis)
        controlsVisible = false
    }

    val isRevealed = controlsVisible
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SoineColors.midnight)
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null,
                onClickLabel = sceneRevealInteractionLabel,
            ) {
                controlsVisible = true
            }
            .semantics { contentDescription = sceneRevealInteractionLabel },
    ) {
        // Hero scene — the companion, centered, with a soft glow.
        HeroCompanion(
            reduceMotion = reduceMotion,
            companionIntent = companionIntent,
            relationshipStage = relationshipStage,
        )

        // Top status row — a single line of small status indicators.
        // Visible in both quiet and revealed states; the row is
        // intentionally short so it does not compete with the
        // Top status row — two compact indicators (audio + timer).
        // The Box.align is applied at the call site so the function
        // can be reused outside the Box scope in future polish slices.
        StatusIndicator(
            audioPlaying = audioPlaying,
            remainingTimerLabel = remainingTimerLabel,
            isVisible = isRevealed,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = SoineTokens.SpacingLg),
        )

        // Bottom wake CTA. Always anchored. Always reachable in
        // one tap. The "quiet" version is muted; the revealed
        // version uses the full accent.
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = SoineTokens.SpacingLg)
                .padding(bottom = SoineTokens.SpacingLg),
            verticalArrangement = Arrangement.spacedBy(SoineTokens.SpacingSm),
        ) {
            AnimatedVisibility(
                visible = isRevealed,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                SecondaryControls(
                    onOpenSettings = onOpenSettings,
                    onToggleAudio = onToggleAudio,
                    onSetTimer = { timerDialog = true },
                    onCancelTimer = onCancelTimer,
                    audioPlaying = audioPlaying,
                    ambientSoundLabel = ambientSoundLabel,
                    remainingTimerLabel = remainingTimerLabel,
                )
            }
            SoinePrimaryButton(
                text = "起きる",
                onClick = onWake,
                contentDescription = AccessibilityPolicy.WAKE_CONTENT_DESCRIPTION,
            )
        }
    }

    if (timerDialog) {
        SleepTimerDialog(
            onDismiss = { timerDialog = false },
            onSetTimer = {
                timerDialog = false
                onSetTimer(it)
            },
        )
    }
}

@Composable
private fun HeroCompanion(
    reduceMotion: Boolean,
    companionIntent: CompanionIntent?,
    relationshipStage: CompanionRelationshipStage,
) {
    val presentation = CompanionStagePresentationPolicy.forStage(relationshipStage)
    val glowAlpha = presentation.sleepingAccentAlpha
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        // Soft glow behind the companion. The fade from cream to
        // midnight echoes the bedtime scene but stays still. The
        // alpha is stage-gated so a closer relationship warms the
        // bedside scene without changing the camera.
        Surface(
            modifier = Modifier
                .size(360.dp)
                .alpha(0.5f),
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
                                    SoineColors.cream.copy(alpha = glowAlpha),
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
            state = SleepState.SLEEPING,
            reduceMotion = reduceMotion,
            intentOverride = companionIntent,
        )
    }
}

@Composable
private fun StatusIndicator(
    audioPlaying: Boolean,
    remainingTimerLabel: String?,
    isVisible: Boolean,
    modifier: Modifier = Modifier,
) {
    // Two compact indicators in a top row. Each is a single dot
    // with a label. The label is the screen-reader content
    // description; the visual is a small filled circle.
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = SoineTokens.MinTouchTargetDp.dp)
                .padding(horizontal = SoineTokens.SpacingMd),
            horizontalArrangement = Arrangement.spacedBy(SoineTokens.SpacingSm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusDot(
                label = if (audioPlaying) "環境音 ON" else "環境音 OFF",
            )
            remainingTimerLabel?.let { label ->
                StatusDot(label = label)
            }
        }
    }
}

@Composable
private fun StatusDot(label: String) {
    Row(
        modifier = Modifier
            .heightIn(min = SoineTokens.MinTouchTargetDp.dp)
            .padding(horizontal = SoineTokens.SpacingSm)
            .semantics { contentDescription = label },
        horizontalArrangement = Arrangement.spacedBy(SoineTokens.SpacingXs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(8.dp),
            shape = CircleShape,
            color = SoineColors.glow,
            contentColor = SoineColors.midnight,
        ) {}
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = SoineColors.cream,
        )
    }
}

@Composable
private fun SecondaryControls(
    onOpenSettings: () -> Unit,
    onToggleAudio: () -> Unit,
    onSetTimer: () -> Unit,
    onCancelTimer: () -> Unit,
    audioPlaying: Boolean,
    ambientSoundLabel: String,
    remainingTimerLabel: String?,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SoineTokens.SpacingXs),
    ) {
        SoineSectionHeader("環境音: " + ambientSoundLabel)
        if (remainingTimerLabel != null) {
            SoineSectionHeader("タイマー: " + remainingTimerLabel)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SoineTokens.SpacingSm),
        ) {
            SoineQuietButton(
                text = if (audioPlaying) "一時停止" else "再生",
                onClick = onToggleAudio,
                contentDescription = AccessibilityPolicy.SLEEPING_AUDIO_TOGGLE_CONTENT_DESCRIPTION,
                modifier = Modifier.weight(1f),
            )
            SoineQuietButton(
                text = "タイマー",
                onClick = onSetTimer,
                contentDescription = AccessibilityPolicy.SLEEPING_TIMER_CONTENT_DESCRIPTION,
                modifier = Modifier.weight(1f),
            )
            SoineQuietButton(
                text = "設定",
                onClick = onOpenSettings,
                contentDescription = AccessibilityPolicy.SLEEPING_SETTINGS_CONTENT_DESCRIPTION,
                modifier = Modifier.weight(1f),
            )
        }
        if (remainingTimerLabel != null) {
            SoineQuietButton(
                text = "タイマーを解除",
                onClick = onCancelTimer,
                contentDescription = AccessibilityPolicy.SLEEPING_TIMER_CANCEL_CONTENT_DESCRIPTION,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * The duration the secondary controls stay visible after a
 * tap-to-reveal. After this period, the controls auto-hide so
 * the screen returns to the bedside scene. 6 seconds is short
 * enough to feel quiet, long enough to read the labels.
 */
const val AUTO_HIDE_AFTER_MILLIS: Long = 6_000L

@Composable
internal fun SleepTimerDialog(
    onDismiss: () -> Unit,
    onSetTimer: (Int) -> Unit,
) {
    var custom by remember { mutableStateOf("") }
    val parsed = custom.toIntOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("スリープタイマー") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(30, 60, 90).forEach { minutes ->
                        OutlinedButton(
                            onClick = { onSetTimer(minutes) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = SoineTokens.MinTouchTargetDp.dp),
                        ) {
                            Text(minutes.toString() + "分")
                        }
                    }
                }
                OutlinedTextField(
                    value = custom,
                    onValueChange = { custom = it.filter(Char::isDigit).take(4) },
                    label = { Text("カスタム（分）") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { parsed?.takeIf { it > 0 }?.let(onSetTimer) },
                enabled = parsed != null && parsed > 0,
            ) { Text("設定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("キャンセル") } },
    )
}
