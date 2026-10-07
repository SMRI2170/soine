package app.soine.health

import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class SleepDataSourceTest {

    @Test
    fun signalKeepsSourceAttributionAndOptionalConfidence() {
        val signal = signal(confidence = null)

        assertEquals(SleepSignalProvider.HEALTH_CONNECT, signal.source.provider)
        assertEquals("com.example.health", signal.source.sourceId)
        assertEquals("Example Health", signal.source.sourceName)
        assertEquals(null, signal.confidence)
    }

    @Test
    fun confidenceMustStayWithinUnitInterval() {
        assertFailsWith<IllegalArgumentException> {
            signal(confidence = 1.1)
        }
    }

    @Test
    fun intervalMustHavePositiveDuration() {
        assertFailsWith<IllegalArgumentException> {
            signal(
                startEpochMillis = 2_000,
                endEpochMillis = 2_000,
            )
        }
    }

    @Test
    fun deniedAndUnavailableAreExplicitResults() = runHealthTest {
        val source = FakeSleepDataSource(
            permissionState = HealthPermissionState.DENIED,
        )

        assertIs<SleepSignalReadResult.PermissionDenied>(
            source.readSleepSignals(1_000, 2_000),
        )

        source.permissionState = HealthPermissionState.UNAVAILABLE

        assertIs<SleepSignalReadResult.Unavailable>(
            source.readSleepSignals(1_000, 2_000),
        )
    }

    @Test
    fun notRequestedDoesNotImplicitlyAskForPermission() = runHealthTest {
        val source = FakeSleepDataSource()

        assertIs<SleepSignalReadResult.PermissionRequired>(
            source.readSleepSignals(1_000, 2_000),
        )
        assertEquals(0, source.permissionRequestCount)
    }

    @Test
    fun fakeReturnsOnlySignalsOverlappingRequestedWindow() = runHealthTest {
        val included = signal(startEpochMillis = 1_500, endEpochMillis = 2_500)
        val excluded = signal(startEpochMillis = 3_000, endEpochMillis = 4_000)
        val source = FakeSleepDataSource(
            permissionState = HealthPermissionState.GRANTED,
            signals = listOf(included, excluded),
        )

        val result = source.readSleepSignals(1_000, 3_000)

        val available = assertIs<SleepSignalReadResult.Available>(result)
        assertEquals(listOf(included), available.signals)
        assertEquals(1, source.readRequestCount)
    }

    @Test
    fun permissionRequestIsObservableInFake() = runHealthTest {
        val source = FakeSleepDataSource(
            permissionState = HealthPermissionState.GRANTED,
        )

        assertEquals(
            HealthPermissionState.GRANTED,
            source.requestReadPermission(),
        )
        assertEquals(1, source.permissionRequestCount)
    }

    private fun signal(
        startEpochMillis: Long = 1_000,
        endEpochMillis: Long = 2_000,
        confidence: Double? = 0.8,
    ) = SleepSignal(
        startEpochMillis = startEpochMillis,
        endEpochMillis = endEpochMillis,
        type = SleepSignalType.ASLEEP,
        source = SleepSignalSource(
            provider = SleepSignalProvider.HEALTH_CONNECT,
            sourceId = "com.example.health",
            sourceName = "Example Health",
        ),
        confidence = confidence,
    )
}

private fun <T> runHealthTest(block: suspend () -> T): T {
    var outcome: Result<T>? = null
    block.startCoroutine(
        object : Continuation<T> {
            override val context = EmptyCoroutineContext

            override fun resumeWith(result: Result<T>) {
                outcome = result
            }
        },
    )
    return checkNotNull(outcome).getOrThrow()
}
