package app.soine.companion

import kotlin.test.*

class CompanionAnimationMappingTest {
    @Test fun everySemanticIntentHasAPreferredClip() {
        CompanionIntent.entries.forEach { intent ->
            val clip = CompanionAnimationMapping.preferredClip(intent)
            assertTrue(clip.id.isNotBlank(), "Missing preferred clip for $intent")
        }
    }

    @Test fun fullAssetSetResolvesEveryIntentWithoutFallbackArtwork() {
        CompanionIntent.entries.forEach { intent ->
            val selection = CompanionAnimationMapping.resolve(
                intent,
                CompanionAnimationMapping.requiredClipIds,
            )
            assertNotNull(selection.clip, "No clip resolved for $intent")
            assertFalse(selection.usesStaticFallback)
        }
    }

    @Test fun missingSpecificClipUsesOrderedSemanticFallback() {
        val selection = CompanionAnimationMapping.resolve(
            CompanionIntent.NOTICE_USER,
            setOf("look_at_user", "idle"),
        )

        assertEquals("look_at_user", selection.clip?.id)
        assertFalse(selection.clip?.loop ?: true)
    }

    @Test fun sleepCanFallBackToBreathingLoop() {
        val selection = CompanionAnimationMapping.resolve(
            CompanionIntent.SLEEP,
            setOf("breathe", "idle"),
        )

        assertEquals("breathe", selection.clip?.id)
        assertTrue(selection.clip?.loop == true)
    }

    @Test fun microReactionFallsBackToStableSleepPose() {
        val selection = CompanionAnimationMapping.resolve(
            CompanionIntent.EAR_TWITCH,
            setOf("sleep", "idle"),
        )

        assertEquals("sleep", selection.clip?.id)
        assertTrue(selection.clip?.loop == true)
    }

    @Test fun noAvailableClipSignalsStaticFallback() {
        val selection = CompanionAnimationMapping.resolve(
            CompanionIntent.STRETCH,
            emptySet(),
        )

        assertNull(selection.clip)
        assertTrue(selection.usesStaticFallback)
    }

    @Test fun animationCatalogIdsAreStableAndUnique() {
        val ids = CompanionAnimationMapping.requiredClipIds
        assertEquals(14, ids.size)
        assertTrue("idle" in ids)
        assertTrue("breathe" in ids)
        assertTrue("wake" in ids)
    }
}
