package app.soine.sound

enum class MicrophonePermissionState {
    NOT_REQUESTED,
    GRANTED,
    DENIED,
    PERMANENTLY_DENIED,
    UNAVAILABLE,
}

fun interface MicrophonePermissionObserver {
    fun onPermissionStateChanged(state: MicrophonePermissionState)
}

interface MicrophonePermissionController {
    val state: MicrophonePermissionState

    fun observe(observer: MicrophonePermissionObserver): AutoCloseable

    /**
     * Must only be called after the user explicitly enables the optional
     * overnight sound-analysis feature and accepts the pre-permission copy.
     */
    fun requestPermission()

    fun openAppSettings()

    /**
     * Re-reads OS state. Platform hosts should call this after returning to
     * foreground so permission revocation is reflected in shared UI.
     */
    fun refresh()
}

object UnavailableMicrophonePermissionController : MicrophonePermissionController {
    override val state: MicrophonePermissionState =
        MicrophonePermissionState.UNAVAILABLE

    override fun observe(observer: MicrophonePermissionObserver): AutoCloseable {
        observer.onPermissionStateChanged(state)
        return AutoCloseable {}
    }

    override fun requestPermission() = Unit
    override fun openAppSettings() = Unit
    override fun refresh() = Unit
}

enum class MicrophoneEnableAction {
    ENABLE,
    SHOW_PRE_PERMISSION,
    OPEN_SETTINGS,
    UNAVAILABLE,
}

fun microphoneEnableAction(
    permissionState: MicrophonePermissionState,
): MicrophoneEnableAction = when (permissionState) {
    MicrophonePermissionState.GRANTED ->
        MicrophoneEnableAction.ENABLE

    MicrophonePermissionState.NOT_REQUESTED,
    MicrophonePermissionState.DENIED ->
        MicrophoneEnableAction.SHOW_PRE_PERMISSION

    MicrophonePermissionState.PERMANENTLY_DENIED ->
        MicrophoneEnableAction.OPEN_SETTINGS

    MicrophonePermissionState.UNAVAILABLE ->
        MicrophoneEnableAction.UNAVAILABLE
}

object MicrophonePermissionCopy {
    const val PRE_PERMISSION =
        "寝言や大きな音など、夜の音イベントを端末内で見つけるためにマイクを使います。音声そのものは標準では保存しません。許可しなくても睡眠記録は使えます。"

    const val DENIED =
        "マイクの許可がないため、夜間の音解析はオフです。睡眠記録やSoineの中心機能はそのまま使えます。"

    const val SETTINGS_REQUIRED =
        "マイクの許可は端末の設定から変更できます。許可しなくてもSoineはそのまま使えます。"
}
