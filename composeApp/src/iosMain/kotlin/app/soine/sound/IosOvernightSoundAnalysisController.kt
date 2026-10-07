package app.soine.sound

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.get
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioPCMBuffer
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryOptionDefaultToSpeaker
import platform.AVFAudio.AVAudioSessionCategoryPlayAndRecord
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.AVAudioSessionInterruptionNotification
import platform.AVFAudio.AVAudioSessionInterruptionOptionKey
import platform.AVFAudio.AVAudioSessionInterruptionTypeKey
import platform.Foundation.NSNotification
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSNumber
import platform.Foundation.NSOperationQueue
import platform.darwin.NSObjectProtocol
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/**
 * iOS feasibility spike for optional overnight sound-event detection.
 *
 * Audio is consumed from an AVAudioEngine input tap and converted immediately
 * to a derived SoundEvent. No AVAudioFile, raw PCM payload, or recording path is
 * persisted.
 */
@OptIn(ExperimentalForeignApi::class)
class IosOvernightSoundAnalysisController :
    OvernightSoundAnalysisController,
    AutoCloseable {
    private val session = AVAudioSession.sharedInstance()
    private val engine = AVAudioEngine()
    private val notificationCenter = NSNotificationCenter.defaultCenter

    private var activeSessionId: String? = null
    private var tapInstalled = false
    private var interruptionObserver: NSObjectProtocol? = null
    private var resumeAfterInterruption = false

    private val events = mutableListOf<SoundEvent>()
    private val lastEventAtByType = mutableMapOf<SoundEventType, Long>()

    init {
        interruptionObserver = notificationCenter.addObserverForName(
            name = AVAudioSessionInterruptionNotification,
            `object` = session,
            queue = NSOperationQueue.mainQueue,
        ) { notification ->
            notification?.let(::handleInterruption)
        }
    }

    override fun start(sessionId: String) {
        require(sessionId.isNotBlank()) { "Sound analysis session id must not be blank." }

        if (activeSessionId == sessionId && engine.running) return
        if (activeSessionId != null && activeSessionId != sessionId) {
            stopCapture(resetSessionCategory = false)
        }

        activeSessionId = sessionId
        events.clear()
        lastEventAtByType.clear()

        configurePlayAndRecordSession()
        installTap(sessionId)
        engine.prepare()

        if (!engine.startAndReturnError(null)) {
            stopCapture(resetSessionCategory = true)
            error("Unable to start iOS overnight sound analysis.")
        }
    }

    override fun stop(sessionId: String): List<SoundEvent>? {
        if (activeSessionId != sessionId) return null

        val result = events.toList()
        stopCapture(resetSessionCategory = true)
        return result
    }

    private fun configurePlayAndRecordSession() {
        val categorySet = session.setCategory(
            AVAudioSessionCategoryPlayAndRecord,
            withOptions = AVAudioSessionCategoryOptionDefaultToSpeaker,
            error = null,
        )
        if (!categorySet) error("Unable to configure iOS play-and-record audio session.")

        val activated = session.setActive(true, error = null)
        if (!activated) error("Unable to activate iOS audio session.")
    }

    private fun installTap(sessionId: String) {
        if (tapInstalled) return

        val inputNode = engine.inputNode
        val format = inputNode.outputFormatForBus(INPUT_BUS)
        inputNode.installTapOnBus(
            bus = INPUT_BUS,
            bufferSize = TAP_BUFFER_FRAMES,
            format = format,
        ) { buffer, _ ->
            buffer?.let { handleBuffer(sessionId, it) }
        }
        tapInstalled = true
    }

    private fun handleBuffer(sessionId: String, buffer: AVAudioPCMBuffer) {
        val channelData = buffer.floatChannelData ?: return
        val firstChannel = channelData[0] ?: return
        val sampleCount = buffer.frameLength.toInt()
        if (sampleCount <= 0) return

        val samples = ShortArray(sampleCount)
        for (index in 0 until sampleCount) {
            val normalized = firstChannel[index].coerceIn(-1.0f, 1.0f)
            samples[index] = (normalized * Short.MAX_VALUE).toInt().toShort()
        }

        val detected = PrototypeSoundFrameClassifier.classify(
            samples = samples,
            sampleRateHz = buffer.format.sampleRate.toInt(),
            occurredAtEpochMillis = currentEpochMillis(),
        )?.copy(modelVersion = IOS_MODEL_VERSION) ?: return

        dispatch_async(dispatch_get_main_queue()) {
            recordEvent(sessionId, detected)
        }
    }

    private fun recordEvent(sessionId: String, event: SoundEvent) {
        if (activeSessionId != sessionId || events.size >= MAX_EVENTS_PER_SESSION) return

        val lastAt = lastEventAtByType[event.type]
        if (lastAt != null && event.occurredAtEpochMillis - lastAt < EVENT_COOLDOWN_MILLIS) {
            return
        }

        events += event
        lastEventAtByType[event.type] = event.occurredAtEpochMillis
    }

    private fun handleInterruption(notification: NSNotification) {
        val userInfo = notification.userInfo ?: return
        val type = (userInfo[AVAudioSessionInterruptionTypeKey] as? NSNumber)
            ?.unsignedIntegerValue
            ?.toLong()
            ?: return

        when (type) {
            INTERRUPTION_BEGAN -> {
                resumeAfterInterruption = activeSessionId != null && engine.running
                engine.stop()
            }

            INTERRUPTION_ENDED -> {
                val options = (userInfo[AVAudioSessionInterruptionOptionKey] as? NSNumber)
                    ?.unsignedIntegerValue
                    ?.toLong()
                    ?: 0L
                val shouldResume = options and SHOULD_RESUME_OPTION != 0L

                if (resumeAfterInterruption && shouldResume && activeSessionId != null) {
                    resumeAfterInterruption = false
                    runCatching {
                        configurePlayAndRecordSession()
                        engine.prepare()
                        if (!engine.startAndReturnError(null)) {
                            error("Unable to resume iOS overnight sound analysis.")
                        }
                    }
                } else {
                    resumeAfterInterruption = false
                }
            }
        }
    }

    private fun stopCapture(resetSessionCategory: Boolean) {
        resumeAfterInterruption = false
        engine.stop()

        if (tapInstalled) {
            engine.inputNode.removeTapOnBus(INPUT_BUS)
            tapInstalled = false
        }

        activeSessionId = null
        events.clear()
        lastEventAtByType.clear()

        if (resetSessionCategory) {
            session.setCategory(AVAudioSessionCategoryPlayback, error = null)
        }
    }

    override fun close() {
        stopCapture(resetSessionCategory = true)
        interruptionObserver?.let(notificationCenter::removeObserver)
        interruptionObserver = null
    }

    private fun currentEpochMillis(): Long =
        (platform.Foundation.NSDate().timeIntervalSince1970 * 1_000.0).toLong()

    private companion object {
        const val INPUT_BUS = 0uL
        const val TAP_BUFFER_FRAMES = 4_096u
        const val EVENT_COOLDOWN_MILLIS = 30_000L
        const val MAX_EVENTS_PER_SESSION = 256
        const val IOS_MODEL_VERSION = "ios-heuristic-v1"
        const val INTERRUPTION_BEGAN = 1L
        const val INTERRUPTION_ENDED = 0L
        const val SHOULD_RESUME_OPTION = 1L
    }
}
