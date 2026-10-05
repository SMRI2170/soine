package app.soine.companion

import kotlin.test.*

class CompanionStaticFallbackTest {
    @Test fun staticArtworkReflectsCurrentSemanticState() {
        assertEquals(
            "companion_awake",
            CompanionStaticFallback.forIntent(CompanionIntent.IDLE).artKey,
        )
        assertEquals(
            "companion_settling",
            CompanionStaticFallback.forIntent(CompanionIntent.SETTLE).artKey,
        )
        assertEquals(
            "companion_sleeping",
            CompanionStaticFallback.forIntent(CompanionIntent.BREATHE).artKey,
        )
        assertEquals(
            "companion_waking",
            CompanionStaticFallback.forIntent(CompanionIntent.WAKE).artKey,
        )
    }

    @Test fun allIntentsHaveStaticArtwork() {
        CompanionIntent.entries.forEach { intent ->
            val artwork = CompanionStaticFallback.forIntent(intent)
            assertTrue(artwork.artKey.isNotBlank())
            assertTrue(artwork.glyph.isNotBlank())
            assertTrue(artwork.contentDescription.isNotBlank())
        }
    }

    @Test fun automaticRetryStopsAfterConfiguredLimit() {
        val policy = CompanionRendererFallbackPolicy(maxAutomaticRetries = 1)

        assertEquals(
            CompanionRendererRecoveryAction.RETRY_AUTOMATICALLY,
            policy.onFailure("init-1"),
        )
        assertEquals(
            CompanionRendererRecoveryAction.USE_STATIC_FALLBACK,
            policy.onFailure("init-2"),
        )
        assertEquals(2, policy.consecutiveFailures)
    }

    @Test fun successfulInitializationResetsCrashLoopGuard() {
        val policy = CompanionRendererFallbackPolicy(maxAutomaticRetries = 1)
        policy.onFailure()
        policy.onReady()

        assertEquals(0, policy.consecutiveFailures)
        assertEquals(
            CompanionRendererRecoveryAction.RETRY_AUTOMATICALLY,
            policy.onFailure(),
        )
    }

    @Test fun failureTelemetryGetsNonSensitiveFailureMetadata() {
        val events = mutableListOf<CompanionRendererFailureEvent>()
        val policy = CompanionRendererFallbackPolicy(
            telemetry = CompanionRendererTelemetry { events += it },
        )

        policy.onFailure("asset-load")

        assertEquals(
            listOf(CompanionRendererFailureEvent("asset-load", 1)),
            events,
        )
    }

    @Test fun manualRetryDoesNotResetCrashLoopCounter() {
        val policy = CompanionRendererFallbackPolicy(maxAutomaticRetries = 1)
        policy.onFailure()
        policy.onFailure()

        assertEquals(
            CompanionRendererRecoveryAction.RETRY_AUTOMATICALLY,
            policy.onManualRetryRequested(),
        )
        assertEquals(2, policy.consecutiveFailures)
    }
}
