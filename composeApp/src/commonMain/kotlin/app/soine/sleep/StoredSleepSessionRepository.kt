package app.soine.sleep

import app.soine.storage.ForwardSchemaMigrator
import app.soine.storage.SchemaMigration
import app.soine.storage.SleepSessionStore

/**
 * Local-first SleepSessionRepository backed by one versioned text snapshot.
 *
 * The snapshot format is deliberately platform-neutral. Android/iOS adapters
 * only provide a durable string store.
 */
class StoredSleepSessionRepository(
    private val store: SleepSessionStore,
) : SleepSessionRepository {

    override suspend fun getActiveSession(): SleepSessionRecord? =
        readSnapshot().active

    override suspend fun saveSession(session: SleepSessionRecord) {
        val current = readSnapshot()

        val next = if (session.isActive) {
            val otherActive = current.active
            require(otherActive == null || otherActive.id == session.id) {
                "Only one active sleep session is allowed."
            }

            current.copy(
                active = session,
                completed = current.completed.filterNot { it.id == session.id },
            )
        } else {
            current.copy(
                active = current.active?.takeUnless { it.id == session.id },
                completed = replaceCompleted(current.completed, session),
            )
        }

        writeSnapshot(next)
    }

    override suspend fun completeSession(
        sessionId: String,
        endedAtEpochMillis: Long,
        updatedAtEpochMillis: Long,
    ): SleepSessionRecord? {
        val current = readSnapshot()

        current.completed.firstOrNull { it.id == sessionId }?.let { return it }

        val active = current.active?.takeIf { it.id == sessionId } ?: return null
        val completed = active.copy(
            endedAtEpochMillis = endedAtEpochMillis.coerceAtLeast(active.startedAtEpochMillis),
            status = SleepSessionStatus.COMPLETED,
            updatedAtEpochMillis = updatedAtEpochMillis.coerceAtLeast(active.updatedAtEpochMillis),
        )

        writeSnapshot(
            current.copy(
                active = null,
                completed = replaceCompleted(current.completed, completed),
            ),
        )
        return completed
    }

    override suspend fun getCompletedSessions(): List<SleepSessionRecord> =
        readSnapshot().completed.sortedByDescending { it.startedAtEpochMillis }

    private fun readSnapshot(): SleepSessionSnapshot {
        val raw = store.read() ?: return SleepSessionSnapshot()
        if (raw.isBlank()) return SleepSessionSnapshot()

        return try {
            SleepSessionSnapshotCodec.decode(raw)
        } catch (cause: Throwable) {
            throw SleepSessionStorageCorruptedException(cause)
        }
    }

    private fun writeSnapshot(snapshot: SleepSessionSnapshot) {
        store.write(SleepSessionSnapshotCodec.encode(snapshot))
    }

    private fun replaceCompleted(
        completed: List<SleepSessionRecord>,
        replacement: SleepSessionRecord,
    ): List<SleepSessionRecord> =
        completed.filterNot { it.id == replacement.id } + replacement
}

class SleepSessionStorageCorruptedException(
    cause: Throwable,
) : IllegalStateException(
    "Stored sleep-session data could not be decoded. The original value was left untouched.",
    cause,
)

internal data class SleepSessionSnapshot(
    val active: SleepSessionRecord? = null,
    val completed: List<SleepSessionRecord> = emptyList(),
)

internal object SleepSessionSnapshotCodec {
    private const val SNAPSHOT_VERSION = "2"
    private val migrator = ForwardSchemaMigrator(
        currentVersion = SNAPSHOT_VERSION.toInt(),
        migrations = listOf(SleepSessionSnapshotV1ToV2),
    )

    fun encode(snapshot: SleepSessionSnapshot): String = buildList {
        add("V\t$SNAPSHOT_VERSION")
        snapshot.active?.let { add("A\t${encodeRecord(it)}") }
        snapshot.completed
            .sortedByDescending { it.startedAtEpochMillis }
            .forEach { add("C\t${encodeRecord(it)}") }
    }.joinToString("\n")

