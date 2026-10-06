# Companion sleeping distance

Relationship progression is communicated visually through how close the companion sleeps, rather than by exposing XP or a level number.

## Semantic bed coordinate

The shared renderer contract uses `bedOffsetFraction` rather than SceneKit/Filament/world-space coordinates.

- `0.0`: directly beside the user
- `1.0`: far edge of the companion's usable bed area

The fixed platform camera maps this normalized bed coordinate into its own world or screen coordinate. Relationship policy therefore stays identical between Android 3D, iOS 3D and the static 2D fallback.

| Relationship stage | Distance | bedOffsetFraction |
| --- | --- | ---: |
| NEW | FAR | 0.78 |
| WARMING_UP | NEAR | 0.60 |
| FAMILIAR | CLOSER | 0.38 |
| CLOSE | BESIDE | 0.18 |

## Transition

Normal distance changes use a 700 ms transition. The renderer decides the exact easing curve but must end at the requested normalized placement.

When Reduce Motion is enabled, `transitionDurationMillis` is 0. The final relationship distance remains the same; only motion is removed.

## Fallback

`CompanionStaticFallback.forRequest()` carries the exact same `CompanionSleepingPlacement` as the 3D request. A 2D implementation can map the normalized offset to layout alignment or padding without changing relationship logic.
