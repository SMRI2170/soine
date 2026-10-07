package app.soine.health

data class PlatformSleepRecord(
    val startEpochMillis: Long,
    val endEpochMillis: Long,
    val stages: List<PlatformSleepStage> = emptyList(),
    val sourceId: String? = null,
    val sourceName: String? = null,
)

data class PlatformSleepStage(
    val startEpochMillis: Long,
    val endEpochMillis: Long,
    val type: SleepSignalType,
)

fun normalizePlatformSleepRecord(
    record: PlatformSleepRecord,
    provider: SleepSignalProvider,
): List<SleepSignal> {
    if (record.endEpochMillis <= record.startEpochMillis) return emptyList()

    val source = SleepSignalSource(
        provider = provider,
        sourceId = record.sourceId,
        sourceName = record.sourceName,
    )

    val validStages = record.stages
        .filter { it.endEpochMillis > it.startEpochMillis }
        .map { stage ->
            SleepSignal(
                startEpochMillis = stage.startEpochMillis,
                endEpochMillis = stage.endEpochMillis,
                type = stage.type,
                source = source,
            )
        }

    return if (validStages.isNotEmpty()) {
        validStages.sortedBy(SleepSignal::startEpochMillis)
    } else {
        listOf(
            SleepSignal(
                startEpochMillis = record.startEpochMillis,
                endEpochMillis = record.endEpochMillis,
                type = SleepSignalType.ASLEEP,
                source = source,
            ),
        )
    }
}
