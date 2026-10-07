# iOS HealthKit sleep adapter

HealthKit is optional enrichment. Soine's manual sleep session remains
authoritative and continues normally when HealthKit is unavailable, not
requested, denied, revoked, or contains no visible sleep samples.

## Availability and authorization

`IosHealthKitSleepDataSource` first checks
`HKHealthStore.isHealthDataAvailable()`.

Soine requests read access only for the HealthKit sleep-analysis category.
The app declares `NSHealthShareUsageDescription`; it does not request write
access.

Apple intentionally does not reveal whether the user granted or denied read
permission for a HealthKit type. After the authorization flow has been
completed, the shared contract therefore reports
`HealthPermissionState.READ_STATUS_UNKNOWN` instead of claiming `GRANTED`.

A denied or later-revoked read permission is expected to produce no visible
samples rather than a reliable "denied" status. Code must not interpret an
empty HealthKit result as evidence that permission was denied.

## Reading and normalization

Sleep samples are queried for the requested interval as `HKCategorySample`
values.

Mapping:

- awake -> `AWAKE`
- in bed -> `IN_BED`
- core / deep / REM / unspecified asleep -> `ASLEEP`
- unknown future values -> `UNKNOWN`

Each sample preserves its HealthKit source bundle identifier and display name
when available.

HealthKit may return overlapping `IN_BED` and detailed sleep-stage samples.
The adapter preserves both; #53 owns the merge/precedence policy.

## No-data and failures

- authorization has not been requested -> `PermissionRequired`
- HealthKit unavailable or query error -> `Unavailable`
- query succeeds with no visible samples -> `Available(emptyList())`

Because read denial is intentionally not observable on iOS, no-data is not
converted into `PermissionDenied`.

## Xcode capability

The repository contains the KMP/iOS source and Info.plist but no checked-in
Xcode project configuration. The host iOS target must enable the **HealthKit**
Signing & Capabilities entitlement before physical-device use.
