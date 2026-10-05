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

## SleepSummaryRecord

- sessionId
- durationMillis
- estimatedSleepStartEpochMillis?
- estimatedWakeEpochMillis?
- confidence?
- schemaVersion

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
