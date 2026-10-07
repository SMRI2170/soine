package app.soine.health

internal class FakeSleepDataSource(
    var permissionState: HealthPermissionState = HealthPermissionState.NOT_REQUESTED,
    var signals: List<SleepSignal> = emptyList(),
) : SleepDataSource {

    var permissionRequestCount: Int = 0
        private set

    var readRequestCount: Int = 0
        private set

    override suspend fun getPermissionState(): HealthPermissionState =
        permissionState

    override suspend fun requestReadPermission(): HealthPermissionState {
        permissionRequestCount += 1
        return permissionState
    }

    override suspend fun readSleepSignals(
        startEpochMillis: Long,
        endEpochMillis: Long,
    ): SleepSignalReadResult {
        require(endEpochMillis > startEpochMillis) {
            "Sleep signal query end must be after start."
        }

        readRequestCount += 1

        return when (permissionState) {
            HealthPermissionState.NOT_REQUESTED ->
                SleepSignalReadResult.PermissionRequired

            HealthPermissionState.DENIED ->
                SleepSignalReadResult.PermissionDenied

            HealthPermissionState.UNAVAILABLE ->
                SleepSignalReadResult.Unavailable

            HealthPermissionState.GRANTED,
            HealthPermissionState.READ_STATUS_UNKNOWN ->
                SleepSignalReadResult.Available(
                    signals = signals.filter { signal ->
                        signal.endEpochMillis > startEpochMillis &&
                            signal.startEpochMillis < endEpochMillis
                    },
                )
        }
    }
}
