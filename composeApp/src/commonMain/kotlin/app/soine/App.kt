package app.soine

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.soine.accessibility.AccessibilityPolicy
import app.soine.companion.CompanionRelationshipStage
import app.soine.design.SoinePrimaryButton
import app.soine.design.SoineQuietButton
import app.soine.design.SoineTheme
import app.soine.dream.DreamDiscovery
import app.soine.navigation.BedtimeDestination
import app.soine.companion.CompanionIntent
import app.soine.companion.CompanionRenderRequest
import app.soine.companion.CompanionStaticFallback
import app.soine.sleep.SleepSession
import app.soine.sleep.SleepSessionRecord
import app.soine.sleep.SleepState
import app.soine.sleep.summary

@Composable
fun App(
    destination: BedtimeDestination,
    onStartSleep: () -> Unit,
    onWake: () -> Unit,
    onDone: () -> Unit,
    onRetry: () -> Unit,
    onOpenDreamAlbum: () -> Unit,
    onOpenSettings: () -> Unit,
    nightMemoryEntries: List<NightMemoryEntry> = emptyList(),
    discoveries: List<DreamDiscovery> = emptyList(),
    sleepingCompanionIntent: CompanionIntent? = null,
    quietSleepUi: Boolean = false,
    reduceMotion: Boolean = false,
    ambientSoundLabel: String,
    defaultTimerLabel: String,
    audioPlaying: Boolean,
    remainingTimerLabel: String?,
    onToggleAudio: () -> Unit,
    onSetTimer: (Int) -> Unit,
    onCancelTimer: () -> Unit,
    relationshipStage: CompanionRelationshipStage = CompanionRelationshipStage.NEW,
) {
    SoineTheme {
        Surface(Modifier.fillMaxSize()) {
            when (destination) {
                BedtimeDestination.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                BedtimeDestination.Bedtime -> BedtimeScreen(
                    onStartSleep = onStartSleep,
                    onOpenDreamAlbum = onOpenDreamAlbum,
                    onOpenSettings = onOpenSettings,
                    ambientSoundLabel = ambientSoundLabel,
                    defaultTimerLabel = defaultTimerLabel,
                    reduceMotion = reduceMotion,
                    relationshipStage = relationshipStage,
                )
                is BedtimeDestination.Sleeping -> SleepingScreen(
                    session = destination.session.toUiSession(),
                    onWake = onWake,
                    onOpenSettings = onOpenSettings,
                    ambientSoundLabel = ambientSoundLabel,
                    audioPlaying = audioPlaying,
                    remainingTimerLabel = remainingTimerLabel,
                    onToggleAudio = onToggleAudio,
                    onSetTimer = onSetTimer,
                    onCancelTimer = onCancelTimer,
                    reduceMotion = reduceMotion,
                    companionIntent = sleepingCompanionIntent,
                    quietUi = quietSleepUi,
                )
                is BedtimeDestination.Morning -> MorningSummaryScreen(
                    session = destination.session.toUiSession(),
                    nightMemoryEntries = nightMemoryEntries,
                    reduceMotion = reduceMotion,
                    onDone = onDone,
                    dreamDiscoveries = discoveries,
                )
                is BedtimeDestination.Error -> Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("記録を読み込めませんでした")
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onRetry) { Text("もう一度試す") }
                }
            }
        }
    }
}

private fun SleepSessionRecord.toUiSession() = SleepSession(
    state = if (status == app.soine.sleep.SleepSessionStatus.COMPLETED) SleepState.FINISHED else SleepState.SLEEPING,
    startedAtEpochMillis = startedAtEpochMillis,
    endedAtEpochMillis = endedAtEpochMillis,
)

@Composable
private fun SleepingScreen(
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
) {
    SleepingScreen(
        session = session,
        onWake = onWake,
        onOpenSettings = onOpenSettings,
        ambientSoundLabel = ambientSoundLabel,
        audioPlaying = audioPlaying,
        remainingTimerLabel = remainingTimerLabel,
        onToggleAudio = onToggleAudio,
        onSetTimer = onSetTimer,
        onCancelTimer = onCancelTimer,
        reduceMotion = reduceMotion,
        companionIntent = companionIntent,
        quietUi = quietUi,
    )
}

@Composable
private fun MorningSummaryScreen(
    session: SleepSession,
    nightMemoryEntries: List<NightMemoryEntry>,
    reduceMotion: Boolean,
    onDone: () -> Unit,
    dreamDiscoveries: List<DreamDiscovery> = emptyList(),
) {
    MorningScreen(
        session = session,
        nightMemoryEntries = nightMemoryEntries,
        reduceMotion = reduceMotion,
        onDone = onDone,
        dreamDiscoveries = dreamDiscoveries,
    )
}

@Composable
private fun CompanionScene(
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
        )
    )
    val artwork = presentation.artwork
    Surface(
        modifier = Modifier
            .size(width = 190.dp, height = 150.dp)
            .semantics { contentDescription = artwork.contentDescription },
        shape = RoundedCornerShape(64.dp),
        tonalElevation = 2.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                artwork.glyph,
                style = MaterialTheme.typography.displaySmall,
            )
        }
    }
}
