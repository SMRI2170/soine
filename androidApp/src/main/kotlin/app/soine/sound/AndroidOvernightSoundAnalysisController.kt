package app.soine.sound

import android.content.Context
import android.content.Intent

class AndroidOvernightSoundAnalysisController(
    context: Context,
    private val requestNotificationDisclosurePermission: () -> Unit = {},
) : OvernightSoundAnalysisController {
    private val appContext = context.applicationContext

    override fun start(sessionId: String) {
        require(sessionId.isNotBlank()) { "Sound analysis session id must not be blank." }

        // Notification permission is disclosure-only on Android 13+: denial must
        // not block the foreground service or the core sleep session.
        requestNotificationDisclosurePermission()

        if (!AndroidOvernightSoundRuntime.begin(sessionId)) return

        try {
            appContext.startForegroundService(
                Intent(appContext, OvernightSoundAnalysisService::class.java)
                    .setAction(OvernightSoundAnalysisService.ACTION_START)
                    .putExtra(OvernightSoundAnalysisService.EXTRA_SESSION_ID, sessionId),
            )
        } catch (cause: Throwable) {
            AndroidOvernightSoundRuntime.cancel(sessionId)
            throw cause
        }
    }

    override fun stop(sessionId: String): List<SoundEvent>? {
        val events = AndroidOvernightSoundRuntime.finish(sessionId)
        appContext.stopService(Intent(appContext, OvernightSoundAnalysisService::class.java))
        return events
    }
}

internal object AndroidOvernightSoundRuntime {
    private var activeSessionId: String? = null
    private val events = mutableListOf<SoundEvent>()
    private val lastEventAtByType = mutableMapOf<SoundEventType, Long>()

    @Synchronized
    fun begin(sessionId: String): Boolean {
        if (activeSessionId == sessionId) return false
        activeSessionId = sessionId
        events.clear()
        lastEventAtByType.clear()
        return true
    }

    @Synchronized
    fun add(sessionId: String, event: SoundEvent) {
        if (activeSessionId != sessionId || events.size >= MAX_EVENTS_PER_SESSION) return

        val lastAt = lastEventAtByType[event.type]
        if (lastAt != null && event.occurredAtEpochMillis - lastAt < EVENT_COOLDOWN_MILLIS) {
            return
        }

        events += event
        lastEventAtByType[event.type] = event.occurredAtEpochMillis
    }

    @Synchronized
    fun finish(sessionId: String): List<SoundEvent>? {
        if (activeSessionId != sessionId) return null
        val result = events.toList()
        activeSessionId = null
        events.clear()
        lastEventAtByType.clear()
        return result
    }

    @Synchronized
    fun cancel(sessionId: String) {
        if (activeSessionId != sessionId) return
        activeSessionId = null
        events.clear()
        lastEventAtByType.clear()
    }

    private const val EVENT_COOLDOWN_MILLIS = 30_000L
    private const val MAX_EVENTS_PER_SESSION = 256
}
