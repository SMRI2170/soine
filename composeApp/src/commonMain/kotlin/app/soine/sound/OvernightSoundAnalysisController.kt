package app.soine.sound

/**
 * Platform-neutral boundary for optional overnight microphone analysis.
 *
 * Implementations may capture audio only while a Soine sleep session is active,
 * but raw audio must not cross this boundary or be persisted. [stop] returns
 * derived events captured for the session. A null result means this controller
 * did not own an active capture for that session.
 */
interface OvernightSoundAnalysisController {
    fun start(sessionId: String)
    fun stop(sessionId: String): List<SoundEvent>?
}

object NoOpOvernightSoundAnalysisController : OvernightSoundAnalysisController {
    override fun start(sessionId: String) = Unit

    override fun stop(sessionId: String): List<SoundEvent>? = null
}
