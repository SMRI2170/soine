# Night Event System

## Purpose

Turn an overnight session into a remembered shared night without requiring the app to render continuously.

## Principle

Events are generated from a deterministic seed after the session, or scheduled as lightweight semantic records. 3D rendering is not required while the screen is off.

## Model

```kotlin
NightEvent(
  id,
  type,
  occurredAt,
  rarity,
  trigger,
  payloadVersion
)
```

Initial types:
- TURN_OVER
- EAR_TWITCH
- MOVE_CLOSER
- CURL_UP
- BRIEF_WAKE
- FUNNY_POSE
- DREAM
- SOUND_REACTION

## Inputs

MVP-safe inputs:
- session duration
- local clock window
- relationship stage
- selected ambient sound
- previous event history

Later optional inputs:
- derived sound event
- Health sleep interval
- weather/season

## Selection

Use weighted selection with constraints:
- cap meaningful events per night
- avoid repeating the same event on consecutive nights
- unlock event pools gradually
- rare events must not depend on sleeping excessively
- event history prevents novelty from collapsing too quickly

## Reproducibility

Persist seed/version or final selected events. The morning story must not change when the screen is reopened.

## Presentation

Morning timeline should contain at most a few interesting items. Most micro-events remain invisible and only influence pose/dialogue.

The system should create "what happened last night?" curiosity, not a noisy activity log.
