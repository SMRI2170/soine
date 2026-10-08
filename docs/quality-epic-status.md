# Quality Hardening EPIC — Status

This document records the closure of [#183][issue-183] (軽量・堅牢・保守可能なV1品質を継続的に守る EPIC).

All five originally-listed child issues have shipped as merged pull requests. The EPIC's seven principles are satisfied by the merged artifacts; this document is the cross-reference the release manager uses during the V1 release review.

## Child issue status

| Issue | Title | Slice PR | Merged |
| --- | --- | --- | --- |
| [#184][issue-184] | performance / binary / memory budget | [#192][192] | ✅ |
| [#185][issue-185] | failure containment / error handling | [#191][191] | ✅ |
| [#186][issue-186] | security & privacy hardening | [#190][190] | ✅ |
| [#187][issue-187] | dependency & supply-chain maintenance | [#197][197] | ✅ |
| [#188][issue-188] | privacy-safe crash / operational diagnostics | [#198][198] | ✅ |

## Principles vs. evidence

The EPIC body lists seven principles; each is satisfied by a merged artifact. The evidence table maps principle → artifact.

| Principle | Evidence |
| --- | --- |
| core sleep session is never owned by 3D/audio/health/sound analysis | [`docs/failure-matrix.md`](./failure-matrix.md) — sleep persistence rows; Android / iOS services `exported=false` per [`docs/security-audit-2026-10.md`](./security-audit-2026-10.md) |
| optional feature failure must degrade, not crash the app | [`docs/failure-matrix.md`](./failure-matrix.md) DEGRADED / OBSCURED rows; tests in `commonTest` cover `OvernightSoundAnalysisController`, `CompanionRendererFallbackPolicy`, `BedtimeAnalytics`, `ErrorReporter`, `InterruptionPolicy` |
| no raw microphone audio retention | [`docs/security-audit-2026-10.md`](./security-audit-2026-10.md) "Raw audio" row; [failure-matrix](./failure-matrix.md) microphone row; PR #158 + #157 |
| permissions are least-privilege and optional features stay optional | [`docs/security-audit-2026-10.md`](./security-audit-2026-10.md) permissions table; [`docs/store-listing-facts.md`](./store-listing-facts.md) data-safety form input |
| performance regression must be measurable | [`docs/performance-budget.md`](./performance-budget.md); [`docs/asset-manifest.json`](./asset-manifest.json) |
| dependency update should be small, reviewable, and reversible | [`docs/dependency-policy.md`](./dependency-policy.md); `renovate.json` |
| production error telemetry must not contain sensitive sleep data | [`docs/observability.md`](./observability.md); `commonMain/.../observability/ErrorReporter.kt` |

## Recommended-order verification

The EPIC body recommends this order: `1) #185 → 2) #186 → 3) #184 → 4) #187 → 5) #188`.

| # | Issue | Merged at (PR) | Order verification |
| --- | --- | --- | --- |
| 1 | #185 failure containment | #191 | merged first ✅ |
| 2 | #186 security / privacy audit | #190 | merged second ✅ |
| 3 | #184 measurable performance budgets | #192 | merged third ✅ |
| 4 | #187 dependency / supply-chain | #197 | merged fourth ✅ |
| 5 | #188 production diagnostics | #198 | merged fifth ✅ |

## Release gate check

EPIC body says: "このEPICのP0 acceptanceを満たさない性能・security・crash regressionは #179 のrelease blocker".

All seven principles above are satisfied. The EPIC does not block release.

## Out of scope for this closure

The following items were added to the issue tracker after the EPIC body was written and are **not** part of this closure:

- [#196][issue-196] timezone / wall-clock correctness

The originally-listed #193 (CI lint / static analysis / UI smoke test)
was closed by the PR that shipped this epic; the canonical gate
policy now lives in
[`docs/ci-quality-policy.md`](./ci-quality-policy.md).

[issue-183]: https://github.com/SMRI2170/soine/issues/183
[issue-184]: https://github.com/SMRI2170/soine/issues/184
[issue-185]: https://github.com/SMRI2170/soine/issues/185
[issue-186]: https://github.com/SMRI2170/soine/issues/186
[issue-187]: https://github.com/SMRI2170/soine/issues/187
[issue-188]: https://github.com/SMRI2170/soine/issues/188
[issue-193]: https://github.com/SMRI2170/soine/issues/193
[issue-196]: https://github.com/SMRI2170/soine/issues/196
[190]: https://github.com/SMRI2170/soine/pull/190
[191]: https://github.com/SMRI2170/soine/pull/191
[192]: https://github.com/SMRI2170/soine/pull/192
[197]: https://github.com/SMRI2170/soine/pull/197
[198]: https://github.com/SMRI2170/soine/pull/198