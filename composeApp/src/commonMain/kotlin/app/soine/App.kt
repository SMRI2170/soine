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
import app.soine.sleep.summary\nimport app.soine.navigation.BedtimeDestination\nimport app.soine.sleep.SleepSessionRecord

@Composable
fun App(
    destination: BedtimeDestination,
    onStartSleep: () -> Unit,
    onWake: () -> Unit,
    onDone: () -> Unit,
    onRetry: () -> Unit,
) {
    MaterialTheme {
        Surface(Modifier.fillMaxSize()) {
            when (destination) {
                BedtimeDestination.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                BedtimeDestination.Bedtime -> BedtimeScreen(onStartSleep)
                is BedtimeDestination.Sleeping -> SleepingScreen(destination.session.toUiSession(), onWake)
                is BedtimeDestination.Morning -> MorningSummaryScreen(destination.session.toUiSession(), onDone)
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

