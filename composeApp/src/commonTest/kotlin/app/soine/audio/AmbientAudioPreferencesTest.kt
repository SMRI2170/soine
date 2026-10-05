package app.soine.audio

import kotlin.test.*

class AmbientAudioPreferencesTest {
    @Test fun defaultsKeepAmbientAudioOff() {
        val value = AmbientAudioPreferences.safe(null, null, null, null, null)
        assertEquals(AmbientSounds.Rain.id, value.soundId)
        assertEquals(AmbientSounds.Rain.defaultVolume, value.volume)
        assertNull(value.timerPreset)
        assertNull(value.timerStopAtEpochMillis)
        assertTrue(value.muted)
    }

    @Test fun validValuesAreRestored() {
        val value = AmbientAudioPreferences.safe("waves", 0.25f, "MINUTES_60", 123_456L, false)
        assertEquals("waves", value.soundId)
        assertEquals(0.25f, value.volume)
        assertEquals(SleepTimerPreset.MINUTES_60, value.timerPreset)
        assertEquals(123_456L, value.timerStopAtEpochMillis)
        assertFalse(value.muted)
    }

    @Test fun corruptValuesFallBackWithoutCrashing() {
        val value = AmbientAudioPreferences.safe("removed-sound", 4f, "OLD_PRESET", -1L, null)
        assertEquals(AmbientSounds.Rain.id, value.soundId)
        assertEquals(AmbientSounds.Rain.defaultVolume, value.volume)
        assertNull(value.timerPreset)
        assertNull(value.timerStopAtEpochMillis)
        assertTrue(value.muted)
    }
}
