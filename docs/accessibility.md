# Accessibility baseline

Soine's primary Bedtime → Sleeping → Morning flow follows a shared accessibility baseline.

## Screen reader

- the companion static presentation exposes authored content descriptions
- the primary sleep action is announced as starting sleep
- the wake action is announced as opening the morning record
- the morning completion action has an explicit description
- Night Memory and Dream Album rows merge their child semantics into concise descriptions

Visible button labels remain the source of truth for secondary controls.

## Text scaling and layout

Compose typography follows the platform font scale. The main Bedtime, Sleeping and Morning content areas are vertically scrollable so larger text does not remove access to the primary action.

Timer presets use a vertical layout instead of a fixed horizontal row to remain usable at larger font scales.

## Touch targets

Interactive controls in the core sleep flow use at least a 48 dp minimum target. Primary sleep/wake/completion actions retain a 56 dp minimum height.

## Contrast

The core flow uses MaterialTheme surface/content colors and does not reduce text opacity for essential information. No meaning is conveyed by color alone.

## Reduce motion

Platform hosts provide the system reduce-motion preference through AccessibilityPreferences:

- Android reads the system animator duration scale
- iOS reads UIAccessibility reduce-motion
- the shared CompanionRenderRequest receives reduceMotion
- relationship-distance transitions become zero-duration when reduce motion is enabled
- the static companion artwork remains a renderer-independent fallback, including when future 3D rendering is unavailable or motion should be minimized

The accessibility preference is optional at the shared boundary and defaults to normal motion for tests or hosts that do not provide a platform adapter.
