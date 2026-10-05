package app.soine.audio

import kotlin.test.*

class BedtimeAudioCoordinatorTest {
    @Test fun mutedNightStartsWithoutAudioOrTimer() {
        val store = MemoryPreferencesStore(AmbientAudioPreferences(muted = true, timerPreset = SleepTimerPreset.MINUTES_30))
        val audio = InMemoryAmbientAudioController()
        val coordinator = BedtimeAudioCoordinator(audio, store) { 1_000L }

        coordinator.beginNight()

        assertEquals(AmbientPlaybackStatus.STOPPED, audio.state.status)
        assertNull(store.value.timerStopAtEpochMillis)
    }

    @Test fun enabledNightStartsSelectedSoundAndPresetTimer() {
        val store = MemoryPreferencesStore(
            AmbientAudioPreferences(
                soundId = AmbientSounds.Waves.id,
                volume = 0.2f,
                timerPreset = SleepTimerPreset.MINUTES_30,
                muted = false,
            )
        )
        val audio = InMemoryAmbientAudioController()
        val coordinator = BedtimeAudioCoordinator(audio, store) { 1_000L }

        coordinator.beginNight()

        assertEquals(AmbientSounds.Waves, audio.state.sound)
        assertEquals(0.2f, audio.state.volume)
        assertEquals(1_801_000L, store.value.timerStopAtEpochMillis)
    }

    @Test fun expiredRecoveredTimerDoesNotRestartAudio() {
        val store = MemoryPreferencesStore(
            AmbientAudioPreferences(
                timerPreset = SleepTimerPreset.MINUTES_30,
                timerStopAtEpochMillis = 999L,
                muted = false,
            )
        )
        val audio = InMemoryAmbientAudioController()
        val coordinator = BedtimeAudioCoordinator(audio, store) { 1_000L }

        coordinator.recoverNight()

        assertEquals(AmbientPlaybackStatus.STOPPED, audio.state.status)
        assertNull(store.value.timerStopAtEpochMillis)
    }

    @Test fun customTimerCanReplacePresetDeadline() {
        val store = MemoryPreferencesStore(AmbientAudioPreferences(muted = false))
        val coordinator = BedtimeAudioCoordinator(InMemoryAmbientAudioController(), store) { 5_000L }

        coordinator.startTimer(15)

        assertEquals(905_000L, store.value.timerStopAtEpochMillis)
        assertNull(store.value.timerPreset)
    }

    @Test fun endingNightStopsAudioAndClearsActiveDeadline() {
        val store = MemoryPreferencesStore(AmbientAudioPreferences(muted = false, timerPreset = SleepTimerPreset.MINUTES_60))
        val audio = InMemoryAmbientAudioController()
        val coordinator = BedtimeAudioCoordinator(audio, store) { 0L }
        coordinator.beginNight()

        coordinator.endNight()

        assertEquals(AmbientPlaybackStatus.STOPPED, audio.state.status)
        assertNull(store.value.timerStopAtEpochMillis)
        assertEquals(SleepTimerPreset.MINUTES_60, store.value.timerPreset)
    }
}

private class MemoryPreferencesStore(
    var value: AmbientAudioPreferences,
) : AmbientAudioPreferencesStore {
    override fun read() = value
    override fun write(preferences: AmbientAudioPreferences) { value = preferences }
    override fun clear() { value = AmbientAudioPreferences() }
}
