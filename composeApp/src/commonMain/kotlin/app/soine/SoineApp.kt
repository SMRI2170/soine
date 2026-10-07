package app.soine

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import app.soine.accessibility.AccessibilityPreferences
import app.soine.accessibility.DefaultAccessibilityPreferences
import app.soine.audio.*
import app.soine.companion.BedtimeSignatureController
import app.soine.companion.BedtimeSignatureRunner
import app.soine.companion.BedtimeSignatureState
import app.soine.companion.CompanionRenderer
import app.soine.companion.CompanionRelationshipStage
import app.soine.companion.NoOpCompanionRenderer
import app.soine.companion.toCompanionRelationshipStage
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
import app.soine.sound.*
import kotlinx.coroutines.Job
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
    companionRenderer: CompanionRenderer = NoOpCompanionRenderer,
    microphonePermissionController: MicrophonePermissionController,
    soundAnalysisPreferencesStore: SoundAnalysisPreferencesStore,
    soundEventRepository: SoundEventRepository,
    overnightSoundAnalysisController: OvernightSoundAnalysisController =
        NoOpOvernightSoundAnalysisController,
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
    val bedtimeSignatureController = remember(companionRenderer) {
        BedtimeSignatureController(companionRenderer)
    }
    val bedtimeSignatureRunner = remember(bedtimeSignatureController) {
        BedtimeSignatureRunner(
            controller = bedtimeSignatureController,
            wait = { millis -> delay(millis) },
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
    var bedtimeSignatureState by remember { mutableStateOf<BedtimeSignatureState?>(null) }
    var bedtimeSignatureJob by remember { mutableStateOf<Job?>(null) }
    var microphonePermissionState by remember(microphonePermissionController) {
        mutableStateOf(microphonePermissionController.state)
    }
    var soundAnalysisPreferences by remember(soundAnalysisPreferencesStore) {
        mutableStateOf(soundAnalysisPreferencesStore.read())
    }
    var pendingSoundAnalysisEnable by remember { mutableStateOf(false) }
    var soundEventSessions by remember { mutableStateOf<List<SoundEventSessionSummary>>(emptyList()) }
    var deletingSoundEvents by remember { mutableStateOf(false) }
    var soundDeletionMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    DisposableEffect(ambientAudioController) {
        val subscription = ambientAudioController.observe { playbackState = it }
        onDispose { subscription.close() }
    }

    DisposableEffect(microphonePermissionController, soundAnalysisPreferencesStore) {
        val subscription = microphonePermissionController.observe { next ->
            microphonePermissionState = next

            if (next == MicrophonePermissionState.GRANTED && pendingSoundAnalysisEnable) {
                val enabled = SoundAnalysisPreferences(enabled = true)
                soundAnalysisPreferencesStore.write(enabled)
                soundAnalysisPreferences = enabled
                pendingSoundAnalysisEnable = false
            } else if (next != MicrophonePermissionState.GRANTED) {
                pendingSoundAnalysisEnable = false
                if (soundAnalysisPreferences.enabled) {
                    val disabled = SoundAnalysisPreferences(enabled = false)
                    soundAnalysisPreferencesStore.write(disabled)
                    soundAnalysisPreferences = disabled
                }
            }
        }
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
                soundAnalysisEnabled = soundAnalysisPreferences.enabled,
                microphonePermissionState = microphonePermissionState,
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
                onSoundAnalysisEnabledChanged = { enabled ->
                    pendingSoundAnalysisEnable = false
                    val next = SoundAnalysisPreferences(
                        enabled = enabled &&
                            microphonePermissionState == MicrophonePermissionState.GRANTED,
                    )
                    soundAnalysisPreferencesStore.write(next)
                    soundAnalysisPreferences = next

                    val sleepingSession =
                        (destination as? BedtimeDestination.Sleeping)?.session
                    if (sleepingSession != null) {
                        if (next.enabled) {
                            // The Settings action is user-visible, satisfying Android's
                            // while-in-use microphone foreground-service start rule.
                            runCatching {
                                overnightSoundAnalysisController.start(sleepingSession.id)
                            }
                        } else {
                            val events = runCatching {
                                overnightSoundAnalysisController.stop(sleepingSession.id)
                            }.getOrNull()
                            if (events != null) {
                                scope.launch {
                                    runCatching {
                                        soundEventRepository.appendSessionEvents(
                                            sleepingSession.id,
                                            events,
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                onRequestMicrophonePermission = {
                    pendingSoundAnalysisEnable = true
                    microphonePermissionController.requestPermission()
                },
                onOpenMicrophoneSettings = {
                    pendingSoundAnalysisEnable = true
                    microphonePermissionController.openAppSettings()
                },
                onPrivacyData = {
                    deletionResult = null
                    soundDeletionMessage = null
                    secondaryScreen = SecondaryScreen.PRIVACY_DATA
                    scope.launch {
                        soundEventSessions = runCatching {
                            soundEventRepository.getAll().sessionSummaries()
                        }.getOrDefault(emptyList())
                    }
                },
                onBack = { secondaryScreen = null },
            )
            SecondaryScreen.PRIVACY_DATA -> PrivacyDataScreen(
                deleting = deletingLocalData,
                deletionResult = deletionResult,
                soundEventSessions = soundEventSessions,
                deletingSoundEvents = deletingSoundEvents,
                soundDeletionMessage = soundDeletionMessage,
                onDeleteSoundSession = { sessionId ->
                    if (!deletingSoundEvents) {
                        scope.launch {
                            deletingSoundEvents = true
                            val result = runCatching {
                                soundEventRepository.deleteSession(sessionId)
                            }
                            soundDeletionMessage = result.fold(
                                onSuccess = { count ->
                                    if (count > 0) count.toString() + "件の音イベントを削除しました。"
                                    else "削除対象の音イベントはありませんでした。"
                                },
                                onFailure = {
                                    "音イベントを削除できませんでした。もう一度お試しください。"
                                },
                            )
                            soundEventSessions = runCatching {
                                soundEventRepository.getAll().sessionSummaries()
                            }.getOrDefault(soundEventSessions)
                            deletingSoundEvents = false
                        }
                    }
                },
                onDeleteAllSoundEvents = {
                    if (!deletingSoundEvents) {
                        scope.launch {
                            deletingSoundEvents = true
                            val result = runCatching { soundEventRepository.deleteAll() }
                            soundDeletionMessage = result.fold(
                                onSuccess = { count ->
                                    when {
                                        count == null ->
                                            "保存されていた音イベントデータを削除しました。"
                                        count > 0 ->
                                            count.toString() + "件の音イベントをすべて削除しました。"
                                        else ->
                                            "保存された音イベントはありませんでした。"
                                    }
                                },
                                onFailure = {
                                    "音イベントを削除できませんでした。もう一度お試しください。"
                                },
                            )
                            soundEventSessions = runCatching {
                                soundEventRepository.getAll().sessionSummaries()
                            }.getOrDefault(soundEventSessions)
                            deletingSoundEvents = false
                        }
                    }
                },
                onDismissSoundDeletionMessage = { soundDeletionMessage = null },
                onDeleteAll = {
                    if (!deletingLocalData) {
                        scope.launch {
                            deletingLocalData = true
                            val result = localDataDeletionService.deleteAll()
                            // Deletion continues across all local stores even when a later
                            // clearer fails. Always reload this store so the UI reflects
                            // what was actually removed rather than the aggregate result.
                            soundEventSessions = runCatching {
                                soundEventRepository.getAll().sessionSummaries()
                            }.getOrDefault(emptyList())
                            if (result == LocalDataDeletionResult.Deleted) {
                                audioCoordinator.endNight()
                                audioPreferences = audioPreferencesStore.read()
                                soundAnalysisPreferences = soundAnalysisPreferencesStore.read()
                                pendingSoundAnalysisEnable = false
                                soundDeletionMessage = null
                                dreamDiscoveries = emptyList()
                                nightMemoryEntries = emptyList()
                                bedtimeSignatureJob?.cancel()
                                bedtimeSignatureJob = null
                                bedtimeSignatureState = null
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
                    soundDeletionMessage = null
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
                            if (
                                soundAnalysisPreferences.enabled &&
                                microphonePermissionState == MicrophonePermissionState.GRANTED
                            ) {
                                // Optional enrichment: microphone failure must never
                                // invalidate or roll back the persisted sleep session.
                                runCatching {
                                    overnightSoundAnalysisController.start(next.session.id)
                                }
                            }
                            audioCoordinator.beginNight()
                            refreshAudioPreferences()
                            bedtimeSignatureJob?.cancel()
                            bedtimeSignatureState = null
                            val relationshipStage = runCatching {
                                companionProgressRepository
                                    .get()
                                    .familiarityStage
                                    .toCompanionRelationshipStage()
                            }.getOrDefault(CompanionRelationshipStage.NEW)
                            bedtimeSignatureJob = scope.launch {
                                runCatching {
                                    bedtimeSignatureRunner.run(relationshipStage) { state ->
                                        bedtimeSignatureState = state
                                    }
                                }
                            }
                        }
                    }
                },
                onWake = {
                    scope.launch {
                        bedtimeSignatureJob?.cancel()
                        bedtimeSignatureJob = null
                        bedtimeSignatureState = null

                        val activeSessionId =
                            (destination as? BedtimeDestination.Sleeping)?.session?.id
                        if (activeSessionId != null) {
                            val derivedEvents = runCatching {
                                overnightSoundAnalysisController.stop(activeSessionId)
                            }.getOrNull()
                            if (derivedEvents != null) {
                                // Persist derived events before completing the session so
                                // a later sleep-finalization failure cannot lose them.
                                runCatching {
                                    soundEventRepository.appendSessionEvents(
                                        activeSessionId,
                                        derivedEvents,
                                    )
                                }
                            }
                        }

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
                    bedtimeSignatureState = null
                    destination = controller.dismissMorning()
                },
                onRetry = { scope.launch { destination = controller.initialDestination() } },
                onOpenDreamAlbum = { secondaryScreen = SecondaryScreen.DREAM_ALBUM },
                onOpenSettings = {
                    microphonePermissionController.refresh()
                    secondaryScreen = SecondaryScreen.SETTINGS
                },
                nightMemoryEntries = nightMemoryEntries,
                sleepingCompanionIntent = bedtimeSignatureState?.step?.intent,
                quietSleepUi = bedtimeSignatureState?.quietUi == true,
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
