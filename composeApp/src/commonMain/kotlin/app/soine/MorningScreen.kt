package app.soine

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
import app.soine.companion.CompanionRelationshipStage
import app.soine.companion.CompanionStagePresentationPolicy
import app.soine.design.SoineColors
import app.soine.design.SoinePanel
import app.soine.design.SoinePrimaryButton
import app.soine.design.SoineSectionHeader
import app.soine.design.SoineTokens
import app.soine.dream.DreamDiscovery
import app.soine.sleep.SleepSession
import app.soine.sleep.SleepState
import app.soine.sleep.summary
import app.soine.sound.SoundEventSummary
import app.soine.sound.userFacingLine

/**
 * V1 morning screen — the "昨夜を発見する" reveal.
 *
 * The screen follows the Emotion → Memory → Data order that
 * #171 calls out. The companion is the hero; the night memory
 * and dream discovery are the next layer; the sleep duration
 * is a small summary card at the bottom.
 *
 * The screen shares [CompanionSceneContent] with the bedtime
 * and sleeping screens so a future 3D renderer can replace
 * the static fallback in one place.
 */
@Composable
fun MorningScreen(
    session: SleepSession,
    nightMemoryEntries: List<NightMemoryEntry>,
    reduceMotion: Boolean,
    onDone: () -> Unit,
    dreamDiscoveries: List<DreamDiscovery> = emptyList(),
    morningGreeting: String = DEFAULT_MORNING_GREETING,
    relationshipStage: CompanionRelationshipStage = CompanionRelationshipStage.NEW,
    previousStage: CompanionRelationshipStage? = null,
    soundEventSummary: SoundEventSummary? = null,
    onOpenPrivacy: () -> Unit = {},
) {
    val summary = session.summary()
    val presentation = CompanionStagePresentationPolicy.forStage(relationshipStage)
    val stageAdvancedSinceLastWake =
        previousStage != null && previousStage != relationshipStage
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = SoineTokens.SpacingLg)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(SoineTokens.SpacingMd))

        // Emotion layer: companion wake reaction. The hero glow
        // color and alpha are stage-gated so a closer
        // relationship warms the morning scene.
        MorningHero(
            reduceMotion = reduceMotion,
            relationshipStage = relationshipStage,
        )
        Spacer(Modifier.height(SoineTokens.SpacingMd))

        Text(
            text = morningGreeting + (presentation.morningGreetingSuffix ?: ""),
            style = MaterialTheme.typography.headlineSmall,
            color = SoineColors.cream,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(SoineTokens.SpacingXs))
        Text(
            text = "昨夜のふりかえり",
            style = MaterialTheme.typography.bodyMedium,
            color = SoineColors.hush,
        )
        Spacer(Modifier.height(SoineTokens.SpacingLg))

        // Quiet one-shot "stage advanced" notice. Renders
        // only when the stage has crossed since the last wake
        // and the policy has copy for the new stage. The notice
        // is a single SoinePanel line, not a celebration.
        if (stageAdvancedSinceLastWake) {
            presentation.stageAdvancedReveal?.let { revealCopy ->
                RelationshipChangeReveal(copy = revealCopy)
                Spacer(Modifier.height(SoineTokens.SpacingLg))
            }
        }

        // Dream discovery special reveal: when the user found a
        // new dream this morning, surface it before the night
        // memory so the "見つけた" feeling lands first.
        if (dreamDiscoveries.isNotEmpty()) {
            DreamDiscoveryReveal(
                count = dreamDiscoveries.size,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(SoineTokens.SpacingLg))
        }

        // Memory layer: night memory timeline.
        if (nightMemoryEntries.isNotEmpty()) {
            NightMemoryTimeline(nightMemoryEntries)
            Spacer(Modifier.height(SoineTokens.SpacingLg))
        }

        // Quiet one-line sound summary. Renders only when
        // the optional sound analysis produced events for
        // the session. The copy never makes a medical
        // claim ("寝言のような音"), never shows a
        // confidence percentage, and never references raw
        // audio. The "詳細と削除は設定から" link routes
        // to the privacy-and-data screen so the user can
        // inspect and delete the events.
        soundEventSummary?.let { summary ->
            summary.userFacingLine()?.let { line ->
                SoundSummaryReveal(
                    line = line,
                    onOpenPrivacy = onOpenPrivacy,
                )
                Spacer(Modifier.height(SoineTokens.SpacingLg))
            }
        }

        // Data layer: a compact summary card with the duration
        // and the bedtime / wake time. The duration is not the
        // hero anymore.
        SummaryCard(summary = summary)
        Spacer(Modifier.height(SoineTokens.SpacingLg))

        SoinePrimaryButton(
            text = "今日をはじめる",
            onClick = onDone,
            contentDescription = AccessibilityPolicy.MORNING_DONE_CONTENT_DESCRIPTION,
        )
        Spacer(Modifier.height(SoineTokens.SpacingXl))
    }
}

/**
 * Dream discovery special reveal. Renders a small
 * "夢を見つけた" panel above the night memory so the user feels
 * the discovery before they read the rest of the morning
 * summary. The label is intentionally short — the actual
 * discoveries are surfaced in the dream album.
 */
