package app.soine.audio

import kotlin.test.*

class AmbientAudioControllerTest {
    @Test fun playPauseResumeStopTransitionsAreObservable() {
        val controller = InMemoryAmbientAudioController()
        val seen = mutableListOf<AmbientPlaybackStatus>()
        val subscription = controller.observe { seen += it.status }

        controller.play(AmbientSounds.Rain)
        controller.pause()
        controller.resume()
        controller.stop()
        subscription.close()

        assertEquals(listOf(
            AmbientPlaybackStatus.STOPPED,
            AmbientPlaybackStatus.PLAYING,
            AmbientPlaybackStatus.PAUSED,
            AmbientPlaybackStatus.PLAYING,
            AmbientPlaybackStatus.STOPPED,
        ), seen)
        assertNull(controller.state.sound)
    }

    @Test fun volumeAndStopDeadlineArePartOfState() {
        val controller = InMemoryAmbientAudioController()
        controller.play(AmbientSounds.Waves)
        controller.setVolume(0.2f)
        controller.setStopAt(123_456L)
        assertEquals(0.2f, controller.state.volume)
        assertEquals(123_456L, controller.state.stopAtEpochMillis)
    }

    @Test fun invalidVolumeIsRejected() {
        assertFailsWith<IllegalArgumentException> { InMemoryAmbientAudioController().setVolume(-0.1f) }
    }
}
