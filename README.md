# Soine

A Kotlin Multiplatform sleep companion where the night itself becomes time spent with a small creature.

## Product

Soine is not designed around a sleep score. The core experience is:

```
meet at night -> sleep together in the real world -> discover the shared night in the morning -> relationship changes -> return tonight
```

The companion notices routines, develops new sleeping behavior, experiences lightweight night events, and can reveal collectible dreams. Sleep and health data support that experience rather than replacing it.

## Principles

- never lose an active sleep session
- bedtime must work without login, Health, microphone, or cloud
- local-first and privacy-minimizing by default
- 3D failure must never break sleep recording
- emotion -> memory -> data in the morning
- no guilt streaks or rewards for excessive sleep

## Architecture

Kotlin Multiplatform owns domain logic and Compose Multiplatform owns the initial shared UI. Native adapters own OS-specific audio, health, microphone, lifecycle, and 3D integration.

```
composeApp
  commonMain
    sleep
    companion
    night
    relationship
    dream
  androidMain
  iosMain
iosApp
docs
```

The 3D renderer receives semantic intents such as `SETTLE`, `SLEEP`, `BREATHE`, `ROLL_OVER`, and `WAKE`. It does not own sleep state.

## Current scope

Implemented foundation:
- KMP / Compose shell
- Android/iOS entry points
- sleep session and summary domain
- companion semantic states/intents
- relationship domain skeleton
- night-event and dream models
- product/architecture/experience specifications

Next delivery order:
1. reliable local session persistence and recovery
2. complete bedtime/wake/morning vertical slice
3. ambient audio + timer
4. 3D companion physical-device PoC
5. night memory / relationship / dream engine
6. optional Health and microphone enrichment

## Documentation

Start with [docs/README.md](docs/README.md). It links the MVP, experience, architecture, data model, companion, privacy, analytics and testing specifications.

## Character

Working name: **ねむ**. An original, small, species-ambiguous cream-colored creature designed to feel calm beside the user every night.

## Status

Pre-MVP. The repository intentionally prioritizes a reliable overnight lifecycle before advanced sleep inference.
