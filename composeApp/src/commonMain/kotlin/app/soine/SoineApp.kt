package app.soine

import androidx.compose.material3.MaterialTheme
import app.soine.accessibility.AccessibilityPreferences
import app.soine.accessibility.DefaultAccessibilityPreferences
import androidx.compose.runtime.*
import app.soine.audio.*
import app.soine.dream.DreamDiscovery
import app.soine.dream.DreamDiscoveryCoordinator
import app.soine.dream.DreamDiscoveryRepository
import app.soine.dream.InitialDreamCatalog
import app.soine.navigation.*
import app.soine.night.InitialNightEventCatalog
import app.soine.night.NightEventEngineInput
import app.soine.privacy.LocalDataDeletionResult
import app.soine.privacy.LocalDataDeletionService
import app.soine.relationship.CompanionProgressRepository
import app.soine.sleep.SleepSessionRepository
import app.soine.sleep.currentTimeMillis
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class SecondaryScreen {
    DREAM_ALBUM,
    SETTINGS,
    PRIVACY_DATA,
}

@Composable
fun SoineApp(
    repository: SleepSessionRepository,
    audioPreferencesStore: AmbientAudioPreferencesStore,
    localDataDeletionService: LocalDataDeletionService,
    ambientAudioController: AmbientAudioController,
    dreamDiscoveryRepository: DreamDiscoveryRepository,
    companionProgressRepository: CompanionProgressRepository,
    accessibilityPreferences: AccessibilityPreferences = DefaultAccessibilityPreferences,
    appVersion: String = "0.1.0",
) {
    val controller = remember(repository) { BedtimeFlowController(repository) }
    val audioCoordinator = remember(ambientAudioController, audioPreferencesStore) {
        BedtimeAudioCoordinator(ambientAudioController, audioPreferencesStore, ::currentTimeMillis)
    }
    val dreamCoordinator = remember(dreamDiscoveryRepository, companionProgressRepository) {
        DreamDiscoveryCoordinator(
            repository = dreamDiscoveryRepository,
            relationshipRepository = companionProgressRepository,
        )
    }
    var destination by remember { mutableStateOf<BedtimeDestination>(BedtimeDestination.Loading) }
    var secondaryScreen by remember { mutableStateOf<SecondaryScreen?>(null) }
    var audioPreferences by remember(audioPreferencesStore) { mutableStateOf(audioPreferencesStore.read()) }
    var playbackState by remember { mutableStateOf(ambientAudioController.state) }
    var nowEpochMillis by remember { mutableStateOf(currentTimeMillis()) }
    var deletingLocalData by remember { mutableStateOf(false) }
    var deletionResult by remember { mutableStateOf<LocalDataDeletionResult?>(null) }
    var dreamDiscoveries by remember { mutableStateOf<List<DreamDiscovery>>(emptyList()) }
    var nightMemoryEntries by remember { mutableStateOf<List<NightMemoryEntry>>(emptyList()) }
    val scope = rememberCoroutineScope()

    DisposableEffect(ambientAudioController) {
        val subscription = ambientAudioController.observe { playbackState = it }
        onDispose { subscription.close() }
    }

    fun refreshAudioPreferences() {
        audioPreferences = audioPreferencesStore.read()
        nowEpochMillis = currentTimeMillis()
    }

    fun persistAudioPreferences(next: AmbientAudioPreferences) {
        audioPreferencesStore.write(next)
        audioPreferences = next
    }

    LaunchedEffect(controller, dreamCoordinator) {
        destination = controller.initialDestination()
        if (destination is BedtimeDestination.Sleeping) {
            audioCoordinator.recoverNight()
            refreshAudioPreferences()
        }

        try {
            repository.getCompletedSessions().firstOrNull()?.let { latest ->
                dreamCoordinator.evaluateIfNeeded(latest)
            }
            dreamDiscoveries = dreamCoordinator.discoveries()
        } catch (_: Throwable) {
            // Dream persistence is optional enrichment and must not block the sleep loop.
            dreamDiscoveries = emptyList()
        }
    }

    LaunchedEffect(destination, audioPreferences.timerStopAtEpochMillis) {
        while (destination is BedtimeDestination.Sleeping) {
            delay(1_000)
            nowEpochMillis = currentTimeMillis()
            if (audioCoordinator.tick()) refreshAudioPreferences()
        }
    }

    val selectedSound = AmbientSounds.find(audioPreferences.soundId) ?: AmbientSounds.Rain
    val ambientLabel = if (audioPreferences.muted) "なし" else selectedSound.displayName
    val defaultTimerLabel = audioPreferences.timerPreset?.let { it.minutes.toString() + "分" } ?: "オフ"
    val remainingMillis = audioPreferences.timerStopAtEpochMillis?.let {
        (it - nowEpochMillis).coerceAtLeast(0L)
    }
    val reduceMotion = accessibilityPreferences.reduceMotionEnabled()
    val remainingLabel = remainingMillis?.let {
        val totalSeconds = (it + 999L) / 1_000L
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        minutes.toString() + "分" + seconds.toString().padStart(2, '0') + "秒"
    }
    val dreamAlbumEntries = remember(dreamDiscoveries) {
        buildDreamAlbumEntries(
            definitions = InitialDreamCatalog.definitions,
            discoveries = dreamDiscoveries,
        )
    }

    MaterialTheme {
        when (secondaryScreen) {
            SecondaryScreen.DREAM_ALBUM -> DreamAlbumScreen(
                entries = dreamAlbumEntries,
                onBack = { secondaryScreen = null },
            )
            SecondaryScreen.SETTINGS -> SettingsScreen(
                preferences = audioPreferences,
                appVersion = appVersion,
                onSoundSelected = { soundId ->
                    persistAudioPreferences(audioPreferences.copy(soundId = soundId))
                },
                onMutedChanged = { muted ->
                    persistAudioPreferences(audioPreferences.copy(muted = muted))
                },
                onVolumeChanged = { volume ->
                    persistAudioPreferences(audioPreferences.copy(volume = volume))
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
                                audioCoordinator.endNight()
                                audioPreferences = audioPreferencesStore.read()
                                dreamDiscoveries = emptyList()
                                nightMemoryEntries = emptyList()
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
                onStartSleep = {
                    scope.launch {
                        val next = controller.start()
                        destination = next
                        if (next is BedtimeDestination.Sleeping) {
                            audioCoordinator.beginNight()
                            refreshAudioPreferences()
                        }
                    }
                },
                onWake = {
                    scope.launch {
                        audioCoordinator.endNight()
                        refreshAudioPreferences()
                        val next = controller.finish()
                        destination = next
                        if (next is BedtimeDestination.Morning) {
                            try {
                                dreamCoordinator.evaluateIfNeeded(next.session)
                                dreamDiscoveries = dreamCoordinator.discoveries()
                            } catch (_: Throwable) {
                                // The completed sleep session remains valid even if dream storage fails.
                            }
                            nightMemoryEntries = try {
                                val relationship = companionProgressRepository.get()
                                val events = InitialNightEventCatalog.engine.generate(
                                    NightEventEngineInput(
                                        session = next.session,
                                        relationship = relationship,
                                        maxEvents = 3,
                                    )
                                )
                                buildNightMemoryEntries(events)
                            } catch (_: Throwable) {
                                emptyList()
                            }
                        }
                    }
                },
                onDone = {
                    nightMemoryEntries = emptyList()
                    destination = controller.dismissMorning()
                },
                onRetry = { scope.launch { destination = controller.initialDestination() } },
                onOpenDreamAlbum = { secondaryScreen = SecondaryScreen.DREAM_ALBUM },
                onOpenSettings = { secondaryScreen = SecondaryScreen.SETTINGS },
                nightMemoryEntries = nightMemoryEntries,
                reduceMotion = reduceMotion,
                ambientSoundLabel = ambientLabel,
                defaultTimerLabel = defaultTimerLabel,
                audioPlaying = playbackState.status == AmbientPlaybackStatus.PLAYING,
                remainingTimerLabel = remainingLabel,
                onToggleAudio = {
                    audioCoordinator.togglePlayback()
                    refreshAudioPreferences()
                },
                onSetTimer = { minutes ->
                    audioCoordinator.startTimer(minutes)
                    refreshAudioPreferences()
                },
                onCancelTimer = {
                    audioCoordinator.cancelTimer()
                    refreshAudioPreferences()
                },
            )
        }
    }
}
