package app.soine.observability

import kotlin.test.Test
import kotlin.test.assertEquals

class ErrorReporterTest {

    @Test
    fun eventNamesMatchPublishedVocabulary() {
        assertEquals(
            listOf(
                "renderer_fallback_occurred",
                "renderer_asset_decode_failure",
                "audio_interruption",
                "audio_decode_failure",
                "audio_focus_denied",
                "health_permission_denied",
                "health_unavailable",
                "microphone_unavailable",
                "session_recovery_occurred",
                "storage_corruption",
                "local_deletion_failed",
            ),
            ErrorEvent.entries.map { it.eventName },
        )
    }

    @Test
    fun noOpReporterAcceptsEveryEvent() {
        ErrorEvent.entries.forEach(NoOpErrorReporter::recordNonFatal)
    }

    @Test
    fun noOpReporterAcceptsFatalThrowables() {
        NoOpErrorReporter.recordFatal(IllegalStateException("test"))
        NoOpErrorReporter.recordFatal(RuntimeException())
        NoOpErrorReporter.recordFatal(Throwable())
    }

    @Test
    fun fakeReporterRecordsEventsInOrder() {
        val reporter = RecordingErrorReporter()
        reporter.recordNonFatal(ErrorEvent.RENDERER_FALLBACK_OCCURRED)
        reporter.recordNonFatal(ErrorEvent.AUDIO_INTERRUPTION)

        assertEquals(
            listOf(
                ErrorEvent.RENDERER_FALLBACK_OCCURRED,
                ErrorEvent.AUDIO_INTERRUPTION,
            ),
            reporter.events,
        )
    }

    @Test
    fun fakeReporterCapturesFatalThrowables() {
        val reporter = RecordingErrorReporter()
        val fatal = IllegalStateException("boom")

        reporter.recordFatal(fatal)

        assertEquals(listOf<Throwable>(fatal), reporter.fatals)
    }

    @Test
    fun diagnosticFingerprintUsesQualifiedClassNameWithoutMessage() {
        val throwable = IllegalStateException("sensitive sleep timeline payload")

        val fingerprint = throwable.diagnosticFingerprint()

        assertEquals("java.lang.IllegalStateException", fingerprint)
    }
}

private class RecordingErrorReporter : ErrorReporter {
    val events: MutableList<ErrorEvent> = mutableListOf()
    val fatals: MutableList<Throwable> = mutableListOf()

    override fun recordNonFatal(event: ErrorEvent) {
        events += event
    }

    override fun recordFatal(throwable: Throwable) {
        fatals += throwable
    }
}