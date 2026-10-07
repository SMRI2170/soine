package app.soine.health

import app.soine.sleep.SleepSessionRecord
import app.soine.sleep.SleepSummary
import app.soine.sleep.summary

data class HealthEnrichedSleepSummary(
    val manual: SleepSummary,
    val healthEstimates: List<HealthSleepEstimate>,
) {
    val manualLabel: String
        get() = "一緒に寝た時間"
}

data class HealthSleepEstimate(
    val provider: SleepSignalProvider,
    val estimatedSleepStartEpochMillis: Long,
    val estimatedWakeEpochMillis: Long,
    val asleepMillis: Long,
    val awakeMillis: Long,
    val confidence: Double?,
) {
    init {
        require(estimatedWakeEpochMillis >= estimatedSleepStartEpochMillis) {
            "Estimated wake must not precede estimated sleep start."
        }
        require(asleepMillis >= 0L) { "Estimated asleep duration must not be negative." }
        require(awakeMillis >= 0L) { "Estimated awake duration must not be negative." }
        require(confidence == null || confidence in 0.0..1.0) {
            "Health estimate confidence must be between 0 and 1."
        }
    }

    val sourceLabel: String
        get() = when (provider) {
            SleepSignalProvider.HEALTH_CONNECT -> "Health Connect"
            SleepSignalProvider.HEALTH_KIT -> "Apple Health"
        }

    val displayLabel: String
        get() = "推定睡眠（$sourceLabel）"
}

/**
 * Combines a completed manual Soine session with optional normalized Health
 * signals without ever replacing the canonical manual interval.
 *
 * Policy:
 * - manual start/end/duration/source remain authoritative;
 * - external providers remain separate rather than being blended together;
 * - only signal portions overlapping the manual session are considered;
 * - within one provider, conflicting states use AWAKE > ASLEEP > IN_BED > UNKNOWN;
 * - IN_BED and UNKNOWN never count as asleep;
 * - confidence is retained conservatively only when every contributing ASLEEP
 *   signal has confidence, using the minimum value.
 */
fun SleepSessionRecord.summaryWithHealth(
    signals: List<SleepSignal>,
): HealthEnrichedSleepSummary? {
    val manualSummary = summary() ?: return null

    val estimates = signals
        .groupBy { it.source.provider }
        .mapNotNull { (provider, providerSignals) ->
            buildProviderEstimate(
                provider = provider,
                signals = providerSignals,
                manualStart = manualSummary.bedtimeEpochMillis,
                manualEnd = manualSummary.wakeTimeEpochMillis,
            )
        }
        .sortedBy { it.provider.ordinal }

    return HealthEnrichedSleepSummary(
        manual = manualSummary,
        healthEstimates = estimates,
    )
}

private data class ClippedSignal(
    val original: SleepSignal,
    val start: Long,
    val end: Long,
)

private enum class ResolvedState {
    ASLEEP,
    AWAKE,
    OTHER,
}

private data class ResolvedSegment(
    val start: Long,
    val end: Long,
    val state: ResolvedState,
)

private fun buildProviderEstimate(
    provider: SleepSignalProvider,
    signals: List<SleepSignal>,
    manualStart: Long,
    manualEnd: Long,
): HealthSleepEstimate? {
    val clipped = signals.mapNotNull { signal ->
        val start = maxOf(signal.startEpochMillis, manualStart)
        val end = minOf(signal.endEpochMillis, manualEnd)
        if (end > start) {
            ClippedSignal(signal, start, end)
        } else {
            null
        }
    }

    if (clipped.isEmpty()) return null

    val boundaries = clipped
        .flatMap { listOf(it.start, it.end) }
        .distinct()
        .sorted()

    val resolved = boundaries
        .zipWithNext()
        .mapNotNull { (start, end) ->
            if (end <= start) return@mapNotNull null

            val active = clipped.filter { it.start < end && it.end > start }
            if (active.isEmpty()) return@mapNotNull null

            val state = when {
                active.any { it.original.type == SleepSignalType.AWAKE } ->
                    ResolvedState.AWAKE

                active.any { it.original.type == SleepSignalType.ASLEEP } ->
                    ResolvedState.ASLEEP

                else ->
                    ResolvedState.OTHER
            }

            ResolvedSegment(start, end, state)
        }

    val asleepSegments = resolved.filter { it.state == ResolvedState.ASLEEP }
    if (asleepSegments.isEmpty()) return null

    val asleepMillis = asleepSegments.sumOf { it.end - it.start }
    val awakeMillis = resolved
        .filter { it.state == ResolvedState.AWAKE }
        .sumOf { it.end - it.start }

    val contributingAsleepSignals = clipped
        .filter { clippedSignal ->
            clippedSignal.original.type == SleepSignalType.ASLEEP &&
                asleepSegments.any { segment ->
                    clippedSignal.start < segment.end && clippedSignal.end > segment.start
                }
        }
        .map(ClippedSignal::original)

    val confidence = contributingAsleepSignals
        .takeIf { it.isNotEmpty() && it.all { signal -> signal.confidence != null } }
        ?.minOf { requireNotNull(it.confidence) }

    return HealthSleepEstimate(
        provider = provider,
        estimatedSleepStartEpochMillis = asleepSegments.first().start,
        estimatedWakeEpochMillis = asleepSegments.last().end,
        asleepMillis = asleepMillis,
        awakeMillis = awakeMillis,
        confidence = confidence,
    )
}
