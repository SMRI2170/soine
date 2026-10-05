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

@Composable
fun SettingsScreen(
    preferences: AmbientAudioPreferences,
    appVersion: String,
    onSoundSelected: (String) -> Unit,
    onTimerPresetSelected: (SleepTimerPreset?) -> Unit,
    onPrivacyData: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Header("設定", onBack)

        SettingSection("環境音") {
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
            SettingValueRow("夜間の音解析", "オフ（任意）")
        }

        OutlinedButton(onClick = onPrivacyData, modifier = Modifier.fillMaxWidth()) {
            Text("プライバシーとデータ")
        }

        Spacer(Modifier.weight(1f))
        Text("Soine v" + appVersion, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun PrivacyDataScreen(
    deleting: Boolean,
    deletionResult: LocalDataDeletionResult?,
    onDeleteAll: () -> Unit,
    onDismissDeletionResult: () -> Unit,
    onBack: () -> Unit,
) {
    var confirmDeletion by remember { mutableStateOf(false) }

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
            Text("夜間の音解析は任意機能です。標準ではオフで、raw audioを保存しない設計です。")
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
