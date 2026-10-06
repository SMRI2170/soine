package app.soine

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.soine.navigation.BedtimeDestination
import app.soine.companion.CompanionIntent
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
    ambientSoundLabel: String,
    defaultTimerLabel: String,
    audioPlaying: Boolean,
    remainingTimerLabel: String?,
    onToggleAudio: () -> Unit,
    onSetTimer: (Int) -> Unit,
    onCancelTimer: () -> Unit,
    companionIntent: CompanionIntent?,
    quietUi: Boolean,
) {
    MaterialTheme {
        Surface(Modifier.fillMaxSize()) {
            when (destination) {
                BedtimeDestination.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                BedtimeDestination.Bedtime -> BedtimeScreen(
                    onStartSleep = onStartSleep,
                    onOpenDreamAlbum = onOpenDreamAlbum,
                    onOpenSettings = onOpenSettings,
                    ambientSoundLabel = ambientSoundLabel,
                    defaultTimerLabel = defaultTimerLabel,
                )
                is BedtimeDestination.Sleeping -> SleepingScreen(
                    session = destination.session.toUiSession(),
                    onWake = onWake,
                    ambientSoundLabel = ambientSoundLabel,
                    audioPlaying = audioPlaying,
                    remainingTimerLabel = remainingTimerLabel,
                    onToggleAudio = onToggleAudio,
                    onSetTimer = onSetTimer,
                    onCancelTimer = onCancelTimer,
                    companionIntent = sleepingCompanionIntent,
                    quietUi = quietSleepUi,
                )
                is BedtimeDestination.Morning -> MorningSummaryScreen(
                    session = destination.session.toUiSession(),
                    nightMemoryEntries = nightMemoryEntries,
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
private fun BedtimeScreen(
    onStartSleep: () -> Unit,
    onOpenDreamAlbum: () -> Unit,
    onOpenSettings: () -> Unit,
    ambientSoundLabel: String,
    defaultTimerLabel: String,
) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("soine", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            Row {
                TextButton(onClick = onOpenDreamAlbum) { Text("夢のアルバム") }
                TextButton(onClick = onOpenSettings) { Text("設定") }
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CompanionScene(SleepState.READY)
            Spacer(Modifier.height(20.dp))
            Text("今日も一緒に眠ろう", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text("音や計測を設定しなくても、そのまま始められます")
        }
        Column(Modifier.fillMaxWidth()) {
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("環境音", fontWeight = FontWeight.Medium)
                    Text(ambientSoundLabel)
                    Spacer(Modifier.height(12.dp))
                    Text("スリープタイマー", fontWeight = FontWeight.Medium)
                    Text(defaultTimerLabel)
                }
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onStartSleep,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(18.dp),
            ) { Text("一緒に寝る") }
        }
    }
}

@Composable
private fun SleepingScreen(
    session: SleepSession,
    onWake: () -> Unit,
    ambientSoundLabel: String,
    audioPlaying: Boolean,
    remainingTimerLabel: String?,
    onToggleAudio: () -> Unit,
    onSetTimer: (Int) -> Unit,
    onCancelTimer: () -> Unit,
) {
    var timerDialog by remember { mutableStateOf(false) }
    val startedAt = session.startedAtEpochMillis
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("おやすみ", style = MaterialTheme.typography.titleMedium)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CompanionScene(
                state = SleepState.SLEEPING,
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
        }
        Column(Modifier.fillMaxWidth()) {
            TextButton(onClick = onToggleAudio, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(if (audioPlaying) "環境音を一時停止" else "環境音を再生")
            }
            TextButton(onClick = { timerDialog = true }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("スリープタイマーを変更")
            }
            if (remainingTimerLabel != null) {
                TextButton(onClick = onCancelTimer, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("タイマーを解除")
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onWake,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(18.dp),
            ) { Text("起きる") }
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(30, 60, 90).forEach { minutes ->
                        OutlinedButton(onClick = { onSetTimer(minutes) }) {
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
            CompanionScene(SleepState.FINISHED)
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
        Button(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(18.dp),
        ) { Text("今日をはじめる") }
    }
}

@Composable
private fun CompanionScene(
    state: SleepState,
    intentOverride: CompanionIntent? = null,
) {
    val intent = intentOverride ?: when (state) {
        SleepState.SLEEPING -> CompanionIntent.SLEEP
        SleepState.FINISHED -> CompanionIntent.WAKE
        SleepState.READY -> CompanionIntent.IDLE
    }
    val artwork = CompanionStaticFallback.forIntent(intent)
    Surface(
        modifier = Modifier.size(width = 190.dp, height = 150.dp),
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
