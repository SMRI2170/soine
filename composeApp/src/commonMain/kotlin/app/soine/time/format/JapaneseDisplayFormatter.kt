package app.soine.time.format

import app.soine.time.LocalTimeZone

/**
 * V1 default [DisplayFormatter] for the Japanese locale.
 *
 * The style is the existing Soine V1 shape:
 *
 *   - discovery date: "yyyy年MM月dd日" (year / month / day)
 *   - night event time: 24-hour "HH:mm"
 *
 * The formatter is pure and stateless. It does not reach into
 * [app.soine.time.LocalTimeZones.current] on its own; the caller
 * passes the [LocalTimeZone] so the test path can exercise travel
 * scenarios without mutating global state.
 */
object JapaneseDisplayFormatter : DisplayFormatter {

    override fun formatDiscoveryDate(epochMillis: Long, timeZone: LocalTimeZone): String {
        require(epochMillis >= 0L) { "Discovery timestamp must not be negative." }

        val localMillis = epochMillis + timeZone.utcOffsetMillisAt(epochMillis)
        val epochDay = localMillis / MILLIS_PER_DAY
        val date = civilDateFromEpochDay(epochDay)
        return date.year.toString() + "年" + date.month + "月" + date.day + "日"
    }

    override fun formatNightEventTime(epochMillis: Long, timeZone: LocalTimeZone): String {
        require(epochMillis >= 0L) { "NightEvent timestamp must not be negative." }
        val localMillis = epochMillis + timeZone.utcOffsetMillisAt(epochMillis)
        val millisOfDay = ((localMillis % MILLIS_PER_DAY) + MILLIS_PER_DAY) % MILLIS_PER_DAY
        val hours = millisOfDay / MILLIS_PER_HOUR
        val minutes = (millisOfDay % MILLIS_PER_HOUR) / MILLIS_PER_MINUTE
        return hours.toString().padStart(2, '0') + ":" +
            minutes.toString().padStart(2, '0')
    }
}

private data class CivilDate(
    val year: Int,
    val month: Int,
    val day: Int,
)

private fun civilDateFromEpochDay(epochDay: Long): CivilDate {
    val z = epochDay + 719_468L
    val era = if (z >= 0L) z / 146_097L else (z - 146_096L) / 146_097L
    val dayOfEra = z - era * 146_097L
    val yearOfEra =
        (dayOfEra - dayOfEra / 1_460L + dayOfEra / 36_524L - dayOfEra / 146_096L) / 365L
    var year = (yearOfEra + era * 400L).toInt()
    val dayOfYear = dayOfEra - (365L * yearOfEra + yearOfEra / 4L - yearOfEra / 100L)
    val monthPrime = (5L * dayOfYear + 2L) / 153L
    val day = (dayOfYear - (153L * monthPrime + 2L) / 5L + 1L).toInt()
    val month = (monthPrime + if (monthPrime < 10L) 3L else -9L).toInt()
    year += if (month <= 2) 1 else 0
    return CivilDate(year, month, day)
}

internal const val MILLIS_PER_DAY: Long = 86_400_000L
internal const val MILLIS_PER_HOUR: Long = 3_600_000L
internal const val MILLIS_PER_MINUTE: Long = 60_000L
