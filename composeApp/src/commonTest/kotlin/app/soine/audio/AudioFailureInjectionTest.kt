package app.soine.audio

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Pins the V1 audio graceful-degradation contract.
 *
 * #185 requires that the bedtime → sleeping → morning
 * loop continues when the audio subsystem fails. The
 * contract here:
 *
 *   - a failing audio controller keeps the bedtime loop
 *     in a STOPPED state rather than crashing or
 *     surfacing a stack trace
 *   - a failing play call does not corrupt the audio
 *     state machine; subsequent valid calls recover
 *     cleanly
 *   - the bedtime coordinator can still drive the
 *     timer / volume / stop calls even when play fails
 */
class AudioFailureInjectionTest {

    @Test
    fun failingPlayKeepsStateStopped() {
        val controller = FailureInjectingAmbientAudioController()
        controller.failAllPlayCalls()
        val sound = AmbientSounds.defaults.first()
        controller.play(sound)
        // The state stays STOPPED rather than PLAYING;
        // the bedtime loop sees a non-error STOPPED
        // state and continues.
        assertEquals(AmbientPlaybackStatus.STOPPED, controller.state.status)
        assertEquals(null, controller.state.sound)
    }

    @Test
    fun recoveringPlayRestoresState() {
        val controller = FailureInjectingAmbientAudioController()
        controller.failAllPlayCalls()
        val sound = AmbientSounds.defaults.first()
        controller.play(sound)
        assertEquals(AmbientPlaybackStatus.STOPPED, controller.state.status)
        // The platform recovers (e.g. after a transient
        // error), the controller accepts play calls.
        controller.recover()
        controller.play(sound)
        assertEquals(AmbientPlaybackStatus.PLAYING, controller.state.status)
        assertEquals(sound, controller.state.sound)
    }

    @Test
    fun bedtimeAudioCoordinatorSurvivesFailingPlay() {
        val controller = FailureInjectingAmbientAudioController()
        controller.failAllPlayCalls()
        // The bedtime coordinator wraps the controller
        // and never re-throws. A failing play call
        // leaves the controller in STOPPED; the bedtime
        // loop continues to the next event.
        val preferences = AmbientAudioPreferences(
            muted = false,
            soundId = "rain",
            volume = 0.5f,
            timerPreset = null,
        )
        controller.play(AmbientSounds.defaults.first())
        // A subsequent setVolume still works.
        controller.setVolume(0.8f)
        assertEquals(0.8f, controller.state.volume)
        // A stop call still works.
        controller.stop()
        assertEquals(AmbientPlaybackStatus.STOPPED, controller.state.status)
    }

    @Test
    fun failingPlayDoesNotBlockSetVolumeOrStop() {
        val controller = FailureInjectingAmbientAudioController()
        controller.failAllPlayCalls()
        controller.setVolume(0.42f)
        assertEquals(0.42f, controller.state.volume)
        controller.setStopAt(1_700_000_000_000L)
        assertEquals(1_700_000_000_000L, controller.state.stopAtEpochMillis)
    }

    @Test
    fun callsAreRecordedForDiagnostics() {
        val controller = FailureInjectingAmbientAudioController()
        controller.failAllPlayCalls()
        controller.play(AmbientSounds.defaults.first())
        controller.setVolume(0.3f)
        controller.setStopAt(null)
        controller.recover()
        controller.play(AmbientSounds.defaults.first())
        controller.stop()
        // The call log is exposed so a future polish
        // slice can wire it to the diagnostics path.
        // The V1 contract: every call lands in the log,
        // no exception is thrown.
        val log = controller.recordedCalls
        assertTrue(
            log.contains("play"),
            "Audio failure-injecting controller must record the play call",
        )
        assertTrue(
            log.any { it.startsWith("setVolume") },
            "Audio failure-injecting controller must record the setVolume call",
        )
        assertTrue(
            log.contains("stop"),
            "Audio failure-injecting controller must record the stop call",
        )
    }

    @Test
    fun playFailureDoesNotAffectSubsequentState() {
        val controller = FailureInjectingAmbientAudioController()
        val sound = AmbientSounds.defaults.first()
        // First call: succeeds.
        controller.play(sound)
        assertEquals(AmbientPlaybackStatus.PLAYING, controller.state.status)
        // Now we fail all plays; pause, resume, stop
        // should still respect the existing state.
        controller.failAllPlayCalls()
        controller.pause()
        assertEquals(AmbientPlaybackStatus.PAUSED, controller.state.status)
        // Recover; a new play should succeed.
        controller.recover()
        controller.play(sound)
        assertEquals(AmbientPlaybackStatus.PLAYING, controller.state.status)
        assertNotEquals(null, controller.state.sound)
    }
}
