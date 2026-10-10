# Failure Containment Matrix

This document is the canonical map of every optional and critical path
in Soine, the failures it can hit, the existing typed-result / fallback
that absorbs each failure, and the severity classification.

It is the first slice of [#185][issue-185] (V1 failure containment /
graceful degradation). It is intentionally a snapshot, not a contract.
Re-evaluate on every release candidate or whenever an optional feature
lands.

## Severity scale

| Severity | Meaning | User-visible reaction |
| --- | --- | --- |
| `FATAL` | Cannot complete the operation, but the active sleep session must still be preserved. | Short actionable copy; offer recovery action. Never show stack traces. |
| `RECOVERABLE` | Operation fails today, but the user can retry or grant a missing capability. | Surface a single retry / settings CTA. No retry loops. |
| `DEGRADED` | Operation continues with reduced capability. | Silent or one-line acknowledgement. |
| `OBSCURED` | Failure absorbed inside an idempotent no-op. | None. |

Every failure below is **fade-fade**: the core loop
(`bedtime → sleeping → morning`) never depends on the failed subsystem
being healthy.

## Sleep persistence (critical)

| Failure | Severity | Existing handling | Where |
| --- | --- | --- | --- |
| Schema migration target newer than current | `FATAL` (recoverable UI) | `ForwardSchemaMigrator` rejects; `SleepSessionStorageCorruptedException` thrown | [`StoredSleepSessionRepository`][sleep-repo] |
| Snapshot row with wrong kind tag (`A` / `C`) | `FATAL` (recoverable UI) | `require(...)` in codec | same |
| Duplicate completed session ids | `FATAL` (recoverable UI) | `require(...)` in codec | same |
| `write` / `clear` SharedPreferences commit fails | `FATAL` (recoverable UI) | `check(...)` surfaces I/O failure | [`AndroidSleepSessionStore`][sleep-store] |
| Recovery on cold boot | `DEGRADED` | `RecoverSleepSessionUseCase` returns `Recovered` / `AlreadyActive` / `Missing` sealed result | [`RecoverSleepSessionUseCase`][recover-usecase] |

Acceptance check: `StoredSleepSessionRepositoryTest.corruptedSnapshotIsNotSilentlyOverwritten`
proves the corrupted value is preserved until a recovery action runs.

## Ambient audio (optional)

| Failure | Severity | Existing handling | Where |
| --- | --- | --- | --- |
| Audio focus denied by OS | `DEGRADED` | `requestFocus()` returns false → `stopSelf()` | [`AmbientAudioService`][audio-service] |
| `MediaPlayer.create` returns null (decode failure) | `DEGRADED` | null branch → `stopSelf()` | same |
| `setVolume` outside `[0,1]` | `FATAL` (validation) | `require(...)` in `AmbientAudioState.init` | [`AmbientAudioController`][audio-ctrl] |
| Invalid stored preferences (corrupt JSON) | `DEGRADED` | `AmbientAudioPreferencesTest.corruptValuesFallBackWithoutCrashing` | existing test |
| Timer expiry while playing | `DEGRADED` | `PersistentSleepTimer.tick()` returns true; `BedtimeAudioCoordinator` stops audio | [`BedtimeAudioCoordinator`][bedtime-audio] |
| iOS audio session interruption | `DEGRADED` | `IosAmbientAudioController` surfaces `PAUSED` state and observes focus changes | platform layer |

User-facing copy: `「環境音を一時停止しました」` is enough. No SDK error text.

## 3D companion renderer (optional with graceful fallback)

| Failure | Severity | Existing handling | Where |
| --- | --- | --- | --- |
| Renderer init / asset decode fails | `DEGRADED` | `CompanionRendererFallbackPolicy.onFailure` returns `USE_STATIC_FALLBACK` after `maxAutomaticRetries` | [`CompanionStaticFallback`][static-fallback] |
| Renderer status `FAILED` repeats | `DEGRADED` (escalated) | Telemetry receives `CompanionRendererFailureEvent(code, consecutiveFailures)` | same |
| Renderer crashes (process) | `DEGRADED` | `companionRenderer = NoOpCompanionRenderer` in `SoineApp` default | [`SoineApp`][soine-app] |
| `CompanionIntent` unknown / new | `DEGRADED` | `CompanionStaticFallback.forIntent` has exhaustive `when` and falls back to `awake` | [`CompanionStaticFallback`][static-fallback] |
| Sleeping placement not configured | `DEGRADED` | `CompanionSleepingDistancePolicy.forStage` falls back to `BESIDE` | [`CompanionSleepingDistance`][sleeping-distance] |

Existing tests: `CompanionStaticFallbackTest.automaticRetryStopsAfterConfiguredLimit`,
`CompanionRendererTest.statusCanBeObservedAndFailureDoesNotDiscardRequest`.

## Health Connect / HealthKit (optional)

