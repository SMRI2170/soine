# Observability Policy

This document is the privacy contract for crash and non-fatal
diagnostics shipped from Soine to a third-party provider. It is the
first slice of [#188][issue-188] (sensitive-data-free crash and error
diagnostics).

It is intentionally a policy, not a contract. Re-evaluate when the
team picks a new provider SDK or adds a new non-fatal event.

## Why this exists

- Once the install base reaches 1000 users we cannot reproduce
  device-specific failures locally. We need a signal to triage them.
- The data we are allowed to receive is much narrower than the data a
  crash SDK may try to attach by default.
- Sleep content, Health samples and microphone buffers must never
  reach the diagnostics backend.

## Privacy contract

### Always safe to send

| Field | Source | Notes |
| --- | --- | --- |
| App version (`versionName` / `versionCode`) | `BuildConfig` | Identifies the build under triage. |
| OS version | platform API | e.g. `Android 14`, `iOS 17.4`. |
| Coarse device model | platform API | e.g. `Pixel 6a` / `iPhone 13`. Coarse means model family, not serial number. |
| Anonymous error fingerprint | `Throwable::class.qualifiedName` | Groups by stack origin without leaking message text. |
| Non-fatal `eventName` (enum) | common domain | See [vocabulary](#non-fatal-event-vocabulary). |
| Renderer fallback occurrence | `CompanionRendererFallbackPolicy` | Tied to fallback counter only. |
| Session recovery occurrence | `RecoverSleepSessionUseCase` | One event per recovery, no payload. |

### Never send

| Field | Reason |
| --- | --- |
| Raw audio | Privacy contract. Soine does not retain raw audio by default ([`docs/privacy.md`][privacy]). |
| Microphone buffers | Same. |
| Exact sleep times / timeline | Sensitive. |
| Health samples / sleep stage detail | Sensitive. |
| User-written private text | Future-proofing. Soine does not author user text today. |
| Local persisted payload | Sensitive snapshots. |
| Email / name / precise location | Not collected, full stop. |
| Free-form `Throwable.message` text | Often contains user input. Use class name only. |

### Conditional / scrubbed

| Field | Rule |
| --- | --- |
| Crash stack frames | Allowed only after scrubber strips file paths inside user-controlled directories and obfuscates any user-named symbol. Production adapters MUST run the scrubber. |
| Thread name | Acceptable. |
| Locale | Acceptable. |
| Anonymous install ID | Acceptable if randomly generated, never derived from hardware IDs. |

## Architecture

```
commonMain              app.soine.observability.ErrorReporter
                          ├── recordFatal(Throwable)
                          └── recordNonFatal(ErrorEvent)

androidMain / iosMain   Vendor SDK adapters that implement ErrorReporter
                          and forward to the configured provider.
```

- `commonMain` owns the contract and the event vocabulary.
- Platform source sets own the vendor SDK adapter.
- `NoOpErrorReporter` is the default until a provider is wired.
- The vendor adapter is a `runCatching` boundary: an SDK failure
  must not crash the host.

## Non-fatal event vocabulary

Each entry is `ErrorEvent.X` mapped to `eventName`. Adding a new
event means appending to the enum. Removing an event means keeping
the enum entry but stopping emission (so historic reports still
group correctly).

| `eventName` | Triggered by | Privacy shape |
| --- | --- | --- |
| `renderer_fallback_occurred` | `CompanionRendererFallbackPolicy.USE_STATIC_FALLBACK` | counter only |
| `renderer_asset_decode_failure` | platform renderer adapter (future) | code only |
| `audio_interruption` | `AmbientAudioService` audio focus loss | none |
| `audio_decode_failure` | `MediaPlayer.create()` null branch | none |
| `audio_focus_denied` | `requestAudioFocus` non-GRANTED | none |
| `health_permission_denied` | `SleepDataSource.getPermissionState() == DENIED` | none |
| `health_unavailable` | `SleepDataSource.getPermissionState() == UNAVAILABLE` | none |
| `microphone_unavailable` | `MicrophonePermissionState.UNAVAILABLE` | none |
| `session_recovery_occurred` | `RecoverSleepSessionUseCase` returned `Recovered` | none |
| `storage_corruption` | typed `*StorageCorruptedException` thrown | none |
| `local_deletion_failed` | `LocalDataDeletionResult.Failed` | none |

## Acceptance criteria

- production crash reports can be grouped by version,
- no privacy-sensitive context is attached to any report,
- reporter failure does not crash the host process,
- disabling the reporter (default `NoOpErrorReporter`) does not break
  the core sleep flow.

## Provider selection criteria

Any candidate crash / non-fatal SDK must:

- provide a way to attach only the fields we choose (allowlist, not
  blocklist),
- run on the device side and scrub before egress,
- ship with documented data retention and opt-out semantics,
- be re-audited before each release candidate.

Drop any provider that ships on-device data to its own analytics layer
without our scrubbing in front.

## Re-evaluation triggers

Re-evaluate this policy when any of the following change:

- a new vendor SDK is selected,
- a new non-fatal event is added,
- privacy policy / store listing changes,
- a privacy incident is reported upstream in the SDK.

[issue-188]: https://github.com/SMRI2170/soine/issues/188
[privacy]: ./privacy.md