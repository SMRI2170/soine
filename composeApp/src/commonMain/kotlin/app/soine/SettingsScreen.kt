package app.soine

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.soine.audio.AmbientAudioPreferences
import app.soine.audio.AmbientSounds
import app.soine.audio.SleepTimerPreset
import app.soine.privacy.LocalDataDeletionResult
import app.soine.sound.MicrophoneEnableAction
import app.soine.sound.MicrophonePermissionCopy
import app.soine.sound.MicrophonePermissionState
import app.soine.sound.microphoneEnableAction
import app.soine.sound.SoundEventSessionSummary

@Composable
fun SettingsScreen(
    preferences: AmbientAudioPreferences,
    appVersion: String,
    onSoundSelected: (String) -> Unit,
    onMutedChanged: (Boolean) -> Unit,
    onVolumeChanged: (Float) -> Unit,
    onTimerPresetSelected: (SleepTimerPreset?) -> Unit,
    soundAnalysisEnabled: Boolean,
    microphonePermissionState: MicrophonePermissionState,
    onSoundAnalysisEnabledChanged: (Boolean) -> Unit,
    onRequestMicrophonePermission: () -> Unit,
    onOpenMicrophoneSettings: () -> Unit,
    onPrivacyData: () -> Unit,
    onBack: () -> Unit,
) {
    var showMicrophoneExplanation by remember { mutableStateOf(false) }
    var showMicrophoneSettings by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Header("設定", onBack)

        SettingSection("環境音") {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("寝るときに環境音を再生")
                Switch(
                    checked = !preferences.muted,
                    onCheckedChange = { onMutedChanged(!it) },
                )
            }
            Text("寝るときの標準の音")
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AmbientSounds.defaults.forEach { sound ->
                    FilterChip(
                        selected = preferences.soundId == sound.id,
                        onClick = { onSoundSelected(sound.id) },
                        label = { Text(sound.displayName) },
                    )
                }
            }
            Text("音量 " + (preferences.volume * 100).toInt() + "%")
            Slider(
                value = preferences.volume,
                onValueChange = onVolumeChanged,
                valueRange = 0f..1f,
            )
        }

        SettingSection("スリープタイマー") {
            Text("環境音だけを自動で停止します。睡眠記録は続きます。")
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = preferences.timerPreset == null,
                    onClick = { onTimerPresetSelected(null) },
                    label = { Text("オフ") },
                )
                listOf(SleepTimerPreset.MINUTES_30, SleepTimerPreset.MINUTES_60).forEach { preset ->
                    FilterChip(
                        selected = preferences.timerPreset == preset,
                        onClick = { onTimerPresetSelected(preset) },
                        label = { Text(preset.minutes.toString() + "分") },
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = preferences.timerPreset == SleepTimerPreset.MINUTES_90,
                    onClick = { onTimerPresetSelected(SleepTimerPreset.MINUTES_90) },
                    label = { Text("90分") },
                )
                Text(
                    "カスタム時間は就寝中のタイマー画面で設定予定",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.CenterVertically),
                )
            }
        }

        SettingSection("連携") {
            SettingValueRow("Health", "未接続（任意）")
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("夜間の音解析")
                    Text(
                        when {
                            soundAnalysisEnabled &&
                                microphonePermissionState == MicrophonePermissionState.GRANTED ->
                                "オン（端末内で解析）"

                            microphonePermissionState == MicrophonePermissionState.PERMANENTLY_DENIED ->
                                "マイク許可が必要です"

                            microphonePermissionState == MicrophonePermissionState.UNAVAILABLE ->
                                "この端末では利用できません"

                            else ->
                                "オフ（任意）"
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Switch(
                    checked = soundAnalysisEnabled &&
                        microphonePermissionState == MicrophonePermissionState.GRANTED,
                    enabled = microphonePermissionState != MicrophonePermissionState.UNAVAILABLE,
                    onCheckedChange = { checked ->
                        if (!checked) {
                            onSoundAnalysisEnabledChanged(false)
                        } else {
                            when (microphoneEnableAction(microphonePermissionState)) {
                                MicrophoneEnableAction.ENABLE ->
                                    onSoundAnalysisEnabledChanged(true)

                                MicrophoneEnableAction.SHOW_PRE_PERMISSION ->
                                    showMicrophoneExplanation = true

                                MicrophoneEnableAction.OPEN_SETTINGS ->
                                    showMicrophoneSettings = true

                                MicrophoneEnableAction.UNAVAILABLE -> Unit
                            }
                        }
                    },
                )
            }

            if (
                microphonePermissionState == MicrophonePermissionState.DENIED ||
                microphonePermissionState == MicrophonePermissionState.PERMANENTLY_DENIED
            ) {
                Text(
                    MicrophonePermissionCopy.DENIED,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        OutlinedButton(onClick = onPrivacyData, modifier = Modifier.fillMaxWidth()) {
            Text("プライバシーとデータ")
        }

        Spacer(Modifier.weight(1f))
        Text("Soine v" + appVersion, style = MaterialTheme.typography.bodySmall)
    }

    if (showMicrophoneExplanation) {
        AlertDialog(
            onDismissRequest = { showMicrophoneExplanation = false },
            title = { Text("夜間の音解析を使いますか？") },
            text = { Text(MicrophonePermissionCopy.PRE_PERMISSION) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showMicrophoneExplanation = false
                        onRequestMicrophonePermission()
                    },
                ) { Text("マイクを許可する") }
            },
            dismissButton = {
                TextButton(onClick = { showMicrophoneExplanation = false }) {
                    Text("今は使わない")
                }
            },
        )
    }

    if (showMicrophoneSettings) {
        AlertDialog(
            onDismissRequest = { showMicrophoneSettings = false },
            title = { Text("マイクの許可が必要です") },
            text = { Text(MicrophonePermissionCopy.SETTINGS_REQUIRED) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showMicrophoneSettings = false
                        onOpenMicrophoneSettings()
                    },
                ) { Text("端末の設定を開く") }
            },
            dismissButton = {
                TextButton(onClick = { showMicrophoneSettings = false }) {
                    Text("閉じる")
                }
            },
        )
    }
}

