# Android Health Connect sleep adapter

Soine reads Health Connect sleep data only as optional enrichment. The manual
sleep-session lifecycle remains authoritative and does not depend on Health
Connect availability or permission.

## SDK and permission

- SDK: `androidx.health.connect:connect-client:1.1.0`
- declared permission: `android.permission.health.READ_SLEEP`
- no Health Connect write permission is requested

The adapter checks `HealthConnectClient.getSdkStatus()` before use. Unsupported
devices or missing/outdated providers are represented as `UNAVAILABLE`.

## Permission flow

`AndroidHealthPermissionRequestCoordinator` bridges the Health Connect
Activity Result contract into the suspending `SleepDataSource` API.

The adapter persists only whether read permission has been requested before so
it can distinguish first-use `NOT_REQUESTED` from later `DENIED`.

Permission is checked again on every read, so revocation is handled without
affecting the manual Soine session.

## Reading and normalization

`SleepSessionRecord` values are read for the requested time window. Pagination
continues until the page token is null or empty.

Stage mapping:

- sleeping / light / deep / REM -> `ASLEEP`
- awake / out of bed -> `AWAKE`
- awake in bed -> `IN_BED`
- unknown -> `UNKNOWN`

If a sleep session has no stages, the whole session becomes one `ASLEEP`
signal. Health Connect data origin package name is retained as source
attribution.

## Failure behavior

- not requested -> `PermissionRequired`
- denied or revoked -> `PermissionDenied`
- SDK/provider/service unavailable -> `Unavailable`
- no records -> `Available(emptyList())`

No Health Connect failure starts, ends, or invalidates a Soine sleep session.
