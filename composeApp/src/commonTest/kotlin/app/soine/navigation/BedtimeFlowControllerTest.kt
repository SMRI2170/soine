package app.soine.navigation

import app.soine.sleep.*
import kotlin.test.*
import kotlin.coroutines.startCoroutine

class BedtimeFlowControllerTest {
    @Test fun startupWithActiveSessionRestoresSleeping() = runSuspend {
        val repo = FakeRepo(active = record("night-1", SleepSessionStatus.PREPARING))
        val result = BedtimeFlowController(repo).initialDestination()
        assertIs<BedtimeDestination.Sleeping>(result)
        assertEquals(SleepSessionStatus.SLEEPING, repo.active?.status)
        assertEquals(SleepSessionSource.RECOVERED, repo.active?.source)
    }

    @Test fun startupWithoutActiveSessionShowsBedtime() = runSuspend {
        assertIs<BedtimeDestination.Bedtime>(BedtimeFlowController(FakeRepo()).initialDestination())
    }

    @Test fun finishMovesToMorningUsingPersistedCompletedRecord() = runSuspend {
        val repo = FakeRepo(active = record("night-1", SleepSessionStatus.SLEEPING))
        val result = BedtimeFlowController(repo).finish()
        assertIs<BedtimeDestination.Morning>(result)
        assertNull(repo.active)
        assertEquals(1, repo.completed.size)
    }

    @Test fun repeatedFinishDoesNotCreateAnotherSession() = runSuspend {
        val completed = record("night-1", SleepSessionStatus.COMPLETED, endedAt = 2_000)
        val repo = FakeRepo(completed = mutableListOf(completed))
        val result = BedtimeFlowController(repo).finish()
        assertEquals(BedtimeDestination.Morning(completed), result)
        assertEquals(1, repo.completed.size)
    }
}

private class FakeRepo(
    var active: SleepSessionRecord? = null,
    val completed: MutableList<SleepSessionRecord> = mutableListOf(),
) : SleepSessionRepository {
    override suspend fun getActiveSession() = active
    override suspend fun saveSession(session: SleepSessionRecord) { active = session.takeIf { it.isActive } }
    override suspend fun completeSession(sessionId: String, endedAtEpochMillis: Long, updatedAtEpochMillis: Long): SleepSessionRecord? {
        completed.firstOrNull { it.id == sessionId }?.let { return it }
        val source = active?.takeIf { it.id == sessionId } ?: return null
        return source.copy(status = SleepSessionStatus.COMPLETED, endedAtEpochMillis = endedAtEpochMillis, updatedAtEpochMillis = updatedAtEpochMillis).also {
            active = null
            completed += it
        }
    }
    override suspend fun getCompletedSessions() = completed.toList()
}
private fun record(id: String, status: SleepSessionStatus, endedAt: Long? = null) = SleepSessionRecord(id, 1_000, endedAt, status, createdAtEpochMillis = 1_000, updatedAtEpochMillis = 1_000)
private fun runSuspend(block: suspend () -> Unit) { block.startCoroutine(object : kotlin.coroutines.Continuation<Unit> {
    override val context = kotlin.coroutines.EmptyCoroutineContext
    override fun resumeWith(result: Result<Unit>) = result.getOrThrow()
}) }