| Failure | Severity | Existing handling | Where |
| --- | --- | --- | --- |
| SDK unavailable on device | `DEGRADED` | `HealthPermissionState.UNAVAILABLE` → `SleepSignalReadResult.Unavailable` | [`SleepDataSource`][sleep-data-source] |
| User denies permission | `RECOVERABLE` | `HealthPermissionState.DENIED` → `SleepSignalReadResult.PermissionDenied` | same |
| User has never been asked | `RECOVERABLE` | `HealthPermissionState.NOT_REQUESTED` → `PermissionRequired` (does NOT auto-ask) | same |
| `HealthConnectClient.permissionController` throws | `DEGRADED` | `runCatching` → `UNAVAILABLE` | [`AndroidHealthConnectSleepDataSource`][health-android] |
| `readRecords` throws `SecurityException` mid-pagination | `RECOVERABLE` | caught → `PermissionDenied` | same |
| `readRecords` throws any other `Throwable` | `DEGRADED` | caught → `Unavailable` | same |
| iOS HealthKit unauthorized | `RECOVERABLE` | mirrors Health Connect path | iOS adapter |
| Manual session collides with Health signal | `DEGRADED` | `HealthSleepMergePolicy` keeps manual session and ingests Health remainder | [`HealthSleepMergePolicy`][merge-policy] |

Existing tests: `SleepDataSourceTest.deniedAndUnavailableAreExplicitResults`,
`notRequestedDoesNotImplicitlyAskForPermission`,
`HealthSleepMergePolicyTest`.

User-facing copy: skip mic permission because no permission screen. Settings has the toggle.

## Overnight sound analysis (optional)

| Failure | Severity | Existing handling | Where |
| --- | --- | --- | --- |
| Microphone permission denied | `DEGRADED` | Controller is `NoOpOvernightSoundAnalysisController` | [`OvernightSoundAnalysisController`][sound-ctrl] |
| Microphone unavailable on device | `DEGRADED` | `MicrophonePermissionState.UNAVAILABLE` → `MicrophoneEnableAction.UNAVAILABLE` | [`MicrophonePermission`][mic-permission] |
| Foreground service promotion fails | `DEGRADED` | `runCatching` cancels runtime, no throw | [`OvernightSoundAnalysisService`][sound-service] |
| `AudioRecord.getMinBufferSize` returns ≤ 0 | `DEGRADED` | cancels runtime, `stopSelf()` | same |
| `AudioRecord.state != INITIALIZED` | `DEGRADED` | `release()` + cancels runtime | same |
| Capture loop throws | `DEGRADED` | `catch (_: Throwable)` in capture loop | same |
| Notification permission denied on Tiramisu+ | `DEGRADED` | notification is disclosure-only, denial does not block service | [`MainActivity`][main-activity] |

User-facing copy: `「夜間の音を解析できません」` and Settings toggle.

## Persistence (local-first)

| Failure | Severity | Existing handling | Where |
| --- | --- | --- | --- |
| Snapshot corruption (any subsystem) | `FATAL` (recoverable UI) | `require(...)` + typed exception; never silently cleared | all `Stored*` repositories |
| SharedPreferences `commit()` returns `false` | `FATAL` (recoverable UI) | `check(...)` surfaces failure | all platform stores |
| Schema version higher than known | `FATAL` (recoverable UI) | `ForwardSchemaMigrator.migrate` throws | [`ForwardSchemaMigrator`][migrator] |
| `LocalDataDeletionService` partially fails | `OBSCURED` | first failure wins, remaining clearers still run | [`LocalDataDeletion`][local-deletion] |
| Active session present during delete | `OBSCURED` | `BlockedByActiveSession` result | same |

Existing tests: `StoredSleepSessionRepositoryTest.corruptedSnapshotIsNotSilentlyOverwritten`,
`StoredCompanionProgressRepositoryTest.corruptSnapshotFailsWithoutClearingOriginalValue`,
`SoundEventRepositoryTest.corruptSnapshotIsNotSilentlyOverwritten`,
`LocalDataDeletionServiceTest`.

## Night memory / dream decoration

| Failure | Severity | Existing handling | Where |
| --- | --- | --- | --- |
| Engine returns no candidates | `DEGRADED` | morning flow proceeds without decoration | [`WeightedNightEventEngine`][night-engine] |
| `DreamDiscoveryEngine` returns ineligible | `DEGRADED` | `DreamDiscoveryOutcome.Ineligible` | [`DreamDiscoveryEngine`][dream-engine] |
| Stored discovery corruption | `FATAL` (recoverable UI) | typed exception | `StoredDreamDiscoveryRepository` |

User-facing copy: empty morning summary is acceptable.

## Dialogue / content catalog

| Failure | Severity | Existing handling | Where |
| --- | --- | --- | --- |
| Catalog empty for current stage | `DEGRADED` | `CompanionDialogueSelector` falls back to neutral line | [`CompanionDialogueSelector`][dialogue-selector] |
| Catalog returns invalid phase | `FATAL` (validation) | `require(...)` in catalog builder | catalogs |

## Shared AppError evaluation

`#185` asks whether a unified `AppError` is needed. Evaluation:

- Every existing failure path already returns a **typed sealed
  interface** at the domain boundary (`StartSleepSessionResult`,
  `SleepSignalReadResult`, `LocalDataDeletionResult`, `RecoverSleepSessionResult`,
  `CompanionRendererStatus.FAILED`, …).
