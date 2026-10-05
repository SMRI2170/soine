package app.soine.audio

import kotlin.test.*

class PersistentSleepTimerTest {
    @Test fun presetPersistsAbsoluteDeadline() {
        val store = FakePreferencesStore()
        val audio = InMemoryAmbientAudioController()
        val timer = PersistentSleepTimer(SleepTimer(audio) { 1_000L }, store)

        timer.start(SleepTimerPreset.MINUTES_30)

        assertEquals(SleepTimerPreset.MINUTES_30, store.value.timerPreset)
        assertEquals(1_801_000L, store.value.timerStopAtEpochMillis)
    }

    @Test fun restoreKeepsFutureDeadline() {
        val store = FakePreferencesStore(
            AmbientAudioPreferences(timerStopAtEpochMillis = 70_000L)
        )
        val audio = InMemoryAmbientAudioController()
        val timer = PersistentSleepTimer(SleepTimer(audio) { 10_000L }, store)

        timer.restore()

        assertEquals(70_000L, timer.state.stopAtEpochMillis)
        assertEquals(60_000L, timer.state.remainingMillis(10_000L))
    }

    @Test fun restoreClearsExpiredDeadlineAndStopsAudio() {
        val store = FakePreferencesStore(
            AmbientAudioPreferences(timerStopAtEpochMillis = 9_999L)
        )
        val audio = InMemoryAmbientAudioController().also { it.play(AmbientSounds.Rain) }
        val timer = PersistentSleepTimer(SleepTimer(audio) { 10_000L }, store)

        timer.restore()

        assertEquals(AmbientPlaybackStatus.STOPPED, audio.state.status)
        assertNull(store.value.timerStopAtEpochMillis)
    }

    @Test fun customTimerClearsPresetButPersistsDeadline() {
        val store = FakePreferencesStore(
            AmbientAudioPreferences(timerPreset = SleepTimerPreset.MINUTES_60)
        )
        val timer = PersistentSleepTimer(
            SleepTimer(InMemoryAmbientAudioController()) { 0L },
            store,
        )

        timer.startCustom(15)

        assertNull(store.value.timerPreset)
        assertEquals(900_000L, store.value.timerStopAtEpochMillis)
    }

    @Test fun cancelClearsDeadlineWithoutChangingPreset() {
        val store = FakePreferencesStore(
            AmbientAudioPreferences(timerPreset = SleepTimerPreset.MINUTES_90)
        )
        val timer = PersistentSleepTimer(
            SleepTimer(InMemoryAmbientAudioController()) { 0L },
            store,
        )
        timer.start(SleepTimerPreset.MINUTES_90)

        timer.cancel()

        assertEquals(SleepTimerPreset.MINUTES_90, store.value.timerPreset)
        assertNull(store.value.timerStopAtEpochMillis)
    }
}

private class FakePreferencesStore(
    var value: AmbientAudioPreferences = AmbientAudioPreferences(),
) : AmbientAudioPreferencesStore {
    override fun read() = value
    override fun write(preferences: AmbientAudioPreferences) { value = preferences }
    override fun clear() { value = AmbientAudioPreferences() }
}
