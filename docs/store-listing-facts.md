# V1 Store Listing — Technical Facts

This document is the technical source-of-truth the store listing copy
team uses to fill in Play Console and App Store Connect for [#181][issue-181].
It is not the store listing itself — the copy team owns the marketing
language.

## Identity

| Field | Android | iOS |
| --- | --- | --- |
| Bundle identifier | `app.soine` | `app.soine` |
| Application ID | `app.soine` | n/a |
| Namespace | `app.soine` | n/a |
| Display name | `Soine` | `Soine` |
| Short name (≤ 12 chars) | `Soine` | `Soine` |
| Initial version | `0.1.0` (versionCode `1`, CFBundleVersion `1`) | same |

The version is held at `0.1.0` until the V1 release tag is cut.
Bump version before the first store submission per the rule in
[`docs/release-candidate-scenarios.md`][rc-scenarios].

## Permissions and capabilities (data-safety / privacy nutrition labels)

### Android

Declared in
[`androidApp/src/main/AndroidManifest.xml`](../androidApp/src/main/AndroidManifest.xml):

| Permission | Required at install | Triggered at runtime | Notes |
| --- | --- | --- | --- |
| `FOREGROUND_SERVICE` | yes | always when service starts | Android 9+ |
| `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | yes | when ambient audio is on | required by `foregroundServiceType` |
| `FOREGROUND_SERVICE_MICROPHONE` | yes | when overnight sound analysis is on | required by `foregroundServiceType` |
| `RECORD_AUDIO` | yes | when user enables optional mic analysis | runtime-prompted via `AndroidMicrophonePermissionController` |
| `POST_NOTIFICATIONS` | yes (Android 13+) | when foreground service starts | disclosure-only; denial does not block the service |
| `health.READ_SLEEP` | yes | when user enables optional Health integration | runtime-prompted via `HealthPermissionRequestCoordinator` |

Components:

- `MainActivity` — `exported=true`, LAUNCHER only. No deep links / external intent input.
- `AmbientAudioService` — `exported=false`, foreground service, type `mediaPlayback`.
- `OvernightSoundAnalysisService` — `exported=false`, foreground service, type `microphone`.

### iOS

Declared in
[`iosApp/iosApp/Info.plist`](../iosApp/iosApp/Info.plist):

| Key | Triggered at runtime | Notes |
| --- | --- | --- |
| `NSHealthShareUsageDescription` | when user enables Health integration | "Apple Healthの睡眠データを、Soineの睡眠記録を補助するために読み取ります。" |
| `NSMicrophoneUsageDescription` | when user enables optional mic analysis | "寝言や大きな音など、夜の音イベントを端末内で見つけるために使います。" |
| `UIBackgroundModes: audio` | when ambient audio is on | required for overnight playback |
| URL schemes | none | no deep link / `LSApplicationQueriesSchemes` |

## Data collected, used, and shared

### Local-only (never leaves the device)

- `SleepSessionRecord` / `SleepSummaryRecord` — local `SharedPreferences` / `NSUserDefaults`.
- `CompanionProgress` — same.
- `DreamDiscovery` — same.
- Derived `SoundEvent` (e.g. night cough, sleep talking) — same.
- Ambient audio preference, sound analysis preference, accessibility preference.

### Optional Health read

- Sleep stage samples from Health Connect / HealthKit.
- **Only** when the user has granted the optional permission.
- Stored locally; never sent to any server.
- Removable via the in-app privacy / data screen
  ([`LocalDataDeletionService`][local-deletion]).

### Network and on-device telemetry (V1)

The V1 default for both product analytics and crash / non-fatal
diagnostics is `NoOp` (see [`docs/analytics-vendor-selection.md`][analytics-vendor]
and [`docs/observability.md`][observability]).

When a vendor is wired in a future release:

- analytics: fixed-vocabulary event names, no payload, opt-in
  ([`AnalyticsPreferences.enabled`][analytics-prefs], default `false`)
- diagnostics: allowlisted or opt-out, vendor SDK may be initialised
  on opt-in
- the privacy policy must be updated to match the vendor contract

For the V1 submission, the data-safety / privacy-nutrition form
should declare: **no data collected, no data linked to identity, no
data used for tracking**. Once a vendor is wired, the forms are
re-filed before the next release.

## Categories and ratings

| Store | Category | Age rating |
| --- | --- | --- |
| Google Play | Health & Fitness | Everyone |
| App Store | Health & Fitness | 4+ |

## Privacy policy text (draft)

> Soine is a bedtime companion. The data you generate — your sleep
> sessions, the companion's progression, and any optional overnight
> sound events — is stored locally on your device and is not sent to a
> server. Soine does not retain raw audio. Health integration and
> overnight sound analysis are optional features that you enable
> separately. The microphone is only used while a sleep session is
> active. You can delete all locally stored data from the in-app
> Settings → Privacy screen.

This is the privacy policy paragraph referenced by the store
listing team. It must match the actual implementation — see
[`docs/security-audit-2026-10.md`][security-audit] for the audit
snapshot that backs every sentence.

## Re-evaluation triggers

Re-evaluate the store listing facts when any of the following change:

- the bundle identifier or version scheme changes,
- a new permission or capability is added,
- a new optional subsystem lands (e.g. Health, microphone, dream
  visual),
- a vendor analytics or diagnostics SDK is wired,
- the privacy policy / store data-safety declaration changes.

[issue-181]: https://github.com/SMRI2170/soine/issues/181
[rc-scenarios]: ./release-candidate-scenarios.md
[local-deletion]: ../composeApp/src/commonMain/kotlin/app/soine/privacy/LocalDataDeletion.kt
[analytics-vendor]: ./analytics-vendor-selection.md
[observability]: ./observability.md
[analytics-prefs]: ../composeApp/src/commonMain/kotlin/app/soine/analytics/AnalyticsPreferences.kt
[security-audit]: ./security-audit-2026-10.md