package app.soine.sleep

data class SleepSummary(val durationMillis: Long) {
    val durationMinutes: Long get() = durationMillis / 60_000
    val hours: Long get() = durationMinutes / 60
    val minutes: Long get() = durationMinutes % 60

    fun displayDuration(): String =
        if (hours > 0) "${hours}時間${minutes}分" else "${minutes}分"
}

fun SleepSession.summary(): SleepSummary? = durationMillis()?.let(::SleepSummary)
