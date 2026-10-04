# Soine

A calm sleep companion app built with Kotlin Multiplatform and Compose Multiplatform.

## Product direction

Soine combines sleep sessions, ambient sound, lightweight sleep insights, and a small 3D companion that sleeps beside the user.

## MVP

- Android + iOS from a shared KMP codebase
- Start/end a sleep session
- Companion state machine: awake, settling, sleeping, waking
- Bedtime screen and morning summary
- Architecture prepared for native 3D rendering and Health Connect / HealthKit

## Architecture

```
composeApp
  commonMain   shared UI + domain logic
  androidMain  Android integrations
  iosMain      iOS integrations
iosApp         iOS entry point
docs           product/architecture decisions
```

3D is intentionally behind a `CompanionScene` boundary. The MVP starts with a lightweight placeholder; native renderers can be integrated without coupling sleep-domain logic to a rendering engine.

## Next milestones

1. Bootstrap and run on Android/iOS
2. Replace placeholder companion with GLB/animation renderer
3. Ambient audio and timer
4. Persistent sleep history
5. Health Connect / HealthKit adapters
6. Microphone-based sound events (explicit permission)

See `docs/architecture.md`.