@Composable
private fun DreamDiscoveryReveal(
    count: Int,
    modifier: Modifier = Modifier,
) {
    SoinePanel(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(SoineTokens.SpacingXs)) {
            SoineSectionHeader("今朝の発見")
            Text(
                text = "夢を見つけた",
                style = MaterialTheme.typography.titleMedium,
                color = SoineColors.sunrise,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "$count 件の新しい夢。今日の終わりにアルバムで会いましょう。",
                style = MaterialTheme.typography.bodyMedium,
                color = SoineColors.cream,
            )
        }
    }
}

/**
 * Hero: companion wake reaction. The companion sits on a soft
 * sunrise-tinted glow that echoes the bedtime cream-tinted
 * glow but signals the new day. The glow color and alpha are
 * stage-gated so a closer relationship warms the morning scene
 * without changing the camera.
 */
@Composable
private fun MorningHero(
    reduceMotion: Boolean,
    relationshipStage: CompanionRelationshipStage,
) {
    val presentation = CompanionStagePresentationPolicy.forStage(relationshipStage)
    val glowColor = presentation.morningGlowColor
    val glowAlpha = presentation.morningGlowAlpha
    Box(
        modifier = Modifier.size(260.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .size(260.dp)
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
                                    glowColor.copy(alpha = glowAlpha),
                                    glowColor.copy(alpha = 0.0f),
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
            state = SleepState.FINISHED,
            reduceMotion = reduceMotion,
        )
    }
}

/**
 * Quiet one-shot "stage advanced" notice. Renders as a single
 * SoinePanel line above the dream discovery reveal so the
 * "少し近づけた気がする" feeling lands before the user reads
 * the rest of the morning summary. The notice is intentionally
 * short — it is not a celebration, and it does not expose a
 * number.
 */
@Composable
private fun RelationshipChangeReveal(copy: String) {
    SoinePanel(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(SoineTokens.SpacingXs)) {
            SoineSectionHeader("少しだけ、近づいた朝")
            Text(
                text = copy,
                style = MaterialTheme.typography.titleMedium,
                color = SoineColors.sunrise,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

/**
 * Quiet one-line sound summary reveal. Renders only when
 * the optional sound analysis produced events for the
 * session. The copy is intentionally short — one
 * phrase, no category list, no count, no confidence
 * percentage, no medical claim. The "詳細と削除は設定
 * から" link routes to the privacy-and-data screen so
 * the user can inspect and delete the events.
 *
 * The reveal sits between the night memory timeline and
 * the summary card so the user's eye lands on the
 * memory layer first, the sound summary second (only if
 * present), and the data layer last.
 */
@Composable
private fun SoundSummaryReveal(
    line: String,
    onOpenPrivacy: () -> Unit,
) {
    SoinePanel(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(SoineTokens.SpacingXs)) {
            SoineSectionHeader("今夜の音")
            Text(
                text = line,
                style = MaterialTheme.typography.bodyMedium,
                color = SoineColors.cream,
            )
            androidx.compose.material3.TextButton(
                onClick = onOpenPrivacy,
                modifier = Modifier
                    .heightIn(min = SoineTokens.QuietCtaHeight)
                    .align(Alignment.End),
            ) {
                Text(
                    "詳細と削除は設定から",
                    color = SoineColors.hush,
                )
            }
        }
    }
}

/**
 * Compact summary card with the sleep duration. The duration
 * is shown in `titleLarge` (smaller than the bedtime headline)
 * so it does not compete with the companion or the morning
 * greeting. The card is intentionally the last memory-layer
 * element, so the user's eye reaches the duration last.
 */
@Composable
private fun SummaryCard(summary: app.soine.sleep.SleepSummary?) {
    SoinePanel {
        Column(verticalArrangement = Arrangement.spacedBy(SoineTokens.SpacingSm)) {
            SoineSectionHeader("睡眠時間")
            Text(
                text = summary?.displayDuration() ?: "記録なし",
                style = MaterialTheme.typography.titleLarge,
                color = SoineColors.cream,
                fontWeight = FontWeight.SemiBold,
            )
            if (summary != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(SoineTokens.SpacingLg),
                ) {
                    SummaryField(label = "就寢", value = formatTime(summary.bedtimeEpochMillis))
                    SummaryField(label = "起床", value = formatTime(summary.wakeTimeEpochMillis))
                }
            }
        }
    }
}

@Composable
private fun SummaryField(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(SoineTokens.SpacingXxs)) {
        SoineSectionHeader(label)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = SoineColors.cream,
        )
    }
}

internal const val DEFAULT_MORNING_GREETING: String = "今日も一緒に起きられたね"
private const val MORNING_TIME_FORMAT_INDEX: Int = 0

private fun formatTime(epochMillis: Long): String {
    // The morning screen formats times as HH:mm through the
    // existing DisplayFormatter pipeline. We import it lazily
    // through LocalTimeZones so a future locale change moves
    // through the existing boundary.
    return app.soine.formatNightEventTime(epochMillis)
}
