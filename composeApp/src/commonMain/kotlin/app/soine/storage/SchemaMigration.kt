package app.soine.storage

/** A single forward-only step for a versioned persisted payload. */
interface SchemaMigration {
    val fromVersion: Int
    val toVersion: Int
    fun migrate(payload: String): String
}

/**
 * Applies explicit migrations one version at a time.
 *
 * It never downgrades or resets data. Unsupported/missing migrations fail so
 * callers can preserve the original persisted payload.
 */
class ForwardSchemaMigrator(
    private val currentVersion: Int,
    migrations: List<SchemaMigration>,
) {
    private val bySource = migrations.associateBy { it.fromVersion }

    init {
        require(currentVersion > 0) { "Current schema version must be positive." }
        require(bySource.size == migrations.size) { "Duplicate migration source version." }
        migrations.forEach {
            require(it.toVersion == it.fromVersion + 1) {
                "Migrations must advance exactly one schema version."
            }
        }
    }

    fun migrate(fromVersion: Int, payload: String): String {
        require(fromVersion > 0) { "Stored schema version must be positive." }
        if (fromVersion > currentVersion) {
            throw UnsupportedSchemaVersionException(fromVersion, currentVersion)
        }

        var version = fromVersion
        var result = payload
        while (version < currentVersion) {
            val migration = bySource[version]
                ?: throw MissingSchemaMigrationException(version, version + 1)
            result = migration.migrate(result)
            version = migration.toVersion
        }
        return result
    }
}

class UnsupportedSchemaVersionException(
    storedVersion: Int,
    currentVersion: Int,
) : IllegalStateException(
    "Stored schema version $storedVersion is newer than supported version $currentVersion.",
)

class MissingSchemaMigrationException(
    fromVersion: Int,
    toVersion: Int,
) : IllegalStateException("Missing schema migration $fromVersion -> $toVersion.")
