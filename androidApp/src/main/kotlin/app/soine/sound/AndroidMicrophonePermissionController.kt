package app.soine.sound

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings

class AndroidMicrophonePermissionController(
    private val activity: Activity,
    private val launchPermissionRequest: () -> Unit,
) : MicrophonePermissionController {
    private val preferences = activity.getSharedPreferences(
        "soine_microphone_permission",
        Context.MODE_PRIVATE,
    )
    private val observers = mutableSetOf<MicrophonePermissionObserver>()

    override var state: MicrophonePermissionState = readState()
        private set

    override fun observe(observer: MicrophonePermissionObserver): AutoCloseable {
        observers += observer
        observer.onPermissionStateChanged(state)
        return AutoCloseable { observers -= observer }
    }

    override fun requestPermission() {
        when (state) {
            MicrophonePermissionState.GRANTED,
            MicrophonePermissionState.PERMANENTLY_DENIED,
            MicrophonePermissionState.UNAVAILABLE -> return

            MicrophonePermissionState.NOT_REQUESTED,
            MicrophonePermissionState.DENIED -> {
                preferences.edit().putBoolean(KEY_REQUESTED_BEFORE, true).apply()
                launchPermissionRequest()
            }
        }
    }

    override fun openAppSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", activity.packageName, null),
        )
        activity.startActivity(intent)
    }

    override fun refresh() {
        val next = readState()
        state = next
        observers.toList().forEach { it.onPermissionStateChanged(next) }
    }

    private fun readState(): MicrophonePermissionState {
        if (
            activity.packageManager.hasSystemFeature(PackageManager.FEATURE_MICROPHONE)
                .not()
        ) {
            return MicrophonePermissionState.UNAVAILABLE
        }

        if (
            activity.checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            return MicrophonePermissionState.GRANTED
        }

        val requestedBefore = preferences.getBoolean(KEY_REQUESTED_BEFORE, false)
        if (!requestedBefore) {
            return MicrophonePermissionState.NOT_REQUESTED
        }

        return if (activity.shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)) {
            MicrophonePermissionState.DENIED
        } else {
            MicrophonePermissionState.PERMANENTLY_DENIED
        }
    }

    companion object {
        private const val KEY_REQUESTED_BEFORE = "requested_before"
    }
}
