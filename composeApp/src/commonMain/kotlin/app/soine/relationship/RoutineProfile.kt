package app.soine.relationship

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

enum class RoutineDayType {
    WEEKDAY,
    WEEKEND,
}

enum class RoutineConfidence {
    MEDIUM,
    HIGH,
}

data class RoutineObservation(
    val sessionId: String,
    val bedtimeMinuteOfDay: Int,
    val wakeMinuteOfDay: Int,
    val dayType: RoutineDayType,
    val ambientSoundId: String? = null,
) {
    init {
        require(sessionId.isNotBlank()) { "Session id must not be blank." }
        require(bedtimeMinuteOfDay in 0..1439) { "Bedtime minute must be in 0..1439." }
        require(wakeMinuteOfDay in 0..1439) { "Wake minute must be in 0..1439." }
        require(ambientSoundId == null || ambientSoundId.isNotBlank()) {
            "Ambient sound id must be null or non-blank."
        }
    }
}

data class RoutineTimeRange(
    val startMinuteOfDay: Int,
    val endMinuteOfDay: Int,
    val centerMinuteOfDay: Int,
    val sampleCount: Int,
    val confidence: RoutineConfidence,
) {
    init {
        require(startMinuteOfDay in 0..1439)
        require(endMinuteOfDay in 0..1439)
        require(centerMinuteOfDay in 0..1439)
        require(sampleCount > 0)
    }

    val crossesMidnight: Boolean
        get() = startMinuteOfDay > endMinuteOfDay
}

data class RoutineAmbientSoundPreference(
    val soundId: String,
    val useCount: Int,
    val observedSoundSessions: Int,
    val confidence: RoutineConfidence,
)

data class RoutineWeekdayWeekendTendency(
    val weekendBedtimeShiftMinutes: Int,
    val weekendWakeShiftMinutes: Int,
    val weekdaySampleCount: Int,
    val weekendSampleCount: Int,
    val confidence: RoutineConfidence,
)

data class RoutineProfile(
    val observationCount: Int,
    val typicalBedtime: RoutineTimeRange?,
    val typicalWakeTime: RoutineTimeRange?,
    val frequentAmbientSound: RoutineAmbientSoundPreference?,
    val weekdayWeekendTendency: RoutineWeekdayWeekendTendency?,
)

object RoutineProfileGenerator {
    const val MIN_TIME_SAMPLES = 5
    const val HIGH_CONFIDENCE_TIME_SAMPLES = 10
    const val MIN_SOUND_SAMPLES = 3
    const val MIN_DAY_TYPE_SAMPLES = 3
    const val MEANINGFUL_DAY_SHIFT_MINUTES = 45

    fun generate(observations: List<RoutineObservation>): RoutineProfile {
        val unique = observations.distinctBy { it.sessionId }

        return RoutineProfile(
            observationCount = unique.size,
            typicalBedtime = timeRange(unique.map { it.bedtimeMinuteOfDay }),
            typicalWakeTime = timeRange(unique.map { it.wakeMinuteOfDay }),
            frequentAmbientSound = ambientPreference(unique.mapNotNull { it.ambientSoundId }),
            weekdayWeekendTendency = weekdayWeekendTendency(unique),
        )
    }

    private fun timeRange(minutes: List<Int>): RoutineTimeRange? {
        if (minutes.size < MIN_TIME_SAMPLES) return null

        val center = circularMeanMinute(minutes)
        val unwrapped = minutes
            .map { center + signedCircularDifference(center, it) }
            .sorted()

        val start = percentile(unwrapped, 0.20)
        val end = percentile(unwrapped, 0.80)

        return RoutineTimeRange(
            startMinuteOfDay = normalizeMinute(start),
            endMinuteOfDay = normalizeMinute(end),
            centerMinuteOfDay = center,
            sampleCount = minutes.size,
            confidence = if (minutes.size >= HIGH_CONFIDENCE_TIME_SAMPLES) {
                RoutineConfidence.HIGH
            } else {
                RoutineConfidence.MEDIUM
            },
        )
    }

    private fun ambientPreference(soundIds: List<String>): RoutineAmbientSoundPreference? {
        if (soundIds.size < MIN_SOUND_SAMPLES) return null

        val counts = soundIds.groupingBy { it }.eachCount()
        val ranked = counts.entries.sortedWith(
            compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key }
        )
        val first = ranked.firstOrNull() ?: return null
        val secondCount = ranked.getOrNull(1)?.value ?: 0

        if (first.value == secondCount || first.value * 2 < soundIds.size) return null

        return RoutineAmbientSoundPreference(
            soundId = first.key,
            useCount = first.value,
            observedSoundSessions = soundIds.size,
            confidence = if (soundIds.size >= 8 && first.value * 3 >= soundIds.size * 2) {
                RoutineConfidence.HIGH
            } else {
                RoutineConfidence.MEDIUM
            },
        )
    }

    private fun weekdayWeekendTendency(
        observations: List<RoutineObservation>,
    ): RoutineWeekdayWeekendTendency? {
        val weekdays = observations.filter { it.dayType == RoutineDayType.WEEKDAY }
        val weekends = observations.filter { it.dayType == RoutineDayType.WEEKEND }

        if (
            weekdays.size < MIN_DAY_TYPE_SAMPLES ||
            weekends.size < MIN_DAY_TYPE_SAMPLES
        ) return null

        val weekdayBed = circularMeanMinute(weekdays.map { it.bedtimeMinuteOfDay })
        val weekendBed = circularMeanMinute(weekends.map { it.bedtimeMinuteOfDay })
        val weekdayWake = circularMeanMinute(weekdays.map { it.wakeMinuteOfDay })
        val weekendWake = circularMeanMinute(weekends.map { it.wakeMinuteOfDay })

        val bedShift = signedCircularDifference(weekdayBed, weekendBed)
        val wakeShift = signedCircularDifference(weekdayWake, weekendWake)

        if (
            kotlin.math.abs(bedShift) < MEANINGFUL_DAY_SHIFT_MINUTES &&
            kotlin.math.abs(wakeShift) < MEANINGFUL_DAY_SHIFT_MINUTES
        ) return null

        return RoutineWeekdayWeekendTendency(
            weekendBedtimeShiftMinutes = bedShift,
            weekendWakeShiftMinutes = wakeShift,
            weekdaySampleCount = weekdays.size,
            weekendSampleCount = weekends.size,
            confidence = if (weekdays.size >= 6 && weekends.size >= 4) {
                RoutineConfidence.HIGH
            } else {
                RoutineConfidence.MEDIUM
            },
        )
    }

    private fun circularMeanMinute(minutes: List<Int>): Int {
        require(minutes.isNotEmpty())
        var x = 0.0
        var y = 0.0
        minutes.forEach { minute ->
            val angle = minute.toDouble() / 1440.0 * 2.0 * PI
            x += cos(angle)
            y += sin(angle)
        }
        var angle = atan2(y, x)
        if (angle < 0.0) angle += 2.0 * PI
        return normalizeMinute((angle / (2.0 * PI) * 1440.0).roundToInt())
    }

    private fun signedCircularDifference(from: Int, to: Int): Int {
        var difference = normalizeMinute(to) - normalizeMinute(from)
        if (difference >= 720) difference -= 1440
        if (difference < -720) difference += 1440
        return difference
    }

    private fun percentile(values: List<Int>, quantile: Double): Int {
        require(values.isNotEmpty())
        val index = ((values.lastIndex) * quantile).roundToInt().coerceIn(values.indices)
        return values[index]
    }

    private fun normalizeMinute(value: Int): Int =
        ((value % 1440) + 1440) % 1440
}
