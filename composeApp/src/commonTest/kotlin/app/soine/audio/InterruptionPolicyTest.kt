package app.soine.audio

import kotlin.test.Test
import kotlin.test.assertEquals

class InterruptionPolicyTest {

    @Test
    fun interruptionBeganAlwaysPauses() {
        assertEquals(
            InterruptionDecision.PAUSE,
            InterruptionPolicy.decide(began = true, wasPlaying = false, shouldResume = false),
        )
        assertEquals(
            InterruptionDecision.PAUSE,
            InterruptionPolicy.decide(began = true, wasPlaying = true, shouldResume = false),
        )
        assertEquals(
            InterruptionDecision.PAUSE,
            InterruptionPolicy.decide(began = true, wasPlaying = false, shouldResume = true),
        )
        assertEquals(
            InterruptionDecision.PAUSE,
            InterruptionPolicy.decide(began = true, wasPlaying = true, shouldResume = true),
        )
    }

    @Test
    fun endedAfterPlayingWithShouldResumeResumes() {
        assertEquals(
            InterruptionDecision.RESUME,
            InterruptionPolicy.decide(began = false, wasPlaying = true, shouldResume = true),
        )
    }

    @Test
    fun endedAfterPlayingWithoutShouldResumeStaysPaused() {
        assertEquals(
            InterruptionDecision.STAY_PAUSED,
            InterruptionPolicy.decide(began = false, wasPlaying = true, shouldResume = false),
        )
    }

    @Test
    fun endedWhileNotPlayingStaysPaused() {
        assertEquals(
            InterruptionDecision.STAY_PAUSED,
            InterruptionPolicy.decide(began = false, wasPlaying = false, shouldResume = true),
        )
        assertEquals(
            InterruptionDecision.STAY_PAUSED,
            InterruptionPolicy.decide(began = false, wasPlaying = false, shouldResume = false),
        )
    }
}