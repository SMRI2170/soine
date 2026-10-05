package app.soine

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import app.soine.audio.AndroidAmbientAudioPreferencesStore
import app.soine.audio.ForegroundAmbientAudioController
import app.soine.privacy.LocalDataClearer
import app.soine.privacy.LocalDataDeletionService
import app.soine.sleep.StoredSleepSessionRepository
import app.soine.storage.AndroidSleepSessionStore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sleepStore = AndroidSleepSessionStore(applicationContext)
        val repository = StoredSleepSessionRepository(sleepStore)
        val audioPreferences = AndroidAmbientAudioPreferencesStore(applicationContext)
        val audioController = ForegroundAmbientAudioController(applicationContext) { sound ->
            when (sound.id) {
                "rain" -> R.raw.ambient_rain
                "waves" -> R.raw.ambient_waves
                "white-noise" -> R.raw.ambient_white_noise
                else -> 0
            }
        }
        val deletionService = LocalDataDeletionService(
            repository = repository,
            clearers = listOf(
                LocalDataClearer { sleepStore.clear() },
                LocalDataClearer { audioPreferences.clear() },
            ),
        )
        setContent {
            SoineApp(
                repository = repository,
                audioPreferencesStore = audioPreferences,
                localDataDeletionService = deletionService,
                ambientAudioController = audioController,
            )
        }
    }
}
