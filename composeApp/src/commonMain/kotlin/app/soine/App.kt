package app.soine

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.soine.sleep.SleepSession
import app.soine.sleep.SleepState

@Composable
fun App() {
    var session by remember { mutableStateOf(SleepSession()) }

    MaterialTheme {
        Surface(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("soine", style = MaterialTheme.typography.headlineLarge)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        if (session.state == SleepState.SLEEPING)
                            "一緒に眠っています"
                        else
                            "今日も一緒に眠ろう"
                    )
                }

                CompanionScene(session.state)

                Button(
                    onClick = {
                        session = if (session.state == SleepState.SLEEPING)
                            session.finish()
                        else session.start()
                    }
                ) {
                    Text(if (session.state == SleepState.SLEEPING) "起きる" else "一緒に寝る")
                }
            }
        }
    }
}

@Composable
private fun CompanionScene(state: SleepState) {
    // Rendering boundary: replace this placeholder with the native 3D scene.
    Text(
        when (state) {
            SleepState.SLEEPING -> "（ ᵕ ᵕ ） zzz"
            SleepState.FINISHED -> "（ ˶ᵔ ᵕ ᵔ˶ ）"
            else -> "（ ・ᴗ・ ）"
        },
        style = MaterialTheme.typography.displaySmall
    )
}
