# iOS Target Architecture Matrix

This document is the source-of-truth for which iOS architectures the
KMP framework and the iOS app ship against. The change in this PR
removes `iosX64` from the KMP framework target list to unblock CI
that runs on Apple Silicon hosts.

## Current matrix

| Slice | Built? | Linked into the app? |
| --- | --- | --- |
| `iosArm64` (device) | ✅ | ✅ |
| `iosSimulatorArm64` (Apple Silicon simulator) | ✅ | ✅ (Xcode on Apple Silicon) |
| `iosX64` (Intel Mac simulator) | ❌ removed | ❌ |

## Why `iosX64` is removed

- GitHub Actions `macos-latest` runners have been Apple Silicon
  since 2024. The CI cannot compile, link or run an `iosX64` slice.
- The KMP `kmpPartiallyResolvedDependenciesChecker` and the
  Compose-specific `checkIosSimulatorArm64MainComposeLibrariesCompatibility`
  task both fail to fetch the `iosX64` artifacts from Maven on Apple
  Silicon hosts. The failure breaks the build before any
  compilation runs.
- Apple deprecated the Intel Mac simulator in Xcode 16 and is expected
  to remove it in a future Xcode release.
- The V1 launch targets Japan; the team operates on Apple Silicon
  Macs.

## Trade-off

A developer with an Intel Mac running an Xcode simulator will not be
able to launch the app on that simulator after this change.

Mitigations:

- Use the Apple Silicon simulator (Rosetta) if available.
- Use an actual iOS device for local validation.

The trade-off is acceptable because (a) the team is on Apple Silicon,
(b) physical-device validation is the V1 acceptance path per
[`docs/release-candidate-scenarios.md`][rc-scenarios], and (c)
keeping `iosX64` blocks every CI run for the team.

## Re-adding `iosX64`

Re-add `iosX64` to the framework target list only when the team
needs to support an Intel Mac simulator again. The precondition is a
working CI host that can run `iosX64Test`, otherwise the same
dependency-resolution failure will return.

[rc-scenarios]: ./release-candidate-scenarios.md