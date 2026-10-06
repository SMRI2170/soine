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


## Initial content catalog

The initial authored catalog contains 20 NightEvent definitions across every current NightEventType.

Each definition owns presentation and generation metadata together:

- stable content id and NightEventType
- weighted selection value and minimum familiarity eligibility
- COMMON / UNCOMMON / RARE band
- one short observational Japanese morning line
- semantic animation intent for later 2D/3D renderers
- candidate-specific cooldown nights
- payload version and content version

The catalog converts definitions into WeightedNightEventCandidate values for the deterministic engine. Generated event ids keep the candidate id after the session prefix, so morning presentation can resolve the exact authored line and animation intent without guessing from event type alone.

Cooldown is candidate-specific. Existing rare candidates still default to three nights, while authored uncommon events may opt into shorter cooldowns. Pool-shortage fallback remains available so cooldown does not produce a broken empty story when no alternative exists.
