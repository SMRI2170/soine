package app.soine

import androidx.compose.ui.window.ComposeUIViewController
import app.soine.accessibility.IosAccessibilityPreferences
import app.soine.audio.IosAmbientAudioController
import app.soine.audio.IosAmbientAudioPreferencesStore
import app.soine.dream.IosDreamDiscoveryStore
import app.soine.dream.StoredDreamDiscoveryRepository
import app.soine.privacy.LocalDataClearer
import app.soine.privacy.LocalDataDeletionService
import app.soine.relationship.IosRelationshipStateStore
import app.soine.relationship.StoredCompanionProgressRepository
import app.soine.sleep.StoredSleepSessionRepository
import app.soine.sound.IosMicrophonePermissionController
import app.soine.sound.IosSoundAnalysisPreferencesStore
import app.soine.storage.IosSleepSessionStore

fun MainViewController() = ComposeUIViewController {
    val sleepStore = IosSleepSessionStore()
    val repository = StoredSleepSessionRepository(sleepStore)
    val audioPreferences = IosAmbientAudioPreferencesStore()
    val soundAnalysisPreferences = IosSoundAnalysisPreferencesStore()
    val relationshipStore = IosRelationshipStateStore()
    val companionProgressRepository = StoredCompanionProgressRepository(relationshipStore)
    val dreamStore = IosDreamDiscoveryStore()
    val dreamDiscoveryRepository = StoredDreamDiscoveryRepository(dreamStore)
    val accessibilityPreferences = IosAccessibilityPreferences()
    val microphonePermissionController = IosMicrophonePermissionController()
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
            LocalDataClearer { soundAnalysisPreferences.clear() },
            LocalDataClearer { relationshipStore.clear() },
            LocalDataClearer { dreamStore.clear() },
        ),
    )
    SoineApp(
        repository = repository,
        audioPreferencesStore = audioPreferences,
        localDataDeletionService = deletionService,
        ambientAudioController = audioController,
        dreamDiscoveryRepository = dreamDiscoveryRepository,
        companionProgressRepository = companionProgressRepository,
        accessibilityPreferences = accessibilityPreferences,
        microphonePermissionController = microphonePermissionController,
        soundAnalysisPreferencesStore = soundAnalysisPreferences,
    )
}
