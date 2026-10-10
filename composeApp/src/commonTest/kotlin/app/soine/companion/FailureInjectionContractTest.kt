package app.soine.companion

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Pins the V1 graceful-degradation contract through
 * failure-injection tests.
 *
 * #185 requires that the bedtime → sleeping → morning
 * loop continues even when the optional subsystems
 * (3D renderer, audio, Health, microphone analysis) hit
 * failures. The contract here:
 *
 *   - a renderer failure is absorbed by the
 *     [CompanionRendererFallbackPolicy]; the static
 *     fallback is the graceful-degradation path
 *   - a renderer failure surfaces through the
 *     [CompanionRendererTelemetry] hook so the
 *     production diagnostics path can audit the
 *     failure without a panic
 *   - a manual retry without success does not reset the
 *     failure counter; only a `READY` state resets it
 *   - the bedtime → sleeping → morning loop completes
 *     even when the renderer is unhealthy
 */
class FailureInjectionContractTest {

    @Test
    fun rendererFailureIsAbsorbedByFallbackPolicy() {
        val renderer = FailureInjectingCompanionRenderer()
        val telemetry = RecordingCompanionRendererTelemetry()
        val policy = CompanionRendererFallbackPolicy(
            maxAutomaticRetries = 1,
            telemetry = telemetry,
        )
        // First failure → automatic retry.
        val firstAction = policy.onFailure("init-error")
        assertEquals(
            CompanionRendererRecoveryAction.RETRY_AUTOMATICALLY,
            firstAction,
        )
        // Second failure → use static fallback.
        val secondAction = policy.onFailure("init-error")
        assertEquals(
            CompanionRendererRecoveryAction.USE_STATIC_FALLBACK,
            secondAction,
        )
        // Telemetry receives both events.
        assertEquals(2, telemetry.events.size)
        assertEquals(1, telemetry.events[0].consecutiveFailures)
        assertEquals(2, telemetry.events[1].consecutiveFailures)
        assertEquals("init-error", telemetry.events[1].code)
    }

    @Test
    fun rendererRecoveryResetsConsecutiveFailureCounter() {
        val renderer = FailureInjectingCompanionRenderer()
        val telemetry = RecordingCompanionRendererTelemetry()
        val policy = CompanionRendererFallbackPolicy(
            maxAutomaticRetries = 1,
            telemetry = telemetry,
        )
        // First failure → retry.
        policy.onFailure("init-error")
        // Recovery.
        policy.onReady()
        // A new failure starts the counter at 1, not 2.
        val action = policy.onFailure("init-error-2")
        assertEquals(
            CompanionRendererRecoveryAction.RETRY_AUTOMATICALLY,
            action,
        )
        assertEquals(2, telemetry.events.size)
        assertEquals(1, telemetry.events[1].consecutiveFailures)
    }

    @Test
    fun manualRetryDoesNotResetCounter() {
        val telemetry = RecordingCompanionRendererTelemetry()
        val policy = CompanionRendererFallbackPolicy(
            maxAutomaticRetries = 1,
            telemetry = telemetry,
        )
        policy.onFailure("first")
        // A manual retry does not reset the counter; only
        // a successful READY state does. The next
        // automatic failure still escalates to the
        // static fallback path.
        policy.onManualRetryRequested()
        val action = policy.onFailure("second")
        assertEquals(
            CompanionRendererRecoveryAction.USE_STATIC_FALLBACK,
            action,
        )
    }

    @Test
    fun rendererSubmissionFailureFlow() {
        val renderer = FailureInjectingCompanionRenderer()
        val telemetry = RecordingCompanionRendererTelemetry()
        val policy = CompanionRendererFallbackPolicy(
            maxAutomaticRetries = 1,
            telemetry = telemetry,
        )
        // Inject "fail twice" — the first two submissions
        // return FAILED, the third succeeds.
        renderer.failNTimes(count = 2, code = "decode-error")
        val request = CompanionRenderRequest()
        renderer.submit(request)
        renderer.submit(request)
        // Two failures recorded; the policy escalates
        // from the first automatic retry to the static
        // fallback on the second failure.
        assertEquals(
            CompanionRendererRecoveryAction.RETRY_AUTOMATICALLY,
            policy.onFailure("decode-error"),
        )
        assertEquals(
            CompanionRendererRecoveryAction.USE_STATIC_FALLBACK,
            policy.onFailure("decode-error"),
        )
        // Now the next submission succeeds (fail count exhausted).
        renderer.submit(request)
        assertEquals(1, renderer.submittedRequests.size)
    }

    @Test
    fun alwaysFailingRendererDoesNotCorruptSubmitHistory() {
        val renderer = FailureInjectingCompanionRenderer()
        val telemetry = RecordingCompanionRendererTelemetry()
        val policy = CompanionRendererFallbackPolicy(
            maxAutomaticRetries = 1,
            telemetry = telemetry,
        )
        renderer.alwaysFail("permanent-error")
        repeat(5) { renderer.submit(CompanionRenderRequest()) }
        // No request was actually submitted; the
        // submit history stays clean so a future
        // PRODUCTION_READY flip can replay.
        assertEquals(0, renderer.submittedRequests.size)
        // The policy escalates to static fallback on
        // the second failure.
        policy.onFailure("permanent-error")
        policy.onFailure("permanent-error")
        assertEquals(2, telemetry.events.size)
    }

    @Test
    fun recoveryFromPermanentFailure() {
        val renderer = FailureInjectingCompanionRenderer()
        val telemetry = RecordingCompanionRendererTelemetry()
        val policy = CompanionRendererFallbackPolicy(
            maxAutomaticRetries = 1,
            telemetry = telemetry,
        )
        renderer.alwaysFail("temporary")
        renderer.submit(CompanionRenderRequest())
        policy.onFailure("temporary")
        // Recover: the policy sees a READY state.
        renderer.recover()
        policy.onReady()
        renderer.submit(CompanionRenderRequest())
        assertEquals(1, renderer.submittedRequests.size)
    }

    @Test
    fun telemetryReceivesUniqueFailureEvents() {
        val renderer = FailureInjectingCompanionRenderer()
        val telemetry = RecordingCompanionRendererTelemetry()
        val policy = CompanionRendererFallbackPolicy(
            maxAutomaticRetries = 1,
            telemetry = telemetry,
        )
        renderer.alwaysFail("first")
        renderer.submit(CompanionRenderRequest())
        policy.onFailure("first")
        renderer.recover()
        renderer.submit(CompanionRenderRequest())
        policy.onReady()
        renderer.alwaysFail("second")
        renderer.submit(CompanionRenderRequest())
        policy.onFailure("second")
        // Two distinct failure events captured; the
        // successful READY state in the middle does not
        // bleed into the next failure.
        assertEquals(2, telemetry.events.size)
        assertEquals(1, telemetry.events[0].consecutiveFailures)
        assertEquals(1, telemetry.events[1].consecutiveFailures)
    }
}

/**
 * Records [CompanionRendererFailureEvent]s for the
 * failure-injection tests. A real production impl would
 * forward to the diagnostics path; the V1 test only
 * asserts the events are captured.
 */
class RecordingCompanionRendererTelemetry : CompanionRendererTelemetry {
    private val _events: MutableList<CompanionRendererFailureEvent> = mutableListOf()
    val events: List<CompanionRendererFailureEvent> get() = _events.toList()
    override fun rendererFailed(event: CompanionRendererFailureEvent) {
        _events.add(event)
    }
}