    fun decode(raw: String): SleepSessionSnapshot {
        val lines = raw.lineSequence().filter { it.isNotBlank() }.toList()
        require(lines.isNotEmpty()) { "Missing snapshot version." }

        val version = lines.first().split('\t')
        require(version.size == 2 && version[0] == "V") { "Missing snapshot version." }
        val storedVersion = version[1].toInt()
        val migrated = migrator.migrate(storedVersion, raw)
        if (migrated != raw) return decode(migrated)
        require(version[1] == SNAPSHOT_VERSION) { "Unsupported snapshot version: ${version[1]}" }

        var active: SleepSessionRecord? = null
        val completed = mutableListOf<SleepSessionRecord>()

        lines.drop(1).forEach { line ->
            val separator = line.indexOf('\t')
            require(separator > 0) { "Malformed snapshot row." }

            val kind = line.substring(0, separator)
            val record = decodeRecord(line.substring(separator + 1))

            when (kind) {
                "A" -> {
                    require(active == null) { "Multiple active sessions in snapshot." }
                    require(record.isActive) { "Active row contains completed session." }
                    active = record
                }
                "C" -> {
                    require(!record.isActive) { "Completed row contains active session." }
                    completed += record
                }
                else -> error("Unknown snapshot row kind: $kind")
            }
        }

        require(completed.map { it.id }.distinct().size == completed.size) {
            "Duplicate completed session ids in snapshot."
        }
        require(active == null || completed.none { it.id == active.id }) {
            "Active session is also present in completed sessions."
        }

        return SleepSessionSnapshot(active = active, completed = completed)
    }

    private fun encodeRecord(record: SleepSessionRecord): String = listOf(
        encodeUtf8Hex(record.id),
        record.startedAtEpochMillis.toString(),
        record.endedAtEpochMillis?.toString().orEmpty(),
        record.status.name,
        record.source.name,
        record.createdAtEpochMillis.toString(),
        record.updatedAtEpochMillis.toString(),
        record.schemaVersion.toString(),
    ).joinToString("\t")

    private fun decodeRecord(raw: String): SleepSessionRecord {
        val fields = raw.split('\t')
        require(fields.size == 8) { "Malformed session record." }

        return SleepSessionRecord(
            id = decodeUtf8Hex(fields[0]),
            startedAtEpochMillis = fields[1].toLong(),
            endedAtEpochMillis = fields[2].takeIf { it.isNotEmpty() }?.toLong(),
            status = SleepSessionStatus.valueOf(fields[3]),
            source = SleepSessionSource.valueOf(fields[4]),
            createdAtEpochMillis = fields[5].toLong(),
            updatedAtEpochMillis = fields[6].toLong(),
            schemaVersion = fields[7].toInt(),
        )
    }

    private fun encodeUtf8Hex(value: String): String =
        value.encodeToByteArray().joinToString(separator = "") { byte ->
            byte.toUByte().toString(radix = 16).padStart(2, '0')
        }

    private fun decodeUtf8Hex(value: String): String {
        require(value.length % 2 == 0) { "Malformed hex string." }
        val bytes = ByteArray(value.length / 2) { index ->
            value.substring(index * 2, index * 2 + 2).toInt(radix = 16).toByte()
        }
        return bytes.decodeToString()
    }
}


private object SleepSessionSnapshotV1ToV2 : SchemaMigration {
    override val fromVersion: Int = 1
    override val toVersion: Int = 2

    override fun migrate(payload: String): String {
        val lines = payload.lineSequence().toList()
        require(lines.firstOrNull() == "V\t1") { "Expected SleepSession snapshot v1." }
        return buildList {
            add("V\t2")
            addAll(lines.drop(1))
        }.joinToString("\n")
    }
}
