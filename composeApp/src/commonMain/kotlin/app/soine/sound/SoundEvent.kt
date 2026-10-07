package app.soine.sound

/**
 * A derived overnight sound event.
 *
 * This model intentionally contains no raw-audio path or recording reference.
 * Platform detectors must convert captured audio into derived events before
 * crossing into commonMain.
 */
data class SoundEvent(
    val type: SoundEventType,
    val occurredAtEpochMillis: Long,
    val confidence: Double,
    val source: SoundEventSource,
    val modelVersion: String,
) {
    init {
        require(occurredAtEpochMillis >= 0L) {
            "Sound event timestamp must not be negative."
        }
        require(confidence in 0.0..1.0) {
            "Sound event confidence must be between 0 and 1."
        }
        require(modelVersion.isNotBlank()) {
            "Sound event model version must not be blank."
        }
    }
}

/**
 * Product-facing categories only. They describe detected sound patterns and do
 * not make medical claims.
 */
enum class SoundEventType {
    VOCALIZATION,
    LOUD_SOUND,
    SNORE_LIKE,
    COUGH_LIKE,
    OTHER,
}

enum class SoundEventSource {
    ON_DEVICE_MICROPHONE,
}
