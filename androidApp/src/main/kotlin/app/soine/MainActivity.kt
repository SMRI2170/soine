package app.soine

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import app.soine.audio.AndroidAmbientAudioPreferencesStore
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
        val deletionService = LocalDataDeletionService(
            repository = repository,
            clearers = listOf(
                LocalDataClearer { sleepStore.clear() },
                LocalDataClearer { audioPreferences.clear() },
            ),
        )
        setContent { SoineApp(repository, audioPreferences, deletionService) }
    }
}
