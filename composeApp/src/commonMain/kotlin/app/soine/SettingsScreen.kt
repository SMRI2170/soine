package app.soine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.soine.accessibility.AccessibilityPolicy
import app.soine.audio.AmbientAudioPreferences
import app.soine.audio.AmbientSounds
import app.soine.audio.SleepTimerPreset
import app.soine.design.SoineColors
import app.soine.design.SoinePanel
import app.soine.design.SoineSectionHeader
import app.soine.design.SoineTokens
import app.soine.privacy.LocalDataDeletionResult
import app.soine.sound.MicrophoneEnableAction
import app.soine.sound.MicrophonePermissionCopy
import app.soine.sound.MicrophonePermissionState
import app.soine.sound.microphoneEnableAction
import app.soine.sound.SoundEventSessionSummary

/**
 * V1 Settings screen.
 *
 * The screen follows the [issue-176][issue-176] information
 * architecture: a single column divided into five named
 * groups so the user can see "what I touch every night" and
 * "what is permission / privacy / data" at a glance.
 *
 * The groups are, in order:
 *
 *   1. 睡眠について — sleep defaults (ambient sound, default
 *      timer). These are the settings the user touches most
 *      often; they live at the top of the screen.
 *   2. 相棒について — companion visual / motion options.
 *      The V1 ships a quiet "coming soon" placeholder
 *      because the future 3D renderer EPIC owns the
 *      visual / motion options. The placeholder is
 *      intentional, not a dead end: it sets the
 *      expectation that the section will grow.
 *   3. 連携（任意） — optional integrations. The Health
 *      and the overnight sound analysis both opt in
 *      explicitly. Neither is required for the core
 *      experience.
 *   4. プライバシーとデータ — privacy and data management.
 *      The destructive "delete all data" action lives
 *      behind a navigation, not a button, so a mis-tap
 *      does not erase the user's history.
 *   5. このアプリについて — version, replay-onboarding
 *      action. The version is a small label, not a
 *      primary surface.
 *
 * The screen is a single `verticalScroll` so it stays
 * usable at the largest Dynamic Type setting. The primary
 * navigation (back) and the destructive action
 * (privacy-and-data) are reachable in one tap; the
 * sub-options live inside their own group.
 */
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
    onReplayOnboarding: () -> Unit,
    onBack: () -> Unit,
) {
    var showMicrophoneExplanation by remember { mutableStateOf(false) }
    var showMicrophoneSettings by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = SoineTokens.SpacingLg),
        verticalArrangement = Arrangement.spacedBy(SoineTokens.SpacingMd),
    ) {
        Spacer(Modifier.height(SoineTokens.SpacingSm))
        Header("設定", onBack)
        Spacer(Modifier.height(SoineTokens.SpacingXs))

        SettingsGroup(title = "睡眠について") {
            AmbientSoundGroup(
                preferences = preferences,
                onSoundSelected = onSoundSelected,
                onMutedChanged = onMutedChanged,
                onVolumeChanged = onVolumeChanged,
            )
            SleepTimerGroup(
                preferences = preferences,
                onTimerPresetSelected = onTimerPresetSelected,
            )
        }

        SettingsGroup(title = "相棒について") {
            SoinePanel {
                Column(verticalArrangement = Arrangement.spacedBy(SoineTokens.SpacingXs)) {
                    Text(
                        "見た目と動きのオプション",
                        style = MaterialTheme.typography.titleSmall,
                        color = SoineColors.cream,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "次のアップデートで追加予定です。今は決められた 4 つの " +
                            "ステージ (NEW / WARMING_UP / FAMILIAR / CLOSE) で " +
                            "静かに変化します。",
                        style = MaterialTheme.typography.bodySmall,
                        color = SoineColors.hush,
                    )
                }
            }
        }

        SettingsGroup(title = "連携（任意）") {
            OptionalIntegrationsGroup(
                soundAnalysisEnabled = soundAnalysisEnabled,
                microphonePermissionState = microphonePermissionState,
                onSoundAnalysisEnabledChanged = { checked ->
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

        SettingsGroup(title = "プライバシーとデータ") {
            PrivacyAndDataGroup(onPrivacyData = onPrivacyData)
        }

        SettingsGroup(title = "このアプリについて") {
            AboutGroup(
                appVersion = appVersion,
                onReplayOnboarding = onReplayOnboarding,
            )
        }

        Spacer(Modifier.height(SoineTokens.SpacingXl))
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
private fun AmbientSoundGroup(
    preferences: AmbientAudioPreferences,
    onSoundSelected: (String) -> Unit,
    onMutedChanged: (Boolean) -> Unit,
    onVolumeChanged: (Float) -> Unit,
) {
    SoinePanel {
        Column(verticalArrangement = Arrangement.spacedBy(SoineTokens.SpacingSm)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("寝るときに環境音を再生", color = SoineColors.cream)
                Switch(
                    checked = !preferences.muted,
                    onCheckedChange = { onMutedChanged(!it) },
                )
            }
            SoineSectionHeader("寝るときの標準の音")
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SoineTokens.SpacingSm),
            ) {
                AmbientSounds.defaults.forEach { sound ->
                    FilterChip(
                        selected = preferences.soundId == sound.id,
                        onClick = { onSoundSelected(sound.id) },
                        label = { Text(sound.displayName) },
                    )
                }
            }
            SoineSectionHeader("音量 " + (preferences.volume * 100).toInt() + "%")
            Slider(
                value = preferences.volume,
                onValueChange = onVolumeChanged,
                valueRange = 0f..1f,
            )
        }
    }
}

@Composable
private fun SleepTimerGroup(
    preferences: AmbientAudioPreferences,
    onTimerPresetSelected: (SleepTimerPreset?) -> Unit,
) {
    SoinePanel {
        Column(verticalArrangement = Arrangement.spacedBy(SoineTokens.SpacingSm)) {
            Text(
                "環境音だけを自動で停止します。睡眠記録は続きます。",
                style = MaterialTheme.typography.bodySmall,
                color = SoineColors.hush,
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SoineTokens.SpacingSm),
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
            Row(horizontalArrangement = Arrangement.spacedBy(SoineTokens.SpacingSm)) {
                FilterChip(
                    selected = preferences.timerPreset == SleepTimerPreset.MINUTES_90,
                    onClick = { onTimerPresetSelected(SleepTimerPreset.MINUTES_90) },
                    label = { Text("90分") },
                )
                Text(
                    "カスタム時間は就寝中のタイマー画面で設定予定",
                    style = MaterialTheme.typography.bodySmall,
                    color = SoineColors.hush,
                    modifier = Modifier.align(Alignment.CenterVertically),
                )
            }
        }
    }
}

@Composable
private fun OptionalIntegrationsGroup(
    soundAnalysisEnabled: Boolean,
    microphonePermissionState: MicrophonePermissionState,
    onSoundAnalysisEnabledChanged: (Boolean) -> Unit,
) {
    SoinePanel {
        Column(verticalArrangement = Arrangement.spacedBy(SoineTokens.SpacingSm)) {
            IntegrationRow(
                label = "Health",
                status = "未接続（任意）",
                onClick = null,
            )
            HorizontalDivider()
            IntegrationRow(
                label = "夜間の音解析",
                status = soundAnalysisStatusLabel(
                    soundAnalysisEnabled = soundAnalysisEnabled,
                    microphonePermissionState = microphonePermissionState,
                ),
                onClick = null,
            )
            val isUnavailable = microphonePermissionState == MicrophonePermissionState.UNAVAILABLE
            Switch(
                checked = soundAnalysisEnabled &&
                    microphonePermissionState == MicrophonePermissionState.GRANTED,
                enabled = !isUnavailable,
                onCheckedChange = onSoundAnalysisEnabledChanged,
                modifier = Modifier
                    .heightIn(min = AccessibilityPolicy.MIN_TOUCH_TARGET_DP.dp)
                    .semantics {
                        contentDescription = "夜間の音解析を" +
                            if (soundAnalysisEnabled) "オフにする" else "オンにする"
                    },
            )
            if (
                microphonePermissionState == MicrophonePermissionState.DENIED ||
                microphonePermissionState == MicrophonePermissionState.PERMANENTLY_DENIED
            ) {
                Text(
                    MicrophonePermissionCopy.DENIED,
                    style = MaterialTheme.typography.bodySmall,
                    color = SoineColors.hush,
                )
            }
            if (isUnavailable) {
                Text(
                    "この端末では利用できません",
                    style = MaterialTheme.typography.bodySmall,
                    color = SoineColors.hush,
                )
            }
        }
    }
}

@Composable
private fun PrivacyAndDataGroup(onPrivacyData: () -> Unit) {
    SoinePanel {
        Column(verticalArrangement = Arrangement.spacedBy(SoineTokens.SpacingSm)) {
            Text(
                "睡眠セッションや関係性など、Soineのデータは端末内に保存します。",
                style = MaterialTheme.typography.bodySmall,
                color = SoineColors.hush,
            )
            OutlinedButton(
                onClick = onPrivacyData,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("プライバシーとデータを開く")
            }
        }
    }
}

@Composable
private fun AboutGroup(
    appVersion: String,
    onReplayOnboarding: () -> Unit,
) {
    SoinePanel {
        Column(verticalArrangement = Arrangement.spacedBy(SoineTokens.SpacingSm)) {
            Text(
                "Soine v" + appVersion,
                style = MaterialTheme.typography.bodySmall,
                color = SoineColors.hush,
            )
            TextButton(
                onClick = onReplayOnboarding,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = AccessibilityPolicy.MIN_TOUCH_TARGET_DP.dp)
                    .semantics {
                        contentDescription = AccessibilityPolicy.SETTINGS_REPLAY_ONBOARDING_CONTENT_DESCRIPTION
                    },
            ) {
                Text(
                    "オンボーディングをもう一度見る",
                    color = SoineColors.cream,
                )
            }
        }
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(SoineTokens.SpacingXs)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = SoineColors.hush,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = SoineTokens.SpacingXs),
        )
        content()
    }
}

