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
| **Android lint** | PR | `:androidApp:lintDebug` | PR cannot merge when severity >= error |
| **Kotlin compile warnings** | PR | Gradle `-Werror` for `app.soine` package | PR cannot merge when a new warning lands |
| **Lint baseline** | PR | `androidApp/lint-baseline.xml` committed and shrunk each cycle | PR can land with the same warnings; new warnings fail |
| **Core navigation smoke** | PR | `:composeApp:testAndroidHostTest` (`BedtimeFlowControllerTest`) | PR cannot merge when navigation regresses |
| **Permission UX smoke** | PR | `:composeApp:testAndroidHostTest` (`MicrophonePermissionUxTest`, `SleepDataSourceTest`) | PR cannot merge when opt-in / opt-out wiring breaks |
| **Fallback smoke** | PR | `:composeApp:testAndroidHostTest` (`CompanionStaticFallbackTest`, `OvernightSoundAnalysisControllerTest`) | PR cannot merge when a fallback path silently disappears |

Tools deliberately **not** in scope (tracked as deferred):

- ktlint / ktfmt — formatting policy is left to IDE import settings
  until the team picks a tool and ships the configuration.
- detekt — static analysis beyond what `-Werror` provides is left for
  a follow-up policy slice.
- Compose UI instrumented tests — require a connected Android device
  or emulator; tracked as a follow-up because the host-test path
  already covers the navigation smoke.

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

## Kotlin compile warnings

`kotlinc` emits warnings for unused imports, deprecated APIs,
redundant casts, etc. The CI runner passes `-Werror` for the
`app.soine` package so a new warning fails the build. The baseline
warning count is tracked in
[`docs/dependency-policy.md`][dep-policy] as part of the dependency /
supply-chain maintenance policy.

## Core navigation smoke

`commonTest/.../navigation/BedtimeFlowControllerTest` already covers:

- startup with active session → `Sleeping`
- startup without active → `Bedtime`
- `finish` moves to `Morning` and writes one completed record
- repeated `finish` does not create a second session

The test runs as part of `:composeApp:testAndroidHostTest`. A
regression in any of these scenarios fails the CI build.

## Permission UX smoke

`MicrophonePermissionUxTest` and `SleepDataSourceTest` already cover
the opt-in / opt-out wiring and the typed permission results. A
regression that drops the "explicit permission required" UX fails
the CI build.

## Fallback smoke

`CompanionStaticFallbackTest` covers the automatic-retry cap and the
static fallback path. `OvernightSoundAnalysisControllerTest`
(`NoOpOvernightSoundAnalysisController` contract) covers the
microphone-unavailable fallback. A regression that removes the
fallback fails the CI build.

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

If a gate's failure log does not point at a specific file or test,
that is a follow-up to fix the gate, not a follow-up to add more
gates.

## Re-evaluation triggers

- a new gate is added,
- a gate's failure log no longer points at a specific file or test,
- a flaky test annotation list grows past 5 entries,
- the team picks a formatter or static-analysis tool.

[issue-193]: https://github.com/SMRI2170/soine/issues/193
[dep-policy]: ./dependency-policy.md
[quality-epic-status]: ./quality-epic-status.md