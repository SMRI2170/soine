package app.soine.audio

import kotlin.test.*

class SleepTimerTest {
    @Test fun presetsAndRemainingTime() {
        var now = 1_000L
        val audio = InMemoryAmbientAudioController()
        val timer = SleepTimer(audio) { now }
        timer.start(SleepTimerPreset.MINUTES_30)
        assertEquals(30 * 60_000L, timer.state.remainingMillis(now))
        assertEquals(now + 30 * 60_000L, audio.state.stopAtEpochMillis)
        now += 10 * 60_000L
        assertEquals(20 * 60_000L, timer.state.remainingMillis(now))
    }

    @Test fun customTimerMustBePositive() {
        val timer = SleepTimer(InMemoryAmbientAudioController()) { 0L }
        assertFailsWith<IllegalArgumentException> { timer.startCustom(0) }
    }

    @Test fun expiryStopsAudioOnly() {
        var now = 0L
        val audio = InMemoryAmbientAudioController().also { it.play(AmbientSounds.Rain) }
        val timer = SleepTimer(audio) { now }
        timer.startCustom(1)
        now = 60_000L
        assertTrue(timer.tick())
        assertEquals(AmbientPlaybackStatus.STOPPED, audio.state.status)
        assertNull(timer.state.stopAtEpochMillis)
    }

    @Test fun futureDeadlineRestoresAfterRestart() {
        val audio = InMemoryAmbientAudioController()
        val timer = SleepTimer(audio) { 10_000L }
        timer.restore(70_000L)
        assertEquals(60_000L, timer.state.remainingMillis(10_000L))
        assertEquals(70_000L, audio.state.stopAtEpochMillis)
    }

    @Test fun expiredDeadlineStopsImmediatelyOnRestore() {
        val audio = InMemoryAmbientAudioController().also { it.play(AmbientSounds.Waves) }
        val timer = SleepTimer(audio) { 100_000L }
        timer.restore(99_999L)
        assertEquals(AmbientPlaybackStatus.STOPPED, audio.state.status)
    }

    @Test fun cancelClearsDeadlineWithoutStoppingAudio() {
        val audio = InMemoryAmbientAudioController().also { it.play(AmbientSounds.Rain) }
        val timer = SleepTimer(audio) { 0L }
        timer.start(SleepTimerPreset.MINUTES_90)
        timer.cancel()
        assertEquals(AmbientPlaybackStatus.PLAYING, audio.state.status)
        assertNull(audio.state.stopAtEpochMillis)
    }
}
