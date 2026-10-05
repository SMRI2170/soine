package app.soine.audio

import kotlin.test.*

class AmbientSoundTest {
    @Test fun defaultCatalogHasStableUniqueIds() {
        assertEquals(3, AmbientSounds.defaults.map { it.id }.distinct().size)
        assertEquals(AmbientSounds.Rain, AmbientSounds.find("rain"))
        assertNull(AmbientSounds.find("missing"))
    }

    @Test fun defaultSoundsAreBundledLoopableAndAvailable() {
        AmbientSounds.defaults.forEach {
            assertEquals(AmbientSoundSource.BUNDLED, it.source)
            assertTrue(it.loop)
            assertEquals(AmbientSoundAvailability.AVAILABLE, it.availability)
            assertTrue(it.defaultVolume in 0f..1f)
        }
    }

    @Test fun invalidVolumeIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            AmbientSound("bad", "Bad", AmbientSoundSource.BUNDLED, true, 1.1f)
        }
    }
}
