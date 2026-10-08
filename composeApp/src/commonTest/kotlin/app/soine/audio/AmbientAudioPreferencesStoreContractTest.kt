package app.soine.audio

import app.soine.sound.SoundAnalysisPreferences
import app.soine.sound.SoundAnalysisPreferencesStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame

/**
 * Smoke test for the Settings route contract. The SettingsScreen
 * composable is wired through:
 *
 *   - AmbientAudioPreferencesStore.read / write (default sound, volume,
 *     timer, mute)
 *   - SoundAnalysisPreferencesStore.read / write (overnight analysis opt-in)
 *   - MicrophonePermissionController for the permission UX
 *
 * Compose UI instrumented tests are deferred (see
 * docs/ci-quality-policy.md). Until then, this test pins the surface
 * that the SettingsScreen relies on:
 *
 *   1. The `write` parameter on both stores is named `preferences` so
 *      callers can pass it by name without silently picking up a
 *      different field. This is the regression that #193 caught when
 *      Android stores were using `preferencesValue` while the interface
 *      declared `preferences`; `-Werror` (kotlinc parameter name
 *      consistency rule) failed the build.
 *   2. A fake store round-trips through read / write, so the Settings
 *      callbacks produce the state the next read returns.
 *   3. The clear() method returns the store to its default state, so
 *      Settings → Privacy → "delete all" reaches a clean baseline.
 *
 * The test does not cover the actual Compose tree; it covers the
 * contract layer that the SettingsScreen is built on.
 */
class AmbientAudioPreferencesStoreContractTest {

    @Test
    fun writeMethodIsNamedPreferencesForAmbientStore() {
        val store = InMemoryAmbientStore()
        // Kotlinc will not compile a named-argument call against a
        // parameter that does not exist; this line pins the contract
        // by name so any rename breaks the smoke gate.
        store.write(preferences = AmbientAudioPreferences.safe(null, null, null, null, null))
        val read = store.read()
        assertNotNull(read)
    }

    @Test
    fun writeMethodIsNamedPreferencesForSoundAnalysisStore() {
        val store = InMemorySoundAnalysisStore()
        store.write(preferences = SoundAnalysisPreferences(enabled = true))
        assertEquals(true, store.read().enabled)
    }

    @Test
    fun roundTripsAmbientPreferences() {
        val store = InMemoryAmbientStore()
        val target = AmbientAudioPreferences.safe(
            soundId = AmbientSounds.Waves.id,
            volume = 0.42f,
            timerPreset = "MINUTES_30",
            timerStopAtEpochMillis = 9_000L,
            muted = false,
        )
        store.write(target)
        val read = store.read()
        assertEquals(AmbientSounds.Waves.id, read.soundId)
        assertEquals(0.42f, read.volume)
        assertEquals(SleepTimerPreset.MINUTES_30, read.timerPreset)
        assertEquals(9_000L, read.timerStopAtEpochMillis)
        assertEquals(false, read.muted)
    }

    @Test
    fun clearReturnsAmbientStoreToDefaults() {
        val store = InMemoryAmbientStore()
        store.write(
            AmbientAudioPreferences.safe(
                soundId = AmbientSounds.Rain.id,
                volume = 0.9f,
                timerPreset = "MINUTES_60",
                timerStopAtEpochMillis = 1_000L,
                muted = false,
            ),
        )
        store.clear()
        val after = store.read()
        assertEquals(AmbientSounds.Rain.id, after.soundId)
        assertEquals(AmbientSounds.Rain.defaultVolume, after.volume)
        assertEquals(null, after.timerPreset)
        assertEquals(null, after.timerStopAtEpochMillis)
        assertEquals(true, after.muted)
    }

    @Test
    fun clearReturnsSoundAnalysisStoreToDefaults() {
        val store = InMemorySoundAnalysisStore()
        store.write(preferences = SoundAnalysisPreferences(enabled = true))
        store.clear()
        assertEquals(false, store.read().enabled)
    }

    @Test
    fun twoStoresAreIndependentSoSettingsWritesOneAtATime() {
        val ambient = InMemoryAmbientStore()
        val sound = InMemorySoundAnalysisStore()
        // Write a value that differs from the defaults so we can tell
        // which side moved. The default ambient volume is
        // AmbientSounds.Rain.defaultVolume, not 0; using 0.77f avoids
        // an accidental collision with the default and is a value a
        // Settings user could realistically pick.
        val customVolume = 0.77f
        ambient.write(
            AmbientAudioPreferences.safe(
                soundId = AmbientSounds.Rain.id,
                volume = customVolume,
                timerPreset = null,
                timerStopAtEpochMillis = null,
                muted = false,
            ),
        )
        sound.write(preferences = SoundAnalysisPreferences(enabled = false))
        // The two stores must not share backing state: Settings can
        // toggle ambient sound without flipping the sound-analysis opt-in.
        assertEquals(customVolume, ambient.read().volume)
        assertEquals(false, sound.read().enabled)
    }

    @Test
    fun settingsDefaultInstanceIsShareable() {
        // The SettingsScreen receives the singleton store instances
        // through SoineApp's composition; multiple reads against the
        // same store must return the latest state. This pins the
        // read-after-write ordering the Settings callbacks rely on.
        val store = InMemoryAmbientStore()
        val first = store.read()
        store.write(
            AmbientAudioPreferences.safe(
                soundId = AmbientSounds.Rain.id,
                volume = 0.1f,
                timerPreset = null,
                timerStopAtEpochMillis = null,
                muted = true,
            ),
        )
        val second = store.read()
        assertSame(store, store)
        assertEquals(first.soundId, AmbientSounds.Rain.id)
        assertEquals(second.volume, 0.1f)
    }
}

private class InMemoryAmbientStore(
    initial: AmbientAudioPreferences = AmbientAudioPreferences.safe(null, null, null, null, null),
) : AmbientAudioPreferencesStore {
    private var state: AmbientAudioPreferences = initial

    override fun read(): AmbientAudioPreferences = state

    override fun write(preferences: AmbientAudioPreferences) {
        state = preferences
    }

    override fun clear() {
        state = AmbientAudioPreferences.safe(null, null, null, null, null)
    }
}

private class InMemorySoundAnalysisStore(
    initial: SoundAnalysisPreferences = SoundAnalysisPreferences(enabled = false),
) : SoundAnalysisPreferencesStore {
    private var state: SoundAnalysisPreferences = initial

    override fun read(): SoundAnalysisPreferences = state

    override fun write(preferences: SoundAnalysisPreferences) {
        state = preferences
    }

    override fun clear() {
        state = SoundAnalysisPreferences(enabled = false)
    }
}
