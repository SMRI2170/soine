package app.soine.sound

interface SoundEventStore {
    fun read(): String?
    fun write(value: String)
    fun clear()
}

data class StoredSoundEvent(
    val sessionId: String,
    val event: SoundEvent,
) {
    init {
        require(sessionId.isNotBlank()) { "Sound event session id must not be blank." }
    }
}

data class SoundEventSessionSummary(
    val sessionId: String,
    val eventCount: Int,
    val latestOccurredAtEpochMillis: Long,
)

interface SoundEventRepository {
    suspend fun getAll(): List<StoredSoundEvent>
    suspend fun replaceSessionEvents(sessionId: String, events: List<SoundEvent>)
    suspend fun appendSessionEvents(sessionId: String, events: List<SoundEvent>)
    suspend fun deleteSession(sessionId: String): Int
    suspend fun deleteAll(): Int?
}

class StoredSoundEventRepository(
    private val store: SoundEventStore,
) : SoundEventRepository {
    override suspend fun getAll(): List<StoredSoundEvent> {
        val raw = store.read() ?: return emptyList()
        if (raw.isBlank()) return emptyList()
        return try {
            SoundEventSnapshotCodec.decode(raw)
        } catch (cause: Throwable) {
            throw SoundEventStorageCorruptedException(cause)
        }
    }

    override suspend fun replaceSessionEvents(
        sessionId: String,
        events: List<SoundEvent>,
    ) {
        require(sessionId.isNotBlank()) { "Sound event session id must not be blank." }
        val current = getAll().filterNot { it.sessionId == sessionId }
        val next = current + events.map { StoredSoundEvent(sessionId, it) }
        if (next.isEmpty()) {
            store.clear()
        } else {
            store.write(SoundEventSnapshotCodec.encode(next))
        }
    }

    override suspend fun appendSessionEvents(
        sessionId: String,
        events: List<SoundEvent>,
    ) {
        require(sessionId.isNotBlank()) { "Sound event session id must not be blank." }
        if (events.isEmpty()) return

        val current = getAll()
        val next = current + events.map { StoredSoundEvent(sessionId, it) }
        store.write(SoundEventSnapshotCodec.encode(next))
    }

    override suspend fun deleteSession(sessionId: String): Int {
        val current = getAll()
        val removed = current.count { it.sessionId == sessionId }
        if (removed == 0) return 0

        val next = current.filterNot { it.sessionId == sessionId }
        if (next.isEmpty()) {
            store.clear()
        } else {
            store.write(SoundEventSnapshotCodec.encode(next))
        }
        return removed
    }

    override suspend fun deleteAll(): Int? {
        // Explicit privacy deletion must remain available even when the stored
        // snapshot is corrupt or from an unsupported future schema. A null
        // count means data was cleared successfully but could not be decoded
        // well enough to report an exact number of removed events.
        val count = runCatching { getAll().size }.getOrNull()
        store.clear()
        return count
    }
}

fun List<StoredSoundEvent>.sessionSummaries(): List<SoundEventSessionSummary> =
    groupBy(StoredSoundEvent::sessionId)
        .map { (sessionId, records) ->
            SoundEventSessionSummary(
                sessionId = sessionId,
                eventCount = records.size,
                latestOccurredAtEpochMillis =
                    records.maxOf { it.event.occurredAtEpochMillis },
            )
        }
        .sortedByDescending(SoundEventSessionSummary::latestOccurredAtEpochMillis)

class SoundEventStorageCorruptedException(
    cause: Throwable,
) : IllegalStateException(
    "Stored sound-event data could not be decoded. The original value was left untouched.",
    cause,
)

internal object SoundEventSnapshotCodec {
    private const val VERSION = 1

    fun encode(records: List<StoredSoundEvent>): String = buildList {
        add("V\t$VERSION")
        records.sortedWith(
            compareBy<StoredSoundEvent>(
                StoredSoundEvent::sessionId,
                { it.event.occurredAtEpochMillis },
            ),
        ).forEach { record ->
            add(
                listOf(
                    "E",
                    hex(record.sessionId),
                    record.event.type.name,
                    record.event.occurredAtEpochMillis.toString(),
                    record.event.confidence.toString(),
                    record.event.source.name,
                    hex(record.event.modelVersion),
                ).joinToString("\t"),
            )
        }
    }.joinToString("\n")

    fun decode(raw: String): List<StoredSoundEvent> {
        val lines = raw.lineSequence().filter(String::isNotBlank).toList()
        require(lines.isNotEmpty()) { "Missing sound-event snapshot version." }
        val version = lines.first().split('\t')
        require(version.size == 2 && version[0] == "V" && version[1].toInt() == VERSION) {
            "Unsupported sound-event snapshot."
        }

        return lines.drop(1).map { line ->
            val fields = line.split('\t')
            require(fields.size == 7 && fields[0] == "E") {
                "Malformed sound-event row."
            }
            StoredSoundEvent(
                sessionId = unhex(fields[1]),
                event = SoundEvent(
                    type = SoundEventType.valueOf(fields[2]),
                    occurredAtEpochMillis = fields[3].toLong(),
                    confidence = fields[4].toDouble(),
                    source = SoundEventSource.valueOf(fields[5]),
                    modelVersion = unhex(fields[6]),
                ),
            )
        }
    }

    private fun hex(value: String): String =
        value.encodeToByteArray().joinToString("") {
            it.toUByte().toString(16).padStart(2, '0')
        }

    private fun unhex(value: String): String {
        require(value.length % 2 == 0) { "Malformed hex string." }
        return ByteArray(value.length / 2) { index ->
            value.substring(index * 2, index * 2 + 2).toInt(16).toByte()
        }.decodeToString()
    }
}