@Composable
fun PrivacyDataScreen(
    deleting: Boolean,
    deletionResult: LocalDataDeletionResult?,
    soundEventSessions: List<SoundEventSessionSummary>,
    deletingSoundEvents: Boolean,
    soundDeletionMessage: String?,
    onDeleteSoundSession: (String) -> Unit,
    onDeleteAllSoundEvents: () -> Unit,
    onDismissSoundDeletionMessage: () -> Unit,
    onDeleteAll: () -> Unit,
    onDismissDeletionResult: () -> Unit,
    onBack: () -> Unit,
) {
    var confirmDeletion by remember { mutableStateOf(false) }
    var soundSessionPendingDelete by remember { mutableStateOf<String?>(null) }
    var confirmDeleteAllSoundEvents by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Header("プライバシーとデータ", onBack)

        SettingSection("保存について") {
            Text("睡眠セッション、設定、これから追加される関係性や昨夜の記憶は、基本的に端末内へ保存します。")
            Text("現在、Soineのコア機能はクラウド保存やアカウント登録を必要としません。")
        }

        SettingSection("音") {
            Text("夜間の音解析は任意機能です。標準ではオフで、raw audioは保存しません。保存するのは端末内で判定した音イベントだけです。")

            if (soundEventSessions.isEmpty()) {
                Text(
                    "保存された音イベントはありません。",
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                soundEventSessions.forEachIndexed { index, summary ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("睡眠記録 " + (index + 1))
                            Text(
                                summary.eventCount.toString() + "件の音イベント",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        TextButton(
                            enabled = !deletingSoundEvents,
                            onClick = { soundSessionPendingDelete = summary.sessionId },
                        ) {
                            Text("削除")
                        }
                    }
                }

                OutlinedButton(
                    onClick = { confirmDeleteAllSoundEvents = true },
                    enabled = !deletingSoundEvents,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (deletingSoundEvents) "削除中" else "音イベントをすべて削除")
                }
            }

            soundDeletionMessage?.let { message ->
                Text(message, style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = onDismissSoundDeletionMessage) {
                    Text("閉じる")
                }
            }
        }

        SettingSection("Health") {
            Text("Health Connect / HealthKitとの連携は任意です。許可しなくても、一緒に寝る中心体験は利用できます。")
        }

        SettingSection("データ削除") {
            Text("端末内に保存したSoineの記録と設定を削除します。この操作は元に戻せません。")
            Button(
                onClick = { confirmDeletion = true },
                enabled = !deleting,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (deleting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("削除中")
                } else {
                    Text("すべてのローカルデータを削除")
                }
            }

            when (deletionResult) {
                LocalDataDeletionResult.Deleted ->
                    Text("端末内のSoineデータを削除しました。")
                LocalDataDeletionResult.BlockedByActiveSession ->
                    Text("睡眠記録中は削除できません。先に「起きる」で記録を終了してください。")
                is LocalDataDeletionResult.Failed ->
                    Text("一部のデータを削除できませんでした。もう一度お試しください。")
                null -> Unit
            }
        }
    }

    soundSessionPendingDelete?.let { sessionId ->
        AlertDialog(
            onDismissRequest = { soundSessionPendingDelete = null },
            title = { Text("この睡眠の音イベントを削除しますか？") },
            text = { Text("この睡眠で検出された音イベントだけを端末から削除します。raw audioは保存していません。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        soundSessionPendingDelete = null
                        onDismissSoundDeletionMessage()
                        onDeleteSoundSession(sessionId)
                    },
                ) { Text("削除する") }
            },
            dismissButton = {
                TextButton(onClick = { soundSessionPendingDelete = null }) {
                    Text("キャンセル")
                }
            },
        )
    }

    if (confirmDeleteAllSoundEvents) {
        AlertDialog(
            onDismissRequest = { confirmDeleteAllSoundEvents = false },
            title = { Text("音イベントをすべて削除しますか？") },
            text = { Text("保存されているderived sound eventをすべて削除します。睡眠記録そのものは残ります。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDeleteAllSoundEvents = false
                        onDismissSoundDeletionMessage()
                        onDeleteAllSoundEvents()
                    },
                ) { Text("すべて削除") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteAllSoundEvents = false }) {
                    Text("キャンセル")
                }
            },
        )
    }

    if (confirmDeletion) {
        AlertDialog(
            onDismissRequest = { confirmDeletion = false },
            title = { Text("すべてのローカルデータを削除しますか？") },
            text = { Text("睡眠記録やSoineの設定が端末から削除されます。この操作は元に戻せません。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDeletion = false
                        onDismissDeletionResult()
                        onDeleteAll()
                    },
                ) { Text("削除する") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeletion = false }) { Text("キャンセル") }
            },
        )
    }
}

@Composable
private fun Header(title: String, onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextButton(onClick = onBack) { Text("戻る") }
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SettingSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@Composable
private fun SettingValueRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
