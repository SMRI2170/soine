package app.soine.relationship

import kotlin.test.*

class RoutineDialogueSelectorTest {
    @Test fun noProfileAlwaysUsesGenericFallback() {
        val selected = RoutineDialogueSelector.select(
            profile = null,
            context = RoutineDialogueContext(RoutineDialoguePhase.BEDTIME),
        )

        assertEquals(RoutineDialogueSource.GENERIC, selected.source)
        assertNull(selected.confidence)
    }

    @Test fun highConfidenceBedtimeCanNoticeEarlierNight() {
        val profile = profile(
            bedtime = timeRange(
                center = 23 * 60 + 30,
                confidence = RoutineConfidence.HIGH,
            ),
        )

        val selected = RoutineDialogueSelector.select(
            profile = profile,
            context = RoutineDialogueContext(
                phase = RoutineDialoguePhase.BEDTIME,
                currentMinuteOfDay = 22 * 60 + 30,
            ),
        )

        assertEquals("routine-bedtime-earlier", selected.id)
        assertEquals("今日はいつもより早いね", selected.text)
        assertEquals(RoutineConfidence.HIGH, selected.confidence)
    }

    @Test fun mediumConfidenceTimingDoesNotMakeTimingClaim() {
        val profile = profile(
            bedtime = timeRange(
                center = 23 * 60 + 30,
                confidence = RoutineConfidence.MEDIUM,
            ),
        )

        val selected = RoutineDialogueSelector.select(
            profile = profile,
            context = RoutineDialogueContext(
                phase = RoutineDialoguePhase.BEDTIME,
                currentMinuteOfDay = 22 * 60,
            ),
        )

        assertEquals(RoutineDialogueSource.GENERIC, selected.source)
    }

    @Test fun timingComparisonWorksAcrossMidnight() {
        val profile = profile(
            bedtime = timeRange(
                center = 10,
                confidence = RoutineConfidence.HIGH,
            ),
        )

        val selected = RoutineDialogueSelector.select(
            profile = profile,
            context = RoutineDialogueContext(
                phase = RoutineDialoguePhase.BEDTIME,
                currentMinuteOfDay = 23 * 60,
            ),
        )

        assertEquals("routine-bedtime-earlier", selected.id)
    }

    @Test fun supportedSoundPreferenceCanSuggestKnownSound() {
        val profile = profile(
            sound = RoutineAmbientSoundPreference(
                soundId = "rain",
                useCount = 4,
                observedSoundSessions = 5,
                confidence = RoutineConfidence.MEDIUM,
            ),
        )

        val selected = RoutineDialogueSelector.select(
            profile = profile,
            context = RoutineDialogueContext(
                phase = RoutineDialoguePhase.BEDTIME,
                activeAmbientSoundId = "waves",
                ambientSoundLabels = mapOf("rain" to "雨"),
            ),
        )

        assertEquals("routine-sound-rain", selected.id)
        assertEquals("雨の音にする？", selected.text)
        assertEquals(RoutineDialogueSource.AMBIENT_SOUND, selected.source)
    }

    @Test fun soundSuggestionIsSuppressedWhenPreferredSoundIsAlreadyActive() {
        val profile = profile(
            sound = RoutineAmbientSoundPreference(
                soundId = "rain",
                useCount = 4,
                observedSoundSessions = 5,
                confidence = RoutineConfidence.HIGH,
            ),
        )

        val selected = RoutineDialogueSelector.select(
            profile = profile,
            context = RoutineDialogueContext(
                phase = RoutineDialoguePhase.BEDTIME,
                activeAmbientSoundId = "rain",
                ambientSoundLabels = mapOf("rain" to "雨"),
            ),
        )

        assertEquals(RoutineDialogueSource.GENERIC, selected.source)
    }

