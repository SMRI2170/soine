package app.soine.audio

/**
 * Pure decision function extracted from the iOS ambient-audio
 * controller's interruption handling.
 *
 * AVAudioSession interruption handling requires two pieces of
 * information to decide what to do:
 *
 * - whether the notification marks the start or the end of the
 *   interruption, and
 * - whether the system signalled that playback should resume once it
 *   ends (the `shouldResume` flag from
 *   `AVAudioSessionInterruptionOptionKey`).
 *
 * The controller already knows whether it was playing at the moment
 * the interruption began (it caches that fact in `resumeAfterInterruption`).
 * This object turns the three inputs into a single decision so the
 * state machine is unit-testable without driving AVAudioSession.
 */
enum class InterruptionDecision {
    PAUSE,
    RESUME,
    STAY_PAUSED,
}

object InterruptionPolicy {
    /**
     * @param began true if this is the interruption-began notification;
     *              false if it is the interruption-ended notification.
     * @param wasPlaying true if the controller was actively playing
     *                  when the interruption began.
     * @param shouldResume true if the system attached the
     *                    `AVAudioSession.InterruptionOptionShouldResume`
     *                    flag to the interruption-ended notification.
     */
    fun decide(
        began: Boolean,
        wasPlaying: Boolean,
        shouldResume: Boolean,
    ): InterruptionDecision = when {
        began -> InterruptionDecision.PAUSE
        wasPlaying && shouldResume -> InterruptionDecision.RESUME
        else -> InterruptionDecision.STAY_PAUSED
    }
}