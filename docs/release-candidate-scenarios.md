# Release Candidate 7-Night Scenarios

This document is the canonical scenario sheet that the release manager
uses to drive the V1 release-candidate beta for [#182][issue-182] (and
the closing acceptance slice of [#179][issue-179]).

It builds on the per-device verification docs:

- [`docs/overnight-battery-benchmark.md`][battery-bench] — battery
  measurement template
- [`docs/ios-ambient-audio-verification.md`][ios-audio] — iOS audio
  device scenarios
- [`docs/security-audit-2026-10.md`][security-audit] — privacy contract
- [`docs/observability.md`][observability] — diagnostics allowlist

## Why this exists

A single beta run on a single network on a single day does not prove
the core loop. Soine ships an overnight reliability product; the only
acceptable evidence is a 7-night stretch under realistic conditions
on at least one Android device and one iPhone.

## Fixed test conditions

- Battery start: target 80 - 100%; record the exact value.
- Power: unplugged unless the scenario explicitly tests charging.
- Radios: record Wi-Fi / cellular / Bluetooth state per night.
- Display: brightness unchanged between sessions.
- Environment: approximate room temperature; whether the device was
  warm at start.
- Build: the V1 release candidate commit + bundle identifier.
- Duration: target 7 consecutive nights.
- Repeatability: each scenario recorded once per night; once-during
  scenarios recorded once across the 7-night window.

## Per-night scenarios

Each night, run the core loop and record the result.

| Step | Expected | Severity |
| --- | --- | --- |
| Open the app cold. | Bedtime screen. | TURN_PRIORITY |
| Tap to start sleep. | Sleeping scene with companion visible. | CORE |
| Lock the device. | Renderer suspends / lockscreen visible. | CORE |
| Optional: enable ambient audio. | Audio continues with screen off. | DEGRADED |
| Optional: enable overnight sound analysis. | Microphone foreground service runs. | DEGRADED |
| Sleep at least 4 hours. | No app restart, no crash. | CORE |
| Wake the device. | App re-foregrounds; bedtime screen or sleeping scene. | CORE |
| Tap to wake. | Morning summary opens with night memory / dream entries. | CORE |
| Open night memory entries. | Entries render. | DEGRADED |
| Open Dream Album. | Dream entries render. | DEGRADED |
| Close the app. | Sleep session persisted, audio released, on-device | CORE |

## Once-during-period scenarios

At least one occurrence across the 7-night window.

| Scenario | How to trigger | Expected | Reference |
| --- | --- | --- | --- |
| Android process death | `adb shell am force-stop app.soine` while a session is active | Session continues to morning via `RecoverSleepSessionUseCase`; morning summary shows the recovered session. | [#75][issue-75] |
| iOS app termination / relaunch | Background → swipe up to kill; relaunch; if the original `db` shows Sleeping, recovery continues. | Same as Android. | [#78][issue-78] |
| Audio interruption | Incoming call or Siri during ambient playback. | `AmbientAudioState` cycles through PAUSED / PLAYING; core sleep flow unchanged. | [iOS audio doc][ios-audio] |
| Network off | Airplane mode for one night. | No regression; analytics + Health requests fail gracefully; app remains usable. | [failure matrix][failure-matrix] |
| Renderer fallback | Disable / break GLB asset; observe automatic static fallback after `maxAutomaticRetries`. | Companion appears as `CompanionStaticFallback`; sleep flow continues. | [failure matrix][failure-matrix] |
| Reduce-motion on | Toggle reduce-motion in Settings; lock and unlock. | Animations drop to instant transitions; companion still readable. | accessibility doc |
| App update | Sideload a newer build over the existing one. | `SleepSessionStore` / `DreamDiscoveryRepository` migrations run; no data loss. | `ForwardSchemaMigrator` |

## Release blockers

If any of these occur once across the 7-night window, the release
candidate fails. The list is the literal content of
[`#182`][issue-182]'s Release blocker section.

- session loss
- duplicate completion
- "cannot wake" — the wake action produces no morning summary
- app startup crash
- local data corruption
- permission loop
- sleep-disturbing audio / screen behaviour

## Acceptance criteria

- 7 nights with zero P0 release blockers.
- All P1 issues are tracked as new GitHub issues before the release
  tag; no P1 unblocks the core loop.
- Android and iOS release builds both verified on the corresponding
  device class.

## Recording template

| Field | Value |
| --- | --- |
| Date | |
| Device | |
| OS version | |
| Build SHA | |
| Build type | debug / release |
| Per-night step | 1..11 |
| Result | pass / partial / fail |
| Notes | |

Append one row per step per night. Do not retroactively edit earlier
rows.

## Re-evaluation triggers

Re-evaluate this scenario sheet when any of the following change:

- a new release blocker class is added to [`#182`][issue-182]
- the per-night flow gains or loses a step
- a new optional subsystem is added (e.g. Health, microphone, dream)

[issue-179]: https://github.com/SMRI2170/soine/issues/179
[issue-182]: https://github.com/SMRI2170/soine/issues/182
[issue-75]: https://github.com/SMRI2170/soine/issues/75
[issue-78]: https://github.com/SMRI2170/soine/issues/78
[battery-bench]: ./overnight-battery-benchmark.md
[ios-audio]: ./ios-ambient-audio-verification.md
[security-audit]: ./security-audit-2026-10.md
[observability]: ./observability.md
[failure-matrix]: ./failure-matrix.md