package app.soine.dream

interface DreamDiscoveryStore {
    fun read(): String?
    fun write(value: String)
    fun clear()
}

data class DreamDiscoverySnapshot(
    val decisions: List<DreamDiscoveryDecision> = emptyList(),
    val discoveries: List<DreamDiscovery> = emptyList(),
) {
    init {
        require(decisions.map { it.sessionId }.distinct().size == decisions.size) {
            "Dream discovery decisions must be unique per session."
        }
        require(discoveries.map { it.dreamId }.distinct().size == discoveries.size) {
            "Dream discoveries must be unique per dream."
        }
        require(discoveries.map { it.sessionId }.distinct().size == discoveries.size) {
            "Dream discoveries must be unique per session."
        }
    }
}

interface DreamDiscoveryRepository {
    suspend fun get(): DreamDiscoverySnapshot

    /**
     * Persists the decision and optional discovery as one snapshot write.
     * Re-saving an already decided session is a no-op so a session cannot reroll.
     */
    suspend fun saveEvaluation(
        decision: DreamDiscoveryDecision,
        discovery: DreamDiscovery?,
    ): DreamDiscoverySnapshot
}

class StoredDreamDiscoveryRepository(
    private val store: DreamDiscoveryStore,
) : DreamDiscoveryRepository {
    override suspend fun get(): DreamDiscoverySnapshot {
        val raw = store.read() ?: return DreamDiscoverySnapshot()
        if (raw.isBlank()) return DreamDiscoverySnapshot()
        return try {
            DreamDiscoverySnapshotCodec.decode(raw)
        } catch (cause: Throwable) {
            throw DreamDiscoveryStorageCorruptedException(cause)
        }
    }

    override suspend fun saveEvaluation(
        decision: DreamDiscoveryDecision,
        discovery: DreamDiscovery?,
    ): DreamDiscoverySnapshot {
        val current = get()
        if (current.decisions.any { it.sessionId == decision.sessionId }) return current

        require(discovery == null || discovery.sessionId == decision.sessionId) {
            "Dream discovery must belong to the evaluated session."
        }
        require(discovery == null || discovery.dreamId == decision.dreamId) {
            "Dream discovery must match the decision dream id."
        }
        require(discovery == null || current.discoveries.none { it.dreamId == discovery.dreamId }) {
            "Dream has already been discovered."
        }

        val next = DreamDiscoverySnapshot(
            decisions = current.decisions + decision,
            discoveries = current.discoveries + listOfNotNull(discovery),
        )
        store.write(DreamDiscoverySnapshotCodec.encode(next))
        return next
    }
}

class DreamDiscoveryStorageCorruptedException(
    cause: Throwable,
) : IllegalStateException(
    "Stored dream discovery data could not be decoded. The original value was left untouched.",
    cause,
)

internal object DreamDiscoverySnapshotCodec {
    private const val SNAPSHOT_VERSION = 1
    private const val NO_DREAM = "-"

    fun encode(snapshot: DreamDiscoverySnapshot): String = buildList {
        add("V\t" + SNAPSHOT_VERSION)
        snapshot.decisions.sortedBy { it.sessionId }.forEach { decision ->
            add(
                listOf(
                    "E",
                    encodeUtf8Hex(decision.sessionId),
                    decision.algorithmVersion.toString(),
                    decision.dreamId?.let(::encodeUtf8Hex) ?: NO_DREAM,
                    decision.evaluatedAtEpochMillis.toString(),
                ).joinToString("\t")
            )
        }
        snapshot.discoveries.sortedBy { it.dreamId }.forEach { discovery ->
            add(
                listOf(
                    "D",
                    encodeUtf8Hex(discovery.dreamId),
                    encodeUtf8Hex(discovery.sessionId),
                    discovery.discoveredAtEpochMillis.toString(),
                ).joinToString("\t")
            )
        }
    }.joinToString("\n")

    fun decode(raw: String): DreamDiscoverySnapshot {
        val lines = raw.lineSequence().filter { it.isNotBlank() }.toList()
        require(lines.isNotEmpty()) { "Missing dream snapshot version." }

        val version = lines.first().split('\t')
        require(version.size == 2 && version[0] == "V") {
            "Missing dream snapshot version."
        }
        require(version[1].toInt() == SNAPSHOT_VERSION) {
            "Unsupported dream snapshot version: " + version[1]
        }

        val decisions = mutableListOf<DreamDiscoveryDecision>()
        val discoveries = mutableListOf<DreamDiscovery>()
        lines.drop(1).forEach { line ->
            val fields = line.split('\t')
            when (fields.firstOrNull()) {
                "E" -> {
                    require(fields.size == 5) { "Malformed dream evaluation row." }
                    decisions += DreamDiscoveryDecision(
                        sessionId = decodeUtf8Hex(fields[1]),
                        algorithmVersion = fields[2].toInt(),
                        dreamId = fields[3].takeUnless { it == NO_DREAM }?.let(::decodeUtf8Hex),
                        evaluatedAtEpochMillis = fields[4].toLong(),
                    )
                }
                "D" -> {
                    require(fields.size == 4) { "Malformed dream discovery row." }
                    discoveries += DreamDiscovery(
                        dreamId = decodeUtf8Hex(fields[1]),
                        sessionId = decodeUtf8Hex(fields[2]),
                        discoveredAtEpochMillis = fields[3].toLong(),
                    )
                }
                else -> error("Unknown dream snapshot row.")
            }
        }

        val snapshot = DreamDiscoverySnapshot(
            decisions = decisions,
            discoveries = discoveries,
        )
        val decisionsBySession = decisions.associateBy { it.sessionId }
        discoveries.forEach { discovery ->
            val decision = decisionsBySession[discovery.sessionId]
            require(decision?.dreamId == discovery.dreamId) {
                "Dream discovery has no matching persisted decision."
            }
        }
        return snapshot
    }

    private fun encodeUtf8Hex(value: String): String =
        value.encodeToByteArray().joinToString(separator = "") { byte ->
            byte.toUByte().toString(radix = 16).padStart(2, '0')
        }

    private fun decodeUtf8Hex(value: String): String {
        require(value.length % 2 == 0) { "Malformed dream hex string." }
        val bytes = ByteArray(value.length / 2) { index ->
            value.substring(index * 2, index * 2 + 2).toInt(radix = 16).toByte()
        }
        return bytes.decodeToString()
    }
}
