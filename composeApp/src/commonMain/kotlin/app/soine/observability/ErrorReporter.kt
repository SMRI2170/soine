package app.soine.observability

/**
 * Vendor-neutral crash / non-fatal diagnostics owned by commonMain.
 *
 * Mirrors the privacy shape of [app.soine.analytics.AnalyticsTracker]:
 * events carry a fixed vocabulary name and no free-form payload so that
 * sensitive sleep content, Health samples, raw audio, user-written text
 * and exact sleep timelines cannot be attached accidentally.
 *
 * Vendor SDK adapters belong in platform source sets or a future
 * integration layer. [NoOpErrorReporter] is the default-safe
 * implementation until an explicit provider is selected.
 *
 * See docs/observability.md for the privacy contract.
 */
/**
 * Diagnostics sink owned by commonMain. Declared as a regular interface
 * (not a `fun interface`) because it exposes two abstract methods —
 * `recordNonFatal` and `recordFatal` — and a `fun interface` may only
 * declare a single abstract member. SAM-conversion is not required by
 * any call site; the loss of that single-abstract-method convenience
 * is intentional and keeps the contract future-proof if a third
 * channel (e.g. breadcrumb) is added.
 */
interface ErrorReporter {
    /**
     * Records a typed non-fatal event. The reporter decides whether the
     * event is sent; [NoOpErrorReporter] discards everything.
     */
    fun recordNonFatal(event: ErrorEvent)

    /**
     * Records a fatal throwable. Implementations must not let the
     * reporter itself throw — failures inside the reporter must not
     * crash the host process.
     */
    fun recordFatal(throwable: Throwable)
}

/**
 * Fixed vocabulary of non-fatal diagnostics events. Each entry maps to
 * a snake_case identifier that the production reporter sends. Adding a new
 * entry is the only way to attach a new diagnostic name; the absence of
 * a payload field is the privacy guarantee.
 */
enum class ErrorEvent(val eventName: String) {
    RENDERER_FALLBACK_OCCURRED("renderer_fallback_occurred"),
    RENDERER_ASSET_DECODE_FAILURE("renderer_asset_decode_failure"),
    AUDIO_INTERRUPTION("audio_interruption"),
    AUDIO_DECODE_FAILURE("audio_decode_failure"),
    AUDIO_FOCUS_DENIED("audio_focus_denied"),
    HEALTH_PERMISSION_DENIED("health_permission_denied"),
    HEALTH_UNAVAILABLE("health_unavailable"),
    MICROPHONE_UNAVAILABLE("microphone_unavailable"),
    SESSION_RECOVERY_OCCURRED("session_recovery_occurred"),
    STORAGE_CORRUPTION("storage_corruption"),
    LOCAL_DELETION_FAILED("local_deletion_failed"),
}

object NoOpErrorReporter : ErrorReporter {
    override fun recordNonFatal(event: ErrorEvent) = Unit
    override fun recordFatal(throwable: Throwable) = Unit
}

/**
 * Helper that converts a [Throwable] into a non-sensitive fingerprint
 * suitable for grouping in the diagnostics backend. The fingerprint is
 * the throwable's class name; message text and stack frames are not
 * included at this layer (the production adapter may attach a redacted
 * stack after scrubbing).
 */
fun Throwable.diagnosticFingerprint(): String =
    this::class.qualifiedName ?: this::class.simpleName ?: "unknown"