@Composable
private fun IntegrationRow(
    label: String,
    status: String,
    onClick: (() -> Unit)?,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, color = SoineColors.cream)
            Text(
                status,
                style = MaterialTheme.typography.bodySmall,
                color = SoineColors.hush,
            )
        }
        if (onClick != null) {
            TextButton(onClick = onClick) { Text("接続", color = SoineColors.cream) }
        }
    }
}

@Composable
private fun HorizontalDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(SoineTokens.Divider),
    ) {
        androidx.compose.material3.HorizontalDivider(color = SoineColors.divider)
    }
}

private fun soundAnalysisStatusLabel(
    soundAnalysisEnabled: Boolean,
    microphonePermissionState: MicrophonePermissionState,
): String = when {
    soundAnalysisEnabled &&
        microphonePermissionState == MicrophonePermissionState.GRANTED ->
        "オン（端末内で解析）"

    microphonePermissionState == MicrophonePermissionState.PERMANENTLY_DENIED ->
        "マイク許可が必要です"

    microphonePermissionState == MicrophonePermissionState.UNAVAILABLE ->
        "この端末では利用できません"

    else ->
        "オフ（任意）"
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
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
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
                soundEventSessions.forEach { summary ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "最終イベント " +
                                    formatJapaneseDiscoveryDate(summary.latestOccurredAtEpochMillis) +
                                    " " +
                                    formatNightEventTime(summary.latestOccurredAtEpochMillis),
                            )
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
            }

            // Keep this action visible even when the snapshot cannot be decoded:
            // explicit privacy deletion must still be able to clear unreadable data.
            OutlinedButton(
                onClick = { confirmDeleteAllSoundEvents = true },
                enabled = !deletingSoundEvents,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (deletingSoundEvents) "削除中" else "音イベントをすべて削除")
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
                colors = ButtonDefaults.buttonColors(
                    containerColor = SoineColors.ember,
                    contentColor = SoineColors.midnight,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (deleting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.size(SoineTokens.SpacingSm))
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
        horizontalArrangement = Arrangement.spacedBy(SoineTokens.SpacingSm),
    ) {
        TextButton(onClick = onBack) { Text("もどる") }
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = SoineColors.cream,
        )
    }
}

@Composable
private fun SettingSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    androidx.compose.material3.Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(SoineTokens.SpacingMd),
            verticalArrangement = Arrangement.spacedBy(SoineTokens.SpacingSm),
        ) {
            Text(
                title,
                fontWeight = FontWeight.SemiBold,
                color = SoineColors.cream,
            )
            content()
        }
    }
}
