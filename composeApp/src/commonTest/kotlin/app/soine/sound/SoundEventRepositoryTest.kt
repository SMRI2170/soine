package app.soine.sound

import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SoundEventRepositoryTest {
    @Test
    fun replaceSessionIsIdempotentAndKeepsOtherSessions() = runSoundTest {
        val store = FakeSoundEventStore()
        val repository = StoredSoundEventRepository(store)

        repository.replaceSessionEvents("a", listOf(event(1_000), event(2_000)))
        repository.replaceSessionEvents("b", listOf(event(3_000)))
        repository.replaceSessionEvents("a", listOf(event(4_000)))

        val all = repository.getAll()
        assertEquals(2, all.size)
        assertEquals(listOf("a", "b"), all.map { it.sessionId }.sorted())
        assertEquals(4_000, all.first { it.sessionId == "a" }.event.occurredAtEpochMillis)
    }

    @Test
    fun appendSessionKeepsPreviouslyPersistedSegments() = runSoundTest {
        val repository = StoredSoundEventRepository(FakeSoundEventStore())

        repository.appendSessionEvents("night", listOf(event(1_000), event(2_000)))
        repository.appendSessionEvents("night", listOf(event(3_000)))

        val nightEvents = repository.getAll()
            .filter { it.sessionId == "night" }
            .map { it.event.occurredAtEpochMillis }

        assertEquals(listOf(1_000L, 2_000L, 3_000L), nightEvents)
    }

    @Test
    fun deleteSessionDeletesOnlyThatSession() = runSoundTest {
        val repository = StoredSoundEventRepository(FakeSoundEventStore())
        repository.replaceSessionEvents("a", listOf(event(1_000), event(2_000)))
        repository.replaceSessionEvents("b", listOf(event(3_000)))

        assertEquals(2, repository.deleteSession("a"))
        assertEquals(listOf("b"), repository.getAll().map { it.sessionId })
        assertEquals(0, repository.deleteSession("missing"))
    }

    @Test
    fun deleteAllReturnsDeletedCountAndClearsStore() = runSoundTest {
        val store = FakeSoundEventStore()
        val repository = StoredSoundEventRepository(store)
        repository.replaceSessionEvents("a", listOf(event(1_000), event(2_000)))

        assertEquals(2, repository.deleteAll())
        assertTrue(repository.getAll().isEmpty())
        assertEquals(null, store.value)
    }

    @Test
    fun deleteAllClearsCorruptSnapshot() = runSoundTest {
        val store = FakeSoundEventStore("broken")
        val repository = StoredSoundEventRepository(store)

        assertEquals(null, repository.deleteAll())
        assertEquals(null, store.value)
        assertTrue(repository.getAll().isEmpty())
    }

    @Test
    fun codecRoundTripsWithoutRawAudioFields() {
        val original = listOf(
            StoredSoundEvent(
                sessionId = "night-1",
                event = SoundEvent(
                    type = SoundEventType.SNORE_LIKE,
                    occurredAtEpochMillis = 5_000,
                    confidence = 0.72,
                    source = SoundEventSource.ON_DEVICE_MICROPHONE,
                    modelVersion = "detector-v1",
                ),
            ),
        )

        assertEquals(original, SoundEventSnapshotCodec.decode(SoundEventSnapshotCodec.encode(original)))
    }

    @Test
    fun corruptSnapshotIsNotSilentlyOverwritten() = runSoundTest {
        val store = FakeSoundEventStore("broken")
        val repository = StoredSoundEventRepository(store)

        assertFailsWith<SoundEventStorageCorruptedException> {
            runSoundTest { repository.getAll() }
        }
        assertEquals("broken", store.value)
    }

    @Test
    fun sessionSummariesAreNewestFirst() {
        val summaries = listOf(
            StoredSoundEvent("older", event(1_000)),
            StoredSoundEvent("newer", event(3_000)),
            StoredSoundEvent("newer", event(2_000)),
        ).sessionSummaries()

        assertEquals(listOf("newer", "older"), summaries.map { it.sessionId })
        assertEquals(2, summaries.first().eventCount)
        assertEquals(3_000, summaries.first().latestOccurredAtEpochMillis)
    }

    private fun event(at: Long) = SoundEvent(
        type = SoundEventType.LOUD_SOUND,
        occurredAtEpochMillis = at,
        confidence = 0.8,
        source = SoundEventSource.ON_DEVICE_MICROPHONE,
        modelVersion = "detector-v1",
    )
}

private class FakeSoundEventStore(
    var value: String? = null,
) : SoundEventStore {
    override fun read(): String? = value
    override fun write(value: String) { this.value = value }
    override fun clear() { value = null }
}

private fun <T> runSoundTest(block: suspend () -> T): T {
    var outcome: Result<T>? = null
    block.startCoroutine(
        object : Continuation<T> {
            override val context = EmptyCoroutineContext
            override fun resumeWith(result: Result<T>) { outcome = result }
        },
    )
    return checkNotNull(outcome).getOrThrow()
}
