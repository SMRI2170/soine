package app.soine.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant

class AndroidHealthConnectSleepDataSource(
    context: Context,
    private val permissionRequester: suspend (Set<String>) -> Set<String>,
) : SleepDataSource {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(
        "soine_health_connect",
        Context.MODE_PRIVATE,
    )
    private val readSleepPermission =
        HealthPermission.getReadPermission(SleepSessionRecord::class)

    val requiredPermissions: Set<String>
        get() = setOf(readSleepPermission)

    override suspend fun getPermissionState(): HealthPermissionState {
        if (!isAvailable()) return HealthPermissionState.UNAVAILABLE

        return runCatching {
            HealthConnectClient.getOrCreate(appContext)
                .permissionController
                .getGrantedPermissions()
        }.fold(
            onSuccess = { granted ->
                when {
                    readSleepPermission in granted -> HealthPermissionState.GRANTED
                    preferences.getBoolean(KEY_REQUESTED_BEFORE, false) ->
                        HealthPermissionState.DENIED
                    else -> HealthPermissionState.NOT_REQUESTED
                }
            },
            onFailure = { HealthPermissionState.UNAVAILABLE },
        )
    }

    override suspend fun requestReadPermission(): HealthPermissionState {
        if (!isAvailable()) return HealthPermissionState.UNAVAILABLE

        preferences.edit().putBoolean(KEY_REQUESTED_BEFORE, true).apply()
        val granted = runCatching {
            permissionRequester(requiredPermissions)
        }.getOrElse {
            return HealthPermissionState.DENIED
        }

        return if (readSleepPermission in granted) {
            HealthPermissionState.GRANTED
        } else {
            HealthPermissionState.DENIED
        }
    }

    override suspend fun readSleepSignals(
        startEpochMillis: Long,
        endEpochMillis: Long,
    ): SleepSignalReadResult {
        require(endEpochMillis > startEpochMillis) {
            "Health Connect query end must be after start."
        }

        return when (getPermissionState()) {
            HealthPermissionState.NOT_REQUESTED ->
                SleepSignalReadResult.PermissionRequired

            HealthPermissionState.DENIED ->
                SleepSignalReadResult.PermissionDenied

            HealthPermissionState.READ_STATUS_UNKNOWN ->
                SleepSignalReadResult.PermissionRequired

            HealthPermissionState.UNAVAILABLE ->
                SleepSignalReadResult.Unavailable

            HealthPermissionState.GRANTED -> readGrantedSignals(
                startEpochMillis = startEpochMillis,
                endEpochMillis = endEpochMillis,
            )
        }
    }

    private suspend fun readGrantedSignals(
        startEpochMillis: Long,
        endEpochMillis: Long,
    ): SleepSignalReadResult {
        val client = runCatching {
            HealthConnectClient.getOrCreate(appContext)
        }.getOrElse {
            return SleepSignalReadResult.Unavailable
        }

        val records = mutableListOf<SleepSessionRecord>()
        var pageToken: String? = null

        try {
            do {
                val response = client.readRecords(
                    ReadRecordsRequest(
                        recordType = SleepSessionRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(
                            Instant.ofEpochMilli(startEpochMillis),
                            Instant.ofEpochMilli(endEpochMillis),
                        ),
                        pageToken = pageToken,
                    ),
                )
                records += response.records
                pageToken = response.pageToken
            } while (!pageToken.isNullOrEmpty())
        } catch (_: SecurityException) {
            return SleepSignalReadResult.PermissionDenied
        } catch (_: Throwable) {
            return SleepSignalReadResult.Unavailable
        }

        return SleepSignalReadResult.Available(
            signals = records
                .flatMap(::normalizeRecord)
                .sortedBy(SleepSignal::startEpochMillis),
        )
    }

    private fun normalizeRecord(record: SleepSessionRecord): List<SleepSignal> =
        normalizePlatformSleepRecord(
            record = PlatformSleepRecord(
                startEpochMillis = record.startTime.toEpochMilli(),
                endEpochMillis = record.endTime.toEpochMilli(),
                stages = record.stages.map { stage ->
                    PlatformSleepStage(
                        startEpochMillis = stage.startTime.toEpochMilli(),
                        endEpochMillis = stage.endTime.toEpochMilli(),
                        type = stage.stage.toSignalType(),
                    )
                },
                sourceId = record.metadata.dataOrigin.packageName,
            ),
            provider = SleepSignalProvider.HEALTH_CONNECT,
        )

    private fun Int.toSignalType(): SleepSignalType = when (this) {
        SleepSessionRecord.STAGE_TYPE_SLEEPING,
        SleepSessionRecord.STAGE_TYPE_LIGHT,
        SleepSessionRecord.STAGE_TYPE_DEEP,
        SleepSessionRecord.STAGE_TYPE_REM -> SleepSignalType.ASLEEP

        SleepSessionRecord.STAGE_TYPE_AWAKE,
        SleepSessionRecord.STAGE_TYPE_OUT_OF_BED -> SleepSignalType.AWAKE

        SleepSessionRecord.STAGE_TYPE_AWAKE_IN_BED -> SleepSignalType.IN_BED
        else -> SleepSignalType.UNKNOWN
    }

    private fun isAvailable(): Boolean =
        HealthConnectClient.getSdkStatus(appContext) ==
            HealthConnectClient.SDK_AVAILABLE

    companion object {
        private const val KEY_REQUESTED_BEFORE = "read_sleep_requested_before"
    }
}
