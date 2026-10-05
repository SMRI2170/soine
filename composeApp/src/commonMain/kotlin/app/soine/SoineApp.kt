package app.soine

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import app.soine.audio.AmbientAudioPreferences
import app.soine.audio.AmbientAudioPreferencesStore
import app.soine.audio.SleepTimerPreset
import app.soine.navigation.*
import app.soine.privacy.LocalDataDeletionResult
import app.soine.privacy.LocalDataDeletionService
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
    localDataDeletionService: LocalDataDeletionService,
    appVersion: String = "0.1.0",
) {
    val controller = remember(repository) { BedtimeFlowController(repository) }
    var destination by remember { mutableStateOf<BedtimeDestination>(BedtimeDestination.Loading) }
    var secondaryScreen by remember { mutableStateOf<SecondaryScreen?>(null) }
    var audioPreferences by remember(audioPreferencesStore) {
        mutableStateOf(audioPreferencesStore.read())
    }
    var deletingLocalData by remember { mutableStateOf(false) }
    var deletionResult by remember { mutableStateOf<LocalDataDeletionResult?>(null) }
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
                onPrivacyData = {
                    deletionResult = null
                    secondaryScreen = SecondaryScreen.PRIVACY_DATA
                },
                onBack = { secondaryScreen = null },
            )
            SecondaryScreen.PRIVACY_DATA -> PrivacyDataScreen(
                deleting = deletingLocalData,
                deletionResult = deletionResult,
                onDeleteAll = {
                    if (!deletingLocalData) {
                        scope.launch {
                            deletingLocalData = true
                            val result = localDataDeletionService.deleteAll()
                            if (result == LocalDataDeletionResult.Deleted) {
                                audioPreferences = audioPreferencesStore.read()
                                destination = BedtimeDestination.Bedtime
                            }
                            deletionResult = result
                            deletingLocalData = false
                        }
                    }
                },
                onDismissDeletionResult = { deletionResult = null },
                onBack = {
                    deletionResult = null
                    secondaryScreen = SecondaryScreen.SETTINGS
                },
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
