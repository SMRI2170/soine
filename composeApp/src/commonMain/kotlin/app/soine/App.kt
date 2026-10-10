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
    var timerDialog by remember { mutableStateOf(false) }
    val startedAt = session.startedAtEpochMillis
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("おやすみ", style = MaterialTheme.typography.titleMedium)
                TextButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.heightIn(min = AccessibilityPolicy.MIN_TOUCH_TARGET_DP.dp),
                ) {
                    Text("設定")
                }
            }
            Spacer(Modifier.height(24.dp))
            CompanionScene(
                state = SleepState.SLEEPING,
                reduceMotion = reduceMotion,
                intentOverride = companionIntent,
            )
            Spacer(Modifier.height(20.dp))
            Text("一緒に眠っています", style = MaterialTheme.typography.headlineSmall)
            if (!quietUi) {
                if (startedAt != null) {
                    Spacer(Modifier.height(8.dp))
                    Text("開始済み", style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(16.dp))
                Text("環境音: " + ambientSoundLabel)
                remainingTimerLabel?.let { Text("タイマー: " + it) }
            }
            Spacer(Modifier.height(12.dp))
            TextButton(
                onClick = onToggleAudio,
                modifier = Modifier.heightIn(min = AccessibilityPolicy.MIN_TOUCH_TARGET_DP.dp),
            ) {
                Text(if (audioPlaying) "環境音を一時停止" else "環境音を再生")
            }
            TextButton(
                onClick = { timerDialog = true },
                modifier = Modifier.heightIn(min = AccessibilityPolicy.MIN_TOUCH_TARGET_DP.dp),
            ) {
                Text("スリープタイマーを変更")
            }
            if (remainingTimerLabel != null) {
                TextButton(
                    onClick = onCancelTimer,
                    modifier = Modifier.heightIn(min = AccessibilityPolicy.MIN_TOUCH_TARGET_DP.dp),
                ) {
                    Text("タイマーを解除")
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        SoinePrimaryButton(
            text = "起きる",
            onClick = onWake,
            contentDescription = AccessibilityPolicy.WAKE_CONTENT_DESCRIPTION,
        )
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
private fun SleepTimerDialog(
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
                                .heightIn(min = AccessibilityPolicy.MIN_TOUCH_TARGET_DP.dp),
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

@Composable
private fun MorningSummaryScreen(
    session: SleepSession,
    nightMemoryEntries: List<NightMemoryEntry>,
    reduceMotion: Boolean,
    onDone: () -> Unit,
) {
    val summary = session.summary()
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("おはよう", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CompanionScene(SleepState.FINISHED, reduceMotion)
            Spacer(Modifier.height(16.dp))
            Text("今日も一緒に起きられたね", style = MaterialTheme.typography.headlineSmall)
            if (nightMemoryEntries.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                NightMemoryTimeline(nightMemoryEntries)
            }
            Spacer(Modifier.height(16.dp))
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Text("睡眠時間", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        summary?.displayDuration() ?: "記録なし",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        SoinePrimaryButton(
            text = "今日をはじめる",
            onClick = onDone,
            contentDescription = "朝の記録を閉じて今日を始める",
        )
    }
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
