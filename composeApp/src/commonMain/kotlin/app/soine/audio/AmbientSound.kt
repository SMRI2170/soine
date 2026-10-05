package app.soine.audio

enum class AmbientSoundSource { BUNDLED, REMOTE }
enum class AmbientSoundAvailability { AVAILABLE, UNAVAILABLE, REQUIRES_DOWNLOAD }

data class AmbientSound(
    val id: String,
    val displayName: String,
    val source: AmbientSoundSource,
    val loop: Boolean,
    val defaultVolume: Float,
    val availability: AmbientSoundAvailability = AmbientSoundAvailability.AVAILABLE,
) {
    init {
        require(id.isNotBlank()) { "Sound id must not be blank." }
        require(displayName.isNotBlank()) { "Display name must not be blank." }
        require(defaultVolume in 0f..1f) { "Default volume must be between 0 and 1." }
    }
}

object AmbientSounds {
    val Rain = AmbientSound("rain", "雨", AmbientSoundSource.BUNDLED, loop = true, defaultVolume = 0.45f)
    val Waves = AmbientSound("waves", "波", AmbientSoundSource.BUNDLED, loop = true, defaultVolume = 0.40f)
    val WhiteNoise = AmbientSound("white-noise", "ホワイトノイズ", AmbientSoundSource.BUNDLED, loop = true, defaultVolume = 0.30f)

    val defaults: List<AmbientSound> = listOf(Rain, Waves, WhiteNoise)
    fun find(id: String): AmbientSound? = defaults.firstOrNull { it.id == id }
}
