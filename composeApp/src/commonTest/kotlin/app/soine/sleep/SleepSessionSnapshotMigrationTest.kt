package app.soine.sleep

import kotlin.test.Test
import kotlin.test.assertEquals

class SleepSessionSnapshotMigrationTest {
    @Test
    fun v1FixtureMigratesWithoutLosingSession() {
        val fixture = "V\t1\nA\t6e696768742d31\t1000\t\tSLEEPING\tMANUAL\t1000\t1000\t1"

        val snapshot = SleepSessionSnapshotCodec.decode(fixture)

        assertEquals("night-1", snapshot.active?.id)
        assertEquals(SleepSessionStatus.SLEEPING, snapshot.active?.status)
        assertEquals(1000L, snapshot.active?.startedAtEpochMillis)
        assertEquals("V\t2", SleepSessionSnapshotCodec.encode(snapshot).lineSequence().first())
    }
}
