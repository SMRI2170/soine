package app.soine.sleep

data class SleepSummary(
    val durationMillis: Long,
    val bedtimeEpochMillis: Long,
    val wakeTimeEpochMillis: Long,
    val source: SleepSessionSource,
    val estimates: SleepSummaryEstimates? = null,
) {
    init {
        require(durationMillis >= 0) { "Duration must not be negative." }
        require(wakeTimeEpochMillis >= bedtimeEpochMillis) { "Wake time must not precede bedtime." }
    }

    val durationMinutes: Long get() = durationMillis / 60_000
    val hours: Long get() = durationMinutes / 60
    val minutes: Long get() = durationMinutes % 60

    fun displayDuration(): String =
        if (hours > 0) "${hours}時間${minutes}分" else "${minutes}分"
}

data class SleepSummaryEstimates(
    val asleepMillis: Long? = null,
    val awakeMillis: Long? = null,
)

fun SleepSessionRecord.summary(): SleepSummary? {
    if (status != SleepSessionStatus.COMPLETED) return null
    val end = endedAtEpochMillis ?: return null
    if (end < startedAtEpochMillis) return null
    return SleepSummary(
        durationMillis = end - startedAtEpochMillis,
        bedtimeEpochMillis = startedAtEpochMillis,
        wakeTimeEpochMillis = end,
        source = source,
    )
}

/** Legacy UI model adapter. Prefer SleepSessionRecord.summary() for persisted sessions. */
fun SleepSession.summary(): SleepSummary? {
    val start = startedAtEpochMillis ?: return null
    val end = endedAtEpochMillis ?: return null
    if (state != SleepState.FINISHED || end < start) return null
    return SleepSummary(
        durationMillis = end - start,
        bedtimeEpochMillis = start,
        wakeTimeEpochMillis = end,
        source = SleepSessionSource.MANUAL,
    )
}
