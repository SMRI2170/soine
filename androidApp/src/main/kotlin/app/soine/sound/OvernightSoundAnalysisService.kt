package app.soine.sound

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder

/**
 * Android feasibility spike for overnight sound-event detection.
 *
 * Raw PCM exists only in the reusable in-memory [ShortArray] below. No audio
 * files, byte payloads or recording identifiers are written to storage.
 */
class OvernightSoundAnalysisService : Service() {
    @Volatile
    private var running = false

    @Volatile
    private var recorder: AudioRecord? = null

    private var captureThread: Thread? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action != ACTION_START) return START_NOT_STICKY
        val sessionId = intent.getStringExtra(EXTRA_SESSION_ID)
            ?.takeIf(String::isNotBlank)
            ?: run {
                stopSelf(startId)
                return START_NOT_STICKY
            }

        // Foreground promotion can fail after startForegroundService() returned
        // successfully (background restriction / permission race). This analysis
        // is optional, so fail closed without crashing the app process.
        val promoted = runCatching { startMicrophoneForeground() }.isSuccess
        if (!promoted) {
            AndroidOvernightSoundRuntime.cancel(sessionId)
            stopSelf(startId)
            return START_NOT_STICKY
        }

        if (captureThread?.isAlive != true) {
            startCapture(sessionId)
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        running = false
        recorder?.runCatching { stop() }
        captureThread?.interrupt()
        captureThread = null
        recorder = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    private fun startCapture(sessionId: String) {
        running = true
        captureThread = Thread(
            {
                captureLoop(sessionId)
            },
            "soine-sound-analysis",
        ).apply {
            isDaemon = true
            start()
        }
    }

    private fun captureLoop(sessionId: String) {
        val minBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE_HZ,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        if (minBufferSize <= 0) {
            AndroidOvernightSoundRuntime.cancel(sessionId)
            running = false
            stopSelf()
            return
        }

        val record = try {
            AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE_HZ,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                maxOf(minBufferSize, FRAME_SAMPLES * PCM_BYTES_PER_SAMPLE),
            )
        } catch (_: Throwable) {
            AndroidOvernightSoundRuntime.cancel(sessionId)
            running = false
            stopSelf()
            return
        }

        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            AndroidOvernightSoundRuntime.cancel(sessionId)
            running = false
            stopSelf()
            return
        }

        recorder = record
        val frame = ShortArray(FRAME_SAMPLES)
        var filled = 0

        try {
            record.startRecording()

            while (running && !Thread.currentThread().isInterrupted) {
                val read = record.read(
                    frame,
                    filled,
                    frame.size - filled,
                    AudioRecord.READ_BLOCKING,
                )
                if (read <= 0) break
                filled += read

                if (filled == frame.size) {
                    PrototypeSoundFrameClassifier.classify(
                        samples = frame,
                        sampleCount = filled,
                        sampleRateHz = SAMPLE_RATE_HZ,
                        occurredAtEpochMillis = System.currentTimeMillis(),
                    )?.let { event ->
                        AndroidOvernightSoundRuntime.add(sessionId, event)
                    }
                    // The same buffer is reused. Raw samples are never retained.
                    filled = 0
                }
            }
        } catch (_: Throwable) {
            // Microphone analysis is optional and must never affect sleep state.
        } finally {
            runCatching {
                if (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    record.stop()
                }
            }
            record.release()
            recorder = null
            running = false
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun startMicrophoneForeground() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "夜間の音解析",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Soineが睡眠中の音を端末内で解析します"
            setSound(null, null)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification =
        Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("Soine")
            .setContentText("夜間の音を端末内で解析中")
            .setOngoing(true)
            .build()

    companion object {
        const val ACTION_START = "app.soine.sound.START_OVERNIGHT_ANALYSIS"
        const val EXTRA_SESSION_ID = "session_id"

        private const val CHANNEL_ID = "soine_sound_analysis"
        private const val NOTIFICATION_ID = 2002
        private const val SAMPLE_RATE_HZ = 8_000
        private const val FRAME_SAMPLES = SAMPLE_RATE_HZ
        private const val PCM_BYTES_PER_SAMPLE = 2
    }
}
