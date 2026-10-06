package app.soine.companion

data class CompanionStaticArtwork(
    val artKey: String,
    val glyph: String,
    val contentDescription: String,
)

data class CompanionStaticPresentation(
    val artwork: CompanionStaticArtwork,
    val sleepingPlacement: CompanionSleepingPlacement,
)

object CompanionStaticFallback {
    private val awake = CompanionStaticArtwork(
        artKey = "companion_awake",
        glyph = "（ ・ᴗ・ ）",
        contentDescription = "ねむがこちらを見ている",
    )
    private val settling = CompanionStaticArtwork(
        artKey = "companion_settling",
        glyph = "（ ᵕ ᴗ ᵕ ）",
        contentDescription = "ねむが眠る準備をしている",
    )
    private val sleeping = CompanionStaticArtwork(
        artKey = "companion_sleeping",
        glyph = "（ ᵕ ᵕ ） zzz",
        contentDescription = "ねむが眠っている",
    )
    private val waking = CompanionStaticArtwork(
        artKey = "companion_waking",
        glyph = "（ ˶ᵔ ᵕ ᵔ˶ ）",
        contentDescription = "ねむが目を覚ました",
    )

    fun forIntent(intent: CompanionIntent): CompanionStaticArtwork =
        when (intent) {
            CompanionIntent.SETTLE,
            CompanionIntent.CURL_UP,
            CompanionIntent.YAWN -> settling

            CompanionIntent.SLEEP,
            CompanionIntent.BREATHE,
            CompanionIntent.EAR_TWITCH,
            CompanionIntent.ROLL_OVER,
            CompanionIntent.BRIEF_WAKE -> sleeping

            CompanionIntent.WAKE,
            CompanionIntent.STRETCH -> waking

            CompanionIntent.IDLE,
            CompanionIntent.NOTICE_USER,
            CompanionIntent.LOOK_AT_USER,
            CompanionIntent.MOVE_CLOSER -> awake
        }

    fun forRequest(request: CompanionRenderRequest): CompanionStaticPresentation =
        CompanionStaticPresentation(
            artwork = forIntent(request.intent),
            sleepingPlacement = request.sleepingPlacement,
        )
}

data class CompanionRendererFailureEvent(
    val code: String?,
    val consecutiveFailures: Int,
)

fun interface CompanionRendererTelemetry {
    fun rendererFailed(event: CompanionRendererFailureEvent)
}

enum class CompanionRendererRecoveryAction {
    RETRY_AUTOMATICALLY,
    USE_STATIC_FALLBACK,
}

/**
 * Caps automatic renderer initialization retries to prevent crash/retry loops.
 * Manual retry does not reset the failure counter; only a successful READY
 * state resets it.
 */
class CompanionRendererFallbackPolicy(
    private val maxAutomaticRetries: Int = 1,
    private val telemetry: CompanionRendererTelemetry = CompanionRendererTelemetry {},
) {
    init {
        require(maxAutomaticRetries >= 0) { "Retry count must not be negative." }
    }

    var consecutiveFailures: Int = 0
        private set

    fun onFailure(code: String? = null): CompanionRendererRecoveryAction {
        consecutiveFailures += 1
        telemetry.rendererFailed(
            CompanionRendererFailureEvent(
                code = code,
                consecutiveFailures = consecutiveFailures,
            )
        )
        return if (consecutiveFailures <= maxAutomaticRetries) {
            CompanionRendererRecoveryAction.RETRY_AUTOMATICALLY
        } else {
            CompanionRendererRecoveryAction.USE_STATIC_FALLBACK
        }
    }

    fun onReady() {
        consecutiveFailures = 0
    }

    fun onManualRetryRequested(): CompanionRendererRecoveryAction =
        CompanionRendererRecoveryAction.RETRY_AUTOMATICALLY
}
