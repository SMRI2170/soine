package app.soine

import androidx.compose.runtime.*
import androidx.compose.material3.MaterialTheme
import app.soine.audio.AmbientAudioPreferences
import app.soine.audio.AmbientAudioPreferencesStore
import app.soine.audio.SleepTimerPreset
import app.soine.navigation.*
import app.soine.sleep.SleepSessionRepository
import kotlinx.coroutines.launch

private enum class SecondaryScreen {
    SETTINGS,
    PRIVACY_DATA,
}

@Composable
fun SoineApp(
    repository: SleepSessionRepository,
    audioPreferencesStore: AmbientAudioPreferencesStore,
    appVersion: String = "0.1.0",
) {
    val controller = remember(repository) { BedtimeFlowController(repository) }
    var destination by remember { mutableStateOf<BedtimeDestination>(BedtimeDestination.Loading) }
    var secondaryScreen by remember { mutableStateOf<SecondaryScreen?>(null) }
    var audioPreferences by remember(audioPreferencesStore) {
        mutableStateOf(audioPreferencesStore.read())
    }
    val scope = rememberCoroutineScope()

    fun persistAudioPreferences(next: AmbientAudioPreferences) {
        audioPreferencesStore.write(next)
        audioPreferences = next
    }

    LaunchedEffect(controller) { destination = controller.initialDestination() }

    MaterialTheme {
        when (secondaryScreen) {
            SecondaryScreen.SETTINGS -> SettingsScreen(
                preferences = audioPreferences,
                appVersion = appVersion,
                onSoundSelected = { soundId ->
                    persistAudioPreferences(audioPreferences.copy(soundId = soundId))
                },
                onTimerPresetSelected = { preset: SleepTimerPreset? ->
                    persistAudioPreferences(audioPreferences.copy(timerPreset = preset))
                },
                onPrivacyData = { secondaryScreen = SecondaryScreen.PRIVACY_DATA },
                onBack = { secondaryScreen = null },
            )
            SecondaryScreen.PRIVACY_DATA -> PrivacyDataScreen(
                onBack = { secondaryScreen = SecondaryScreen.SETTINGS },
            )
            null -> App(
                destination = destination,
                onStartSleep = { scope.launch { destination = controller.start() } },
                onWake = { scope.launch { destination = controller.finish() } },
                onDone = { destination = controller.dismissMorning() },
                onRetry = { scope.launch { destination = controller.initialDestination() } },
                onOpenSettings = { secondaryScreen = SecondaryScreen.SETTINGS },
            )
        }
    }
}
