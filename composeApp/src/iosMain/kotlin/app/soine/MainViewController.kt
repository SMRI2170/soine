package app.soine

import androidx.compose.ui.window.ComposeUIViewController
import app.soine.audio.IosAmbientAudioController
import app.soine.audio.IosAmbientAudioPreferencesStore
import app.soine.privacy.LocalDataClearer
import app.soine.privacy.LocalDataDeletionService
import app.soine.sleep.StoredSleepSessionRepository
import app.soine.storage.IosSleepSessionStore

fun MainViewController() = ComposeUIViewController {
    val sleepStore = IosSleepSessionStore()
    val repository = StoredSleepSessionRepository(sleepStore)
    val audioPreferences = IosAmbientAudioPreferencesStore()
    val audioController = IosAmbientAudioController { sound ->
        when (sound.id) {
            "rain" -> "ambient_rain.wav"
            "waves" -> "ambient_waves.wav"
            "white-noise" -> "ambient_white_noise.wav"
            else -> null
        }
    }
    val deletionService = LocalDataDeletionService(
        repository = repository,
        clearers = listOf(
            LocalDataClearer { sleepStore.clear() },
            LocalDataClearer { audioPreferences.clear() },
        ),
    )
    SoineApp(repository, audioPreferences, deletionService, audioController)
}
