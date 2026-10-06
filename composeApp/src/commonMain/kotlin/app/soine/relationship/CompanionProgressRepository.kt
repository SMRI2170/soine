package app.soine.relationship

interface CompanionProgressRepository {
    suspend fun get(): RelationshipState
    suspend fun save(state: RelationshipState)
}

class StoredCompanionProgressRepository(
    private val store: RelationshipStateStore,
) : CompanionProgressRepository {
    override suspend fun get(): RelationshipState {
        val raw = store.read() ?: return RelationshipState()
        if (raw.isBlank()) return RelationshipState()
        return try {
            RelationshipStateCodec.decode(raw)
        } catch (cause: Throwable) {
            throw RelationshipStateStorageCorruptedException(cause)
        }
    }

    override suspend fun save(state: RelationshipState) {
        store.write(RelationshipStateCodec.encode(state))
    }
}

class RelationshipStateStorageCorruptedException(
    cause: Throwable,
) : IllegalStateException(
    "Stored relationship data could not be decoded. The original value was left untouched.",
    cause,
)

internal object RelationshipStateCodec {
    private const val SNAPSHOT_VERSION = 1

    fun encode(state: RelationshipState): String = buildList {
        add("V\t" + SNAPSHOT_VERSION)
        add(
            listOf(
                "S",
                state.schemaVersion.toString(),
                state.totalCompletedSleepMillis.toString(),
                state.completedSessions.toString(),
                state.familiarity.toString(),
            ).joinToString("\t")
        )
        state.discoveredBehaviorIds.sorted().forEach { add("B\t" + encodeUtf8Hex(it)) }
        state.processedSessionIds.sorted().forEach { add("P\t" + encodeUtf8Hex(it)) }
        state.achievedMilestoneIds.sorted().forEach { add("M\t" + encodeUtf8Hex(it)) }
    }.joinToString("\n")

    fun decode(raw: String): RelationshipState {
        val lines = raw.lineSequence().filter { it.isNotBlank() }.toList()
        require(lines.isNotEmpty()) { "Missing relationship snapshot version." }

        val version = lines.first().split('\t')
        require(version.size == 2 && version[0] == "V") { "Missing relationship snapshot version." }
        require(version[1].toInt() == SNAPSHOT_VERSION) {
            "Unsupported relationship snapshot version: " + version[1]
        }

        var stateHeader: List<String>? = null
        val behaviors = linkedSetOf<String>()
        val processed = linkedSetOf<String>()
        val milestones = linkedSetOf<String>()

        lines.drop(1).forEach { line ->
            val fields = line.split('\t')
            when (fields.firstOrNull()) {
                "S" -> {
                    require(stateHeader == null) { "Duplicate relationship state row." }
                    require(fields.size == 5) { "Malformed relationship state row." }
                    stateHeader = fields
                }
                "B" -> {
                    require(fields.size == 2) { "Malformed behavior row." }
                    behaviors += decodeUtf8Hex(fields[1])
                }
                "P" -> {
                    require(fields.size == 2) { "Malformed processed-session row." }
                    processed += decodeUtf8Hex(fields[1])
                }
                "M" -> {
                    require(fields.size == 2) { "Malformed milestone row." }
                    milestones += decodeUtf8Hex(fields[1])
                }
                else -> error("Unknown relationship snapshot row.")
            }
        }

        val header = requireNotNull(stateHeader) { "Missing relationship state row." }
        return RelationshipState(
            schemaVersion = header[1].toInt(),
            totalCompletedSleepMillis = header[2].toLong(),
            completedSessions = header[3].toInt(),
            familiarity = header[4].toInt(),
            discoveredBehaviorIds = behaviors,
            processedSessionIds = processed,
            achievedMilestoneIds = milestones,
        )
    }

    private fun encodeUtf8Hex(value: String): String =
        value.encodeToByteArray().joinToString(separator = "") { byte ->
            byte.toUByte().toString(radix = 16).padStart(2, '0')
        }

    private fun decodeUtf8Hex(value: String): String {
        require(value.length % 2 == 0) { "Malformed relationship hex string." }
        val bytes = ByteArray(value.length / 2) { index ->
            value.substring(index * 2, index * 2 + 2).toInt(radix = 16).toByte()
        }
        return bytes.decodeToString()
    }
}
