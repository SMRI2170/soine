package app.soine.sound

import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionRecordPermissionDenied
import platform.AVFAudio.AVAudioSessionRecordPermissionGranted
import platform.AVFAudio.AVAudioSessionRecordPermissionUndetermined
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationDidBecomeActiveNotification
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.darwin.NSObjectProtocol
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

@OptIn(ExperimentalForeignApi::class)
class IosMicrophonePermissionController : MicrophonePermissionController, AutoCloseable {
    private val session = AVAudioSession.sharedInstance()
    private val notificationCenter = NSNotificationCenter.defaultCenter
    private val observers = mutableSetOf<MicrophonePermissionObserver>()
    private var foregroundObserver: NSObjectProtocol? = null

    override var state: MicrophonePermissionState = readState()
        private set

    init {
        foregroundObserver = notificationCenter.addObserverForName(
            name = UIApplicationDidBecomeActiveNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue,
        ) {
            refresh()
        }
    }

    override fun observe(observer: MicrophonePermissionObserver): AutoCloseable {
        observers += observer
        observer.onPermissionStateChanged(state)
        return AutoCloseable { observers -= observer }
    }

    override fun requestPermission() {
        if (state != MicrophonePermissionState.NOT_REQUESTED) return

        session.requestRecordPermission {
            dispatch_async(dispatch_get_main_queue()) {
                refresh()
            }
        }
    }

    override fun openAppSettings() {
        val url = NSURL.URLWithString(UIApplicationOpenSettingsURLString) ?: return
        UIApplication.sharedApplication.openURL(url)
    }

    override fun refresh() {
        val next = readState()
        state = next
        observers.toList().forEach { it.onPermissionStateChanged(next) }
    }

    override fun close() {
        foregroundObserver?.let(notificationCenter::removeObserver)
        foregroundObserver = null
        observers.clear()
    }

    private fun readState(): MicrophonePermissionState =
        when (session.recordPermission) {
            AVAudioSessionRecordPermissionUndetermined ->
                MicrophonePermissionState.NOT_REQUESTED

            AVAudioSessionRecordPermissionGranted ->
                MicrophonePermissionState.GRANTED

            AVAudioSessionRecordPermissionDenied ->
                MicrophonePermissionState.PERMANENTLY_DENIED

            else ->
                MicrophonePermissionState.UNAVAILABLE
        }
}
