package app.soine.night

import app.soine.sleep.SleepSessionRecord
import app.soine.sleep.SleepSessionSource
import app.soine.sleep.SleepSessionStatus
import kotlin.test.*

class NightEventGenerationMetadataTest {
    @Test fun sameCompletedSessionAlwaysProducesSameSeed() {
        val session = completedSession()

        val first = NightEventGenerationMetadata.forSession(session)
        val reopened = NightEventGenerationMetadata.forSession(session)

        assertEquals(first, reopened)
        assertEquals(session.id, first.sessionId)
        assertEquals(NightEventGenerationMetadata.CURRENT_ALGORITHM_VERSION, first.algorithmVersion)
    }

    @Test fun differentSessionIdentityOrTimesChangeSeed() {
        val base = completedSession()
        val baseSeed = NightEventGenerationMetadata.forSession(base).seed

        assertNotEquals(
            baseSeed,
            NightEventGenerationMetadata.forSession(base.copy(id = "night-2")).seed,
        )
        assertNotEquals(
            baseSeed,
            NightEventGenerationMetadata.forSession(
                base.copy(endedAtEpochMillis = base.endedAtEpochMillis!! + 1),
            ).seed,
        )
    }

    @Test fun persistedVersionWinsAfterAppAlgorithmUpgrade() {
        val session = completedSession()
        val persisted = NightEventGenerationMetadata.forSession(session, algorithmVersion = 1)

        val reopened = NightEventGenerationMetadata.reuseOrCreate(
            session = session,
            persisted = persisted,
            currentAlgorithmVersion = 2,
        )

        assertSame(persisted, reopened)
        assertEquals(1, reopened.algorithmVersion)
    }

    @Test fun newSessionUsesCurrentAlgorithmVersion() {
        val metadata = NightEventGenerationMetadata.reuseOrCreate(
            session = completedSession(),
            persisted = null,
            currentAlgorithmVersion = 2,
        )

        assertEquals(2, metadata.algorithmVersion)
    }

    @Test fun metadataCannotBeReusedForAnotherSession() {
        assertFailsWith<IllegalArgumentException> {
            NightEventGenerationMetadata.reuseOrCreate(
                session = completedSession().copy(id = "night-2"),
                persisted = NightEventGenerationMetadata.forSession(completedSession()),
            )
        }
    }

    @Test fun seedRequiresCompletedSessionWithEndTime() {
        assertFailsWith<IllegalArgumentException> {
            NightEventGenerationMetadata.forSession(
                completedSession().copy(
                    status = SleepSessionStatus.SLEEPING,
                    endedAtEpochMillis = null,
                ),
            )
        }
    }

    @Test fun explicitRandomProducesSameSequenceFromSameSeed() {
        val a = NightEventRandom(123456789L)
        val b = NightEventRandom(123456789L)

        assertEquals(
            List(20) { a.nextInt(10_000) },
            List(20) { b.nextInt(10_000) },
        )
    }

    private fun completedSession() = SleepSessionRecord(
        id = "night-1",
        startedAtEpochMillis = 1_000,
        endedAtEpochMillis = 28_801_000,
        status = SleepSessionStatus.COMPLETED,
        source = SleepSessionSource.MANUAL,
        createdAtEpochMillis = 1_000,
        updatedAtEpochMillis = 28_801_000,
    )
}
