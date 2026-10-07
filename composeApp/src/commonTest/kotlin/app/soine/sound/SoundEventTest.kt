package app.soine.sound

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class SoundEventTest {

    @Test
    fun keepsRequiredDerivedEventMetadata() {
        val event = event()

        assertEquals(SoundEventType.VOCALIZATION, event.type)
        assertEquals(1_234L, event.occurredAtEpochMillis)
        assertEquals(0.82, event.confidence)
        assertEquals(SoundEventSource.ON_DEVICE_MICROPHONE, event.source)
        assertEquals("detector-v1", event.modelVersion)
    }

    @Test
    fun confidenceMustBeNormalized() {
        assertFailsWith<IllegalArgumentException> {
            event(confidence = 1.01)
        }
        assertFailsWith<IllegalArgumentException> {
            event(confidence = -0.01)
        }
    }

    @Test
    fun timestampCannotBeNegative() {
        assertFailsWith<IllegalArgumentException> {
            event(occurredAtEpochMillis = -1)
        }
    }

    @Test
    fun modelVersionMustBePresent() {
        assertFailsWith<IllegalArgumentException> {
            event(modelVersion = " ")
        }
    }

    @Test
    fun modelHasNoRawAudioReference() {
        val propertyNames = listOf(
            "type",
            "occurredAtEpochMillis",
            "confidence",
            "source",
            "modelVersion",
        )

        assertFalse(propertyNames.any { it.contains("audioPath", ignoreCase = true) })
        assertFalse(propertyNames.any { it.contains("recording", ignoreCase = true) })
    }

    private fun event(
        occurredAtEpochMillis: Long = 1_234L,
        confidence: Double = 0.82,
        modelVersion: String = "detector-v1",
    ) = SoundEvent(
        type = SoundEventType.VOCALIZATION,
        occurredAtEpochMillis = occurredAtEpochMillis,
        confidence = confidence,
        source = SoundEventSource.ON_DEVICE_MICROPHONE,
        modelVersion = modelVersion,
    )
}
