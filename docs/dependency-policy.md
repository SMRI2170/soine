# Dependency & Supply-chain Policy

This document is the canonical operating procedure for adding, updating
and removing dependencies in the Soine repository. It is the first
slice of [#187][issue-187] (Dependency / GitHub Actions / supply-chain
maintenance).

It is intentionally a policy, not a contract. Re-evaluate when the
team's appetite for automated dependency PRs changes.

## Why this exists

- Kotlin / Compose / AGP / AndroidX / GitHub Actions ship breaking
  changes regularly. Mixing them with feature PRs makes cause and effect
  impossible to attribute.
- The current dependency surface is small (3 first-party libraries + 5
  plugins). Keeping it small is part of the V1 quality bar (see
  [`docs/performance-budget.md`][performance-budget], "Cold-start
  dependency cap").

## Add-only-with-justification

A new runtime dependency is added only when one of the following holds:

1. The feature requires it (no reasonable implementation without it).
2. The standard library or platform API does not cover the use case.
3. The cost is bounded and recorded in the PR description.

If any of those fail, prefer the platform API or remove the feature.

The PR description for a runtime dependency must answer:

- what user-visible behavior depends on it,
- what alternative was rejected and why,
- whether the dependency is opt-in (gated) or always-on,
- whether the dependency ships telemetry of its own (if yes, what
  payload categories).

Library-level `devDependencies` for test fixtures do not require a
PR justification but must follow the cold-start dependency cap.

## Update policy

- **Patch updates** of existing libraries and plugins can land via the
  automated dependency tool ([Renovate](#renovate)) grouped per package.
- **Minor / major updates** to Kotlin, Compose Multiplatform, AGP or
  AndroidX land in their **own PR**, never bundled with a feature.
- **Minor / major updates** to test-only dependencies may share a PR
  with each other but never with a feature.
- **Reactive updates** to known CVE advisories are exempt from the
  "one PR per package" rule but must still be reviewed before merge.

## Removal policy

A dependency is removed when:

- it is no longer referenced by any source set, or
- the feature it supported was dropped, or
- it duplicates a platform API we now prefer.

Removal candidates are listed in the next "Dependency removal audit"
section each release candidate cycle.

## GitHub Actions

GitHub Actions are dependencies of the system even though they are not
Gradle dependencies. The audit for actions follows the same
principles:

- Pin to commit SHA when possible (work follows up after this PR).
- Review provenance before adopting a new action.
- Drop actions that have not been updated in 12+ months unless they
  are critical and there is no replacement.
- Local rule: prefer first-party actions (`actions/checkout`,
  `actions/setup-java`, `gradle/actions/setup-gradle`) over third-party
  ones.

Current pinned actions:

| Action | Current ref | Notes |
| --- | --- | --- |
| `actions/checkout` | `@v5` | follow up: pin to SHA |
| `actions/setup-java` | `@v5` | follow up: pin to SHA |
| `gradle/actions/setup-gradle` | `@v5` | follow up: pin to SHA |

A future PR converts each `@v5` to a commit SHA reference and adds
`renovate.json` rules for those.

## Renovate

Renovate is the chosen automated dependency tool because it supports
fine-grained grouping, schedule windows and concurrent-PR limits that
match this policy. The configuration lives at
[`renovate.json`][renovate-config] (or `.github/renovate.json` if a
self-hosted config is preferred).

The Renovate configuration enforces:

- weekly schedule window,
- one PR per logical package group (Kotlin / Compose / AGP / AndroidX /
  GitHub Actions / test-only),
- patch updates auto-merged for test-only deps,
- no auto-merge for runtime deps (human review required),
- concurrency cap.

If Renovate is not adopted (operational concern, cost, or team
preference), replace with Dependabot using the same grouping
semantics. The policy does not depend on the tool.

## Transitive review

A change that adds a new transitive dependency must:

- list the new transitive in the PR description,
- state whether the transitive is actively maintained (last release
  within 18 months),
- call out any license change.

This applies whether the transitive is introduced by a runtime update
or by adding a new first-party dependency.

## Abandoned-dependency signal

A first-party dependency is flagged for review when any of:

- the upstream has not released in 18 months,
- the upstream has fewer than 3 maintainers AND fewer than 100
  reverse-dependencies,
- the upstream's last release introduced a security advisory that
  is still unfixed in our version.

The signal does not mean "remove now", it means "plan replacement".

## Gradle dependency verification

[Gradle dependency verification](https://docs.gradle.org/current/userguide/dependency_verification.html)
is the team's preferred mechanism to catch tampering at the supply-chain
level. Adoption is staged:

1. Generate the initial verification metadata (`gradle --write-verification-metadata sha256`).
2. Commit `gradle/verification-metadata.xml` and enable strict mode.
3. Rotate the metadata when an intentional dependency change lands.

Step 1 is the only step included in this PR's slice; steps 2 and 3 are
follow-up issues tracked separately.

## Rollback

A bad dependency update is reverted by:

1. `git revert` the dependency PR.
2. Re-push.
3. Trigger a CI run to confirm the revert is green.

The dependency tool's auto-merge gate is configured to never include
reverts in the auto-merge group, so the revert always waits on a
reviewer.

## Re-evaluation triggers

Re-evaluate this policy when any of the following change:

- the dependency surface grows past the cold-start dependency cap,
- a new supply-chain attack is in the news,
- the team changes the automated dependency tool,
- a major Kotlin / Compose / AGP release ships.

[issue-187]: https://github.com/SMRI2170/soine/issues/187
[performance-budget]: ./performance-budget.md
[renovate-config]: ../renovate.json