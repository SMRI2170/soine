package app.soine.relationship

import kotlin.test.*

class RoutineProfileGeneratorTest {
    @Test fun tooFewNightsDoNotCreateRoutineClaims() {
        val profile = RoutineProfileGenerator.generate(
            listOf(
                observation("1", 23 * 60, 7 * 60),
                observation("2", 23 * 60 + 10, 7 * 60 + 10),
                observation("3", 22 * 60 + 50, 6 * 60 + 50),
                observation("4", 23 * 60 + 20, 7 * 60 + 20),
            )
        )

        assertNull(profile.typicalBedtime)
        assertNull(profile.typicalWakeTime)
        assertNull(profile.weekdayWeekendTendency)
    }

    @Test fun bedtimeRangeHandlesMidnightWithoutAveragingToNoon() {
        val profile = RoutineProfileGenerator.generate(
            listOf(
                observation("1", 23 * 60 + 30, 7 * 60),
                observation("2", 23 * 60 + 50, 7 * 60 + 10),
                observation("3", 5, 6 * 60 + 50),
                observation("4", 20, 7 * 60 + 20),
                observation("5", 23 * 60 + 40, 7 * 60 + 5),
            )
        )

        val bedtime = assertNotNull(profile.typicalBedtime)
        assertTrue(bedtime.centerMinuteOfDay >= 23 * 60 || bedtime.centerMinuteOfDay <= 30)
        assertTrue(bedtime.crossesMidnight)
        assertEquals(RoutineConfidence.MEDIUM, bedtime.confidence)
    }

    @Test fun tenSamplesPromoteTimeRoutineToHighConfidence() {
        val profile = RoutineProfileGenerator.generate(
            (0 until 10).map { index ->
                observation(
                    index.toString(),
                    23 * 60 + (index % 5),
                    7 * 60 + (index % 5),
                )
            }
        )

        assertEquals(RoutineConfidence.HIGH, profile.typicalBedtime?.confidence)
        assertEquals(RoutineConfidence.HIGH, profile.typicalWakeTime?.confidence)
    }

    @Test fun dominantAmbientSoundRequiresEnoughSamplesAndNoTie() {
        val profile = RoutineProfileGenerator.generate(
            listOf(
                observation("1", 1380, 420, sound = "rain"),
                observation("2", 1390, 430, sound = "rain"),
                observation("3", 1400, 440, sound = "waves"),
                observation("4", 1410, 450),
                observation("5", 1420, 460),
            )
        )

        val sound = assertNotNull(profile.frequentAmbientSound)
        assertEquals("rain", sound.soundId)
        assertEquals(2, sound.useCount)
        assertEquals(3, sound.observedSoundSessions)
    }

    @Test fun tiedAmbientSoundsDoNotBecomeAClaim() {
        val profile = RoutineProfileGenerator.generate(
            listOf(
                observation("1", 1380, 420, sound = "rain"),
                observation("2", 1390, 430, sound = "waves"),
                observation("3", 1400, 440, sound = "rain"),
                observation("4", 1410, 450, sound = "waves"),
                observation("5", 1420, 460),
            )
        )

        assertNull(profile.frequentAmbientSound)
    }

    @Test fun weekendLaterPatternRequiresSamplesFromBothDayTypes() {
        val observations = listOf(
            observation("w1", 23 * 60, 7 * 60, RoutineDayType.WEEKDAY),
            observation("w2", 23 * 60 + 10, 7 * 60 + 5, RoutineDayType.WEEKDAY),
            observation("w3", 22 * 60 + 50, 6 * 60 + 55, RoutineDayType.WEEKDAY),
            observation("e1", 60, 9 * 60, RoutineDayType.WEEKEND),
            observation("e2", 70, 9 * 60 + 10, RoutineDayType.WEEKEND),
            observation("e3", 50, 8 * 60 + 50, RoutineDayType.WEEKEND),
        )

        val tendency = assertNotNull(
            RoutineProfileGenerator.generate(observations).weekdayWeekendTendency
        )

        assertTrue(tendency.weekendBedtimeShiftMinutes in 100..140)
        assertTrue(tendency.weekendWakeShiftMinutes in 110..130)
        assertEquals(RoutineConfidence.MEDIUM, tendency.confidence)
    }

    @Test fun similarWeekdayAndWeekendSchedulesDoNotCreateTendency() {
        val observations = listOf(
            observation("w1", 1380, 420, RoutineDayType.WEEKDAY),
            observation("w2", 1390, 430, RoutineDayType.WEEKDAY),
            observation("w3", 1370, 410, RoutineDayType.WEEKDAY),
            observation("e1", 1395, 435, RoutineDayType.WEEKEND),
            observation("e2", 1385, 425, RoutineDayType.WEEKEND),
            observation("e3", 1400, 440, RoutineDayType.WEEKEND),
        )

        assertNull(RoutineProfileGenerator.generate(observations).weekdayWeekendTendency)
    }

    @Test fun duplicateSessionIdsAreCountedOnlyOnce() {
        val observations = listOf(
            observation("same", 1380, 420, sound = "rain"),
            observation("same", 60, 600, sound = "waves"),
            observation("2", 1380, 420),
            observation("3", 1380, 420),
            observation("4", 1380, 420),
            observation("5", 1380, 420),
        )

        val profile = RoutineProfileGenerator.generate(observations)

        assertEquals(5, profile.observationCount)
        assertTrue(profile.typicalBedtime!!.centerMinuteOfDay > 1300)
    }

    private fun observation(
        id: String,
        bed: Int,
        wake: Int,
        dayType: RoutineDayType = RoutineDayType.WEEKDAY,
        sound: String? = null,
    ) = RoutineObservation(
        sessionId = id,
        bedtimeMinuteOfDay = bed,
        wakeMinuteOfDay = wake,
        dayType = dayType,
        ambientSoundId = sound,
    )
}
