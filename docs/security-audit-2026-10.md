# Security & Privacy Audit — 2026-10

This document captures the manual least-privilege / privacy audit performed as
the first slice of [#186][issue-186] (V1 security & privacy hardening).

It is intentionally a snapshot, not a contract. Re-run the audit on every
release candidate.

## Scope

- repository and history
- Android manifest, build types and ProGuard rules
- iOS Info.plist, capabilities and background modes
- local persistence codecs and migration framework
- analytics payload surface
- microphone and Health Connect data flow

## Findings — secrets

| Check | Result |
| --- | --- |
| API keys / tokens / passwords in repo or history | ✅ none |
| `.env*` ignored | ✅ `.gitignore` covers `.env` and `.env.*` |
| `*.keystore`, `*.jks`, `*.p12` in repo | ✅ none |
| Hard-coded production secrets in code | ✅ none |
| Default analytics tracker | ✅ `NoOpAnalyticsTracker` (no payload) |

No changes required for this section.

## Findings — Android permissions

Declared in [`androidApp/src/main/AndroidManifest.xml`](../androidApp/src/main/AndroidManifest.xml):

| Permission | Used by | Verdict |
| --- | --- | --- |
| `FOREGROUND_SERVICE` | both services | ✅ required for Android 9+ |
| `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | `AmbientAudioService` | ✅ required by foregroundServiceType |
| `FOREGROUND_SERVICE_MICROPHONE` | `OvernightSoundAnalysisService` | ✅ required by foregroundServiceType |
| `RECORD_AUDIO` | `OvernightSoundAnalysisService` (opt-in) | ✅ least-privilege, gated by [#157][pr-157] |
| `POST_NOTIFICATIONS` | both services | ✅ runtime-requested only on Tiramisu+ |
| `health.READ_SLEEP` | Health Connect adapter (opt-in) | ✅ least-privilege, gated by [#159][pr-159] |

Components:

- `MainActivity` is `exported="true"` only because of the `LAUNCHER` filter.
  No deep-link / external intent input.
- `AmbientAudioService` and `OvernightSoundAnalysisService` are
  `exported="false"`. Intent extras (`session_id`, `resource_id`,
  `sound_id`, …) are read with defaults and `String::isNotBlank` checks
  — no external surface area for malformed data.

No changes required for this section.

## Findings — iOS capabilities

Declared in [`iosApp/iosApp/Info.plist`](../iosApp/iosApp/Info.plist):

| Key | Verdict |
| --- | --- |
| `NSHealthShareUsageDescription` | ✅ present, scoped to sleep data |
| `NSMicrophoneUsageDescription` | ✅ present |
| `UIBackgroundModes` | ✅ `audio` only — required for overnight ambient playback |
| URL schemes / deep links | ✅ none declared |

No changes required for this section.

## Findings — local data

- All persistence uses `SharedPreferences` with `MODE_PRIVATE` and dedicated
  names per subsystem.
- Snapshot codecs carry an explicit `schemaVersion` (`SNAPSHOT_VERSION = "2"`
  for sleep sessions). Migration is handled by `ForwardSchemaMigrator` with
  per-version migration objects (see
  [`StoredSleepSessionRepository`][sleep-repo]).
- `decode` validates every line and field with `require(...)` and wraps
  malformed payloads in `SleepSessionStorageCorruptedException` without
  silently clearing the original snapshot. Covered by
  `corruptedSnapshotIsNotSilentlyOverwritten` and `corruptSnapshotFailsWithoutClearingOriginalValue`.
- Microphone capture keeps samples only in a reused `ShortArray` inside the
  capture thread). Derived events are the only persisted microphone output.
- Local data deletion routes every store through `LocalDataDeletionService`
  and is covered by `LocalDataDeletionServiceTest`.

No changes required for this section.

## Findings — network / telemetry

- No HTTP client code is shipped (`OkHttp`, `HttpURLConnection`, `Ktor`
  are not declared as libraries).
- `android:usesCleartextTraffic` is not set, so the platform default
  (false on API 28+) applies.
- [`AnalyticsTracker`][analytics-tracker] is a `fun interface` taking a
  fixed `AnalyticsEvent` enum. No string payload, no free-form
  properties — sensitive data cannot be attached accidentally.

No changes required for this section.

## Findings — build hardening (gap closed in this PR)

Before this PR, `androidApp/build.gradle.kts` and
`composeApp/build.gradle.kts` declared neither `buildTypes` nor any
ProGuard / R8 configuration. Release artifacts therefore shipped:

- without R8 shrinking or obfuscation,
- with debug symbols preserved (larger download, easier reverse-engineering),
- without an explicit `isDebuggable = false`.

This PR introduces an explicit `release` build type with R8
(`isMinifyEnabled = true`) and a minimal `proguard-rules.pro` so the
codec's enum `valueOf(...)` calls and Health Connect client remain
functional after shrinking.

## Findings — input handling recap

- Service `onStartCommand` validates `intent.action` and rejects
  unintended extras. Both services are `exported="false"` so this is
  defense-in-depth.
- SharedPreferences are read with `commit()` and write failures are
  surfaced via `check(...)`. Read failures are wrapped in typed exceptions.

These items are deliberately left as-is; they are already covered by
common tests in `composeApp/src/commonTest`.

## Recommendations (out of scope for this PR)

- [ ] Wire a real analytics provider behind `AnalyticsTracker` and
      re-audit the provider SDK payload surface — see [#180][issue-180].
- [ ] Add an `assembleRelease` step to CI that consumes the new release
      build type and runs R8.
- [ ] Move the audit checklist into a CI job (lint for `Log.*`, secret
      scanning, manifest diff vs. last release) — see [#187][issue-187].
- [ ] Add an instrumented test that verifies Health Connect and
      microphone flows fail closed when their permissions are revoked
      mid-session — see [#185][issue-185].

## Re-audit trigger

Re-run this audit when any of the following change:

- new `uses-permission` or `Info.plist` key,
- new third-party SDK that performs network or disk I/O,
- any new persistence store or codec,
- any new analytics event or payload shape.

[issue-186]: https://github.com/SMRI2170/soine/issues/186
[issue-180]: https://github.com/SMRI2170/soine/issues/180
[issue-185]: https://github.com/SMRI2170/soine/issues/185
[issue-187]: https://github.com/SMRI2170/soine/issues/187
[pr-157]: https://github.com/SMRI2170/soine/pull/157
[pr-159]: https://github.com/SMRI2170/soine/pull/159
[sleep-repo]: ../composeApp/src/commonMain/kotlin/app/soine/sleep/StoredSleepSessionRepository.kt
[analytics-tracker]: ../composeApp/src/commonMain/kotlin/app/soine/analytics/AnalyticsTracker.kt