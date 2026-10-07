package app.soine.health

import platform.Foundation.NSDate
import platform.Foundation.NSSortDescriptor
import platform.Foundation.timeIntervalSince1970
import platform.HealthKit.HKAuthorizationRequestStatusUnnecessary
import platform.HealthKit.HKCategorySample
import platform.HealthKit.HKCategoryTypeIdentifierSleepAnalysis
import platform.HealthKit.HKCategoryValueSleepAnalysisAsleepCore
import platform.HealthKit.HKCategoryValueSleepAnalysisAsleepDeep
import platform.HealthKit.HKCategoryValueSleepAnalysisAsleepREM
import platform.HealthKit.HKCategoryValueSleepAnalysisAsleepUnspecified
import platform.HealthKit.HKCategoryValueSleepAnalysisAwake
import platform.HealthKit.HKCategoryValueSleepAnalysisInBed
import platform.HealthKit.HKHealthStore
import platform.HealthKit.HKObjectQueryNoLimit
import platform.HealthKit.HKObjectType
import platform.HealthKit.HKQuery
import platform.HealthKit.HKQueryOptionStrictStartDate
import platform.HealthKit.HKSampleQuery
import platform.HealthKit.HKSampleSortIdentifierEndDate
import platform.HealthKit.HKSampleType
import platform.HealthKit.predicateForSamplesWithStartDate
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class IosHealthKitSleepDataSource(
    private val healthStore: HKHealthStore = HKHealthStore(),
) : SleepDataSource {

    override suspend fun getPermissionState(): HealthPermissionState {
        val sleepType = sleepTypeOrNull()
            ?: return HealthPermissionState.UNAVAILABLE

        return suspendCoroutine { continuation ->
            healthStore.getRequestStatusForAuthorizationToShareTypes(
                typesToShare = emptySet<HKSampleType>(),
                readTypes = setOf(sleepType),
            ) { status, error ->
                when {
                    error != null ->
                        continuation.resume(HealthPermissionState.UNAVAILABLE)

                    status == HKAuthorizationRequestStatusUnnecessary ->
                        continuation.resume(HealthPermissionState.READ_STATUS_UNKNOWN)

                    else ->
                        continuation.resume(HealthPermissionState.NOT_REQUESTED)
                }
            }
        }
    }

    override suspend fun requestReadPermission(): HealthPermissionState {
        val sleepType = sleepTypeOrNull()
            ?: return HealthPermissionState.UNAVAILABLE

        return suspendCoroutine { continuation ->
            healthStore.requestAuthorizationToShareTypes(
                typesToShare = emptySet<HKSampleType>(),
                readTypes = setOf(sleepType),
            ) { success, error ->
                when {
                    error != null ->
                        continuation.resume(HealthPermissionState.UNAVAILABLE)

                    success ->
                        continuation.resume(HealthPermissionState.READ_STATUS_UNKNOWN)

                    else ->
                        continuation.resume(HealthPermissionState.NOT_REQUESTED)
                }
            }
        }
    }

    override suspend fun readSleepSignals(
        startEpochMillis: Long,
        endEpochMillis: Long,
    ): SleepSignalReadResult {
        require(endEpochMillis > startEpochMillis) {
            "HealthKit query end must be after start."
        }

        return when (getPermissionState()) {
            HealthPermissionState.NOT_REQUESTED ->
                SleepSignalReadResult.PermissionRequired

            HealthPermissionState.UNAVAILABLE ->
                SleepSignalReadResult.Unavailable

            HealthPermissionState.DENIED ->
                SleepSignalReadResult.PermissionDenied

            HealthPermissionState.GRANTED,
            HealthPermissionState.READ_STATUS_UNKNOWN ->
                readVisibleSleepSamples(
                    startEpochMillis = startEpochMillis,
                    endEpochMillis = endEpochMillis,
                )
        }
    }

    private suspend fun readVisibleSleepSamples(
        startEpochMillis: Long,
        endEpochMillis: Long,
    ): SleepSignalReadResult {
        val sleepType = sleepTypeOrNull()
            ?: return SleepSignalReadResult.Unavailable

        return suspendCoroutine { continuation ->
            val query = HKSampleQuery(
                sampleType = sleepType,
                predicate = HKQuery.predicateForSamplesWithStartDate(
                    startDate = startEpochMillis.toNSDate(),
                    endDate = endEpochMillis.toNSDate(),
                    options = HKQueryOptionStrictStartDate,
                ),
                limit = HKObjectQueryNoLimit,
                sortDescriptors = listOf(
                    NSSortDescriptor(HKSampleSortIdentifierEndDate, ascending = true),
                ),
            ) { _, samples, error ->
                if (error != null) {
                    continuation.resume(SleepSignalReadResult.Unavailable)
                    return@HKSampleQuery
                }

                val signals = samples
                    .orEmpty()
                    .filterIsInstance<HKCategorySample>()
                    .flatMap(::normalizeSample)
                    .sortedBy(SleepSignal::startEpochMillis)

                continuation.resume(
                    SleepSignalReadResult.Available(signals),
                )
            }

            healthStore.executeQuery(query)
        }
    }

    private fun normalizeSample(sample: HKCategorySample): List<SleepSignal> {
        val source = sample.sourceRevision.source
        return normalizePlatformSleepRecord(
            record = PlatformSleepRecord(
                startEpochMillis = sample.startDate.toEpochMillis(),
                endEpochMillis = sample.endDate.toEpochMillis(),
                stages = listOf(
                    PlatformSleepStage(
                        startEpochMillis = sample.startDate.toEpochMillis(),
                        endEpochMillis = sample.endDate.toEpochMillis(),
                        type = sample.value.toSignalType(),
                    ),
                ),
                sourceId = source.bundleIdentifier,
                sourceName = source.name,
            ),
            provider = SleepSignalProvider.HEALTH_KIT,
        )
    }

    private fun Long.toNSDate(): NSDate =
        NSDate(
            timeIntervalSinceReferenceDate =
                toDouble() / 1_000.0 - SECONDS_FROM_1970_TO_REFERENCE_DATE,
        )

    private fun NSDate.toEpochMillis(): Long =
        (timeIntervalSince1970 * 1_000.0).toLong()

    private fun Long.toSignalType(): SleepSignalType = when (this) {
        HKCategoryValueSleepAnalysisAwake ->
            SleepSignalType.AWAKE

        HKCategoryValueSleepAnalysisInBed ->
            SleepSignalType.IN_BED

        HKCategoryValueSleepAnalysisAsleepCore,
        HKCategoryValueSleepAnalysisAsleepDeep,
        HKCategoryValueSleepAnalysisAsleepREM,
        HKCategoryValueSleepAnalysisAsleepUnspecified ->
            SleepSignalType.ASLEEP

        else ->
            SleepSignalType.UNKNOWN
    }

    private fun sleepTypeOrNull() =
        if (HKHealthStore.isHealthDataAvailable()) {
            HKObjectType.categoryTypeForIdentifier(
                HKCategoryTypeIdentifierSleepAnalysis!!,
            )
        } else {
            null
        }

    companion object {
        private const val SECONDS_FROM_1970_TO_REFERENCE_DATE = 978_307_200.0
    }
}
