# Data Model

All MVP data is local-first.

## SleepSessionRecord

- id: String
- startedAtEpochMillis: Long
- endedAtEpochMillis: Long?
- status: PREPARING | SLEEPING | COMPLETED
- source: MANUAL | RECOVERED
- createdAtEpochMillis: Long
- updatedAtEpochMillis: Long
- schemaVersion: Int

### MVP persistence

The initial implementation stores the sleep-session repository as one versioned snapshot.

- commonMain owns snapshot encoding/decoding and repository semantics.
- Android persists the snapshot in a dedicated SharedPreferences file using synchronous commit for critical transitions.
- iOS persists the same snapshot in NSUserDefaults.
- corrupt snapshots fail closed instead of being silently overwritten.
- the storage boundary is replaceable with a database adapter later without changing sleep-domain use cases.

This is intentionally small for the MVP. A database becomes preferable once query volume/history size or migrations justify it.

## SleepSummaryRecord

- sessionId
- durationMillis
- estimatedSleepStartEpochMillis?
- estimatedWakeEpochMillis?
- confidence?
- schemaVersion

### Health enrichment policy

The persisted/manual session remains authoritative. Health-derived estimates
are modeled separately and must not overwrite the canonical session interval.
See `health-merge-policy.md`.

## CompanionProgress

- totalCompletedSleepMillis
- completedSessions
- familiarity
- lastMilestone?
- discoveredBehaviorIds
- schemaVersion

## NightEvent

- id
- sessionId
- type
- occurredAtEpochMillis
- rarity
- payloadVersion

## DreamDiscovery

- dreamId
- sessionId
- discoveredAtEpochMillis

## Preferences

- ambientSoundId?
- ambientVolume
- stopTimerMinutes?
- microphoneAnalysisEnabled
- healthIntegrationEnabled

## Migration policy

Every persisted aggregate has a schema version. Migrations must be forward-only and covered by fixture tests before public release.