    @Test fun unknownSoundLabelNeverProducesUnsupportedText() {
        val profile = profile(
            sound = RoutineAmbientSoundPreference(
                soundId = "future-sound",
                useCount = 5,
                observedSoundSessions = 5,
                confidence = RoutineConfidence.HIGH,
            ),
        )

        val selected = RoutineDialogueSelector.select(
            profile = profile,
            context = RoutineDialogueContext(
                phase = RoutineDialoguePhase.BEDTIME,
                ambientSoundLabels = mapOf("rain" to "雨"),
            ),
        )

        assertEquals(RoutineDialogueSource.GENERIC, selected.source)
        assertFalse(selected.text.contains("future-sound"))
    }

    @Test fun recentRoutineLineFallsThroughToAnotherSupportedRoutineLine() {
        val profile = profile(
            bedtime = timeRange(
                center = 23 * 60 + 30,
                confidence = RoutineConfidence.HIGH,
            ),
            sound = RoutineAmbientSoundPreference(
                soundId = "rain",
                useCount = 7,
                observedSoundSessions = 8,
                confidence = RoutineConfidence.HIGH,
            ),
        )

        val selected = RoutineDialogueSelector.select(
            profile = profile,
            context = RoutineDialogueContext(
                phase = RoutineDialoguePhase.BEDTIME,
                currentMinuteOfDay = 22 * 60,
                ambientSoundLabels = mapOf("rain" to "雨"),
            ),
            recentDialogueIds = setOf("routine-bedtime-earlier"),
        )

        assertEquals("routine-sound-rain", selected.id)
    }

    @Test fun recentGenericFallbackRotatesBeforeRepeating() {
        val first = RoutineDialogueSelector.select(
            profile = null,
            context = RoutineDialogueContext(RoutineDialoguePhase.MORNING),
            recentDialogueIds = setOf("generic-morning-1"),
        )

        assertEquals("generic-morning-2", first.id)
    }

    @Test fun morningTimingClaimRequiresHighConfidenceWakeProfile() {
        val profile = profile(
            wake = timeRange(
                center = 7 * 60,
                confidence = RoutineConfidence.HIGH,
            ),
        )

        val selected = RoutineDialogueSelector.select(
            profile = profile,
            context = RoutineDialogueContext(
                phase = RoutineDialoguePhase.MORNING,
                currentMinuteOfDay = 8 * 60,
            ),
        )

        assertEquals("routine-wake-later", selected.id)
        assertEquals(RoutineDialogueSource.WAKE_TIMING, selected.source)
    }

    @Test fun highConfidenceWeekendPatternCanProduceWeekendLine() {
        val profile = profile(
            tendency = RoutineWeekdayWeekendTendency(
                weekendBedtimeShiftMinutes = 90,
                weekendWakeShiftMinutes = 100,
                weekdaySampleCount = 8,
                weekendSampleCount = 5,
                confidence = RoutineConfidence.HIGH,
            ),
        )

        val selected = RoutineDialogueSelector.select(
            profile = profile,
            context = RoutineDialogueContext(
                phase = RoutineDialoguePhase.MORNING,
                dayType = RoutineDayType.WEEKEND,
            ),
        )

        assertEquals("routine-weekend-morning", selected.id)
        assertEquals(RoutineDialogueSource.WEEKDAY_WEEKEND, selected.source)
    }

    private fun profile(
        bedtime: RoutineTimeRange? = null,
        wake: RoutineTimeRange? = null,
        sound: RoutineAmbientSoundPreference? = null,
        tendency: RoutineWeekdayWeekendTendency? = null,
    ) = RoutineProfile(
        observationCount = 10,
        typicalBedtime = bedtime,
        typicalWakeTime = wake,
        frequentAmbientSound = sound,
        weekdayWeekendTendency = tendency,
    )

    private fun timeRange(
        center: Int,
        confidence: RoutineConfidence,
    ) = RoutineTimeRange(
        startMinuteOfDay = (center - 30 + 1440) % 1440,
        endMinuteOfDay = (center + 30) % 1440,
        centerMinuteOfDay = center,
        sampleCount = if (confidence == RoutineConfidence.HIGH) 10 else 5,
        confidence = confidence,
    )
}
