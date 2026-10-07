package app.soine.health

import androidx.activity.result.ActivityResultLauncher
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class AndroidHealthPermissionRequestCoordinator {
    private var pending: Continuation<Set<String>>? = null

    suspend fun request(
        launcher: ActivityResultLauncher<Set<String>>,
        permissions: Set<String>,
    ): Set<String> = suspendCoroutine { continuation ->
        check(pending == null) {
            "A Health Connect permission request is already in progress."
        }
        pending = continuation
        launcher.launch(permissions)
    }

    fun complete(grantedPermissions: Set<String>) {
        val continuation = pending ?: return
        pending = null
        continuation.resume(grantedPermissions)
    }

    fun cancel() {
        val continuation = pending ?: return
        pending = null
        continuation.resume(emptySet())
    }
}
