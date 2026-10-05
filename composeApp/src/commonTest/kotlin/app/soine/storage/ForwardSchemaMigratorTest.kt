package app.soine.storage

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ForwardSchemaMigratorTest {
    @Test
    fun appliesMigrationsInOrder() {
        val migrator = ForwardSchemaMigrator(
            currentVersion = 3,
            migrations = listOf(step(1, 2, "v2"), step(2, 3, "v3")),
        )
        assertEquals("v3", migrator.migrate(1, "v1"))
    }

    @Test
    fun currentVersionPassesThrough() {
        assertEquals("same", ForwardSchemaMigrator(2, emptyList()).migrate(2, "same"))
    }

    @Test
    fun newerDataIsNeverDowngraded() {
        assertFailsWith<UnsupportedSchemaVersionException> {
            ForwardSchemaMigrator(2, emptyList()).migrate(3, "future")
        }
    }

    @Test
    fun missingMigrationFailsInsteadOfResetting() {
        assertFailsWith<MissingSchemaMigrationException> {
            ForwardSchemaMigrator(2, emptyList()).migrate(1, "keep-me")
        }
    }

    private fun step(from: Int, to: Int, output: String) = object : SchemaMigration {
        override val fromVersion = from
        override val toVersion = to
        override fun migrate(payload: String) = output
    }
}
