package app.soine

import androidx.compose.runtime.*
import app.soine.navigation.*
import app.soine.sleep.SleepSessionRepository
import kotlinx.coroutines.launch

@Composable
fun SoineApp(repository: SleepSessionRepository) {
    val controller = remember(repository) { BedtimeFlowController(repository) }
    var destination by remember { mutableStateOf<BedtimeDestination>(BedtimeDestination.Loading) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(controller) { destination = controller.initialDestination() }

    App(
        destination = destination,
        onStartSleep = { scope.launch { destination = controller.start() } },
        onWake = { scope.launch { destination = controller.finish() } },
        onDone = { destination = controller.dismissMorning() },
        onRetry = { scope.launch { destination = controller.initialDestination() } },
    )
}
