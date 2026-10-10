# Performance Budget

Concrete targets for binary size, runtime behavior, memory and battery
that the Soine app must stay within. This document is the first slice
of [#184][issue-184] (performance / binary / memory budget).

The numbers below are starting points calibrated against the current
`main` commit. Tighten them as the team measures real-device behavior
in [#182][issue-182] (7-night RC).

## Methodology

- All binary numbers are measured against `assembleRelease` /
  Xcode `Release` configuration. R8 is enabled for Android
  ([#186][issue-186]).
- Relative regression detection in CI is the primary signal. Absolute
  thresholds are reviewed each release candidate.
- Recording format and result templates live in
  [`docs/overnight-battery-benchmark.md`][battery-bench].
- Re-run the inventory script (`scripts/perf-inventory.sh`) before
  changing a number. The inventory script is intentionally simple so
  it can run on every CI step.

## 1. Binary / asset budget

### 1.1 Release artifact size

| Artifact | Target | Hard ceiling | Measurement |
| --- | --- | --- | --- |
| Android `app-release.apk` | ≤ 12 MB | 18 MB | `:androidApp:assembleRelease` artifact size |
| Android `app-release.aab` | ≤ 12 MB | 18 MB | Play upload size |
| iOS app IPA (`Release` config) | ≤ 25 MB | 35 MB | Xcode `Archive` post-export |
| iOS-side embedded KMP framework | ≤ 8 MB | 12 MB | `ComposeApp.framework` size inside IPA |

The hard ceiling triggers a CI failure. The target is the engineering
alert level.

### 1.2 Per-asset budget

Authored assets must stay within these individual limits:

| Asset class | Target per file | Hard ceiling | Notes |
| --- | --- | --- | --- |
| Companion GLB (`soine_companion.glb`) | ≤ 60 KB | 200 KB | Current: 30,616 B |
| Ambient sound (bundled) | ≤ 24 KB | 96 KB | See compression note below |
| Background / UI texture | ≤ 32 KB | 128 KB | n/a yet, applies when added |
| Font subset | ≤ 48 KB | 96 KB | per family, subset per script |

Total ambient audio budget:

| Platform | Per file (current) | Target total | Hard ceiling |
| --- | --- | --- | --- |
| Android `res/raw` (rain / waves / white-noise) | 96,044 B each (288 KB) | ≤ 60 KB total | ≤ 288 KB total |
| iOS `Resources` (same three files) | 96,044 B each (288 KB) | ≤ 60 KB total | ≤ 288 KB total |

**Compression note.** The current bundled WAVs are 16-bit PCM mono
loops. They should be re-encoded to OGG Vorbis (q≈3) or AAC (≈64 kbps)
before #181 (release assets). Re-encoded size estimate: ≤ 12 KB per
loop. Until that lands, the budget is intentionally permissive
(`hard ceiling` matches the current size) so the budget does not
fail CI on the existing assets.

### 1.3 Asset duplication rules

- No duplicate texture by content hash.
- No duplicate audio by content hash.
- No unused assets (assets not referenced from `commonMain` or
  platform resources).
- No external textures outside the GLB container.

CI hook (deferred): `scripts/perf-inventory.sh` writes
`build/perf-inventory.json` next to the build artifact with the
current sizes. A follow-up PR wires a size-diff step into CI that
fails when any tracked asset grows by > 25 % versus the previous
release.

The V1 ships the inventory + check scripts as a #184
deliverable:

- [`scripts/perf-inventory.sh`][perf-inventory-script] —
  emits `build/perf-inventory.json` with the current
  release artifact size, the KMP framework size, and
  the first-party dependency count.
- [`scripts/perf-budget-check.sh`][perf-budget-script] —
  reads the inventory and verifies the hard ceilings.
  Exits non-zero on a violation so a CI step can fail
  the build.
- [`app.soine.perf.PerfBudget`][perf-budget-source] —
  the source-of-truth `PerfBudget.Binary` /
  `PerfBudget.Dependencies` values. The check script
  mirrors them; a future contributor who lowers a
  ceiling must update both. The
  `PerfBudgetContractTest` pins the values so a
  drift in either direction fails a unit test.

## 2. Runtime budget

Targets are measured on a mid-range physical device
(reference: Pixel 6a / iPhone 13 class). All times measured from a
cold process start to the named UI state.

| Metric | Target | Hard ceiling | Measurement |
| --- | --- | --- | --- |
| Cold start → first Soine screen | ≤ 1.5 s | 3.0 s | frame-timestamp of `MainActivity.onCreate` / `applicationDidFinishLaunching` |
| Cold start → bedtime screen | ≤ 2.5 s | 4.0 s | first frame of bedtime destination |
| Cold start → companion visible (when renderer ready) | ≤ 3.0 s | 5.0 s | first `CompanionRendererStatus.READY` after status observer attached |
| Renderer initialization (asset load + first frame) | ≤ 2.0 s | 4.0 s | time inside `CompanionRenderer` from `submit` to `READY` |
| Audio focus → first sample audible | ≤ 500 ms | 1.0 s | `MediaPlayer.start` / `AVAudioPlayer.play` |
| Sleep session start → persisted snapshot | ≤ 300 ms | 800 ms | `commit()` latency of `SleepSessionStore.write` |
| Morning summary rendered | ≤ 800 ms | 1.5 s | Compose first composition of morning summary |

## 3. Memory budget

Steady-state means the device is in a settled sleep session with
the screen off and ambient audio playing.

| State | Target RSS (Android) / footprint (iOS) | Hard ceiling |
| --- | --- | --- |
| App foreground, no sleep session | ≤ 80 MB | 120 MB |
| Sleep session active, screen on, renderer visible | ≤ 180 MB | 240 MB |
| Sleep session active, screen off, audio playing | ≤ 130 MB | 180 MB |
| Sleep session active, screen off, no audio | ≤ 100 MB | 140 MB |
| Morning summary | ≤ 140 MB | 180 MB |

`maxAutomaticRetries` from
[`CompanionRendererFallbackPolicy`][renderer-fallback-policy] is the
circuit for renderer memory explosions: if init fails twice in a
row the renderer is replaced with the static fallback, which must
release all GLB-derived memory.

## 4. Frame-rate budget

| Surface | Target FPS | Floor |
| --- | --- | --- |
| Sleeping scene (companion visible, screen on) | 60 | 30 |
| Sleeping scene (companion hidden, screen off) | n/a — renderer suspended | n/a |
| Bedtime flow / UI navigation | 60 | 30 |
| Morning summary | 60 | 30 |

The renderer is suspended while the screen is off. A regression that
keeps the render loop alive in the background would also blow past the
overnight battery ceiling below and is treated as a release blocker.

## 5. Overnight battery / thermal budget

Per [`docs/overnight-battery-benchmark.md`][battery-bench] scenario B
(sleep session + ambient audio + screen off, 6 h minimum):

| Device class | Battery delta / hour | Hard ceiling / hour |
| --- | --- | --- |
| Pixel 6a / iPhone 13 class | ≤ 0.5 % per hour | 1.0 % per hour |
| Pixel 4a / iPhone 11 class | ≤ 0.7 % per hour | 1.3 % per hour |

Thermal observation rule: any OS thermal warning during an overnight
run is a release blocker.

## 6. Cold-start dependency cap

Track the number of first-party dependencies and warn when it grows:

| Module | Target | Hard ceiling |
| --- | --- | --- |
| `composeApp` first-party dependencies | ≤ 8 | 12 |
| `androidApp` first-party dependencies | ≤ 4 | 6 |
| Total first-party dependencies (excluding transitive) | ≤ 12 | 18 |

Counted from `*.gradle.kts` `dependencies { ... }` blocks. Tracked in
[#187][issue-187].

## 7. Diagnostic surface

Per `assumenosideeffects android.util.Log` in
[`androidApp/proguard-rules.pro`][proguard-rules], release builds do
not emit log lines. Performance regressions surface through:

- [ ] `assembleRelease` artifact size diff in CI (deferred, see 1.3).
- [x] `assembleDebug` Android build verification (#81).
- [x] `testAndroidHostTest` common-module verification (#80).
- [x] `overnight-battery-benchmark` measurements (#182).
- [ ] Production diagnostics for renderer fallback events (see [NIGHT-engine][] and #188).

## 8. Re-evaluation triggers

Re-evaluate these budgets when any of the following change:

- new bundled asset or new first-party dependency,
- Compose / Kotlin / AGP / iOS Xcode major bump,
- Android target SDK bump,
- new optional subsystem lands (e.g. Health, microphone, dream
  visual).

[issue-184]: https://github.com/SMRI2170/soine/issues/184
[issue-182]: https://github.com/SMRI2170/soine/issues/182
[issue-186]: https://github.com/SMRI2170/soine/issues/186
[issue-187]: https://github.com/SMRI2170/soine/issues/187
[issue-188]: https://github.com/SMRI2170/soine/issues/188
[battery-bench]: ./overnight-battery-benchmark.md
[renderer-fallback-policy]: ../composeApp/src/commonMain/kotlin/app/soine/companion/CompanionStaticFallback.kt
[proguard-rules]: ../androidApp/proguard-rules.pro
[perf-inventory-script]: ../scripts/perf-inventory.sh
[perf-budget-script]: ../scripts/perf-budget-check.sh
[perf-budget-source]: ../composeApp/src/commonMain/kotlin/app/soine/perf/PerfBudget.kt