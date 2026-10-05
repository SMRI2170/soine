package app.soine

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.soine.sleep.SleepSession
import app.soine.sleep.SleepState
import app.soine.sleep.summary

@Composable
fun App() {
    var session by remember { mutableStateOf(SleepSession()) }

    MaterialTheme {
        Surface(Modifier.fillMaxSize()) {
            when (session.state) {
                SleepState.READY -> BedtimeScreen(onStartSleep = { session = session.start() })
                SleepState.SLEEPING -> SleepingScreen(
                    session = session,
                    onWake = { session = session.finish() },
                )
                SleepState.FINISHED -> MorningSummaryScreen(
                    session = session,
                    onDone = { session = SleepSession() },
                )
            }
        }
    }
}

@Composable
private fun BedtimeScreen(onStartSleep: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("soine", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            TextButton(onClick = {}) { Text("設定") }
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
                    Text("なし")
                    Spacer(Modifier.height(12.dp))
                    Text("スリープタイマー", fontWeight = FontWeight.Medium)
                    Text("オフ")
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
private fun SleepingScreen(session: SleepSession, onWake: () -> Unit) {
    val startedAt = session.startedAtEpochMillis
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("おやすみ", style = MaterialTheme.typography.titleMedium)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CompanionScene(SleepState.SLEEPING)
            Spacer(Modifier.height(20.dp))
            Text("一緒に眠っています", style = MaterialTheme.typography.headlineSmall)
            if (startedAt != null) {
                Spacer(Modifier.height(8.dp))
                Text("開始済み", style = MaterialTheme.typography.bodyMedium)
            }
        }
        Column(Modifier.fillMaxWidth()) {
            TextButton(onClick = {}, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("環境音を調整")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onWake,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(18.dp),
            ) { Text("起きる") }
        }
    }
}

@Composable
private fun MorningSummaryScreen(session: SleepSession, onDone: () -> Unit) {
    val summary = session.summary()
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("おはよう", style = MaterialTheme.typography.titleMedium)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CompanionScene(SleepState.FINISHED)
            Spacer(Modifier.height(16.dp))
            Text("今日も一緒に起きられたね", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(24.dp))
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Text("昨夜の記録", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(12.dp))
                    Text("昨夜の出来事は、これから少しずつ増えていきます")
                    Spacer(Modifier.height(16.dp))
                    Text("睡眠時間", style = MaterialTheme.typography.labelLarge)
                    Text(summary?.displayDuration() ?: "記録なし", style = MaterialTheme.typography.headlineMedium)
                }
            }
        }
        Button(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(18.dp),
        ) { Text("今日をはじめる") }
    }
}

@Composable
private fun CompanionScene(state: SleepState) {
    Text(
        when (state) {
            SleepState.SLEEPING -> "（ ᵕ ᵕ ） zzz"
            SleepState.FINISHED -> "（ ˶ᵔ ᵕ ᵔ˶ ）"
            else -> "（ ・ᴗ・ ）"
        },
        style = MaterialTheme.typography.displaySmall,
    )
}
