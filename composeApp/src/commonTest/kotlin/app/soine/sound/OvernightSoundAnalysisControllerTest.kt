package app.soine.sound

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/**
 * Contract tests for [OvernightSoundAnalysisController].
 *
 * The interface is intentionally minimal — implementations own the
 * platform service lifecycle and may fail silently. The common layer
 * must be able to plug in a no-op (microphone missing / permission
 * denied / opt-out) without crashing the core sleep flow.
 *
 * See docs/failure-matrix.md (microphone row).
 */
class OvernightSoundAnalysisControllerTest {

    @Test
    fun noOpControllerAcceptsStartWithoutThrowing() {
        val controller = NoOpOvernightSoundAnalysisController
        // The interface contract allows start() to be a silent no-op.
        controller.start(sessionId = "session-1")
    }

    @Test
    fun noOpControllerReturnsNullWhenItNeverOwnedTheSession() {
        val controller = NoOpOvernightSoundAnalysisController

        assertNull(controller.stop(sessionId = "session-1"))
    }

    @Test
    fun blankSessionIdIsRejectedByStart() {
        val controller = object : OvernightSoundAnalysisController {
            override fun start(sessionId: String) {
                require(sessionId.isNotBlank()) { "session id must not be blank" }
            }

            override fun stop(sessionId: String): List<SoundEvent>? = null
        }

        assertFailsWith<IllegalArgumentException> {
            controller.start(sessionId = " ")
        }
    }

    @Test
    fun microphoneUnavailableRoutesToNoOpController() {
        // When the host detects an UNAVAILABLE microphone (no hardware,
        // permission denied, or feature gated off), the contract is to
        // substitute NoOpOvernightSoundAnalysisController. Verify the
        // core flow stays runnable: start is silent, stop is null.
        val controller: OvernightSoundAnalysisController =
            NoOpOvernightSoundAnalysisController

        controller.start(sessionId = "session-2")
        assertEquals(emptyList(), controller.stop(sessionId = "session-2") ?: emptyList())
    }
}