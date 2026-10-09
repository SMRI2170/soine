package app.soine

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.health.connect.client.PermissionController
import app.soine.accessibility.AndroidAccessibilityPreferences
import app.soine.audio.AndroidAmbientAudioPreferencesStore
import app.soine.audio.ForegroundAmbientAudioController
import app.soine.dream.AndroidDreamDiscoveryStore
import app.soine.dream.StoredDreamDiscoveryRepository
import app.soine.health.AndroidHealthConnectSleepDataSource
import app.soine.health.AndroidHealthPermissionRequestCoordinator
import app.soine.onboarding.AndroidFirstRunRepository
import app.soine.privacy.LocalDataClearer
import app.soine.privacy.LocalDataDeletionService
import app.soine.relationship.AndroidRelationshipStateStore
import app.soine.relationship.StoredCompanionProgressRepository
import app.soine.sleep.StoredSleepSessionRepository
import app.soine.sound.AndroidMicrophonePermissionController
import app.soine.sound.AndroidOvernightSoundAnalysisController
import app.soine.sound.AndroidSoundAnalysisPreferencesStore
import app.soine.sound.AndroidSoundEventStore
import app.soine.sound.StoredSoundEventRepository
import app.soine.storage.AndroidSleepSessionStore

class MainActivity : ComponentActivity() {
    private lateinit var microphonePermissionController: AndroidMicrophonePermissionController
    private lateinit var healthPermissionRequestCoordinator: AndroidHealthPermissionRequestCoordinator
    private lateinit var healthSleepDataSource: AndroidHealthConnectSleepDataSource

    private val microphonePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            microphonePermissionController.refresh()
        }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            // Notification permission only controls user-visible disclosure.
            // Sound analysis remains optional and sleep start never depends on it.
        }

    private val healthPermissionLauncher =
        registerForActivityResult(
            PermissionController.createRequestPermissionResultContract(),
        ) { grantedPermissions ->
            if (::healthPermissionRequestCoordinator.isInitialized) {
                healthPermissionRequestCoordinator.complete(grantedPermissions)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sleepStore = AndroidSleepSessionStore(applicationContext)
        val repository = StoredSleepSessionRepository(sleepStore)
        val audioPreferences = AndroidAmbientAudioPreferencesStore(applicationContext)
        val soundAnalysisPreferences = AndroidSoundAnalysisPreferencesStore(applicationContext)
        val soundEventStore = AndroidSoundEventStore(applicationContext)
        val soundEventRepository = StoredSoundEventRepository(soundEventStore)
        val overnightSoundAnalysisController =
            AndroidOvernightSoundAnalysisController(applicationContext) {
                requestSoundAnalysisNotificationPermissionIfNeeded()
            }
        val relationshipStore = AndroidRelationshipStateStore(applicationContext)
        val companionProgressRepository = StoredCompanionProgressRepository(relationshipStore)
        val dreamStore = AndroidDreamDiscoveryStore(applicationContext)
        val dreamDiscoveryRepository = StoredDreamDiscoveryRepository(dreamStore)
        val accessibilityPreferences = AndroidAccessibilityPreferences(applicationContext)
        microphonePermissionController = AndroidMicrophonePermissionController(this) {
            microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
        healthPermissionRequestCoordinator = AndroidHealthPermissionRequestCoordinator()
        healthSleepDataSource = AndroidHealthConnectSleepDataSource(applicationContext) { permissions ->
            healthPermissionRequestCoordinator.request(healthPermissionLauncher, permissions)
        }
        val audioController = ForegroundAmbientAudioController(applicationContext) { sound ->
            when (sound.id) {
                "rain" -> R.raw.ambient_rain
                "waves" -> R.raw.ambient_waves
                "white-noise" -> R.raw.ambient_white_noise
                else -> 0
            }
        }
        val firstRunRepository = AndroidFirstRunRepository(applicationContext)
        val deletionService = LocalDataDeletionService(
            repository = repository,
            clearers = listOf(
                LocalDataClearer { sleepStore.clear() },
                LocalDataClearer { audioPreferences.clear() },
                LocalDataClearer { soundAnalysisPreferences.clear() },
                LocalDataClearer { soundEventStore.clear() },
                LocalDataClearer { relationshipStore.clear() },
                LocalDataClearer { dreamStore.clear() },
                LocalDataClearer { firstRunRepository.resetForReplay() },
            ),
        )
        setContent {
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
                soundEventRepository = soundEventRepository,
                overnightSoundAnalysisController = overnightSoundAnalysisController,
                firstRunRepository = firstRunRepository,
            )
        }
    }

    private fun requestSoundAnalysisNotificationPermissionIfNeeded() {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onResume() {
        super.onResume()
        if (::microphonePermissionController.isInitialized) {
            microphonePermissionController.refresh()
        }
    }

    override fun onDestroy() {
        if (::healthPermissionRequestCoordinator.isInitialized) {
            healthPermissionRequestCoordinator.cancel()
        }
        super.onDestroy()
    }
}
