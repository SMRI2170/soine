# CI Quality Gates Policy

This document is the canonical description of the lint / static
analysis / UI smoke gates enforced on every PR. It is the policy slice
of [#193][issue-193] (CI lint / static analysis / UI smoke gates).

It is intentionally a policy, not a contract. Re-evaluate when a new
gate is added or an existing gate is disabled.

## Why this exists

A build can pass `compile` + `test` while shipping a regression the
community cannot reproduce: a silent permission expansion, a missing
contentDescription, a sleep / wake CTA that is no longer findable,
or a dead fallback path. CI catches the compile-class regressions;
this policy adds the next layer.

## Gate map

| Gate | Triggered on | Tool | Failure consequence |
| --- | --- | --- | --- |
| `commonTest` | PR | JVM unit tests | PR cannot merge |
| Android debug assemble | PR | `:androidApp:assembleDebug` | PR cannot merge |
| iOS simulator compile | PR | `:composeApp:compileKotlinIosSimulatorArm64` | PR cannot merge |
| Android lint | PR | `:androidApp:lintDebug` | PR cannot merge when severity >= error |
| Kotlin compile warnings | PR | `allWarningsAsErrors = true` in `composeApp/build.gradle.kts` (Android + iOS) | PR cannot merge when a new warning lands |
| Lint baseline | PR | `androidApp/lint-baseline.xml` committed and shrunk each cycle | PR can land with the same warnings; new warnings fail |
| Core navigation smoke | PR | `:composeApp:testAndroidHostTest` (`BedtimeFlowControllerTest`) | PR cannot merge when navigation regresses |
| Permission UX smoke | PR | `:composeApp:testAndroidHostTest` (`MicrophonePermissionUxTest`, `SleepDataSourceTest`) | PR cannot merge when opt-in / opt-out wiring breaks |
| Fallback smoke | PR | `:composeApp:testAndroidHostTest` (`CompanionStaticFallbackTest`, `OvernightSoundAnalysisControllerTest`) | PR cannot merge when a fallback path silently disappears |
| Settings route smoke | PR | `:composeApp:testAndroidHostTest` (`AmbientAudioPreferencesStoreContractTest`) | PR cannot merge when the SettingsScreen contract layer is broken |
| Sleep / wake CTA semantics | PR | `:composeApp:testAndroidHostTest` (`SleepStartWakeCtaSemanticsTest`) | PR cannot merge when the bedtime "一緒に寝る" or sleeping "起きる" CTA loses its accessibility label or the wiring to `AccessibilityPolicy` |

Tools deliberately **not** in scope (tracked as deferred):

- ktlint / ktfmt — formatting policy is left to IDE import settings
  until the team picks a tool and ships the configuration.
- detekt — static analysis beyond what `-Werror` provides is left for
  a follow-up policy slice.
- Compose UI instrumented tests — require a connected Android device
  or emulator. The CTA semantics smoke gate reads the `App.kt` source
  through a textual check until the host-test path is upgraded to
  cover the actual Compose tree.

## Android lint

`:androidApp:lintDebug` runs on every PR that touches `androidApp/**`
or `gradle/**`. The HTML report is uploaded as a workflow artifact
named `android-lint-report`. The SARIF is uploaded to the GitHub
code-scanning tab if `GH_TOKEN` is configured for the workflow.

Severity mapping (default AGP rules):

| Lint severity | PR consequence |
| --- | --- |
| `Error` | PR cannot merge |
| `Warning` | Passes if it matches the baseline; fails if it is new |
| `Information` / `Ignore` | Passes |

The baseline file lives at `androidApp/lint-baseline.xml`. It is
checked in and **shrunk** each time the team fixes a warning. New
warnings never pass; we delete from the baseline as warnings are
fixed.

`androidApp/build.gradle.kts` configures the lint task as:

```kotlin
lint {
    baseline = file("lint-baseline.xml")
    abortOnError = true
    warningsAsErrors = false
    checkAllWarnings = false
}
```

`abortOnError = true` means any new lint error fails the PR;
`warningsAsErrors = false` keeps existing warnings in the baseline
until they are fixed.

## Kotlin compile warnings

`kotlinc` emits warnings for unused imports, deprecated APIs,
redundant casts, parameter-name mismatches, etc. The
`composeApp/build.gradle.kts` enables `allWarningsAsErrors = true`
across every `KotlinCompilationTask`, so any new warning in any
source set (commonMain, commonTest, androidMain, iosMain, …) fails
the build. The settings are mirrored for the Android target
(`compilerOptions.allWarningsAsErrors.set(true)`) and for the iOS
targets through `tasks.withType<KotlinCompilationTask<*>>` so the
behavior is consistent across platforms.

The baseline warning count is tracked in
[`docs/dependency-policy.md`][dep-policy] as part of the dependency /
supply-chain maintenance policy. New warnings introduced by a PR
fail the PR; only when a warning is fixed (and the code that emitted
it is gone) is the baseline shrunk.

## Core navigation smoke

`commonTest/.../navigation/BedtimeFlowControllerTest` covers:

- startup with active session → `Sleeping`
- startup without active → `Bedtime`
- `finish` moves to `Morning` and writes one completed record
- repeated `finish` does not create a second session

The test runs as part of `:composeApp:testAndroidHostTest`. A
regression in any of these scenarios fails the CI build.

## Permission UX smoke

`MicrophonePermissionUxTest` and `SleepDataSourceTest` cover the
opt-in / opt-out wiring and the typed permission results. A
regression that drops the "explicit permission required" UX fails
the CI build.

## Fallback smoke

`CompanionStaticFallbackTest` covers the automatic-retry cap and the
static fallback path. `OvernightSoundAnalysisControllerTest`
(`NoOpOvernightSoundAnalysisController` contract) covers the
microphone-unavailable fallback. A regression that removes the
fallback fails the CI build.

## Settings route smoke

`AmbientAudioPreferencesStoreContractTest` covers the contract that
`SettingsScreen` is wired through:

- the `write(preferences: …)` parameter name is consistent across
  the interface and the platform implementation
- a round-trip through `read` / `write` preserves all fields
- `clear()` returns the store to its default state
- the ambient and sound-analysis stores are independent so the
  Settings callbacks do not leak state between sections

The test uses in-memory fakes so it runs as part of
`:composeApp:testAndroidHostTest` without needing a device. Compose
UI instrumented tests remain a follow-up.

## Sleep / wake CTA semantics

The bedtime "一緒に寝る" CTA and the sleeping "起きる" CTA both call
into the core sleep lifecycle. Each one is wrapped in a Compose
`Button` that applies a `semantics { contentDescription = … }`
modifier so TalkBack / VoiceOver can announce the action. The two
labels are exposed through
[`AccessibilityPolicy`][accessibility-policy]:

- `AccessibilityPolicy.SLEEP_START_CONTENT_DESCRIPTION` = "睡眠を開始する"
- `AccessibilityPolicy.WAKE_CONTENT_DESCRIPTION` = "起床して朝の記録を見る"

`SleepStartWakeCtaSemanticsTest` pins:

- the constants are present, non-blank, and stable
- the `App.kt` source uses the constants (not inline literals) in
  both the bedtime and sleeping CTA sites
- the semantics block is wrapped around the matching `onClick =`
  callback

Until the host-test path gains Compose UI rendering, the test reads
`App.kt` from the project directory and checks the surrounding text
window for each CTA. Once the Compose UI instrumented test path is
unblocked, this test should be replaced with a true UI assertion.

## Flaky test policy

Flaky tests are fixed, not retried.

- A test that fails twice in a row on the same commit fails the PR
  until the test is fixed.
- A test that fails once on a single commit is not retried in CI;
  the engineer reruns the workflow from the GitHub UI while they
  investigate.
- A test that is known to be flaky is annotated `@Ignore("flaky:
  reason")` with a tracking issue link and tracked in
  [`docs/quality-epic-status.md`][quality-epic-status].

## CI log diagnosis

A failed gate MUST point the engineer at the offending class or
test. CI surfaces:

- failing test class + test name (JUnit / kotlin.test output)
- failing lint rule + resource (Android lint report HTML / SARIF)
- failing gradle task name (compile error message)
- for `-Werror`, the kotlinc warning line + file path

If a gate's failure log does not point at a specific file or test,
that is a follow-up to fix the gate, not a follow-up to add more
gates.

## Re-evaluation triggers

- a new gate is added,
- a gate's failure log no longer points at a specific file or test,
- a flaky test annotation list grows past 5 entries,
- the team picks a formatter or static-analysis tool,
- the host-test path is upgraded to render Compose UI so the textual
  CTA semantics check can be replaced with a true UI assertion.

[issue-193]: https://github.com/SMRI2170/soine/issues/193
[dep-policy]: ./dependency-policy.md
[quality-epic-status]: ./quality-epic-status.md
[accessibility-policy]: ../composeApp/src/commonMain/kotlin/app/soine/accessibility/AccessibilityPreferences.kt