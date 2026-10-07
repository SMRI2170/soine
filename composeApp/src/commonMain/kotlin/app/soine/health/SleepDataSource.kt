package app.soine.health

/**
 * Normalized sleep signal consumed by common domain logic.
 *
 * Platform Health SDK types must be mapped into this model at the adapter
 * boundary before they enter commonMain.
 */
data class SleepSignal(
    val startEpochMillis: Long,
    val endEpochMillis: Long,
    val type: SleepSignalType,
    val source: SleepSignalSource,
    val confidence: Double? = null,
) {
    init {
        require(endEpochMillis > startEpochMillis) {
            "Sleep signal end must be after start."
        }
        require(confidence == null || confidence in 0.0..1.0) {
            "Sleep signal confidence must be between 0 and 1."
        }
    }
}

enum class SleepSignalType {
    ASLEEP,
    AWAKE,
    IN_BED,
    UNKNOWN,
}

enum class SleepSignalProvider {
    HEALTH_CONNECT,
    HEALTH_KIT,
}

/**
 * Provenance for an externally supplied sleep signal.
 *
 * [sourceId] and [sourceName] are optional because platform APIs do not always
 * expose the same metadata. They are attribution only and must not be used as
 * a stable domain identifier.
 */
data class SleepSignalSource(
    val provider: SleepSignalProvider,
    val sourceId: String? = null,
    val sourceName: String? = null,
)

enum class HealthPermissionState {
    NOT_REQUESTED,
    GRANTED,
    DENIED,

    /**
     * The platform completed the read authorization flow but intentionally
     * does not reveal whether read access was granted. HealthKit uses this
     * state for privacy-preserving read authorization.
     */
    READ_STATUS_UNKNOWN,

    UNAVAILABLE,
}

sealed interface SleepSignalReadResult {
    data class Available(
        val signals: List<SleepSignal>,
    ) : SleepSignalReadResult

    data object PermissionRequired : SleepSignalReadResult
    data object PermissionDenied : SleepSignalReadResult
    data object Unavailable : SleepSignalReadResult
}

/**
 * Optional external sleep-data boundary.
 *
 * Implementations must never own the lifetime of a Soine sleep session.
 * Health permission denial or platform unavailability is represented as data,
 * not as a failure of the core manual sleep flow.
 */
interface SleepDataSource {
    suspend fun getPermissionState(): HealthPermissionState

    suspend fun requestReadPermission(): HealthPermissionState

    suspend fun readSleepSignals(
        startEpochMillis: Long,
        endEpochMillis: Long,
    ): SleepSignalReadResult
}