- Platform exceptions are caught at the platform layer with
  `runCatching { ... }` and converted to the typed result before
  crossing the boundary. Search confirms this is consistent:
  no platform exception types are referenced from `commonMain`.
- A unified `AppError` would add a layer of indirection without
  removing the typed results (typed results encode the operation
  outcome, not just "an error happened").

**Conclusion:** introducing a shared `AppError` is not justified at
this stage. Continue with sealed-result-per-subsystem and revisit if
multiple operations start sharing identical fallback trees.

## User-facing copy rules

- No `Throwable.message`, SDK name, package name or stack trace in user-visible copy.
- No retry-loop: every RECOVERABLE failure offers exactly one of
  `Retry`, `Open settings`, `OK`. Never both at once.
- DEGRADED copy is a single short sentence.
- OBSCURED failures are not surfaced.

## Re-evaluation trigger

Re-run this matrix when any of the following change:

- new optional subsystem lands,
- new `Result` / `Status` sealed interface is introduced,
- a new platform exception type is caught and routed differently,
- the privacy policy gains or loses a data category.

## Open follow-up work (out of scope for this PR)

- ~~Replace `InMemoryAmbientAudioController` with a failure-injecting
      fake in `commonTest` so audio focus loss / decode failure can be
      exercised without a real device (#185 task list).~~ **Done**
      in the V1 failure-injection slice: the
      `FailureInjectingAmbientAudioController` fake lives in
      `commonTest` and the `AudioFailureInjectionTest` exercises the
      graceful-degradation contract without a real device.
- ~~Add a renderer failure-injection test that uses a
      `FailureInjectingCompanionRenderer` to drive the static
      fallback path.~~ **Done** in the V1 failure-injection slice:
      `FailureInjectingCompanionRenderer` (in `commonTest`) +
      `FailureInjectionContractTest` cover the retry / static
      fallback / recovery / counter-reset / manual-retry paths.
- [ ] Add a Health failure surface on iOS mirror that mirrors the
      Android `SleepDataSource` sealed interface (already in place;
      re-verify after the iOS HealthKit adapter changes ship).
- [ ] Add a `CompanionRendererFailureEvent` audit trail in the
      production diagnostics path (#188).
- [ ] Add instrumented tests for permission revocation mid-session
      (Android: revoke `RECORD_AUDIO` while a session is active and
      verify the controller short-circuits to no-op).

[issue-185]: https://github.com/SMRI2170/soine/issues/185
[sleep-repo]: ../composeApp/src/commonMain/kotlin/app/soine/sleep/StoredSleepSessionRepository.kt
[sleep-store]: ../composeApp/src/androidMain/kotlin/app/soine/storage/AndroidSleepSessionStore.kt
[recover-usecase]: ../composeApp/src/commonMain/kotlin/app/soine/sleep/RecoverSleepSessionUseCase.kt
[audio-service]: ../androidApp/src/main/kotlin/app/soine/audio/AmbientAudioService.kt
[audio-ctrl]: ../composeApp/src/commonMain/kotlin/app/soine/audio/AmbientAudioController.kt
[bedtime-audio]: ../composeApp/src/commonMain/kotlin/app/soine/audio/BedtimeAudioCoordinator.kt
[static-fallback]: ../composeApp/src/commonMain/kotlin/app/soine/companion/CompanionStaticFallback.kt
[soine-app]: ../composeApp/src/commonMain/kotlin/app/soine/SoineApp.kt
[sleeping-distance]: ../composeApp/src/commonMain/kotlin/app/soine/companion/CompanionSleepingDistance.kt
[sleep-data-source]: ../composeApp/src/commonMain/kotlin/app/soine/health/SleepDataSource.kt
[health-android]: ../androidApp/src/main/kotlin/app/soine/health/AndroidHealthConnectSleepDataSource.kt
[merge-policy]: ../composeApp/src/commonMain/kotlin/app/soine/health/HealthSleepMergePolicy.kt
[sound-ctrl]: ../composeApp/src/commonMain/kotlin/app/soine/sound/OvernightSoundAnalysisController.kt
[mic-permission]: ../composeApp/src/commonMain/kotlin/app/soine/sound/MicrophonePermission.kt
[sound-service]: ../androidApp/src/main/kotlin/app/soine/sound/OvernightSoundAnalysisService.kt
[main-activity]: ../androidApp/src/main/kotlin/app/soine/MainActivity.kt
[migrator]: ../composeApp/src/commonMain/kotlin/app/soine/storage/SchemaMigration.kt
[local-deletion]: ../composeApp/src/commonMain/kotlin/app/soine/privacy/LocalDataDeletion.kt
[night-engine]: ../composeApp/src/commonMain/kotlin/app/soine/night/WeightedNightEventEngine.kt
[dream-engine]: ../composeApp/src/commonMain/kotlin/app/soine/dream/DreamDiscoveryEngine.kt
[dialogue-selector]: ../composeApp/src/commonMain/kotlin/app/soine/dialogue/CompanionDialogueSelector.kt