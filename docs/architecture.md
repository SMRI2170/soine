# Architecture

## 1. Product boundary

Soine is a bedtime companion first and a sleep-analysis product second.

The critical path is:

```
Prepare -> Settling -> Sleeping -> Waking -> Morning summary
```

A failure in 3D, health integration, or sound analysis must never lose the active sleep session.

## 2. Modules

Keep the first release as one KMP application module. Do not split into many Gradle modules until build time or ownership requires it.

Logical packages:

```
app.soine
├── sleep/       session state, summary, repository contract
├── companion/   companion state and progression
├── audio/       ambient sound contract
├── health/      normalized sleep-data contract
├── storage/     local persistence contract
├── ui/          screens and presentation
└── platform/    platform-specific implementations
```

## 3. Source-set responsibility

### commonMain
Owns:
- state machines
- use cases
- repository interfaces
- normalized data models
- Compose UI that does not require native views
- business rules and tests

Must not know:
- Filament / RealityKit implementation details
- Health Connect / HealthKit types
- Android Service or iOS AVAudioSession types

### androidMain
Owns:
- Android lifecycle integration
- foreground/background audio implementation
- Health Connect adapter
- microphone implementation
- Android 3D renderer bridge

### iosMain
Owns:
- iOS lifecycle integration
- AVAudioSession integration
- HealthKit adapter
- microphone implementation
- iOS 3D renderer bridge

## 4. Sleep state machine

```
IDLE
  -> PREPARING
  -> SLEEPING
  -> WAKING
  -> COMPLETED
```

Rules:
- Persist a session before entering SLEEPING.
- There can be only one active session.
- Starting twice returns the existing active session.
- Ending is idempotent.
- Wall-clock timestamps are persisted; UI elapsed time may use a monotonic clock while the app is alive.
- Interrupted sessions are recoverable after process death/reboot.
- Sensor analysis enriches a session; it does not own session lifetime.

### Repository contract

`SleepSessionRepository` lives in `commonMain` and is the only persistence boundary used by sleep-domain use cases.

It supports:
- retrieving the single active session
- inserting/updating a session
- idempotently completing a session by ID
- listing completed sessions newest-first

The contract uses only common Kotlin/domain types. Database handles, Android/iOS storage types and SDK-specific objects stay inside platform adapters.

## 5. Persistence

Local-first. No account/backend is required for MVP.

Minimum records:

### SleepSessionRecord
- id
- startedAt
- endedAt?
- status
- source = manual | recovered
- createdAt
- updatedAt
- schemaVersion

### SleepSummaryRecord
- sessionId
- durationMillis
- estimatedSleepStart?
- estimatedWakeTime?
- confidence?
- schemaVersion

### CompanionProgress
- totalSleepMillis
- unlockedMilestones
- lastInteractionAt?

Store only derived sound events by default. Raw microphone audio is not part of the default data model.

## 6. Audio

Ambient audio is independent from the 3D render loop.

```
AmbientAudioController
- play(sound)
- pause()
- stop()
- setVolume()
- setStopAt()
```

The sleep session must remain active if audio fails. Audio must remain functional when the screen is off according to each OS's supported background mode.

## 7. 3D companion boundary

commonMain sends semantic intent only:

```
CompanionPose:
AWAKE
SETTLING
SLEEPING
ROLLING
YAWNING
WAKING
```

Platform renderer maps poses to asset animation clips.

Renderer requirements:
- fixed bedside camera
- GLB/glTF source asset
- idle/sleep/breathing/roll/yawn/wake
- tap hit testing
- lifecycle pause
- reduced or zero rendering while screen is off
- no physics required for MVP

Do not couple sleep timing to animation completion.

## 8. Health and sensor data

Normalize external data before it enters domain logic.

```
SleepSignal
- interval
- type
- source
- confidence?
```

Sources may include Health Connect, HealthKit, microphone-derived events, and motion experiments.

Never label phone-only inference as medically accurate sleep staging.

## 9. Failure behavior

- Process killed: recover active session from persistence.
- 3D crash/failure: replace scene with static fallback; session continues.
- Audio interruption: preserve session and expose audio state separately.
- Health permission denied: manual sleep experience remains fully usable.
- Microphone permission denied: omit sound events without degrading the core flow.
- Clock changes: never allow negative duration.

## 10. Testing

commonMain:
- state transitions
- duplicate start/end
- recovery
- duration calculations
- progression thresholds

Platform:
- background audio
- process death recovery
- permission denial
- renderer lifecycle
- Health adapter mapping

Physical-device gates:
- overnight battery consumption
- thermal behavior
- memory
- audio interruptions
- Android OEM process behavior
- iOS background behavior
