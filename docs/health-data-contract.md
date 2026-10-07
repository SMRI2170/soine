# Health sleep-data contract

Health data is optional enrichment. It never owns or gates the manual Soine
sleep-session lifecycle.

## commonMain boundary

`SleepDataSource` exposes only normalized Kotlin/domain types:

- `SleepSignal` with a start/end interval and normalized type
- `SleepSignalSource` provenance
- optional confidence in the `0.0..1.0` range
- `HealthPermissionState`
- explicit read results for permission-required, denied and unavailable states

No Health Connect or HealthKit SDK type may cross this boundary.

## Signal types

The shared contract intentionally stays coarse:

- `ASLEEP`
- `AWAKE`
- `IN_BED`
- `UNKNOWN`

Platform adapters may receive more detailed stage information, but common
domain logic must not claim unsupported medical sleep staging.

## Permission and failure behavior

Permission is requested only when the user explicitly enables Health
integration. Reading data never implicitly triggers a permission request.

If Health access is not requested, denied, or unavailable, the adapter returns
an explicit state/result and the core manual sleep experience continues
unchanged.

## Provenance

Each signal records whether it came through Health Connect or HealthKit and may
include optional source identifiers/names when the platform exposes them. The
optional metadata is attribution only and is not a stable domain identifier.

## Next adapters

- #51 maps Android Health Connect records into this contract.
- #52 maps iOS HealthKit samples into this contract.
- #53 defines how these signals enrich a manual Soine session.
