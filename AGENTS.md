# AGENTS.md

Instructions for coding agents working on Soine.

## Product invariant

Soine is a bedtime companion, not a game engine with a sleep feature attached.

The core experience is:
```
meet at night
→ sleep together
→ survive the real overnight lifecycle
→ wake together
→ discover what happened last night
→ relationship changes subtly
```

## Non-negotiable engineering rules

1. Never lose an active sleep session.
2. A 3D/audio/Health/microphone failure must not invalidate the session.
3. `commonMain` owns business rules and semantic state, not platform SDK types.
4. Android/iOS adapters own OS-specific APIs.
5. Persist critical state before transitioning into long-running sleep state.
6. Start/end operations must be retry-safe and idempotent where specified.
7. Local-first is the default.
8. Do not add mandatory login, cloud, Health or microphone permissions to the sleep-start path.
9. Do not infer medical sleep stages from unsupported phone-only signals.
10. Do not reward unhealthy or artificially extended sleep.

## Before implementing an issue

Read:
- the issue body
- its parent Epic
- `docs/README.md`
- `docs/architecture.md`
- the relevant subsystem document

Check `docs/issues.md` for dependency order.

## Definition of done

- requested behavior implemented
- common domain logic tested
- Android/iOS effect considered
- failure path defined
- process/lifecycle implications considered
- privacy/battery impact considered when relevant
- accessibility considered for UI work
- docs updated if contract changes
- no unrelated refactor mixed into the PR

## Architecture boundaries

### commonMain
Allowed:
- domain models
- use cases
- repository contracts
- deterministic engines
- shared Compose UI where practical

Avoid:
- Android/iOS SDK types
- renderer implementation types
- direct Health SDK models
- direct microphone/audio-session APIs

### platform source sets
Own:
- lifecycle
- storage implementation
- audio implementation
- Health adapters
- microphone implementation
- 3D renderer integration

## 3D rule

The renderer consumes semantic intents such as:
`LOOK_AT_USER`, `MOVE_CLOSER`, `SETTLE`, `SLEEP`, `BREATHE`, `ROLL_OVER`, `WAKE`.

Sleep timing must never depend on an animation completing.

## Content rule

Companion writing is short, quiet and observant. Avoid guilt, diagnosis, exaggerated praise and manipulative streak mechanics.

## PR rule

Reference the issue in the PR body and explain:
- behavior changed
- test evidence
- platform impact
- failure behavior
- screenshots/video for visible UI/3D changes when useful
