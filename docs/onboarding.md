# Onboarding

This document is the canonical description of the first-run
onboarding flow. It is the contract slice of
[#194][issue-194] (初回起動を30秒以内で理解できる
permission-free onboarding へ改善する).

Re-evaluate when:

- a new first-run screen is added to the flow,
- the permission sequencing changes (Health, microphone, notifications),
- a new platform adapter is wired to `FirstRunRepository`,
- the analytics funnel for the flow changes.

## Why this exists

A first launch that demands a Health permission, a microphone
permission, an account creation, or a five-screen tour loses a
significant share of users. The V1 budget is "first sleep within
30 seconds". The onboarding flow is the path that lands the user
on the Bedtime screen as quickly as possible without hiding what
Soine is.

## Principles

The flow is built around five principles, in order of priority:

1. No Health, microphone, or notification permission at first
   launch. The user enables optional features later in Settings.
2. No account creation. The first-run flow does not ask for an
   email, a phone number, or a third-party login.
3. The user can skip the flow from any step. A skipped user
   reaches Bedtime within a few seconds of first launch.
4. The screens describe the experience, not the feature list. Each
   page answers "what is this app?" rather than "what does this
   app do?".
5. The shortest path to the first sleep session is the priority.
   Anything that adds friction to that path is removed.

## Flow

The flow is exactly three steps:

| Step | Headline | Body |
| --- | --- | --- |
| 0 | 夜の相棒、Soine | Soine is a companion that stays from bedtime through morning. No setup is required. |
| 1 | 「一緒に眠る」を体験 | Pressing "一緒に寝る" puts the companion to sleep. The next morning, "起きる" opens the night summary. |
| 2 | 端末の中で完結。 | The data lives on the device. Health / microphone are optional and can be enabled later in Settings. |

The "はじめる" final CTA persists the `COMPLETED` end state and
navigates to Bedtime. The "スキップ" top-right CTA persists the
`SKIPPED` end state and navigates to Bedtime. Both end states
suppress the flow on the next launch.

## Domain model

`app.soine.onboarding.FirstRunState` is the persisted gate. It
has three end states:

- `NOT_STARTED` — fresh install, the flow has not been observed.
- `COMPLETED` — the user stepped through the flow and pressed
  "はじめる".
- `SKIPPED` — the user pressed "スキップ" or backed out without
  finishing.

`FirstRunRepository` is the only surface that mutates the
persisted value. It has three mutation methods:

- `markCompleted()` — flip to `COMPLETED`.
- `markSkipped()` — flip to `SKIPPED`.
- `resetForReplay()` — flip back to `NOT_STARTED` so the next
  launch shows the flow again.

The repository does not persist the current step. The
`OnboardingController` always starts at step 0 so a half-finished
flow never resumes; the user must restart the flow from the
beginning, or skip it.

## State machine

`OnboardingController` is the only place that owns the current
step. The transitions are:

```
[NOT_STARTED, step 0] -- advance --> [step 1]
[step 1]             -- advance --> [step 2]
[step 2]             -- complete --> [COMPLETED, onCompleted()]
[step 0..2]          -- skip    --> [SKIPPED, onSkipped()]
[step 0..2]          -- retreat --> [previous step]
[any state]          -- replay  --> [NOT_STARTED, step 0]
```

The controller's `lastResult` is observed by the UI to navigate
to Bedtime. The `Result` enum has three values: `PENDING`,
`COMPLETED`, `SKIPPED`.

## Permission sequencing

The V1 onboarding flow never requests permissions. The
permission request happens later, lazily, when the user enables
the corresponding feature in Settings:

- **Microphone** — requested the first time the user toggles
  "音声イベントを記録" in Settings. If the user denies, the
  toggle reverts to off and the request can be retried.
- **Health Connect / HealthKit** — requested the first time the
  user opens the Health section in Settings, and only for the
  read permissions Soine uses. The Health data source never
  blocks the sleep loop.
- **Notifications** — requested the first time the
  overnight-sound-analysis foreground service starts on Android
  13+. If the user denies, the service still runs; the
  notification simply does not appear.

The permission state is owned by the platform adapter
(`AndroidMicrophonePermissionController`,
`IosMicrophonePermissionController`) and never lives in the
onboarding state. This keeps the onboarding flow permission-free
even on the iOS path where HealthKit has its own
authorization lifecycle.

## Analytics

The flow emits five `AnalyticsEvent` values, all in the
`onboarding_*` namespace. They are the funnel the product
analytics layer observes:

| Event | When it fires |
| --- | --- |
| `ONBOARDING_STARTED` | The first time the onboarding screen is composed. |
| `ONBOARDING_STEP_VIEWED` | Every time the user advances to a new step. |
| `ONBOARDING_COMPLETED` | The user pressed "はじめる" on the last step. |
| `ONBOARDING_SKIPPED` | The user pressed "スキップ" at any step. |
| `ONBOARDING_REPLAYED` | The user pressed "オンボーディングをもう一度見る" in Settings. |

The tracker is best-effort: a failure in the analytics layer
must not block the onboarding transition. The
`OnboardingControllerTest` pins the funnel ordering so a
refactor that emits two `ONBOARDING_COMPLETED` events for a
single completion fails the smoke gate.

## Privacy

The onboarding state is a single `enum` value plus a schema
version. It contains no user content, no health data, no
microphone samples, and no identifiers. A future privacy export
can surface the file without scanning the rest of the
preferences.

The shared preferences file on Android (`soine_first_run`) and
the iOS defaults suite (`app.soine.first_run`) hold at most two
keys: the schema version and the end state. Both are deleted
when the user hits "すべてのローカルデータを削除" in the
PrivacyData screen.

## Replay

The "オンボーディングをもう一度見る" entry in Settings calls
`OnboardingController.replay()` which:

1. Calls `FirstRunRepository.resetForReplay()` to flip the
   persisted state back to `NOT_STARTED`.
2. Resets the local step counter to 0.
3. Emits `ONBOARDING_REPLAYED`.

The UI then navigates to the `ONBOARDING` secondary screen so
the user sees the flow immediately. The next launch will also
see the flow because the persisted state is back at
`NOT_STARTED`.

## Platform adapters

| Platform | Adapter | Storage |
| --- | --- | --- |
| Android | `AndroidFirstRunRepository` | `SharedPreferences` file `soine_first_run` |
| iOS | `IosFirstRunRepository` | `NSUserDefaults` suite `app.soine.first_run` |

The two adapters share the same key names (`version`,
`end_state`) and the same `FirstRunState.EndState` enum
mapping. A future migration that bumps the schema version
should be applied inside `read()` so a downgraded user still
sees a sensible state.

[issue-194]: https://github.com/SMRI2170/soine/issues/194
